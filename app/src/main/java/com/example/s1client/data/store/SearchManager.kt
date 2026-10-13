package com.example.s1client.data.store

import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.model.Thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.util.HtmlParser

/**
 * 站内搜索。
 *
 * Discuz 的搜索是两步：先 POST search.php?mod=forum 提交关键词，
 * 服务端 302 到一个 searchid 结果页，再 GET 那一页才是结果。
 * OkHttp 默认跟随重定向并把 POST 转成 GET，所以一次调用即可拿到结果页 HTML，
 * 再交给 [HtmlParser.parseSearchResult] 解析。
 */
object SearchManager {

    /** results 非空表示搜到；否则 message 说明原因（未找到 / 风控 / 失败） */
    data class Outcome(
        val results: List<Thread> = emptyList(),
        val message: String? = null
    )

    suspend fun search(keyword: String): Outcome {
        val kw = keyword.trim()
        if (kw.isEmpty()) return Outcome(message = "请输入关键词")

        val fh = fetchFormhash()
        if (fh.isBlank()) return Outcome(message = "获取 formhash 失败，请先登录")

        return withContext(Dispatchers.IO) {
            try {
                val form = okhttp3.FormBody.Builder()
                    .add("formhash", fh)
                    .add("srchtxt", kw)
                    .add("searchsubmit", "yes")
                    .add("srchfid[]", "all")
                    .build()
                val req = okhttp3.Request.Builder()
                    .url("https://stage1st.com/2b/search.php?mod=forum")
                    .post(form)
                    .header("Referer", "https://stage1st.com/2b/")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                // Response 必须关闭，否则每次搜索泄漏一个连接
                val html = RetrofitClient.httpClient.newCall(req).execute().use { resp ->
                    resp.body?.string().orEmpty()
                }
                val list = HtmlParser.parseSearchResult(html)
                if (list.isNotEmpty()) Outcome(results = list)
                else Outcome(message = extractMessage(html) ?: "没有找到相关帖子")
            } catch (e: Exception) {
                Outcome(message = "搜索失败：" + (e.message ?: ""))
            }
        }
    }

    private suspend fun fetchFormhash(): String {
        var fh = ForumDataCache.formhash
        if (fh.isNotBlank()) return fh
        fh = try {
            withContext(Dispatchers.IO) {
                RetrofitClient.api.getForumIndex().variables?.formhash
            }
        } catch (e: Exception) {
            null
        } ?: ""
        if (fh.isNotBlank()) ForumDataCache.formhash = fh
        return fh
    }

    /** 从 Discuz 的提示框里取一句人话，比如「抱歉，您在 10 秒内只能进行一次搜索」 */
    private fun extractMessage(html: String): String? {
        if (html.isBlank()) return null
        return try {
            val doc = Jsoup.parse(html)
            val t = doc.selectFirst("#messagetext")?.text()?.trim()
                ?: doc.selectFirst(".alert_error")?.text()?.trim()
                ?: doc.selectFirst(".alert_info")?.text()?.trim()
            t?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}
