package com.example.s1client.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import com.example.s1client.util.Haptic

/**
 * 回复成功「邮票落章」动画（2026-10-16）。
 *
 * 为什么是落章：论坛回复的语义本就是「往楼里盖一个章」，票据语言里也有票根/印花，
 * 所以"一张小票据盖到新楼层上"同时踩中内容语义与视觉语言，不需要另造隐喻。
 *
 * 时序（总时长约 620ms）：
 * 1. 0→180ms  票据从 0.6 缩放 + 透明渐显，边框高光从左扫到右（盖章的光泽）
 * 2. 落定瞬间 触发 Haptic.SUCCESS（哒—嗒，与"咔哒盖章"同感），并轻微下沉回弹
 * 3. 180→420ms 停留，让用户看清章的样子
 * 4. 420→620ms 上飘淡出
 *
 * 全程自绘一张 Path（圆角矩形 + 右侧齿口 + 虚线内框），无位图、无逐帧对象分配。
 */
class StampOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private var accent = Color.parseColor("#E9C8EE")
    private var ink = Color.WHITE

    private val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD
        )
        textAlign = Paint.Align.CENTER
    }

    /** 高光扫光进度 0..1（只影响一道竖直亮带的位置） */
    private var sweep = 0f

    private val path = Path()
    private val box = RectF()

    /** 刷新主题色（每次显示前调用，跟随当前主题） */
    fun refreshThemeColors() {
        val tv = TypedValue()
        val ok = context.theme.resolveAttribute(com.example.s1client.R.attr.s1Accent, tv, true)
        accent = if (ok) tv.data else Color.parseColor("#E9C8EE")
        val tv2 = TypedValue()
        val ok2 = context.theme.resolveAttribute(com.example.s1client.R.attr.s1BgCard, tv2, true)
        ink = if (ok2) tv2.data else Color.WHITE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val inset = 4f * density
        box.set(inset, inset, w - inset, h - inset)
        buildPath()
    }

    private fun buildPath() {
        path.reset()
        val r = 6f * density
        val teeth = 5
        val toothDepth = 4f * density
        val right = box.right
        val toothH = box.height() / teeth
        path.moveTo(box.left + r, box.top)
        path.lineTo(right - toothDepth, box.top)
        var i = 0
        while (i < teeth) {
            val y = box.top + i * toothH
            path.lineTo(right, y + toothH / 2f)
            path.lineTo(right - toothDepth, y + toothH)
            i++
        }
        path.lineTo(box.left + r, box.bottom)
        path.quadTo(box.left, box.bottom, box.left, box.bottom - r)
        path.lineTo(box.left, box.top + r)
        path.quadTo(box.left, box.top, box.left + r, box.top)
        path.close()
    }

    override fun onDraw(canvas: Canvas) {
        // 纸面（半透明 accent，压住下层但能透出背景）
        paperPaint.color = withAlpha(accent, 190)
        canvas.drawPath(path, paperPaint)
        // 亮白描边
        edgePaint.color = withAlpha(Color.WHITE, 210)
        canvas.drawPath(path, edgePaint)
        // 虚线内框（票据语汇）
        edgePaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(3f * density, 2.5f * density), 0f)
        val inner = RectF(
            box.left + 5f * density, box.top + 5f * density,
            box.right - 5f * density, box.bottom - 5f * density
        )
        canvas.drawRoundRect(inner, 3f * density, 3f * density, edgePaint)
        edgePaint.pathEffect = null

        // 章面文字
        textPaint.color = Color.WHITE
        textPaint.textSize = 13f * density
        canvas.drawText("已回复", box.centerX(), box.centerY() + textPaint.textSize / 3f, textPaint)
        textPaint.textSize = 8.5f * density
        textPaint.color = withAlpha(Color.WHITE, 200)
        canvas.drawText("POSTED", box.centerX(), box.centerY() + textPaint.textSize * 2.4f, textPaint)

        // 扫光：一道竖直亮带只在盖章瞬间存在（sweep 0..1），画在章面上
        if (sweep in 0f..1f) {
            val bandW = 26f * density
            val x = box.left - bandW + (box.width() + bandW * 2) * sweep
            val grad = android.graphics.LinearGradient(
                x - bandW / 2f, 0f, x + bandW / 2f, 0f,
                intArrayOf(0x00FFFFFF, 0x88FFFFFF.toInt(), 0x00FFFFFF),
                null, android.graphics.Shader.TileMode.CLAMP
            )
            paperPaint.shader = grad
            canvas.drawPath(path, paperPaint)
            paperPaint.shader = null
        }
    }

    private fun withAlpha(c: Int, a: Int): Int =
        Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))

    /**
     * 播放落章动画。调用方负责把本View 挂到目标区域并设置尺寸，
     * 结束后 [onFinished] 回调用于移除自身。
     */
    fun playStamp(onFinished: () -> Unit) {
        refreshThemeColors()
        visibility = VISIBLE
        alpha = 0f
        scaleX = 0.62f
        scaleY = 0.62f
        translationY = -10f * density

        // 扫光驱动
        val sweepAnim = ObjectAnimator.ofFloat(this, "sweep", 0f, 1f).apply {
            duration = 260
            interpolator = DecelerateInterpolator()
        }
        // 显隐 + 缩放
        val fadeIn = ObjectAnimator.ofFloat(this, View.ALPHA, 0f, 1f).apply {
            duration = 140
            interpolator = DecelerateInterpolator()
        }
        val pop = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(this@StampOverlayView, View.SCALE_X, 0.62f, 1.08f).apply {
                    duration = 190
                    interpolator = OvershootInterpolator(2.2f)
                },
                ObjectAnimator.ofFloat(this@StampOverlayView, View.SCALE_Y, 0.62f, 1.08f).apply {
                    duration = 190
                    interpolator = OvershootInterpolator(2.2f)
                },
                ObjectAnimator.ofFloat(this@StampOverlayView, View.TRANSLATION_Y, -10f * density, 0f).apply {
                    duration = 190
                    interpolator = DecelerateInterpolator()
                }
            )
        }
        // 落定回弹（1.08 → 1.0）
        val settle = ObjectAnimator.ofFloat(this, View.SCALE_X, 1.08f, 1f).apply {
            duration = 110
            interpolator = DecelerateInterpolator()
        }
        val settleY = ObjectAnimator.ofFloat(this, View.SCALE_Y, 1.08f, 1f).apply {
            duration = 110
            interpolator = DecelerateInterpolator()
        }
        val settleAnim = AnimatorSet().apply { playTogether(settle, settleY) }

        // 停留后淡出
        val fadeOut = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(this@StampOverlayView, View.ALPHA, 1f, 0f).apply {
                    duration = 200
                    startDelay = 220
                    interpolator = AccelerateInterpolator()
                },
                ObjectAnimator.ofFloat(this@StampOverlayView, View.TRANSLATION_Y, 0f, -18f * density).apply {
                    duration = 200
                    startDelay = 220
                    interpolator = AccelerateInterpolator()
                }
            )
        }

        AnimatorSet().apply {
            playTogether(fadeIn, sweepAnim, pop)
            play(pop)
            // 落定那一刻给盖章震动
            pop.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    Haptic.success(context)
                    settleAnim.start()
                    settleAnim.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(a: Animator) {
                            fadeOut.start()
                        }
                    })
                }
            })
            fadeOut.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onFinished()
                }
            })
            start()
        }
    }
}

/**
 * 在 [host] 的指定位置播放一次落章动画（回复成功用）。
 *
 * 位置约定：贴在**新回复楼层所在页**的右上角内侧 —— 与便签里的 stamp 同区域，
 * 视觉上是"这一楼被盖了章"，而不是屏幕中央弹一个无关的提示。
 */
fun playStampOverlay(
    host: android.view.ViewGroup,
    anchor: android.view.View,
    onEnd: () -> Unit = {}
) {
    val ctx = host.context
    val d = ctx.resources.displayMetrics.density
    val size = (92 * d).toInt()
    val stamp = StampOverlayView(ctx).apply {
        layoutParams = FrameLayout.LayoutParams(size, size, android.view.Gravity.TOP or android.view.Gravity.END)
    }
    val lp = stamp.layoutParams as FrameLayout.LayoutParams
    // 贴着锚点右上角，向内让开 14dp
    val anchorLoc = IntArray(2)
    val hostLoc = IntArray(2)
    anchor.getLocationInWindow(anchorLoc)
    host.getLocationInWindow(hostLoc)
    lp.rightMargin = (14 * d).toInt()
    lp.topMargin = anchorLoc[1] - hostLoc[1] + (10 * d).toInt()
    host.addView(stamp)
    stamp.post {
        stamp.playStamp {
            host.removeView(stamp)
            onEnd()
        }
    }
}