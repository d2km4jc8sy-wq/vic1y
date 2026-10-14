package com.example.s1client.data.model

/**
 * 投票数据模型 —— **字段与判定逻辑照搬开源 s1er 的 `models/poll.dart`**，
 * 只把 Dart 语法翻译成 Kotlin，不重新发明字段名或判定规则。
 *
 * Discuz 的投票字段（来自 viewthread HTML / JSON）：
 *   polloptions.{n}.polloptionid / polloption / votes / percent / color
 *   expirations / multiple / maxchoices / voterscount / visiblepoll / allowvote
 *   remaintime: [天, 时, 分, 秒]
 *
 * **API 不返回"我投了哪几项"**，所以本地要缓存（见 PollVoteCache），
 * 拿到数据后用 `withUserVotes()` 合并进来再渲染。
 */
data class PollOption(
    val id: String,
    val text: String,
    val votes: Int,
    val percent: Double,
    /** Discuz 给每个选项分配的颜色（形如 "6750A4"），没给就用主题色 */
    val colorHex: String = "6750A4",
    /** 当前用户是否投了这一项 —— 由本地缓存合并，不是接口字段 */
    val isUserVote: Boolean = false
)

data class ThreadPoll(
    val options: List<PollOption> = emptyList(),
    /** 截止时间戳（秒）；0 表示不限 */
    val expiration: Long = 0,
    /** 是否多选 */
    val multiple: Boolean = false,
    /** 多选时最多可选几项 */
    val maxChoices: Int = 1,
    /** 已投票人数 */
    val votersCount: Int = 0,
    /** 是否允许看结果（未投票也能看） */
    val visibleResults: Boolean = false,
    /** 是否还能投票 */
    val allowVote: Boolean = false,
    /** 剩余时间 [天, 时, 分, 秒]，全 0 = 已结束 */
    val remainTime: List<Int> = List(4) { 0 },
    /** 本地缓存合并进来的"我投了哪几项" */
    val userVotedOptionIds: List<String> = emptyList()
) {
    val isExpired: Boolean
        get() = remainTime.all { it == 0 } ||
            (expiration > 0 && expiration <= System.currentTimeMillis() / 1000)

    val canVote: Boolean get() = allowVote && !isExpired

    /** 该不该显示票数/百分比 */
    val showResults: Boolean get() = visibleResults || !canVote

    val hasUserVoted: Boolean get() = userVotedOptionIds.isNotEmpty()

    val remainTimeLabel: String
        get() {
            if (isExpired) return "投票已结束"
            val parts = mutableListOf<String>()
            if (remainTime[0] > 0) parts.add("${remainTime[0]}天")
            if (remainTime[1] > 0) parts.add("${remainTime[1]}时")
            if (remainTime[2] > 0) parts.add("${remainTime[2]}分")
            if (parts.isEmpty() && remainTime[3] > 0) parts.add("${remainTime[3]}秒")
            if (parts.isEmpty()) return "即将结束"
            return "剩余 ${parts.joinToString("")}"
        }

    val voteModeLabel: String get() = if (multiple) "多选（最多 $maxChoices 项）" else "单选"

    fun withUserVotes(optionIds: List<String>): ThreadPoll {
        if (optionIds.isEmpty()) return this
        val voted = optionIds.toSet()
        return copy(
            options = options.map { it.copy(isUserVote = it.id in voted) },
            userVotedOptionIds = optionIds
        )
    }

    companion object {
        /**
         * 从 JSON 里解析。**选项要按 key 排序**（Discuz 的 polloptions 是
         * 以任意顺序的 Map 返回的，不排序会导致选项顺序每次不一样）。
         */
        fun fromJson(json: org.json.JSONObject): ThreadPoll {
            val options = mutableListOf<PollOption>()
            val raw = json.optJSONObject("polloptions")
            if (raw != null) {
                val keys = raw.keys().asSequence().toMutableList()
                    .sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
                for (key in keys) {
                    val o = raw.optJSONObject(key) ?: continue
                    options.add(
                        PollOption(
                            id = o.optString("polloptionid"),
                            text = o.optString("polloption"),
                            votes = o.optString("votes").toIntOrNull() ?: 0,
                            percent = o.optString("percent").toDoubleOrNull() ?: 0.0,
                            colorHex = o.optString("color").ifBlank { "6750A4" },
                            isUserVote = o.optString("uservote") == "1" ||
                                o.optString("voted") == "1"
                        )
                    )
                }
            }

            val remain = mutableListOf<Int>()
            val rawRemain = json.optJSONArray("remaintime")
            if (rawRemain != null) {
                for (i in 0 until rawRemain.length()) {
                    remain.add(rawRemain.optString(i).toIntOrNull() ?: 0)
                }
            }
            while (remain.size < 4) remain.add(0)

            return ThreadPoll(
                options = options,
                expiration = json.optString("expirations").toLongOrNull() ?: 0,
                multiple = json.optString("multiple") == "1",
                maxChoices = json.optString("maxchoices").toIntOrNull() ?: 1,
                votersCount = json.optString("voterscount").toIntOrNull() ?: 0,
                visibleResults = json.optString("visiblepoll") == "1",
                allowVote = json.optString("allowvote") == "1",
                remainTime = remain
            )
        }

        /** 从 viewthread HTML 里解析（JSON 拿不到时的兜底路径） */
        fun fromHtml(html: String): ThreadPoll? {
            if (html.isBlank()) return null
            val options = mutableListOf<PollOption>()
            // <li><input type="radio" name="pollanswers[]" value="12" class="px">
            // <label for="option_12">选项文字</label>
            val optRe = Regex(
                """value\s*=\s*["'](\d+)["'][^>]*>\s*<label[^>]*>([^<]+)</label>""",
                RegexOption.IGNORE_CASE
            )
            for (m in optRe.findAll(html)) {
                options.add(
                    PollOption(
                        id = m.groupValues[1],
                        text = m.groupValues[2].trim(),
                        votes = 0, percent = 0.0
                    )
                )
            }
            if (options.isEmpty()) return null

            // 已投票时页面会直接渲染百分比
            val pctRe = Regex("""(\d+(?:\.\d+)?)%""")
            if (options.isNotEmpty() && html.contains("pollbar")) {
                val pcts = pctRe.findAll(html).map { it.groupValues[1].toDouble() }.toList()
                options.forEachIndexed { i, o ->
                    if (i < pcts.size) options[i] = o.copy(percent = pcts[i])
                }
            }
            val multiple = html.contains("""name="pollanswers[]" value=""".takeIf { false }
                ?: "") || html.contains("maxchoices") && !html.contains("""type="radio"""")
            val voters = Regex("""(\d+)\s*(?:人已投票|位用户已投)""").find(html)
                ?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val expired = html.contains("投票已结束") || html.contains("投票结束")

            return ThreadPoll(
                options = options,
                multiple = multiple,
                votersCount = voters,
                allowVote = !expired && html.contains("""name="pollanswers[]""""),
                visibleResults = html.contains("pollbar")
            )
        }
    }
}
