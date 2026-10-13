package com.example.s1client.util

import android.content.Context
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.model.ForumDataCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 每日签到。
 *
 * 基准实现：开源 s1er `lib/services/forum_tools_service.dart` 的 `dailySign()` +
 * `parseAttendanceResponse()`，接口与判定口径完全对齐，不自行发明规则。
 *
 * 注意：这是**写操作**（GET 但会改服务端状态），所以
 * ① 只在用户明确点击时调一次；② 失败绝不自动重试，避免重复提交。
 */
object AttendanceManager {

    private const val PREF = "s1_attendance"
    private const val KEY_LAST_DATE = "last_sign_date"
    private const val KEY_STREAK = "streak"
    private const val KEY_TOTAL = "total"

    enum class Outcome {
        /** 这次真的签上了 */
        SIGNED_NOW,
        /** 今天已经签过了 */
        ALREADY_SIGNED,
        /** 请求失败 */
        FAILED,
        /** 响应看不懂 */
        UNKNOWN
    }

    data class Result(val outcome: Outcome, val message: String)

    // ===== 本地记录（连续天数只存在本地，够用且不额外请求）=====

    /** 今天的日期键，如 "2026-10-13"（本地时区） */
    private fun todayKey(now: Long = System.currentTimeMillis()): String {
        val c = java.util.Calendar.getInstance()
        c.timeInMillis = now
        return String.format(
            "%04d-%02d-%02d",
            c.get(java.util.Calendar.YEAR),
            c.get(java.util.Calendar.MONTH) + 1,
            c.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    /** 昨天（用于判断"连续"是否中断） */
    private fun yesterdayKey(now: Long = System.currentTimeMillis()): String =
        todayKey(now - 24L * 60 * 60 * 1000)

    /** 今天是否已签（本地记录，不发请求） */
    fun isSignedToday(ctx: Context, now: Long = System.currentTimeMillis()): Boolean =
        prefs(ctx).getString(KEY_LAST_DATE, "") == todayKey(now)

    /** 当前连续签到天数 */
    fun streak(ctx: Context): Int = prefs(ctx).getInt(KEY_STREAK, 0)

    /** 累计签到天数 */
    fun total(ctx: Context): Int = prefs(ctx).getInt(KEY_TOTAL, 0)

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun recordSign(ctx: Context, now: Long = System.currentTimeMillis()) {
        val p = prefs(ctx)
        val today = todayKey(now)
        val last = p.getString(KEY_LAST_DATE, "")
        if (last == today) return
        val cont = if (last == yesterdayKey(now)) p.getInt(KEY_STREAK, 0) + 1 else 1
        p.edit()
            .putString(KEY_LAST_DATE, today)
            .putInt(KEY_STREAK, cont)
            .putInt(KEY_TOTAL, p.getInt(KEY_TOTAL, 0) + 1)
            .apply()
    }

    // ===== 签到 =====

    /** 执行签到。未登录直接失败，不发请求。 */
    suspend fun sign(ctx: Context): Result {
        if (!ForumDataCache.isLoggedIn()) {
            return Result(Outcome.FAILED, "请先登录")
        }
        val fh = fetchFormhash()
        if (fh.isBlank()) return Result(Outcome.FAILED, "获取 formhash 失败，请刷新后重试")

        return try {
            val body = withContext(Dispatchers.IO) {
                RetrofitClient.api.dailyAttendance(formhash = fh).string()
            }
            val parsed = parseResponse(body)
            if (parsed.outcome == Outcome.SIGNED_NOW) recordSign(ctx)
            parsed
        } catch (e: Exception) {
            android.util.Log.w("Attendance", "sign failed: ${e.message}")
            Result(Outcome.FAILED, e.message ?: "签到失败")
        }
    }

    /**
     * 取 formhash，**三级降级**，口径抄 s1er `ensureFormhash()`：
     * forumindex → login → 缓存。
     *
     * 原来这里用的是 `getThreadDetail(tid = "1")` —— tid=1 是无效帖子，
     * 大概率取不到 formhash，于是签到请求带着空/错 token 发出去，服务端直接拒绝，
     * 表现为「在 s1er 能签到、在这个 App 签到失败」。
     */
    private suspend fun fetchFormhash(): String {
        val fromForumIndex = try {
            withContext(Dispatchers.IO) {
                RetrofitClient.api.getForumIndex().variables?.formhash
            }
        } catch (e: Exception) {
            null
        }
        if (!fromForumIndex.isNullOrBlank()) return fromForumIndex

        // 缓存兜底：登录/浏览过程中已经填过就复用
        return ForumDataCache.formhash
    }

    /**
     * 判断一句话是不是"今天已经签过了"。
     *
     * 服务端措辞不固定：见过「已签到」「今日已签到」「今天已经签到过了」「重复签到」，
     * 所以不能只 match 一种说法，否则会把"已签到"误报成失败。
     */
    private fun looksAlreadySigned(msg: String): Boolean {
        if (msg.isBlank()) return false
        return msg.contains("已签到") ||
                msg.contains("已签过") ||
                msg.contains("已经签到") ||
                msg.contains("重复签到") ||
                msg.contains("今日已") ||
                msg.contains("今天已")
    }

    /**
     * 解析 Discuz 签到响应。口径对齐 s1er 的 `parseAttendanceResponse`：
     * 先剥一层 ajax 外壳，再依次找 `succeedhandle_` / `errorhandle_`。
     */
    fun parseResponse(rawBody: String): Result {
        val body = rawBody.trim()
        if (body.isEmpty()) return Result(Outcome.UNKNOWN, "无数据")

        succeedMessage(body)?.let { msg ->
            return if (looksAlreadySigned(msg)) {
                Result(Outcome.ALREADY_SIGNED, msg.ifBlank { "今日已签到" })
            } else {
                Result(Outcome.SIGNED_NOW, msg.ifBlank { "签到成功" })
            }
        }
        errorMessage(body)?.let { msg ->
            return if (looksAlreadySigned(msg)) {
                Result(Outcome.ALREADY_SIGNED, msg.ifBlank { "今日已签到" })
            } else {
                Result(Outcome.FAILED, msg)
            }
        }
        return Result(Outcome.UNKNOWN, body.take(120))
    }

    /** Discuz 会把消息包在 `succeedhandle_k_attendance(['<b>...</b>',1])` 这类 JS 里 */
    private fun succeedMessage(html: String): String? =
        handlerMessage(html, "succeedhandle_")

    private fun errorMessage(html: String): String? =
        handlerMessage(html, "errorhandle_")

    /**
     * 取出 `xxxhandle_([...])` 里的**第一个**参数（那句给人看的提示语）。
     * 第二个参数是状态码（1/0），不是文案。
     */
    private fun handlerMessage(html: String, prefix: String): String? {
        val p = html.indexOf(prefix)
        if (p < 0) return null
        // 从 prefix 后第一个 '[' 开始收集顶层参数（跳过嵌套的 [ ]）
        val start = html.indexOf('[', p + prefix.length)
        if (start < 0) return null
        var depth = 0
        val sb = StringBuilder()
        var i = start
        while (i < html.length) {
            val c = html[i]
            when (c) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) break
                }
            }
            if (depth >= 1 && c != '[') sb.append(c)
            i++
        }
        val args = splitTopLevel(sb.toString())
        val raw = args.getOrNull(0) ?: return null
        val text = raw.trim().trim('\'', '"')
            .replace(Regex("<[^>]*>"), "")   // 去掉 <b> 之类标签
            .trim()
        // 允许空文案：succeedhandle_x(['',1]) 表示"成功了但没给提示语"，
        // 调用方会用默认文案兜底，所以这里返回空串而不是 null。
        return text
    }

    /** 按逗号切分顶层参数，跳过引号内与嵌套括号里的逗号 */
    private fun splitTopLevel(s: String): List<String> {
        val out = mutableListOf<String>()
        var depth = 0
        var quote: Char? = null
        val sb = StringBuilder()
        for (c in s) {
            when {
                quote != null -> {
                    if (c == quote) quote = null
                    sb.append(c)
                }
                c == '\'' || c == '"' -> { quote = c; sb.append(c) }
                c == '(' || c == '[' || c == '{' -> { depth++; sb.append(c) }
                c == ')' || c == ']' || c == '}' -> { depth--; sb.append(c) }
                c == ',' && depth == 0 -> { out.add(sb.toString()); sb.clear() }
                else -> sb.append(c)
            }
        }
        if (sb.isNotEmpty()) out.add(sb.toString())
        return out
    }
}
