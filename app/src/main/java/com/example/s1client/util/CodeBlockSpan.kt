package com.example.s1client.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.style.LeadingMarginSpan
import android.text.style.LineBackgroundSpan

/**
 * 代码块：整块底色 + 左侧 4dp 留白 + 上下内边距（配合 TypefaceSpan 等宽字体使用）。
 *
 * 与 [QuoteBlockSpan] 同思路：
 * - 缩进用 LeadingMarginSpan
 * - 逐行底色用 LineBackgroundSpan（框架给的 left=0、right=整行宽）
 * 行盒本就连续，上下各外扩 [vPad] 后多行代码块的底色连成一整块。
 */
class CodeBlockSpan(
    private val bgColor: Int,
    context: Context
) : LeadingMarginSpan, LineBackgroundSpan {

    private val density = context.resources.displayMetrics.density
    /** 文本起点内缩 8dp，让代码不贴着底色块边缘 */
    private val indent = (8f * density).toInt()
    private val vPad = 4f * density

    private val bgPaint = Paint().apply { color = bgColor }

    override fun getLeadingMargin(first: Boolean): Int = indent

    override fun drawLeadingMargin(
        c: Canvas, p: Paint, x: Int, dir: Int, top: Int, baseline: Int,
        bottom: Int, text: CharSequence, start: Int, end: Int, first: Boolean, layout: Layout?
    ) {
        // 缩进由 getLeadingMargin 提供，底色统一在 drawBackground 画
    }

    override fun drawBackground(
        c: Canvas, p: Paint, left: Int, right: Int, top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int, lineNumber: Int
    ) {
        c.drawRect(left.toFloat(), top - vPad, right.toFloat(), bottom + vPad, bgPaint)
    }
}
