package com.example.s1client.util

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.view.View
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * 私信正文富文本渲染。
 *
 * **背景**：`PmParser` 取到的 `message` 是**HTML 原文**（`msgEl.html()`），
 * 数据里表情、@提及、链接、加粗本来都在，是详情页自己用 `stripBbcode()`
 * 主动剥成了纯文本。这里把它们还原出来。
 *
 * 支持：加粗/斜体/下划线、链接（可点）、@提及（可点跳用户）、
 * 引用块、行内图片（表情/附件图，点击回调由调用方决定怎么处理）。
 * 剥离：script/style 等不可见内容，以及 Discuz 的 BBCode 标记。
 */
object PmRichText {

    /** 单个样式标记的嵌套深度上限，防止异常 HTML 造成死循环 */
    private const val MAX_DEPTH = 12

    /**
     * @param onImageClick 图片被点击（传入 src 或 file 原值）
     * @param onUserClick  被 @ 的用户被点击（传入 uid）
     */
    fun render(
        html: String,
        onImageClick: (String) -> Unit = {},
        onUserClick: (String) -> Unit = {}
    ): CharSequence {
        if (html.isBlank()) return ""
        val doc = try {
            Jsoup.parse(html)
        } catch (_: Exception) {
            return stripBbcode(html)
        }
        doc.select("script, style, head, noscript").remove()

        val sb = SpannableStringBuilder()
        walk(doc.body(), sb, StyleSpan(Typeface.NORMAL), 0, onImageClick, onUserClick)
        return sb
    }

    private fun walk(
        node: Node,
        sb: SpannableStringBuilder,
        style: StyleSpan,
        depth: Int,
        onImageClick: (String) -> Unit,
        onUserClick: (String) -> Unit
    ) {
        if (depth > MAX_DEPTH) return

        for (child in node.childNodes()) {
            when (child) {
                is TextNode -> appendPlain(sb, child.text(), style)

                is Element -> {
                    when (child.tagName().lowercase()) {
                        "br" -> sb.append('\n')

                        "img" -> {
                            val src = child.attr("src").ifBlank { child.attr("file") }
                            if (src.isNotBlank()) {
                                val alt = child.attr("alt").ifBlank { "[图片]" }
                                val start = sb.length
                                sb.append(alt)
                                sb.setSpan(
                                    object : ClickableSpan() {
                                        override fun onClick(widget: View) = onImageClick(src)
                                    },
                                    start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                            }
                        }

                        "a" -> {
                            val href = child.attr("href")
                            val uid = extractUidFromSpaceUrl(href)
                            val start = sb.length
                            walk(child, sb, style, depth + 1, onImageClick, onUserClick)
                            val end = sb.length
                            if (end > start) {
                                if (!uid.isNullOrBlank()) {
                                    sb.setSpan(
                                        object : ClickableSpan() {
                                            override fun onClick(widget: View) = onUserClick(uid)
                                        },
                                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                } else if (href.isNotBlank()) {
                                    sb.setSpan(
                                        URLSpan(href),
                                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                }
                            }
                        }

                        "b", "strong" -> walk(child, sb, StyleSpan(Typeface.BOLD), depth + 1, onImageClick, onUserClick)
                        "i", "em" -> walk(child, sb, StyleSpan(Typeface.ITALIC), depth + 1, onImageClick, onUserClick)
                        "blockquote" -> {
                            sb.append('\n')
                            walk(child, sb, style, depth + 1, onImageClick, onUserClick)
                            sb.append('\n')
                        }
                        "p", "div", "li", "tr" -> {
                            walk(child, sb, style, depth + 1, onImageClick, onUserClick)
                            sb.append('\n')
                        }
                        else -> walk(child, sb, style, depth + 1, onImageClick, onUserClick)
                    }
                }

                else -> Unit
            }
        }
    }

    /** 纯文本追加：处理实体反转义 + 顺带剥掉残留 BBCode 标记 */
    private fun appendPlain(sb: SpannableStringBuilder, raw: String, style: StyleSpan) {
        val text = stripBbcode(raw)
        if (text.isEmpty()) return
        val start = sb.length
        sb.append(text)
        if (style.style != Typeface.NORMAL) {
            sb.setSpan(style, start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    /** 剥掉 Discuz BBCode 标记（[b] [url] [uid=] 等），保留文字 */
    private fun stripBbcode(s: String): String =
        s.replace(Regex("\\[/?(?:b|i|u|color|size|font|attach|hide|img|image)[^\\]]*\\]"), "")
}

/** 私信正文可能不止文字，统一做一次清理（0/null/- 当空） */
fun normalizePmText(s: String): String {
    val t = s.trim()
    return if (t.isEmpty() || t == "0" || t == "null" || t == "-") "" else t
}