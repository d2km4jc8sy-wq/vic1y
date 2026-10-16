package com.example.s1client.data.store

import android.content.Context
import com.example.s1client.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.data.session.SessionRepository
import org.jsoup.Jsoup

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
            // 诊断：签到是低频写操作，把原始响应留档，便于区分「服务端拒绝」与「本地解析不了」
            android.util.Log.d(
                "Attendance",
                "sign raw(formhash=${fh.take(6)}…): ${body.take(1200)}"
            )
            val parsed = parseResponse(body)
            android.util.Log.d("Attendance", "sign parsed => ${parsed.outcome}: ${parsed.message}")
            // 「已签到」也要落本地：服务端都说了今天签过，本地不记的话每次进「我的」页
            // 又会退回「去签到」，状态自相矛盾（真机验收时踩到）。
            // recordSign 自带「今天已记过就 return」，所以重复点不会重复累加。
            if (parsed.outcome == Outcome.SIGNED_NOW || parsed.outcome == Outcome.ALREADY_SIGNED) {
                recordSign(ctx)
            }
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
        if (!fromForumIndex.isNullOrBlank()) {
            // 回写全局缓存：收藏/评分/发帖等其它写操作也复用同一份，避免各拿各的
            SessionRepository.refreshFormhash(fromForumIndex)
            return fromForumIndex
        }

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

        // 分支 3：Discuz `showmessage` 页面。
        // 签到插件并不总走 succeedhandle_/errorhandle_：某些情况下直接回一张
        // showmessage 页（inajax 时被包成 <?xml?><root><![CDATA[<h3 class="flb">…]]></root>）。
        // 之前缺这一支，于是解析落到 UNKNOWN → 把整段 XML 截 120 字当错误文案弹给用户，
        // 表现就是「点签到后弹一坨乱码」。
        showMessageText(body)?.let { msg ->
            val error = body.contains("alert_error", true) ||
                    msg.contains("抱歉") || msg.contains("失败") ||
                    msg.contains("不符") || msg.contains("不正确")
            return when {
                looksAlreadySigned(msg) -> Result(Outcome.ALREADY_SIGNED, msg.ifBlank { "今日已签到" })
                error -> Result(Outcome.FAILED, msg.ifBlank { "签到失败" })
                else -> Result(Outcome.SIGNED_NOW, msg.ifBlank { "签到成功" })
            }
        }

        // 分支 4：整段响应里就写着「已签到」——按已签到处理，不要误报失败。
        if (body.contains("已签到")) {
            return Result(Outcome.ALREADY_SIGNED, "今日已签到")
        }

        return Result(Outcome.UNKNOWN, body.take(120))
    }

    /**
     * 从 Discuz `showmessage` 页面里取出给人看的提示文本。
     *
     * 判定口径与项目其它写操作（发帖 PostEditorActivity、搜索 SearchManager、
     * 评分 RatingParser）保持一致：优先 `#messagetext`，再退 `.alert_right` / `.alert_error`
     * / `.showmessage`。取不到任何锚点则返回 null，交给调用方的其它分支处理。
     */
    private fun showMessageText(body: String): String? {
        val hasAnchor = body.contains("messagetext", true) ||
                body.contains("alert_right", true) ||
                body.contains("alert_error", true) ||
                body.contains("showmessage", true)
        if (!hasAnchor) return null
        return try {
            // 必须先剥 XML 外壳与 CDATA：jsoup 在 HTML 语境下会把整段 <![CDATA[…]]>
            // 当成一个**文本节点**，不剥的话里面的 .alert_error 根本选不中，
            // 只能靠兜底拿到一整段 HTML 源码当"消息"（真机实测踩过）。
            val doc = Jsoup.parse(unwrapCdata(body))
            doc.select("script, style").remove()
            val text = doc.selectFirst("#messagetext")?.text()
                ?: doc.selectFirst(".alert_right")?.text()
                ?: doc.selectFirst(".alert_error")?.text()
                ?: doc.selectFirst(".showmessage")?.text()
                // showmessage 正文容器：<div class="c"><p>…</p></div>
                ?: doc.selectFirst("div.c p")?.text()
                ?: doc.selectFirst("div.c")?.text()
                // 最后兜底：整页纯文本（下面 cleanMessage 会剃掉标题与按钮词）
                ?: doc.body()?.text()
            cleanMessage(text.orEmpty()).takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            android.util.Log.w("Attendance", "showMessageText failed: ${e.message}")
            null
        }
    }

    /**
     * 剥掉 inajax 的 `<?xml …?><root><![CDATA[ … ]]></root>` 外壳，只留里面的 HTML 片段。
     * 没有 CDATA 时原样返回（普通 HTML 响应也走这里）。
     */
    private fun unwrapCdata(body: String): String {
        val s = body.indexOf("<![CDATA[")
        if (s < 0) return body
        val e = body.indexOf("]]>", s + 9)
        return if (e < 0) body.substring(s + 9) else body.substring(s + 9, e)
    }

    /** 剃掉 showmessage 页面混进来的标题/按钮噪声，只留正文 */
    private fun cleanMessage(raw: String): String {
        var t = raw
        for (noise in listOf(
            "提示信息", "关闭", "返回", "确定", "点击这里返回上一页", "javascript:;"
        )) {
            t = t.replace(noise, "")
        }
        return t.trim()
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
