package com.example.s1client.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** BBCode 编辑辅助：环绕选区、插入片段、粘贴裸 URL 自动转链接 */
object BbCode {

    private val URL_REGEX = Regex("""(?i)^https?://[^\s\[\]<>]+$""")
    private val URL_OPEN_TAIL = Regex("""\[url=[^\]]*$""")

    /** 用 prefix/suffix 包裹选区；无选区时插入成对标记并把光标放中间 */
    fun wrap(value: TextFieldValue, prefix: String, suffix: String): TextFieldValue {
        val start = value.selection.min.coerceIn(0, value.text.length)
        val end = value.selection.max.coerceIn(0, value.text.length)
        val selected = value.text.substring(start, end)
        val newText = value.text.substring(0, start) + prefix + selected + suffix +
                value.text.substring(end)
        val cursor = if (selected.isEmpty()) start + prefix.length
        else start + prefix.length + selected.length + suffix.length
        return TextFieldValue(newText, TextRange(cursor))
    }

    /** 在光标处插入片段，光标落在末尾 */
    fun insert(value: TextFieldValue, snippet: String): TextFieldValue {
        val start = value.selection.min.coerceIn(0, value.text.length)
        val end = value.selection.max.coerceIn(0, value.text.length)
        val newText = value.text.substring(0, start) + snippet + value.text.substring(end)
        return TextFieldValue(newText, TextRange(start + snippet.length))
    }

    /**
     * 检测这一次编辑是不是「粘贴了一个裸 URL」：是就包成 [url]…[/url]。
     * 通过比对 old/new 找出被插入的那段文本，整段命中 URL 才处理，
     * 逐字打字不会误触发。
     */
    fun autoLink(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
        val o = old.text
        val n = new.text
        if (n.length <= o.length) return new

        var p = 0
        while (p < o.length && p < n.length && o[p] == n[p]) p++
        var s = 0
        while (s < o.length - p && s < n.length - p &&
            o[o.length - 1 - s] == n[n.length - 1 - s]
        ) s++

        val inserted = n.substring(p, n.length - s)
        if (!URL_REGEX.matches(inserted)) return new

        val before = n.substring(0, p)
        // 已经在 [url] / [url=… 里了，不再套一层
        if (before.endsWith("[url]") || URL_OPEN_TAIL.containsMatchIn(before)) return new

        val tail = n.substring(n.length - s)
        val wrapped = "[url]$inserted[/url]"
        return TextFieldValue(
            before + wrapped + tail,
            TextRange(p + wrapped.length)
        )
    }
}
