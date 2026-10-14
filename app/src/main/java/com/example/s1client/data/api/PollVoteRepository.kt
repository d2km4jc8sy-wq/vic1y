package com.example.s1client.data.api

import com.example.s1client.data.model.ThreadPoll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject

/**
 * 投票提交 + 本地投票缓存。
 *
 * **URL 与响应判定照搬开源 s1er 的 `votePoll` / `parsePollVoteResponse`**
 * （api_service.dart:1447-1520），不自己猜 Discuz 的投票协议：
 *   POST forum.php?mod=misc&action=votepoll&inajax=1&tid={tid}
 *   body: tid={tid}&pollsubmit=yes&pollanswers[]={id}&pollanswers[]={id}
 *
 * Discuz 的投票成功/失败都返回一段 JS 回调（succeedhandle_pollvote / errorhandle_pollvote），
 * 所以要按内容关键字判定，不能只看 HTTP 状态码。
 */
object PollVoteRepository {

    /** tid:uid -> 我投的选项 id 列表。**API 不返回这个字段，只能本地记** */
    private val cache = HashMap<String, List<String>>()

    /**
     * @return null 表示成功；非 null 是错误信息（已适配成中文可读文案）
     */
    suspend fun votePoll(tid: String, optionIds: List<String>): String? =
        withContext(Dispatchers.IO) {
            if (optionIds.isEmpty()) return@withContext "请至少选择一个选项"
            val body = FormBody.Builder()
                .add("tid", tid)
                .add("pollsubmit", "yes")
                .apply { optionIds.forEach { add("pollanswers[]", it) } }
                .build()
            val req = Request.Builder()
                .url("https://stage1st.com/2b/forum.php?mod=misc&action=votepoll&inajax=1&tid=$tid")
                .header("Referer", "https://stage1st.com/2b/forum.php?mod=viewthread&tid=$tid")
                .post(body)
                .build()
            try {
                RetrofitClient.httpClient.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    val err = parsePollVoteResponse(text)
                    if (err == null) remember(tid, optionIds)
                    err
                }
            } catch (e: Exception) {
                "投票失败：${e.message ?: "网络异常"}"
            }
        }

    /** 与 s1er 同名函数同逻辑：成功返回 null，失败返回错误文案 */
    internal fun parsePollVoteResponse(body: String): String? {
        if (body.contains("thread_poll_succeed") ||
            body.contains("pollvote_succeed") ||
            body.contains("投票成功") ||
            (body.contains("window.location.href") && body.contains("viewthread"))
        ) return null

        // succeedhandle_pollvote('tid', '...') 出现即成功
        if (Regex("""succeedhandle_pollvote\('[^']*'""").containsMatchIn(body)) return null

        Regex("""errorhandle_pollvote\('([^']*)',\s*'([^']*)'\)""").find(body)?.let { m ->
            val g1 = m.groupValues[1]
            val g2 = m.groupValues[2]
            return g1.ifBlank { g2 }.ifBlank { "投票失败" }
        }
        Regex("""showMessage\('([^']*)'""").find(body)?.let {
            return it.groupValues[1]
        }
        if (body.isBlank()) return "投票请求无响应，请检查是否已登录或已投过票"
        return "服务器返回未知响应"
    }

    /** 记住本地选择（key 带 uid，多账号不串） */
    fun remember(tid: String, optionIds: List<String>) {
        val uid = com.example.s1client.data.cache.ForumDataCache.memberUid
        if (tid.isBlank() || uid.isBlank() || optionIds.isEmpty()) return
        cache["$uid:$tid"] = optionIds
    }

    fun remembered(tid: String): List<String> {
        val uid = com.example.s1client.data.cache.ForumDataCache.memberUid
        if (tid.isBlank() || uid.isBlank()) return emptyList()
        return cache["$uid:$tid"] ?: emptyList()
    }

    fun forget(tid: String) {
        val uid = com.example.s1client.data.cache.ForumDataCache.memberUid
        cache.remove("$uid:$tid")
    }

    /** 退出登录时清掉，避免下一个账号看到上一个人的选择 */
    fun clearAll() {
        cache.clear()
    }
}

/** 方便 JSON 解析调用：ThreadPoll.fromJson 的薄封装 */
fun parseThreadPoll(json: JSONObject): ThreadPoll = ThreadPoll.fromJson(json)
