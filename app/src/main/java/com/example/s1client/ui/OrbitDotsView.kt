package com.example.s1client.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin

/**
 * 三色轨道珠子（控件 04 落地版 · View 体系）：
 * 三颗珠子沿圆轨道追逐 —— 领头珠实心主题强调色，后两颗渐隐拖尾，
 * 同色系纯主题派生，不引入任何写死颜色。
 *
 * 用在：帖子详情页「换页加载尾」（替换系统 ProgressBar）。
 * 可见时自转，gone/invisible 自动停转，零后台开销。
 */
class OrbitDotsView @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null
) : View(context, attrs) {

    private var accent = 0xFF6FB5F5.toInt()
    /** 三颗珠子各自的色（默认全部 accent，外部可用 setDotColors 注入三色） */
    private var dotColors = intArrayOf(accent, accent, accent)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var angle = 0f
    private var animator: ValueAnimator? = null

    init {
        val tv = TypedValue()
        if (context.theme.resolveAttribute(com.example.s1client.R.attr.s1Accent, tv, true)) {
            accent = tv.data
        }
        dotPaint.style = Paint.Style.FILL
    }

    /** 实时取色：主题切换后由 Activity onResume 调用 */
    fun refreshThemeColors() {
        val tv = TypedValue()
        if (context.theme.resolveAttribute(com.example.s1client.R.attr.s1Accent, tv, true)) {
            if (tv.data != accent) {
                accent = tv.data
                dotColors = intArrayOf(accent, accent, accent)
                invalidate()
            }
        }
    }

    /** 注入三颗珠子的颜色（对标 uiverse 原版三色追逐） */
    fun setDotColors(colors: List<Int>) {
        if (colors.size >= 3) {
            dotColors = intArrayOf(colors[0], colors[1], colors[2])
            invalidate()
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) start() else stop()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (visibility == VISIBLE) start()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    private fun start() {
        if (animator != null) return
        animator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 1100L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                angle = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stop() {
        animator?.cancel()
        animator = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) / 2f * 0.62f
        val dotR = 3.2f * resources.displayMetrics.density
        // 三颗主题色珠子沿轨道追逐（相位差 120°，原版还原）
        val phases = floatArrayOf(0f, 120f, 240f)
        for (i in phases.indices) {
            val rad = Math.toRadians((angle + phases[i]).toDouble() - 90.0)
            val dx = (r * cos(rad)).toFloat()
            val dy = (r * sin(rad)).toFloat()
            dotPaint.color = dotColors[i % dotColors.size]
            dotPaint.alpha = 255
            canvas.drawCircle(cx + dx, cy + dy, dotR, dotPaint)
        }
    }
}
