package com.example.s1client.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.example.s1client.R

/**
 * 相机过片扳手刷新（实验功能，PULL_IMPL = 1）。
 *
 * **2026-10-18 22:30 重做**：几何与素材整体换成设计者提供的《相机下拉.html》SVG，
 * 只取相机与扳手，不取胶片帘幕 / FRAME 计数 / 提示字等动画元素。
 *
 * ### 为什么要推倒重来（旧版的两个结构性问题）
 * 1. **不连相机**：旧版转轴在 `bodyBottom + 8dp`，即机身轮廓**之外**，
 *    金属杆和机身之间天然有一道缝，看着像外挂的。
 * 2. **往下拉杆却往上走**：旧版球头画在转轴**左侧**，绕右侧轴向下旋转时，
 *    左侧点必然先上升——这是几何决定的，不是动画问题（用户反馈「违和感」）。
 *
 * ### 现在的几何（viewBox 100 × 132，单位 = vb）
 * - 机身：矩形 (14,38,56,62) 圆角 13，镜头在下半部 (42,72)
 * - 转轴：**(58,50)，在机身内部** —— 杆与机身连成一体
 * - 球头：**在转轴右侧**，半径 32，0° 时在 (90,50)，90° 时在 (58,82)，走纯向下的弧
 *
 * ### 触摸模型：球头跟手
 * 角度不再由「下拉多少 dp」换算，而是**由球头当前坐标反算**（atan2），手指到哪球头到哪。
 * 起手热区跟着球头走（[HIT_BALL]）或转轴附近（[HIT_AXIS]），不用按看不见的地方。
 * 松手判定两条：过 [THRESHOLD_ANGLE]，或甩动速度够快（[FLICK_DEG_PER_MS]）直接到位。
 *
 * 配色纪律（宪法 18）：就绪弧、球头到位色取 s1Accent（onDraw 自愈跟色）；
 * 机身与扳手为**拟物固有色**（黑机身 + 金属杆），等同桌面图标第 20 条例外逻辑。
 *
 * 状态权威（宪法 23.A.3）：revealed（滚动隐现）与 refreshing（曝光中）由 HomeFeed 回写。
 */
class CameraLeverView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        // ————————— 画布基准（CameraLeverViewTest 锚点） —————————
        /** viewBox 宽（vb 单位），几何常量都以它为参照 */
        const val VB_W = 100f
        /** viewBox 高（vb 单位） */
        const val VB_H = 132f
        /** View 建议宽度 dp；高度按比例推出 */
        const val VIEW_W_DP = 88f

        /** 转轴（机身内） */
        const val PIVOT_X = 58f
        const val PIVOT_Y = 50f
        /** 球头回转半径 */
        const val BALL_R = 32f
        /** 起始角：扳手水平指向右 */
        const val BASE_ANGLE = 0f
        /** 到底角：扳手垂直指向下 */
        const val MAX_ANGLE = 90f
        /** 松手触发阈值（2026-10-18 22:58 由 52 放宽到 45：实测「有些难操作」） */
        const val THRESHOLD_ANGLE = 45f
        /** 甩动触发：角速度（度/ms）超过此值且已离开起点即到位 */
        const val FLICK_DEG_PER_MS = 1.2f
        /** 甩动判定的最小起角（防止原地轻抖也算甩） */
        const val FLICK_MIN_ANGLE = 18f
        /** 分档咔哒步长（度），跨档给一次 selection 震感 */
        const val TICK_STEP_DEG = 15f
        /** 起手热区：球头半径 vb（22:58 由 28 放宽到 34，配合控件放大，手指更好抓） */
        const val HIT_BALL = 34f
        /** 起手热区：转轴附近半径 vb（46 → 54：拇指不必精确命中） */
        const val HIT_AXIS = 54f
        /** 浮现/隐藏位移 dp */
        const val REVEAL_SHIFT_DP = -56f

        /** 建议 View 高度 dp（按 viewBox 比例） */
        val VIEW_H_DP: Float = VB_H * (VIEW_W_DP / VB_W)

        /** 角度钳制 */
        fun clampAngle(a: Float): Float = a.coerceIn(BASE_ANGLE, MAX_ANGLE)

        /**
         * 球头坐标 → 扳手角度（atan2 反算，1:1 跟手）。
         * 指针落在转轴左侧时按上下半区给端点值，避免 atan2 在 ±180° 抖动。
         */
        fun angleForPoint(px: Float, py: Float): Float {
            val dx = px - PIVOT_X
            val dy = py - PIVOT_Y
            if (dx < 0f) return if (dy >= 0f) MAX_ANGLE else BASE_ANGLE
            return clampAngle(Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat())
        }

        /** 角度 → 球头坐标（与 angleForPoint 互逆） */
        fun ballPosForAngle(angle: Float): Pair<Float, Float> {
            val r = Math.toRadians(clampAngle(angle).toDouble())
            return Pair(
                PIVOT_X + BALL_R * Math.cos(r).toFloat(),
                PIVOT_Y + BALL_R * Math.sin(r).toFloat()
            )
        }

        /** 球头是否在起手热区内 */
        fun hitTest(px: Float, py: Float, currentAngle: Float): Boolean {
            val b = ballPosForAngle(currentAngle)
            val dBall = Math.hypot((px - b.first).toDouble(), (py - b.second).toDouble())
            if (dBall <= HIT_BALL) return true
            val dAxis = Math.hypot((px - PIVOT_X).toDouble(), (py - PIVOT_Y).toDouble())
            return dAxis <= HIT_AXIS
        }

        /** 松手是否触发（角度维度；速度维度在 onTouchEvent 里判） */
        fun shouldFire(angle: Float): Boolean = angle >= THRESHOLD_ANGLE

        /** 当前咔哒档位 */
        fun tickIndex(angle: Float, stepDeg: Float = TICK_STEP_DEG): Int =
            (clampAngle(angle) / stepDeg).toInt()
    }

    // ————————— 拟物固有色（宪法 20 例外） —————————
    private val bodyTop = 0xFF3F3F44.toInt()
    private val bodyMid = 0xFF232327.toInt()
    private val bodyBottom = 0xFF131315.toInt()
    private val bodyEdge = 0xFF4A4A4E.toInt()
    private val decoDark = 0xFF2C2C30.toInt()
    private val decoLight = 0xFF3A3A3F.toInt()
    private val lensRingOuter = 0xFF55555A.toInt()
    private val lensRingInner = 0xFF2E2E33.toInt()
    private val lensInner = 0xFF0A0A0C.toInt()
    private val lensRingDeep = 0xFF38383D.toInt()
    private val leverTop = 0xFFF4F4F4.toInt()
    private val leverMid = 0xFFC4C4C4.toInt()
    private val leverLow = 0xFF8D8D8D.toInt()
    private val leverBottom = 0xFF484848.toInt()
    private val knurl = 0xFF707070.toInt()
    private val ballHi = 0xFFFFFFFF.toInt()
    private val ballMid = 0xFFD4D4D4.toInt()
    private val ballLo = 0xFF6F6F6F.toInt()
    private val ballEdge = 0xFF5A5A5A.toInt()
    private val axisOuter = 0xFF2A2A2E.toInt()
    private val axisRing = 0xFF55555A.toInt()
    private val axisDot = 0xFF77777C.toInt()
    private val trackBase = 0xFF3A3A3F.toInt()

    // ————————— 状态 —————————
    private var angle = BASE_ANGLE
    private var dragging = false
    private var busy = false
    private var revealed = false
    private var exposing = false
    private var resetAnimator: ValueAnimator? = null
    private var lastTick = 0
    private var samples = ArrayList<FloatArray>()   // [timeMs, angle]

    private val density get() = resources.displayMetrics.density
    /** vb 单位 → px */
    private var s = 1f

    // ————————— 画笔 —————————
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rect = RectF()
    private val arcRect = RectF()

    private var accentColor = 0
    private fun refreshThemeColors(): Boolean {
        val tv = android.util.TypedValue()
        context.theme.resolveAttribute(R.attr.s1Accent, tv, true)
        val a = tv.data
        if (a != accentColor) { accentColor = a; return true }
        return false
    }

    var onRefresh: (() -> Unit)? = null

    init {
        alpha = 0f
        translationY = REVEAL_SHIFT_DP * density
        refreshThemeColors()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0) s = w / VB_W
    }

    private fun mix(c1: Int, c2: Int, t: Float): Int {
        fun ch(shift: Int) = (((c1 shr shift) and 0xFF) * (1 - t) + (((c2 shr shift) and 0xFF) * t)).toInt()
        return (ch(24) shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (refreshThemeColors()) invalidate()
        if (width == 0) return

        canvas.save()
        canvas.scale(s, s)
        if (accentColor == 0) refreshThemeColors()

        val ready = angle >= THRESHOLD_ANGLE

        // ---- 机身：竖长圆角矩形 + 顶部装饰 ----
        rect.set(14f, 38f, 70f, 100f)
        fill.shader = LinearGradient(
            14f, 38f, 50f, 100f,
            intArrayOf(bodyTop, bodyMid, bodyBottom), null, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, 13f, 13f, fill)
        fill.shader = null
        stroke.strokeWidth = 1f
        stroke.color = bodyEdge
        canvas.drawRoundRect(rect, 13f, 13f, stroke)

        fill.color = decoDark; canvas.drawRoundRect(21f, 43f, 41f, 46.5f, 1.75f, 1.75f, fill)
        fill.color = decoLight; canvas.drawRoundRect(45f, 43f, 54f, 46.5f, 1.75f, 1.75f, fill)

        // ---- 镜头：同心圆组 + 斜向高光 ----
        stroke.strokeWidth = 2f; stroke.color = lensRingOuter
        canvas.drawCircle(42f, 72f, 19f, stroke)
        fill.shader = RadialGradient(36f, 66f, 30f,
            intArrayOf(0xFF4D4D55.toInt(), 0xFF1B1B1F.toInt(), 0xFF050506.toInt()),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(42f, 72f, 17f, fill)
        fill.shader = null
        stroke.strokeWidth = 1.5f; stroke.color = lensRingInner
        canvas.drawCircle(42f, 72f, 11f, stroke)
        fill.color = lensInner
        canvas.drawCircle(42f, 72f, 7f, fill)
        stroke.strokeWidth = 1f; stroke.color = lensRingDeep
        canvas.drawCircle(42f, 72f, 7f, stroke)
        fill.color = 0x24FFFFFF
        canvas.save()
        canvas.rotate(-30f, 37f, 66f)
        rect.set(32f, 63.2f, 42f, 68.8f)
        canvas.drawOval(rect, fill)
        canvas.restore()

        // ---- 轨迹弧：底弧 + 就绪进度（accent） ----
        arcRect.set(PIVOT_X - BALL_R, PIVOT_Y - BALL_R, PIVOT_X + BALL_R, PIVOT_Y + BALL_R)
        stroke.strokeWidth = 1.5f
        stroke.color = trackBase
        stroke.strokeCap = Paint.Cap.ROUND
        canvas.drawArc(arcRect, 0f, MAX_ANGLE, false, stroke)
        if (angle > 0.5f) {
            stroke.strokeWidth = 2.5f
            stroke.color = accentColor
            canvas.drawArc(arcRect, 0f, angle, false, stroke)
        }

        // ---- 扳手组：绕轴 (58,50) 旋转 ----
        canvas.save()
        canvas.rotate(angle, PIVOT_X, PIVOT_Y)

        rect.set(54f, 46.5f, 88f, 53.5f)
        fill.shader = LinearGradient(0f, 46.5f, 0f, 53.5f,
            intArrayOf(leverTop, leverMid, leverLow, leverBottom),
            floatArrayOf(0f, 0.42f, 0.58f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, 3.5f, 3.5f, fill)
        fill.shader = null
        stroke.strokeWidth = 0.9f; stroke.color = knurl; stroke.strokeCap = Paint.Cap.BUTT
        for (x in floatArrayOf(71f, 75f, 79f)) canvas.drawLine(x, 47.4f, x, 52.6f, stroke)

        // 球头：过阈值时向 accent 靠一档，给「到位」视觉
        val bMid = if (ready) mix(ballMid, accentColor, 0.55f) else ballMid
        val bLo = if (ready) mix(ballLo, accentColor, 0.35f) else ballLo
        fill.shader = RadialGradient(87.5f, 47.5f, 9f,
            intArrayOf(ballHi, bMid, bLo), floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(90f, 50f, 6.5f, fill)
        fill.shader = null
        stroke.strokeWidth = 0.8f; stroke.color = ballEdge
        canvas.drawCircle(90f, 50f, 6.5f, stroke)

        fill.color = axisOuter
        canvas.drawCircle(PIVOT_X, PIVOT_Y, 6f, fill)
        stroke.strokeWidth = 1.2f; stroke.color = axisRing
        canvas.drawCircle(PIVOT_X, PIVOT_Y, 6f, stroke)
        fill.color = axisDot
        canvas.drawCircle(PIVOT_X, PIVOT_Y, 2f, fill)

        canvas.restore()
        canvas.restore()
    }

    // ————————— 手势：球头跟手 —————————
    private fun toVb(x: Float, y: Float): Pair<Float, Float> =
        Pair(x / s, y / s)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!revealed) return false
        val (vx, vy) = toVb(event.x, event.y)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (busy) return false
                if (!hitTest(vx, vy, angle)) return false      // 热区外穿透给下方卡片
                dragging = true
                lastTick = 0
                samples = ArrayList()
                samples.add(floatArrayOf(System.currentTimeMillis().toFloat(), angle))
                resetAnimator?.cancel()
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return false
                angle = angleForPoint(vx, vy)
                val now = System.currentTimeMillis()
                samples.add(floatArrayOf(now.toFloat(), angle))
                if (samples.size > 6) samples.removeAt(0)
                val tick = tickIndex(angle)
                if (tick != lastTick) {
                    lastTick = tick
                    com.example.s1client.util.Haptic.selection(context)
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!dragging) return false
                dragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
                // 甩动判定：取窗口内首尾角速度
                var flick = false
                if (samples.size >= 2) {
                    val f = samples[0]; val l = samples[samples.size - 1]
                    val dt = (l[0] - f[0]).coerceAtLeast(1f)
                    val v = Math.abs((l[1] - f[1]).toDouble() / dt.toDouble())
                    flick = v >= FLICK_DEG_PER_MS && angle >= FLICK_MIN_ANGLE
                }
                val cancelled = event.actionMasked == MotionEvent.ACTION_CANCEL
                if (!cancelled && (flick || shouldFire(angle))) fire(flick) else springBack()
                return true
            }
        }
        return false
    }

    /** 触发：快门组合震 → 扳手 200ms 到底 → 回位 → 刷新回调 */
    private fun fire(flick: Boolean) {
        busy = true
        com.example.s1client.util.Haptic.success(context)
        resetAnimator?.cancel()
        val snapshot = if (flick) MAX_ANGLE else angle
        resetAnimator = ValueAnimator.ofFloat(snapshot, MAX_ANGLE).apply {
            duration = 200L
            interpolator = DecelerateInterpolator()
            addUpdateListener { angle = it.animatedValue as Float; invalidate() }
            start()
        }
        onRefresh?.invoke()
        // 到头后回位（不等刷新结束：回位是手感，刷新是业务）
        postDelayed({
            resetAnimator?.cancel()
            resetAnimator = ValueAnimator.ofFloat(MAX_ANGLE, BASE_ANGLE).apply {
                duration = 260L
                interpolator = DecelerateInterpolator()
                addUpdateListener { angle = it.animatedValue as Float; invalidate() }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) { busy = false }
                })
                start()
            }
        }, 220L)
    }

    /** 未达标回弹：420ms，末尾解锁 */
    private fun springBack() {
        busy = true
        resetAnimator?.cancel()
        resetAnimator = ValueAnimator.ofFloat(angle, BASE_ANGLE).apply {
            duration = 420L
            interpolator = DecelerateInterpolator()
            addUpdateListener { angle = it.animatedValue as Float; invalidate() }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) { busy = false }
            })
            start()
        }
    }

    // ————————— 权威回写入口（HomeFeed 调用） —————————
    fun setRevealed(value: Boolean) {
        if (revealed == value) return
        revealed = value
        animate().cancel()
        if (value) {
            animate().translationY(0f).alpha(1f)
                .setDuration(260L)
                .setInterpolator(android.view.animation.OvershootInterpolator(1.1f))
                .start()
        } else {
            animate().translationY(REVEAL_SHIFT_DP * density).alpha(0f)
                .setDuration(200L)
                .start()
        }
    }

    fun setRefreshing(active: Boolean) {
        if (active) {
            exposing = true
            busy = true
            invalidate()
        } else if (exposing) {
            exposing = false
            postDelayed({
                busy = false
                com.example.s1client.util.Haptic.light(context)
                invalidate()
            }, 240)
            invalidate()
        }
    }

    override fun onDetachedFromWindow() {
        resetAnimator?.cancel()
        animate().cancel()
        super.onDetachedFromWindow()
    }
}