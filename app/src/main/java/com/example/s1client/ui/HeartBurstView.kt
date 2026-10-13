package com.example.s1client.ui

import android.animation.ValueAnimator
import androidx.compose.ui.graphics.toArgb
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin

/**
 * 爱心爆裂（控件 05 落地版 · View 体系）：
 * 6 粒主题色粒子从锚点向外迸发 + 渐隐，600ms 后自动从父布局移除。
 *
 * 用法：收藏成功瞬间
 *   HeartBurstView.burstAt(binding.btnFavorite, binding.root as ViewGroup)
 * 心形本体的缩放弹跳由调用方对锚点 View 做 animate()。
 */
class HeartBurstView(
    context: Context,
    private val cx: Float,
    private val cy: Float
) : View(context) {

    private var progress = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var accent = 0xFF6FB5F5.toInt()
    /** 粒子色板：默认全 accent，可注入主题三色循环 */
    private var burstColors = listOf(accent)
    private var animator: ValueAnimator? = null

    // 十个方向的迸发向量（px，按密度缩放）——环绕一圈 + 内层错落
    private val vectors = arrayOf(
        -30f to -22f, 28f to -26f, -34f to 10f, 32f to 16f, 2f to -36f,
        -16f to 28f, 18f to 30f, -38f to -8f, 40f to -6f, -6f to -40f
    )

    init {
        val tv = TypedValue()
        if (context.theme.resolveAttribute(com.example.s1client.R.attr.s1Accent, tv, true)) {
            accent = tv.data
        }
        paint.style = Paint.Style.FILL
    }

    /** 注入粒子色板（三色循环，对标 uiverse 原版） */
    fun setBurstColors(colors: List<Int>) {
        if (colors.isNotEmpty()) burstColors = colors
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (progress <= 0f || progress >= 1f) return
        val d = resources.displayMetrics.density
        val dist = 1f - (1f - progress) * (1f - progress)   // 缓出位移
        val alpha = (1f - progress).coerceIn(0f, 1f)
        val size = 7f * d * (1f - progress * 0.55f)
        vectors.forEachIndexed { i, (dx, dy) ->
            paint.color = burstColors[i % burstColors.size]
            paint.alpha = (255 * alpha).toInt()
            canvas.drawCircle(
                cx + dx * d * dist,
                cy + dy * d * dist,
                size * (if (i % 3 == 0) 1f else if (i % 3 == 1) 0.85f else 0.7f),
                paint
            )
        }
    }

    fun start(onEnd: () -> Unit = {}) {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600L
            interpolator = LinearInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    (parent as? ViewGroup)?.removeView(this@HeartBurstView)
                    onEnd()
                }
            })
            start()
        }
    }

    companion object {
        /**
         * 在锚点控件中心播放一次爆裂。
         * root 必须是能自由叠放子 View 的容器（CoordinatorLayout / FrameLayout）。
         */
        fun burstAt(anchor: View, root: ViewGroup) {
            val aLoc = IntArray(2).also(anchor::getLocationOnScreen)
            val rLoc = IntArray(2).also(root::getLocationOnScreen)
            val cx = (aLoc[0] - rLoc[0] + anchor.width / 2f).toFloat()
            val cy = (aLoc[1] - rLoc[1] + anchor.height / 2f).toFloat()
            val burst = HeartBurstView(anchor.context, cx, cy)
            // 控件 05：粒子用主题三色（对标原版）
            val p = com.example.s1client.ui.theme.s1PaletteFor(
                com.example.s1client.util.ThemeManager.currentTheme
            )
            burst.setBurstColors(p.flowCoverColors.take(3).map { it.toArgb() })
            root.addView(
                burst,
                ViewGroup.LayoutParams(root.width, root.height)
            )
            burst.start()
            // 心形本体：弹性放大回落（数字弹跳感）
            anchor.animate().scaleX(1.5f).scaleY(1.5f).setDuration(140)
                .withEndAction {
                    anchor.animate().scaleX(1f).scaleY(1f).setDuration(210).start()
                }.start()
        }
    }
}
