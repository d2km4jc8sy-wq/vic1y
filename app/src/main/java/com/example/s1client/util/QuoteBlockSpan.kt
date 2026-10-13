package com.example.s1client.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.style.LeadingMarginSpan
import android.text.style.LineBackgroundSpan
import com.example.s1client.R

/**
 * 引用块：左竖条 + 浅底 + 左缩进（替代原来整段铺满的底色 span）。
 *
 * - 段落级缩进：LeadingMarginSpan（引用必须自成一段，[PostContentParser] 已保证）
 * - 逐行底色 / 竖条：LineBackgroundSpan。框架调用为
 *   `drawBackground(canvas, paint, 0, width, ltop, lbaseline, lbottom, ...)`，
 *   即 left 恒为 0、right 恒为整行宽，竖条直接按 left 画。
 *
 * 两段 rect 上下各外扩 [vPad]：行盒本就连续，外扩后保证多行引用的竖条无缝，
 * 首尾行也有一点内边距。
 */
class QuoteBlockSpan(
    private val barColor: Int,
    private val bgColor: Int,
    context: Context
) : LeadingMarginSpan, LineBackgroundSpan {

    private val density = context.resources.displayMetrics.density
    private val barWidth = 2.5f * density
    private val barOffset = 3f * density
    /** 文本起点：竖条(3~5.5dp) + 6dp 间隙 */
    private val indent = (barOffset + barWidth + 6f * density).toInt()
    private val vPad = 2f * density

    private val bgPaint = Paint().apply { color = bgColor }
    private val barPaint = Paint().apply { color = barColor }

    override fun getLeadingMargin(first: Boolean): Int = indent

    override fun drawLeadingMargin(
        c: Canvas, p: Paint, x: Int, dir: Int, top: Int, baseline: Int,
        bottom: Int, text: CharSequence, start: Int, end: Int, first: Boolean, layout: Layout?
    ) {
        // 缩进只由 getLeadingMargin 提供，竖条/底色统一在 drawBackground 画
    }

    override fun drawBackground(
        c: Canvas, p: Paint, left: Int, right: Int, top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int, lineNumber: Int
    ) {
        val topF = top - vPad
        val bottomF = bottom + vPad
        c.drawRect(left.toFloat(), topF, right.toFloat(), bottomF, bgPaint)
        val barLeft = left + barOffset
        c.drawRect(barLeft, topF, barLeft + barWidth, bottomF, barPaint)
    }
}
