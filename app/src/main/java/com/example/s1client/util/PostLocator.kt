package com.example.s1client.util

import com.example.s1client.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * 楼层定位 —— **三级兜底与页号判定逐行照搬开源 s1er 的 `locatePostPage`**
 * （api_service.dart:2353-2470 及 `pageFromFindpostLocation` / `pageFromViewthreadHtml`），
 * 不重复造轮子。
 *
 * 背景：Discuz 的回复提醒只给 `pid`，而详情是按**页**加载的。想知道 pid 在第几页，
 * 官方做法是请求 `forum.php?mod=redirect&goto=findpost&ptid={tid}&pid={pid}`。
 *
 * s1er 的三级兜底（每一级都要有，缺一级就会在真机上莫名其妙地失败）：
 *   ① `Location` 响应头（**必须 followRedirects=false**，否则 OkHttp 直接跟完、头就没了）
 *      · 页号 < 1 → 归一到 1（Discuz 首页重定向给的是 page=0）
 *      · 无 page、但确实是**同 tid** 的 viewthread / thread- 链接 → 第 1 页
 *   ② 跟随后读**最终 URL**（浏览器或中间代理已经把 301 跟掉时 Location 读不到）
 *   ③ 响应 HTML 里的 `div.pg > strong` 页号；单页帖只有空的 `div.pgt`，
 *      这时靠 `id="postlist"` / `pg_viewthread` / `id="post_` 认出是读帖页 → 1
 *
 * 与 s1er 一致的另一点：**解析不出来就返回 null，由调用方明确报错**，
 * 不静默回落成第 1 页 —— 静默回落的表现就是用户说的"点了没反应"。
 */
object PostLocator {

    /** tid:pid -> page，避免同一批提醒里反复请求 */
    private val cache = HashMap<String, Int>()

    private const val BASE = "https://stage1st.com/2b"

    /**
     * @return 楼层所在页（1 基）；**解析不出来返回 null**（调用方负责提示失败）
     */
    suspend fun locatePostPage(tid: String, pid: String): Int? = withContext(Dispatchers.IO) {
        if (tid.isBlank() || pid.isBlank()) return@withContext null
        val key = "$tid:$pid"
        cache[key]?.let { return@withContext it }

        val url = "$BASE/forum.php?mod=redirect&goto=findpost&ptid=$tid&pid=$pid"
        val req = Request.Builder()
            .url(url)
            .header("Referer", "$BASE/forum.php?mod=viewthread&tid=$tid")
            .build()

        var resolved: Int? = null

        // —— ① Location 头 ——
        try {
            RetrofitClient.httpClient.newBuilder().followRedirects(false).build()
                .newCall(req).execute().use { resp ->
                    val loc = resp.header("Location")
                    android.util.Log.d("S1Locate", "tid=$tid pid=$pid code=${resp.code} location=$loc")
                    resolved = pageFromFindpostLocation(loc, tid)
                }
        } catch (e: Exception) {
            android.util.Log.w("S1Locate", "level1 failed: ${e.javaClass.simpleName}")
        }

        // —— ② 跟随后读最终 URL ——
        if (resolved == null) {
            try {
                RetrofitClient.httpClient.newBuilder().followRedirects(true).build()
                    .newCall(req).execute().use { resp ->
                        val real = resp.request.url.toString()
                        android.util.Log.d("S1Locate", "level2 realUri=$real")
                        if (real != url) resolved = pageFromFindpostLocation(real, tid)
                    }
            } catch (e: Exception) {
                android.util.Log.w("S1Locate", "level2 failed: ${e.javaClass.simpleName}")
            }
        }

        // —— ③ HTML 的 div.pg ——
        if (resolved == null) {
            try {
                RetrofitClient.httpClient.newBuilder().followRedirects(true).build()
                    .newCall(req).execute().use { resp ->
                        val html = resp.body?.string().orEmpty()
                        resolved = pageFromViewthreadHtml(html)
                        android.util.Log.d("S1Locate", "level3 htmlPage=$resolved len=${html.length}")
                    }
            } catch (e: Exception) {
                android.util.Log.w("S1Locate", "level3 failed: ${e.javaClass.simpleName}")
            }
        }

        android.util.Log.d("S1Locate", "tid=$tid pid=$pid resolved=$resolved")
        resolved?.let { if (it >= 1) cache[key] = it }
        resolved
    }

    /**
     * 从 findpost 的 Location（或最终 URL）解析页号。
     * 判定顺序照 s1er `pageFromFindpostLocation`：
     *   1. `page=` 查询参数；
     *   2. 伪静态路径 `thread-{tid}-{page}-1.html`；
     *   3. 页号 < 1 → 1（首页重定向是 page=0）；
     *   4. 无 page 但确实是**同 tid** 的 viewthread / thread- 链接 → 1；
     *   5. 其它一律 null（**不猜**）。
     */
    internal fun pageFromFindpostLocation(location: String?, expectedTid: String? = null): Int? {
        if (location.isNullOrBlank()) return null
        val queryPage = Regex("""[?&]page=(\d+)""").find(location)
            ?.groupValues?.get(1)?.toIntOrNull()
        val pathPage = Regex("""thread-\d+-(\d+)-\d+\.html""").find(location)
            ?.groupValues?.get(1)?.toIntOrNull()
        val page = queryPage ?: pathPage
        if (page != null) return if (page < 1) 1 else page
        if (!expectedTid.isNullOrEmpty() && isViewthreadLocationForTid(location, expectedTid)) return 1
        return null
    }

    /**
     * 从已加载的 viewthread HTML 解析当前页（`div.pg > strong`）。
     * 单页帖常只有空的 `div.pgt`、没有 `div.pg`，此时若能确认是读帖页则返回 1。
     */
    internal fun pageFromViewthreadHtml(html: String): Int? {
        if (html.isEmpty()) return null
        val m = Regex(
            """<div[^>]*class="[^"]*\bpg\b[^"]*"[^>]*>[\s\S]*?<strong[^>]*>\s*(\d+)\s*</strong>""",
            RegexOption.IGNORE_CASE
        ).find(html)
        m?.groupValues?.get(1)?.toIntOrNull()?.let { return if (it < 1) 1 else it }
        val looksLikeViewthread = html.contains("id=\"postlist\"") ||
            html.contains("pg_viewthread") ||
            html.contains("id=\"post_")
        return if (looksLikeViewthread) 1 else null
    }

    private fun isViewthreadLocationForTid(location: String, tid: String): Boolean {
        if (Regex("""[?&]tid=$tid(\D|$)""").containsMatchIn(location)) return true
        return Regex("""thread-$tid-\d+-\d+\.html""").containsMatchIn(location)
    }
}
