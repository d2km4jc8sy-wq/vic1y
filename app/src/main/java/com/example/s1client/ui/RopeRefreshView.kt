package com.example.s1client.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import com.example.s1client.R
import kotlin.math.exp

/**
 * 绳子下拉刷新（矢量绘制，颜色跟随主题）。
 *
 * 样式：顶部导航垂下一根细绳，末端悬挂圆形加号把手（参考截图）。
 * 绳子全程直线；把手为主题强调色，页面底色描边环，加号用 onAccent。
 *
 * 手感：下拉变长、越拉越沉（指数阻尼，接近极限拉不动）→ 到极限震一下
 * → 松手（超过 65% 触发）弹性回弹 + 淡出 → 刷新期间把手旋转。
 */
import kotlin.math.cos
import kotlin.math.sin

class RopeRefreshView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /** 触发刷新（调用方负责回到第一页） */
    var onRefresh: (() -> Unit)? = null

    /** 细节 14：拉力比例上报（0f~1f），供页面做背景微亮等联动 */
    var onPullRatio: ((Float) -> Unit)? = null

    /** 控件 04：刷新时把手外三颗珠子的颜色（外部注入主题三色；缺省用 accent） */
    var orbitColors: List<Int> = emptyList()

    /** 本页滚动比例（0~1）：把手外环显示，回顶进度环迁入绳子 */
    var scrollRatio: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    /** 单击把手（非拖拽）= 回顶 */
    var onKnobClick: (() -> Unit)? = null
    private var downTime = 0L
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val density get() = resources.displayMetrics.density
    /** 静止时垂下的绳长（让把手悬在导航栏下方） */
    private val baseLen get() = 100f * density
    /** 最大可拉距离（指数阻尼的渐近值） */
    private val maxPull get() = 120f * density
    /** 松手触发刷新的阈值比例 */
    private val triggerRatio = 0.65f
    /** 把手半径 */
    private val knobR get() = 19f * density

    private val ropePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val knobEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val knobRect = RectF()

    private var ropeColor = 0
    private var accentColor = 0
    private var onAccentColor = 0
    private var pageColor = 0

    private var pull = 0f
    private var dragging = false
    private var startY = 0f
    private var reachedLimit = false
    /** 触发阈值（triggerRatio）是否已过：过线给一次"快到了"预备震 */
    private var triggerArmed = false
    private var busy = false
    private var refreshing = false
    private var spinDeg = 0f
    private var spinAnimator: ValueAnimator? = null

    init {
        refreshThemeColors()
    }

    /** 实时取色：Compose 侧在主题变化时调用（内部比对，色没变不重绘） */
    fun refreshThemeColors() {
        val rope = resolveAttr(R.attr.s1TextTertiary)
        val accent = resolveAttr(R.attr.s1Accent)
        val onAccent = resolveAttr(R.attr.s1TextOnAccent)
        val page = resolveAttr(R.attr.s1BgPage)
        if (rope != ropeColor || accent != accentColor ||
            onAccent != onAccentColor || page != pageColor
        ) {
            ropeColor = rope
            accentColor = accent
            onAccentColor = onAccent
            pageColor = page
            invalidate()
        }
    }

    private fun resolveAttr(attr: Int): Int {
        val tv = TypedValue()
        context.theme.resolveAttribute(attr, tv, true)
        return tv.data
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == GONE) {
            // 视图隐藏时彻底停掉旋转动画，避免再次浮现时仍在转
            stopSpin()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 自愈取色：每次绘制比对主题属性，切主题即时跟色（resolveAttr 微秒级，无感知开销）
        refreshThemeColors()
        val cx = width / 2f
        val knobCy = baseLen + pull + knobR
        val knobTop = knobCy - knobR

        // ---- 绳：直线，从顶部连到把手顶端 ----
        val ratio = (pull / maxPull).coerceIn(0f, 1f)
        ropePaint.color = ropeColor
        ropePaint.strokeWidth = (2f + 1.2f * ratio) * density
        canvas.drawLine(cx, 0f, cx, knobTop, ropePaint)

        // ---- 把手：圆形，主题强调色，页面底色描边环 ----
        knobRect.set(cx - knobR, knobCy - knobR, cx + knobR, knobCy + knobR)
        // 空心把手（用户拍板：删掉内部咖啡色实心）：
        // 圆面 = 页面底色融入背景，描边圈与箭头/进度环全部用 accent
        knobPaint.color = pageColor
        knobEdgePaint.color = accentColor
        knobEdgePaint.strokeWidth = 2f * density
        canvas.drawCircle(cx, knobCy, knobR, knobPaint)
        canvas.drawCircle(cx, knobCy, knobR - 1.2f * density, knobEdgePaint)

        // 回顶进度环：迁入把手外圈（原右下回顶钮的环），滚过才可见
        if (scrollRatio > 0.01f && !refreshing) {
            ringPaint.color = accentColor
            ringPaint.strokeWidth = 2.5f * density
            val rr = knobR + 4f * density
            val rect = RectF(cx - rr, knobCy - rr, cx + rr, knobCy + rr)
            canvas.drawArc(rect, -90f, 360f * scrollRatio.coerceIn(0f, 1f), false, ringPaint)
        }

        // ---- 把手内部：刷新时旋转圆环，平时加号 ----
        iconPaint.color = accentColor
        iconPaint.strokeWidth = 2.5f * density

        if (refreshing) {
            // 控件 04：三颗主题色珠子沿把手外轨道追逐（120° 相位差，原版还原）
            val orbitR = knobR * 1.15f
            val phases = floatArrayOf(0f, 120f, 240f)
            val cols = if (orbitColors.size >= 3) orbitColors else listOf(accentColor, accentColor, accentColor)
            for (i in phases.indices) {
                val rad = Math.toRadians(((spinDeg + phases[i]) - 90f).toDouble())
                val dx = (orbitR * cos(rad)).toFloat()
                val dy = (orbitR * sin(rad)).toFloat()
                iconPaint.color = cols[i]
                iconPaint.alpha = 255
                canvas.drawCircle(cx + dx, knobCy + dy, 3.4f * density, iconPaint)
            }
            iconPaint.alpha = 255
        } else {
            // 待命态：单线下拉箭头（↓）替代「+」——「+」会被误读成发帖；
            // 拉过触发线（triggerArmed）后箭头翻转 180° 变 ↑，示意「松手即刷新」
            val r = knobR * 0.45f
            canvas.save()
            if (triggerArmed) canvas.rotate(180f, cx, knobCy)
            canvas.drawLine(cx, knobCy - r, cx, knobCy + r * 0.55f, iconPaint)
            canvas.drawLine(cx - r * 0.6f, knobCy, cx, knobCy + r * 0.55f, iconPaint)
            canvas.drawLine(cx + r * 0.6f, knobCy, cx, knobCy + r * 0.55f, iconPaint)
            canvas.restore()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (busy) return false
                dragging = true
                reachedLimit = false
                triggerArmed = false
                downTime = android.os.SystemClock.elapsedRealtime()
                startY = event.y
                onPullRatio?.invoke(0f)
                parent?.requestDisallowInterceptTouchEvent(true)
                animate().alpha(1f).setDuration(80).start()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return false
                val raw = (event.y - startY).coerceAtLeast(0f)
                pull = (maxPull * (1f - exp(-raw / maxPull))).coerceIn(0f, maxPull)
                // 细节 11：两段式张力震感 —— 过触发阈值（快到了）SELECTION，
                // 拉到极限（到底了）LIGHT。震感跟着绳子张力走。
                if (!triggerArmed && pull >= maxPull * triggerRatio) {
                    triggerArmed = true
                    runCatching { com.example.s1client.util.Haptic.selection(context) }
                }
                if (!reachedLimit && pull >= maxPull * 0.96f) {
                    reachedLimit = true
                    limitHaptic()
                }
                onPullRatio?.invoke(pull / maxPull)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!dragging) return false
                dragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
                // 回顶合并：几乎没拉 + 按得短 = 单击把手 → 回顶
                val isTap = pull < maxPull * 0.08f &&
                        android.os.SystemClock.elapsedRealtime() - downTime < 260L
                if (isTap && !busy && !refreshing) onKnobClick?.invoke()
                springBack(pull >= maxPull * triggerRatio)
                return true
            }
        }
        return false
    }

    /** 回弹：过冲弹性；触发刷新时先回弹再淡出，并转起来 */
    private fun springBack(fire: Boolean) {
        busy = true
        if (!fire) {
            refreshing = false
            stopSpin()
        }
        ValueAnimator.ofFloat(pull, 0f).apply {
            duration = if (fire) 260L else 200L
            interpolator = OvershootInterpolator(1.4f)
            addUpdateListener {
                pull = it.animatedValue as Float
                onPullRatio?.invoke(pull / maxPull)
                invalidate()
            }
        }.start()

        if (fire) {
            refreshing = true
            runCatching { com.example.s1client.util.Haptic.light(context) }
            startSpin()
            animate().alpha(0f).setDuration(240).withEndAction {
                onRefresh?.invoke()
                // 刷新需要点时间，之后把绳子淡回来
                postDelayed({
                    refreshing = false
                    stopSpin()
                    animate().alpha(IDLE_ALPHA).setDuration(200).start()
                    busy = false
                }, 2000)
            }.start()
        } else {
            postDelayed({ busy = false }, 220)
        }
    }

    private fun startSpin() {
        stopSpin()
        spinAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 900L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                spinDeg = it.animatedValue as Float
                invalidate()
            }
        }
        spinAnimator?.start()
    }

    private fun stopSpin() {
        spinAnimator?.cancel()
        spinAnimator = null
        spinDeg = 0f
    }

    /** 拉到极限：一下提示震动（走 Haptic 统一门闸，尊重全局开关） */
    private fun limitHaptic() {
        runCatching { com.example.s1client.util.Haptic.light(context) }
    }

    companion object {
        /** 静止时的半透明程度 */
        const val IDLE_ALPHA = 0.75f
    }
}
