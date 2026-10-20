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
import androidx.recyclerview.widget.RecyclerView
import com.example.s1client.util.Haptic

/**
 * 回复成功「邮票落章」动画（2026-10-16）。
 *
 * 为什么是落章：论坛回复的语义本就是「往楼里盖一个章」，票据语言里也有票根/印花，
 * 所以"一张小票据盖到新楼层上"同时踩中内容语义与视觉语言，不需要另造隐喻。
 *
 * 时序（总时长约 1s，停留 620ms —— 用户反馈 220ms "没来得及看就消失"）：
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
    defStyleAttr: Int = 0,
    /** 章面主文字（默认「已回复」；读毕落章传「完」） */
    private val stampText: String = "已回复",
    /** 章面副文字（默认 POSTED；读毕落章传 READ TO THE END） */
    private val stampSub: String = "POSTED",
    /** 章面固有色：不传则跟随 s1Accent；读毕落章传朱红（印泥色恒定，与主题无关） */
    private val forceColor: Int? = null
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

    /** 高光扫光进度 0..1（只影响一道竖直亮带的位置）。
     *  必须是 public：playStamp 里 ObjectAnimator(this, "sweep") 走反射找 setSweep/getSweep，
     *  private 属性不生成访问器 → 扫光动画静默失效（用户实机反馈"动画效果变短了"的一部分）。 */
    var sweep = 0f

    private val path = Path()
    private val box = RectF()

    /** 刷新主题色（每次显示前调用，跟随当前主题；forceColor 优先） */
    fun refreshThemeColors() {
        if (forceColor != null) {
            accent = forceColor
            ink = Color.WHITE
            return
        }
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

        // 章面文字（主文字随字数缩放：单字「完」放大成印章体）
        textPaint.color = Color.WHITE
        textPaint.textSize = (if (stampText.length <= 2) 20f else 13f) * density
        canvas.drawText(stampText, box.centerX(), box.centerY() + textPaint.textSize / 3f, textPaint)
        textPaint.textSize = 8.5f * density
        textPaint.color = withAlpha(Color.WHITE, 200)
        canvas.drawText(stampSub, box.centerX(), box.centerY() + (if (stampText.length <= 2) 44f else 15f) * density / 2.2f, textPaint)

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
            // sweep 不是 View 属性，属性动画不会自动触发重绘，必须手动 invalidate
            addUpdateListener { invalidate() }
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
                    startDelay = 620
                    interpolator = AccelerateInterpolator()
                },
                ObjectAnimator.ofFloat(this@StampOverlayView, View.TRANSLATION_Y, 0f, -18f * density).apply {
                    duration = 200
                    startDelay = 620
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
 * 回复成功的落章动画：**居中**（2026-10-16，用户拍板 A 方案）。
 *
 * 之前取rv.getChildAt(0) 当锚点 —— 那只是"第一个可见楼层"，而新回复在页末，
 * 于是章落在页首/左上角（用户实机反馈"始终停在左上角"）。
 * 居中方案放弃定位逻辑：章永远出现在用户视线中心，100% 看得到、不会跑到角落。
 *
 * 权衡（已知并接受）：章会遮住新回复正文约1s。但回复后用户第一反应就是
 * "我发到哪了"，中央章恰好强化这个确认，且 1s 后淡出不残留。
 */
fun playStampOverlayOnNewPost(
    host: android.view.ViewGroup,
    rv: android.view.View?,
    newPid: String
) {
    val ctx = host.context
    val d = ctx.resources.displayMetrics.density
    val size = (104 * d).toInt()

    val stamp = StampOverlayView(ctx)
    // 居中机制（2026-10-16 二次修复，用户实机再次反馈"还是左上"）：
    // 上一版用带 margin 的 FrameLayout.LayoutParams 显式居中，日志里 margin 算对了
    // （left=384/top=848）但章仍贴左上 —— 根因是 host 是 CoordinatorLayout 时，
    // addView 会把不认识的 LayoutParams 转换成 CoordinatorLayout.LayoutParams，
    // 而 MarginLayoutParams(ViewGroup.LayoutParams) 构造器【不复制 margin】，边距全变 0。
    // 现在包一层全尺寸透明 FrameLayout，居中交给 FrameLayout 自己的 gravity 解析；
    // 全尺寸 wrap 的 LayoutParams 转换不丢 width/height，任何父容器都成立。
    // wrap 不吃触摸：非 clickable 的 ViewGroup 无子可消费事件时 dispatch 返回 false，
    // 触摸会穿透到下层列表，动画约 1.2s 的窗口期内浏览不受影响。
    val wrap = FrameLayout(ctx)
    wrap.addView(
        stamp,
        FrameLayout.LayoutParams(size, size, android.view.Gravity.CENTER)
    )
    host.addView(
        wrap,
        android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
    )
    stamp.post { stamp.playStamp { host.removeView(wrap) } }
}
