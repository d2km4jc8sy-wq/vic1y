package com.example.s1client.util

import com.example.s1client.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * 楼层定位 —— **思路与算法照搬开源 s1er 的 `locatePostPage(tid, pid)`**
 * （api_service.dart:2353），不重复造轮子。
 *
 * 背景：Discuz 的回复提醒只给出 `pid`（楼层 id），而帖子详情是按**页**加载的。
 * 想知道 pid 在第几页，官方做法是请求
 *   `forum.php?mod=redirect&goto=findpost&ptid={tid}&pid={pid}`
 * 它会 302 跳到 `thread-{tid}-{page}-1.html#pid{pid}`，
 * 我们只要解析 Location 头里的 page 就行 —— **必须不跟随重定向**，
 * 否则 OkHttp 直接拿到最终响应、Location 就丢了。
 *
 * s1er 另做了三级兜底（Location → realUri → HTML 的 div.pg）与进程内缓存，
 * 这里保留**页号缓存**与**Location 解析**，其余两级兜底按需再加。
 */
object PostLocator {

    /** tid:pid -> page，避免同一批提醒里反复请求 */
    private val cache = HashMap<String, Int>()

    /**
     * @return 楼层所在页（1 基）；解析不出来返回 null，调用方回退到第 1 页
     */
    suspend fun locatePostPage(tid: String, pid: String): Int? = withContext(Dispatchers.IO) {
        if (tid.isBlank() || pid.isBlank()) return@withContext null
        val key = "$tid:$pid"
        cache[key]?.let { return@withContext it }

        val url = "https://stage1st.com/2b/forum.php?mod=redirect&goto=findpost&ptid=$tid&pid=$pid"
        val page = try {
            val req = Request.Builder()
                .url(url)
                .header("Referer", "https://stage1st.com/2b/forum.php?mod=viewthread&tid=$tid")
                .build()
            // 关键：不跟随 302，否则读不到 Location 头
            RetrofitClient.httpClient.newBuilder().followRedirects(false).build()
                .newCall(req).execute().use { resp ->
                    parsePageFromLocation(resp.header("Location"))
                }
        } catch (_: Exception) {
            null
        }
        if (page != null && page >= 1) cache[key] = page
        page
    }

    /**
     * 从 Location 里解析页号。见过的两种写法：
     *   `thread-2291675-3-1.html#pid70368274`
     *   `forum.php?mod=viewthread&tid=2291675&page=3`
     * 都没有 page 字段时说明在第 1 页（Discuz 单页帖不带 page）。
     */
    internal fun parsePageFromLocation(location: String?): Int? {
        if (location.isNullOrBlank()) return null
        Regex("""thread-\d+-(\d+)-1\.html""").find(location)?.let {
            return it.groupValues[1].toIntOrNull()
        }
        Regex("""[?&]page=(\d+)""").find(location)?.let {
            return it.groupValues[1].toIntOrNull()
        }
        // 跳到了帖子本体但没带页号 → 第 1 页
        return if (location.contains("viewthread") || location.contains("thread-")) 1 else null
    }
}
