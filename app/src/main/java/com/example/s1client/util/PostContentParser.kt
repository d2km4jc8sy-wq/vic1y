package com.example.s1client.util

import android.text.Html

object PostContentParser {

    data class ParsedContent(
        val text: CharSequence,
        val imageUrls: List<String>,
        val emojiUrls: List<String>,
        val quoteStart: Int = -1,
        val quoteEnd: Int = -1,
        /** 引用块纯文本（已从正文剔除，供独立引用卡片渲染） */
        val quoteText: String = "",
        /** 被引楼号（字符串数字），用于点击引用卡片跳转；提取不到为 null */
        val quoteFloor: String? = null,
        /** 代码块区间 [start, end)，渲染成等宽字体 + 底色块 */
        val codeBlocks: List<Pair<Int, Int>> = emptyList(),
        /** 列表项区间 [start, end)，渲染成圆点项目符号 */
        val listItems: List<Pair<Int, Int>> = emptyList()
    )

    private val emojiKeywords = listOf(
        "smiley", "face", "emoji", "sticker", "S1_face", "majiang", "e_face"
    )

    private const val EMOJI_PLACEHOLDER = "\uFFFC"

    // 用 Private Use Area 字符，Html.fromHtml 不会处理它们
    private const val QUOTE_START_MARK = "\uE000"
    private const val QUOTE_END_MARK = "\uE001"
    private const val CODE_START_MARK = "\uE002"
    private const val CODE_END_MARK = "\uE003"
    private const val ITEM_START_MARK = "\uE004"
    private const val ITEM_END_MARK = "\uE005"

    /**
     * 楼层正文解析很重（多轮正则 + 占位符处理），但同一楼层的 message 是不变的。
     * 滚动时 RecyclerView 反复 rebind 同一个楼层会重复解析，造成明显卡顿，
     * 这里按原文缓存解析结果，每个楼层只解析一次。
     */
    private val parseCache = android.util.LruCache<String, ParsedContent>(200)

    fun parse(html: String?): ParsedContent {
        if (html.isNullOrBlank()) return ParsedContent("", emptyList(), emptyList())

        // 命中缓存直接返回，跳过整段正则解析
        parseCache.get(html)?.let { return it }

        var text = html
            .replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#39;", "'")
            .replace("&apos;", "'").replace("&nbsp;", " ")
            .replace("&amp;", "&")

        // ===== 1. 处理引用（Discuz 用 <div class="reply_wrap">）=====
        val replyWrapRegex = Regex(
            """<div[^>]*class="[^"]*reply_wrap[^"]*"[^>]*>(.*?)</div>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        text = replyWrapRegex.replace(text) { m ->
            "\n$QUOTE_START_MARK${m.groupValues[1]}$QUOTE_END_MARK\n"
        }

        val blockquoteRegex = Regex("""<blockquote[^>]*>(.*?)</blockquote>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        text = blockquoteRegex.replace(text) { m ->
            "\n$QUOTE_START_MARK${m.groupValues[1]}$QUOTE_END_MARK\n"
        }

        val bbQuoteRegex = Regex("""\[quote\](.*?)\[/quote\]""", RegexOption.DOT_MATCHES_ALL)
        text = bbQuoteRegex.replace(text) { m ->
            "\n$QUOTE_START_MARK${m.groupValues[1]}$QUOTE_END_MARK\n"
        }

        // ===== 2. 代码块 [code]…[/code] / <pre>…</pre> =====
        // 内容里的尖括号必须重新转义，否则会被后面的 Html.fromHtml 当成标签吃掉
        // （典型场景：帖子里贴一段 HTML/XML 源码，整段直接消失）
        val bbCodeRegex = Regex("""\[code\](.*?)\[/code\]""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        text = bbCodeRegex.replace(text) { m ->
            "\n$CODE_START_MARK${escapeHtml(m.groupValues[1])}$CODE_END_MARK\n"
        }
        val htmlCodeRegex = Regex("""<pre[^>]*>(.*?)</pre>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        text = htmlCodeRegex.replace(text) { m ->
            "\n$CODE_START_MARK${escapeHtml(m.groupValues[1])}$CODE_END_MARK\n"
        }

        // ===== 3. 列表 [list]…[*]项…[/list] =====
        val listRegex = Regex("""\[list[^\]]*\](.*?)\[/list\]""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        text = listRegex.replace(text) { m ->
            val parts = m.groupValues[1].split(Regex("""\[\*\]""", RegexOption.IGNORE_CASE))
            val sb = StringBuilder("\n")
            for (p in parts) {
                val item = p.trim()
                if (item.isEmpty()) continue
                sb.append(ITEM_START_MARK).append(item).append(ITEM_END_MARK).append('\n')
            }
            sb.toString()
        }

        // ===== 4. 提取图片 =====
        val imageUrls = mutableListOf<String>()
        val emojiUrls = mutableListOf<String>()

        val imgRegex = Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE)
        text = imgRegex.replace(text) { m ->
            val url = extractUrl(m.value)
            if (!url.isNullOrBlank()) {
                val n = normalizeUrl(url)
                if (isEmoji(n)) { emojiUrls.add(n); EMOJI_PLACEHOLDER }
                else { imageUrls.add(n); "" }
            } else ""
        }

        val bbImgRegex = Regex("""\[img[^\]]*\]([^\[]+)\[/img\]""", RegexOption.IGNORE_CASE)
        text = bbImgRegex.replace(text) { m ->
            val url = m.groupValues[1].trim()
            if (url.isNotBlank()) {
                val n = normalizeUrl(url)
                if (isEmoji(n)) { emojiUrls.add(n); EMOJI_PLACEHOLDER }
                else { imageUrls.add(n); "" }
            } else ""
        }

        text = text.replace(Regex("""\[s:\d+\]"""), "")

        // ===== 5. 转纯文本 =====
        val raw = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
            .toString()
            .replace(Regex("""\n{3,}"""), "\n\n")

        // ===== 6. 一次扫描剥离全部标记，同时记录各区间 =====
        // 顺序扫描 + 就地构建结果串：得到的下标直接就是最终文本的索引，无需二次修正
        val sb = StringBuilder()
        var quoteStart = -1
        var quoteEnd = -1
        var inQuote = false
        val quoteSb = StringBuilder()
        var quoteText = ""
        var quoteFloor: String? = null
        val codeBlocks = mutableListOf<Pair<Int, Int>>()
        val listItems = mutableListOf<Pair<Int, Int>>()
        var codeStart = -1
        var itemStart = -1

        var i = 0
        while (i < raw.length) {
            when (raw[i]) {
                QUOTE_START_MARK[0] -> { inQuote = true; quoteSb.setLength(0); i++ }
                QUOTE_END_MARK[0] -> {
                    inQuote = false
                    quoteText = quoteSb.toString().trim()
                    quoteFloor = Regex("""(\d{1,4})\s*楼""").find(quoteText)?.groupValues?.get(1)
                        ?: Regex("""#\s*(\d{1,4})""").find(quoteText)?.groupValues?.get(1)
                    quoteStart = sb.length
                    quoteEnd = sb.length
                    i++
                }
                CODE_START_MARK[0] -> { sb.ensureNewLine(); codeStart = sb.length; i++ }
                CODE_END_MARK[0] -> {
                    sb.trimTail()
                    if (codeStart >= 0 && sb.length > codeStart) codeBlocks.add(codeStart to sb.length)
                    codeStart = -1
                    sb.append('\n')
                    i++
                }
                ITEM_START_MARK[0] -> { sb.ensureNewLine(); itemStart = sb.length; i++ }
                ITEM_END_MARK[0] -> {
                    sb.trimTail()
                    if (itemStart >= 0 && sb.length > itemStart) listItems.add(itemStart to sb.length)
                    itemStart = -1
                    sb.append('\n')
                    i++
                }
                else -> { if (inQuote) quoteSb.append(raw[i]) else sb.append(raw[i]); i++ }
            }
        }
        if (codeStart >= 0 && sb.length > codeStart) codeBlocks.add(codeStart to sb.length)
        if (itemStart >= 0 && sb.length > itemStart) listItems.add(itemStart to sb.length)

        val result = ParsedContent(
            sb.toString().trim(), imageUrls, emojiUrls,
            quoteStart, quoteEnd, quoteText, quoteFloor, codeBlocks, listItems
        )
        parseCache.put(html, result)
        return result
    }

    /** 保证块级内容另起一行：先掐掉尾部空白，非空再补一个换行 */
    private fun StringBuilder.ensureNewLine() {
        trimTail()
        if (isNotEmpty()) append('\n')
    }

    /** 去掉尾部连续的换行与空格 */
    private fun StringBuilder.trimTail() {
        while (isNotEmpty() && (last() == '\n' || last() == ' ' || last() == '\r')) {
            deleteCharAt(lastIndex)
        }
    }

    /** 代码块内容转义：避免被 Html.fromHtml 当成标签解析 */
    private fun escapeHtml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private fun extractUrl(tag: String): String? {
        val attrs = listOf("src", "file", "data-original", "data-src", "zoomfile", "original")
        for (attr in attrs) {
            Regex("""\b$attr\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .find(tag)?.groupValues?.get(1)?.let { url ->
                    if (url.isNotBlank() && !url.startsWith("data:")) return url
                }
        }
        return null
    }

    private fun isEmoji(url: String): Boolean =
        emojiKeywords.any { url.lowercase().contains(it) }

    private fun normalizeUrl(url: String): String {
        var n = when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> "https://stage1st.com$url"
            else -> url
        }
        if (n.contains("/attachments/") && !n.contains("/attachments/forum/")) {
            n = n.replace(Regex("""(/attachments)/(\d{6}/)"""), "$1/forum/$2")
        }
        return n
    }
}
