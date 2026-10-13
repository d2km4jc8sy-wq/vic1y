package com.example.s1client.util

import org.jsoup.Jsoup

/** 一条评分记录：谁给这一层加/扣了多少分，理由是什么 */
data class RateEntry(
    val name: String,
    val amount: Int,
    val reason: String,
    val uid: String = "",
    val avatar: String? = null
)

/** 一层楼的评分聚合（来自网页版 viewthread HTML 的 ratelog 区块） */
data class RateInfo(val count: Int, val total: Int, val entries: List<RateEntry>) {
    /** 评分行展示的文案，如「2 人参与 · 战斗力 +2」 */
    val summary: String
        get() = "$count 人参与 · 战斗力 ${if (total >= 0) "+" else ""}$total"
}

/** 评分表单选项（从 Discuz 评分弹窗 HTML 解析） */
data class RateFormOptions(
    val scoreOptions: List<Int>,
    val reasonPresets: List<String>,
    val formHash: String,
    val tid: String,
    val pid: String,
    val referer: String,
    val handleKey: String,
    val minScore: Int? = null,
    val maxScore: Int? = null,
    val remainingToday: Int? = null,
    val notifyAuthorDefault: Boolean = false,
    val notifyAuthorDisabled: Boolean = false,
    val error: String? = null
) {
    val hasError: Boolean get() = !error.isNullOrBlank()

    /**
     * 生成分值选项。
     * 优先用表单解析出的 option；如果不在 [minScore, maxScore] 区间内则过滤掉。
     * 若表单没给 option，但有区间，则按区间生成（不含 0）。
     * 否则回退保守默认值 [-1, 1]。
     */
    fun buildScoreOptions(): List<Int> {
        val min = minScore
        val max = maxScore
        val range = if (min != null && max != null) min..max else null
        val fromOptions = scoreOptions.filter { it != 0 }
        val candidates = if (fromOptions.isNotEmpty()) {
            fromOptions.filter { range == null || it in range }
        } else if (range != null) {
            range.toList()
        } else {
            DEFAULT_SCORE_OPTIONS
        }
        return candidates.filter { it != 0 }.distinct().sortedByDescending { kotlin.math.abs(it) }
    }

    companion object {
        /** 保守默认分值；真实分值应从表单解析。 */
        val DEFAULT_SCORE_OPTIONS = listOf(1, -1)
        val DEFAULT_REASON_PRESETS = listOf("好评加鹅", "欢乐多", "思路广")
    }
}

object RatingParser {

    private val countRegex =
        Regex("""参与人数\s*<span\s+class="xi1">(\d+)</span>""", RegexOption.IGNORE_CASE)
    private val totalRegex =
        Regex("""战斗力\s*<i><span\s+class="xi1">([+-]?\d+)</span></i>""", RegexOption.IGNORE_CASE)
    private val uidFromHref =
        Regex("""(?:space-uid-|uid=)(\d+)""", RegexOption.IGNORE_CASE)

    /** 解析网页版 viewthread 的 ratelog 区块。
     *  优先按 mobile=2 的 <li> 结构解析（论坛大列表用此结构，能列出全部评分）；
     *  取不到时回退到桌面版 <tr> 结构。参考 s1er 的 rate_log_service 实现。 */
    fun parse(html: String?): Map<String, RateInfo> {
        if (html.isNullOrBlank()) return emptyMap()
        val result = HashMap<String, RateInfo>()
        val doc = Jsoup.parse(html)

        doc.select("[id^=ratelog_]").forEach { block ->
            val pid = block.id().removePrefix("ratelog_").trim()
            if (pid.isBlank()) return@forEach

            val entries = ArrayList<RateEntry>()
            var count = 0
            var total = 0

            val lis = block.select("li")
            if (lis.isNotEmpty()) {
                // ===== mobile=2 的 <li> 结构 =====
                for (li in lis) {
                    val txt = li.text()
                    if (txt.contains("参与人数")) {
                        // 头部：参与人数 / 战斗力
                        Regex("""(\d+)""").find(txt)?.groupValues?.get(1)?.toIntOrNull()
                            ?.let { count = it }
                        Regex("""战斗力[^\d\-+]*([+-]?\d+)""").find(txt)?.groupValues?.get(1)
                            ?.replace(" ", "")?.toIntOrNull()?.let { total = it }
                        continue
                    }
                    val divs = li.select("div")
                    if (divs.size < 2) continue
                    val link = divs[0].selectFirst("a") ?: continue
                    val name = link.text().trim()
                    if (name.isEmpty()) continue
                    val uid = link.attr("href").let { uidFromHref.find(it)?.groupValues?.get(1) }.orEmpty()
                    val avatar = link.selectFirst("img")?.attr("src")
                    val score = parseScore(divs[1].text())
                    val reason = if (divs.size >= 3) divs[2].text().trim() else ""
                    entries.add(RateEntry(name, score, reason, uid, avatar))
                }
            } else {
                // ===== 桌面版 <tr> 兜底 =====
                val blockHtml = block.outerHtml()
                count = countRegex.find(blockHtml)?.groupValues?.get(1)?.toIntOrNull()
                    ?: entries.size
                total = totalRegex.find(blockHtml)?.groupValues?.get(1)
                    ?.filter { it.isDigit() || it == '-' || it == '+' }?.toIntOrNull() ?: 0

                block.select("tr").forEach { row ->
                    val amount = row.selectFirst("td.xi1")?.text()
                        ?.replace(" ", "")?.toIntOrNull() ?: return@forEach

                    val anchors = row.select("a[href^=space-uid-]")
                    val best = anchors.firstOrNull { it.text().trim().isNotEmpty() }
                        ?: anchors.firstOrNull()
                    val uid = best?.attr("href")?.let { uidFromHref.find(it)?.groupValues?.get(1) }.orEmpty()
                    val name = best?.text()?.trim()?.let { decode(it) }?.ifEmpty { "匿名" } ?: "匿名"
                    val avatar = anchors.firstNotNullOfOrNull { it.selectFirst("img")?.attr("src") }

                    val reasonCell = row.select("td").find { cell ->
                        !cell.hasClass("xi1") && cell.selectFirst("a[href^=space-uid-]") == null
                    }
                    val reason = reasonCell?.text()?.trim()?.let { decode(it) }.orEmpty()
                    entries.add(RateEntry(name, amount, reason, uid, avatar))
                }
            }

            if (count == 0) count = entries.size
            result[pid] = RateInfo(count, total, entries)
        }
        return result
    }

    /**
     * 解析「完整评分历史」接口（forum.php?mod=misc&action=viewratings&...&mobile=2）。
     * 该接口不受 viewthread 内联 ratelog 的条数截断限制，返回某一层全部评分记录。
     * 参考 s1er 的 fetchFullRateLog / _parseRatingTable：
     *  - 优先解析 [id^=ratelog_] 内的 <li>（移动版结构，与 parse() 一致）；
     *  - 否则回退解析桌面版 <table tbody tr>（Discuz 会把 mobile=2 重定向到桌面模板）。
     */
    fun parseViewRatings(html: String?, fallbackPid: String): RateInfo? {
        if (html.isNullOrBlank()) return null
        val doc = Jsoup.parse(html)
        val entries = ArrayList<RateEntry>()
        var count = 0
        var total = 0

        val blocks = doc.select("[id^=ratelog_]")
        if (blocks.isNotEmpty()) {
            for (block in blocks) {
                val lis = block.select("li")
                for (li in lis) {
                    val txt = li.text()
                    if (txt.contains("参与人数")) {
                        Regex("""(\d+)""").find(txt)?.groupValues?.get(1)?.toIntOrNull()
                            ?.let { count = it }
                        Regex("""战斗力[^\d\-+]*([+-]?\d+)""").find(txt)?.groupValues?.get(1)
                            ?.replace(" ", "")?.toIntOrNull()?.let { total = it }
                        continue
                    }
                    val divs = li.select("div")
                    if (divs.size < 2) continue
                    val link = divs[0].selectFirst("a") ?: continue
                    val name = link.text().trim()
                    if (name.isEmpty()) continue
                    val uid = link.attr("href").let { uidFromHref.find(it)?.groupValues?.get(1) }.orEmpty()
                    val avatar = link.selectFirst("img")?.attr("src")
                    val score = parseScore(divs[1].text())
                    val reason = if (divs.size >= 3) divs[2].text().trim() else ""
                    entries.add(RateEntry(name, score, reason, uid, avatar))
                }
            }
        }

        // 回退：桌面版表格（对齐 s1er _parseRatingTable 的列序：分值 / 用户名 / 时间 / 理由）
        if (entries.isEmpty()) {
            for (row in doc.select("table tbody tr")) {
                val cells = row.select("td")
                if (cells.size < 2) continue
                val amount = run {
                    var a = parseScore(cells[0].text())
                    if (a == 0) {
                        for (k in cells.indices) {
                            if (k == 1) continue
                            val s = parseScore(cells[k].text())
                            if (s != 0) { a = s; break }
                        }
                    }
                    a
                }
                if (amount == 0) continue
                val anchors = cells[1].select("a")
                val best = anchors.firstOrNull { it.text().trim().isNotEmpty() } ?: anchors.firstOrNull()
                val uid = best?.attr("href")?.let { uidFromHref.find(it)?.groupValues?.get(1) }.orEmpty()
                val name = best?.text()?.trim()?.let { decode(it) }?.ifEmpty { "匿名" } ?: "匿名"
                val avatar = anchors.firstNotNullOfOrNull { it.selectFirst("img")?.attr("src") }
                val reasonIdx = if (cells.size >= 4) 3 else if (cells.size >= 3) 2 else -1
                val reason = if (reasonIdx >= 0) cells[reasonIdx].text().trim().let { decode(it) } else ""
                entries.add(RateEntry(name, amount, reason, uid, avatar))
            }
            count = entries.size
        }

        if (entries.isEmpty()) return null
        if (count == 0) count = entries.size
        if (total == 0) total = entries.sumOf { it.amount }
        return RateInfo(count, total, entries)
    }

    /** 从文本里取第一个带符号整数（借鉴 s1er 的 _parseScore） */
    private fun parseScore(raw: String): Int {
        val cleaned = raw.replace(" ", "")
        val m = Regex("""[+-]?\d+""").find(cleaned)
        return m?.value?.toIntOrNull() ?: 0
    }

    /** 从 Discuz 评分弹窗 HTML 解析表单选项与隐藏字段 */
    fun parseRateForm(html: String?, fallbackTid: String, fallbackPid: String): RateFormOptions {
        if (html.isNullOrBlank()) {
            return RateFormOptions.withDefaults("评分表单为空")
        }
        val error = extractMessagetextError(html)
        if (error != null) {
            return RateFormOptions.withDefaults(error)
        }

        return try {
            val doc = Jsoup.parse(html)

            // 隐藏字段
            val hiddenFields = mutableMapOf<String, String>()
            doc.select("#rateform input").forEach { input ->
                val name = input.attr("name").takeIf { it.isNotBlank() }
                    ?: input.attr("id").takeIf { it.isNotBlank() }
                val value = input.attr("value")
                if (name != null && name.isNotBlank()) {
                    hiddenFields[name] = value
                }
            }

            var formHash = hiddenFields["formhash"].orEmpty()
            var parsedTid = hiddenFields["tid"].orEmpty()
            var parsedPid = hiddenFields["pid"].orEmpty()
            var referer = hiddenFields["referer"].orEmpty()
            var handleKey = hiddenFields["handlekey"].orEmpty()

            // 如果 #rateform 找不到，兜底读所有隐藏 input
            if (formHash.isBlank()) {
                doc.select("input[type=hidden]").forEach { input ->
                    when (input.attr("name")) {
                        "formhash" -> if (formHash.isBlank()) formHash = input.attr("value")
                        "tid" -> if (parsedTid.isBlank()) parsedTid = input.attr("value")
                        "pid" -> if (parsedPid.isBlank()) parsedPid = input.attr("value")
                        "referer" -> if (referer.isBlank()) referer = input.attr("value")
                        "handlekey" -> if (handleKey.isBlank()) handleKey = input.attr("value")
                    }
                }
            }

            // 战斗力分值选项
            val scoreOptions = mutableListOf<Int>()
            doc.select("select[name=score1] option, select#rate1 option").forEach { opt ->
                opt.attr("value").toIntOrNull()?.let { scoreOptions.add(it) }
            }

            // 理由预设
            val reasonPresets = mutableListOf<String>()
            doc.select("select[name=reason] option").forEach { opt ->
                val v = opt.attr("value").trim()
                val t = opt.text().trim()
                val reason = if (v.isNotBlank()) v else t
                if (reason.isNotBlank() && reason !in reasonPresets) {
                    reasonPresets.add(reason)
                }
            }

            // 评分区间与今日剩余
            var minScore: Int? = null
            var maxScore: Int? = null
            var remainingToday: Int? = null
            val rangeRegex = Regex("""([+-]?\s*\d+)\s*~\s*([+-]?\s*\d+)""")
            val digitRegex = Regex("""[+-]?\d+""")

            // 先按 table 行结构化解析（Discuz mobile 评分弹窗通常是一行三列：评分项/区间/剩余）
            doc.select("table tr").forEach { row ->
                val cells = row.select("td, th").map { it.text().trim() }
                val rowText = cells.joinToString(" ")

                rangeRegex.find(rowText)?.let { match ->
                    minScore = match.groupValues[1].replace(" ", "").toIntOrNull()
                    maxScore = match.groupValues[2].replace(" ", "").toIntOrNull()
                }

                cells.forEach { cell ->
                    if (cell.contains("剩余") || cell.contains("今日")) {
                        digitRegex.find(cell)?.value?.toIntOrNull()?.let { remainingToday = it }
                    }
                }
            }

            // 兜底：如果 table 行没抓到，再从整个文本里找区间
            if (minScore == null || maxScore == null) {
                doc.select(".dt, .mbm").forEach { el ->
                    val text = el.text()
                    rangeRegex.find(text)?.let { match ->
                        minScore = match.groupValues[1].replace(" ", "").toIntOrNull()
                        maxScore = match.groupValues[2].replace(" ", "").toIntOrNull()
                    }
                }
            }

            // 通知作者复选框
            val notifyAuthorBox = doc.select("#sendreasonpm").firstOrNull()
            val notifyAuthorDefault = notifyAuthorBox?.hasAttr("checked") ?: false
            val notifyAuthorDisabled = notifyAuthorBox?.hasAttr("disabled") ?: false

            RateFormOptions(
                scoreOptions = scoreOptions.takeIf { it.isNotEmpty() }
                    ?: RateFormOptions.DEFAULT_SCORE_OPTIONS,
                reasonPresets = reasonPresets.takeIf { it.isNotEmpty() }
                    ?: RateFormOptions.DEFAULT_REASON_PRESETS,
                formHash = formHash,
                tid = parsedTid.ifBlank { fallbackTid },
                pid = parsedPid.ifBlank { fallbackPid },
                referer = referer.ifBlank { "https://stage1st.com/2b/thread-$fallbackTid-1-1.html" },
                handleKey = handleKey.ifBlank { "rate" },
                minScore = minScore,
                maxScore = maxScore,
                remainingToday = remainingToday,
                notifyAuthorDefault = notifyAuthorDefault,
                notifyAuthorDisabled = notifyAuthorDisabled
            )
        } catch (e: Exception) {
            RateFormOptions.withDefaults("解析评分表单失败：${e.message}")
        }
    }

    /** 从 Discuz 错误弹窗 HTML 提取错误文案 */
    fun extractMessagetextError(html: String?): String? {
        if (html.isNullOrBlank()) return null
        val doc = try {
            Jsoup.parse(html)
        } catch (_: Exception) {
            return null
        }
        val text = doc.select("#messagetext, .alert_error, .showmessage").firstOrNull()?.text()
        return text?.takeIf { it.isNotBlank() }
    }

    /** 判断评分提交响应是否成功 */
    fun isRateSuccess(body: String?): Boolean {
        if (body.isNullOrBlank()) return false
        return body.contains("rate_succeed") ||
                body.contains("评分成功") ||
                body.contains("succeedhandle_rate") ||
                (body.contains("window.location.href") && body.contains("viewthread"))
    }

    private fun decode(s: String): String = s
        .replace("&amp;", "&").replace("&lt;", "<")
        .replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")

    private fun RateFormOptions.Companion.withDefaults(error: String): RateFormOptions {
        return RateFormOptions(
            scoreOptions = DEFAULT_SCORE_OPTIONS,
            reasonPresets = DEFAULT_REASON_PRESETS,
            formHash = "",
            tid = "",
            pid = "",
            referer = "",
            handleKey = "rate",
            minScore = -1,
            maxScore = 1,
            error = error
        )
    }
}
