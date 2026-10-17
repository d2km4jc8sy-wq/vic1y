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
import android.view.animation.OvershootInterpolator
import com.example.s1client.R

/**
 * 相机过片扳手刷新（实验功能，PULL_IMPL = 1）。
 *
 * 视觉与手感基准：设计者提供的《相机扳手.html》（2026-10-18，宪法 19：有 HTML 原型的
 * 自绘一次过；HTML 数值已锁定到 companion 常量并有单测锚点防回归）。
 *
 * 结构：右上角浮件——相机机身（52dp 圆角方）+ 镜头 + 过片扳手（38×8dp 金属条，右端为轴）。
 * 手势：向下拨动扳手（分档咔哒震感）→ 过 55° 发光提示「松手拍摄」→ 松手触发刷新
 * （快门组合震 + 扳手 300ms 回位 + onShutter 全屏白闪回调 + onRefresh）。
 *
 * 配色纪律（宪法 18）：
 * - 发光 / 提示字 / 镜面反光全部主题派生（s1Accent / s1TextTertiary，onDraw 自愈取色）；
 * - 机身与扳手为**拟物固有色**（中性深灰 + 金属银，等同桌面图标第 20 条例外逻辑：
 *   相机这个「物件」本来就是黑机身金属杆，深浅主题一致不产生主题间色彩差异），
 *   具名常量如下，改前先对照 HTML 原型。
 *
 * 状态权威（宪法 23.A.3）：revealed（滚动隐现）与 refreshing（曝光中）由 HomeFeed
 * 的 update lambda 权威回写，View 内部不自行猜测页面状态。
 *
 * 隐现动画在 View 边界内完成（宪法 23.B）：内容从 topPad 60dp 开始绘制，
 * 浮现位移 -58dp 全程不越界，alpha < 1 时不会被离屏层裁掉半截。
 */
class CameraLeverView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // ————————— HTML 基准常量（CameraLeverViewTest 锚点，勿随手改） —————————
    companion object {
        /** 扳手初始角（HTML BASE_ANGLE -15°） */
        const val BASE_ANGLE = -15f
        /** 拨到头的角度（HTML MAX_ANGLE 75°） */
        const val MAX_ANGLE = 75f
        /** 松手触发阈值（HTML THRESHOLD 55） */
        const val THRESHOLD_ANGLE = 55f
        /** 手指最大行程 dp（HTML MAX_PULL 130px） */
        const val MAX_PULL_DP = 130f
        /** 分档咔哒步长 dp（HTML tick = pull / 16） */
        const val TICK_STEP_DP = 16f

        /** 拉动距离 → 扳手角度（HTML: base + pull/max*(max-base)） */
        fun angleForPull(pullDp: Float, maxPullDp: Float = MAX_PULL_DP): Float =
            BASE_ANGLE + (pullDp.coerceIn(0f, maxPullDp) / maxPullDp) * (MAX_ANGLE - BASE_ANGLE)

        /** 当前咔哒档位（跨档给一次 selection 震感） */
        fun tickIndex(pullDp: Float, stepDp: Float = TICK_STEP_DP): Int =
            (pullDp / stepDp).toInt()

        /** 松手是否触发刷新 */
        fun shouldFire(angle: Float): Boolean = angle >= THRESHOLD_ANGLE

        // 布局（HTML：body 52 / lens 30 / lever 38×8 / zone 40×30 / widget 宽 62）
        /** 内容顶部缓冲：浮现动画位移 -58dp 的起点区，保证不越界（宪法 23.B） */
        const val TOP_PAD_DP = 60f
        /** 浮现/隐藏位移（HTML translateY -80px 过大，收进 View 界内） */
        const val REVEAL_SHIFT_DP = -58f
        const val BODY_DP = 52f
        const val BODY_RADIUS_DP = 14f
        const val LENS_DP = 30f
        const val LEVER_LEN_DP = 38f
        const val LEVER_H_DP = 8f
        const val KNOB_DP = 14f
        /** 提示字（HTML 9px 过小；宪法 15 可读性优先抬到 12sp） */
        const val HINT_SP = 12f
        /** View 建议尺寸：宽 62dp + 高 60(缓冲)+52(机身)+30(扳手)+~28(提示) */
        const val VIEW_W_DP = 62f
        const val VIEW_H_DP = 170f
    }

    // ————————— 拟物固有色（具名常量登记，见类注释宪法 20 例外依据） —————————
    private val bodyTop = 0xFF3A3A3C.toInt()      // 机身渐变亮端（HTML #3a3a3c）
    private val bodyBottom = 0xFF1A1A1C.toInt()   // 机身渐变暗端（HTML #1a1a1c）
    private val bodyEdge = 0x0DFFFFFF             // 机身描边 rgba(255,255,255,0.05)
    private val leverTop = 0xFFDDDDDD.toInt()      // 扳手金属亮端（HTML #ddd）
    private val leverMid = 0xFF999999.toInt()     // 金属中段（HTML #999）
    private val leverBottom = 0xFF555555.toInt()  // 金属暗端（HTML #555）
    private val knobHi = 0xFFFFFFFF.toInt()       // 圆头高光（HTML #fff）
    private val knobLo = 0xFF888888.toInt()       // 圆头暗部（HTML #888）
    private val ringOuter = 0xFF2A2A2A.toInt()     // 镜头外环（HTML #2a2a2a）
    private val ringInner = 0xFF111111.toInt()    // 镜头内环（HTML #111）

    // ————————— 主题派生色（resolveAttr，onDraw 自愈跟色） —————————
    private var accentColor = 0
    private var hintIdleColor = 0
    private fun resolveAttr(attr: Int): Int {
        val tv = android.util.TypedValue()
        context.theme.resolveAttribute(attr, tv, true)
        return tv.data
    }
    private fun refreshThemeColors() {
        val accent = resolveAttr(R.attr.s1Accent)
        val hintIdle = resolveAttr(R.attr.s1TextTertiary)
        if (accent != accentColor || hintIdle != hintIdleColor) {
            accentColor = accent
            hintIdleColor = hintIdle
            invalidate()
        }
    }

    // ————————— 回调 —————————
    /** 触发刷新（调用方负责滚回第一页并走刷新流程） */
    var onRefresh: (() -> Unit)? = null
    /** 快门瞬间回调（Compose 侧放全屏白闪 overlay） */
    var onShutter: (() -> Unit)? = null

    // ————————— 状态 —————————
    private var angle = BASE_ANGLE
    private var dragging = false
    private var startY = 0f
    private var lastTick = 0
    private var busy = false          // firing / exposing 期间锁手势
    private var revealed = false      // 由 HomeFeed update 回写（滚动隐现）
    private var exposing = false     // 曝光中（refreshing 回写）
    private var resetAnimator: ValueAnimator? = null

    private val density get() = resources.displayMetrics.density
    private val d get() = density    // 简写

    // ————————— 画笔 —————————
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val lensPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val leverPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
        isFakeBoldText = false
    }
    private val bodyRect = RectF()
    private val tmpRect = RectF()

    init {
        alpha = 0f
        translationY = REVEAL_SHIFT_DP * d
        hintPaint.textSize = HINT_SP * resources.displayMetrics.scaledDensity
        refreshThemeColors()
    }

    // ————————— 布局几何（dp，onDraw 里换算） —————————
    private val bodyTop0 get() = TOP_PAD_DP * d
    private val bodyCx get() = width / 2f
    private val bodyBottomY get() = bodyTop0 + BODY_DP * d
    /** 扳手轴心：机身中线 +20dp、机底 +8dp（HTML zone 右缘 = 中线+20，条中心 y = 机底+8） */
    private val leverPivotX get() = bodyCx + 20f * d
    private val leverPivotY get() = bodyBottomY + 8f * d
    private val hintBaseline get() = bodyBottomY + 28f * d + 12f * d

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        refreshThemeColors()
        val cx = bodyCx

        // ---- 机身：52dp 圆角 14，145° 深灰渐变 + 内高光描边 ----
        bodyRect.set(cx - BODY_DP / 2 * d, bodyTop0, cx + BODY_DP / 2 * d, bodyBottomY)
        bodyPaint.shader = LinearGradient(
            bodyRect.left, bodyRect.top, bodyRect.right, bodyRect.bottom,
            bodyTop, bodyBottom, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(bodyRect, BODY_RADIUS_DP * d, BODY_RADIUS_DP * d, bodyPaint)
        edgePaint.color = bodyEdge
        edgePaint.strokeWidth = 1f * d
        bodyRect.inset(0.5f * d, 0.5f * d)
        canvas.drawRoundRect(bodyRect, BODY_RADIUS_DP * d, BODY_RADIUS_DP * d, edgePaint)
        bodyRect.inset(-0.5f * d, -0.5f * d)

        // ---- 镜头：radial 玻璃 + 双环 + accent 镜面反光 ----
        val lensCx = cx
        val lensCy = (bodyTop0 + bodyBottomY) / 2f
        val lensR = LENS_DP / 2 * d
        lensPaint.shader = RadialGradient(
            lensCx - lensR * 0.3f, lensCy - lensR * 0.4f, lensR * 1.6f,
            intArrayOf(0xFF4A4A4A.toInt(), 0xFF1A1A1A.toInt(), 0xFF000000.toInt()),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawCircle(lensCx, lensCy, lensR, lensPaint)
        // 双环（HTML inset 0 0 0 3px #2a2a2a + 5px #111：外 3dp 灰环叠在内 5dp 黑环上）
        ringPaint.strokeWidth = 5f * d
        ringPaint.color = ringInner
        canvas.drawCircle(lensCx, lensCy, lensR - 2.5f * d, ringPaint)
        ringPaint.strokeWidth = 3f * d
        ringPaint.color = ringOuter
        canvas.drawCircle(lensCx, lensCy, lensR - 1.5f * d, ringPaint)
        // 镜面反光（HTML 红色反光点 → accent 派生，宪法 18）
        val glintR = 7f * d
        val glintPaint = glowPaint
        glintPaint.shader = RadialGradient(
            lensCx - 7f * d, lensCy - 8f * d, glintR,
            (accentColor and 0x00FFFFFF) or 0x66000000, (accentColor and 0x00FFFFFF),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(lensCx - 7f * d, lensCy - 8f * d, glintR, glintPaint)

        // ---- 就绪光晕（垫在扳手下层；HTML .ready box-shadow 0 0 20px accent 0.8） ----
        val ready = dragging && shouldFire(angle)
        if (ready || exposing) {
            val glowR = 20f * d
            glowPaint.shader = RadialGradient(
                leverPivotX - LEVER_LEN_DP / 2 * d, leverPivotY, glowR,
                (accentColor and 0x00FFFFFF) or 0x59000000,
                (accentColor and 0x00FFFFFF),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(leverPivotX - LEVER_LEN_DP / 2 * d, leverPivotY, glowR, glowPaint)
        }

        // ---- 扳手：绕右轴旋转的金属条 + 左端圆头 ----
        canvas.save()
        canvas.rotate(angle, leverPivotX, leverPivotY)
        val half = LEVER_H_DP / 2 * d
        tmpRect.set(leverPivotX - LEVER_LEN_DP * d, leverPivotY - half, leverPivotX, leverPivotY + half)
        leverPaint.shader = LinearGradient(
            0f, tmpRect.top, 0f, tmpRect.bottom,
            intArrayOf(leverTop, leverMid, leverBottom),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(tmpRect, half, half, leverPaint)
        // 圆头（HTML ::after：left:-3 top:-3 14×14，圆心 ≈ 条左端 +4dp）
        knobPaint.shader = RadialGradient(
            tmpRect.left + 4f * d, leverPivotY - 2f * d, KNOB_DP * d,
            knobHi, knobLo, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(tmpRect.left + 4f * d, leverPivotY, KNOB_DP / 2 * d, knobPaint)
        canvas.restore()

        // ---- 提示字：拨动过片 / 松手拍摄 / 曝光中... ----
        val hint = when {
            exposing -> "曝光中..."
            ready -> "松手拍摄"
            else -> "拨动过片"
        }
        hintPaint.color = if (ready || exposing) accentColor else hintIdleColor
        canvas.drawText(hint, cx, hintBaseline, hintPaint)
    }

    // ————————— 手势：只有扳手区消费触摸，其余穿透给下方卡片 —————————
    private fun hitLever(x: Float, y: Float): Boolean {
        // HTML zone 40×30（中心线±20dp、机底-6dp~+24dp），四周 +6dp 容差
        val slop = 6f * d
        return x >= bodyCx - 26f * d && x <= bodyCx + 26f * d &&
                y >= bodyBottomY - 6f * d - slop && y <= bodyBottomY + 28f * d + slop
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!revealed) return false                       // 隐藏态不拦截
        if (event.actionMasked == MotionEvent.ACTION_DOWN && !hitLever(event.x, event.y)) {
            return false                                  // 热区外穿透（票夹 hitTest 同款收紧）
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (busy) return false
                dragging = true
                startY = event.y
                lastTick = 0
                resetAnimator?.cancel()
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return false
                val pullDp = ((event.y - startY) / d).coerceAtLeast(0f)
                angle = angleForPull(pullDp)
                // 分档咔哒：跨档给一次 selection 震感（HTML vibrate(3) + tick 声；音效拍板不做）
                val tick = tickIndex(pullDp)
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
                if (event.actionMasked == MotionEvent.ACTION_UP && shouldFire(angle)) {
                    fire()
                } else {
                    springBack()
                }
                return true
            }
        }
        return false
    }

    /** 松手触发：快门震 + 扳手 300ms 回位 + 白闪回调 + 刷新回调 */
    private fun fire() {
        busy = true
        exposing = true
        // HTML vibrate[20,50,100] 快门节奏 ≈ SUCCESS「哒哒—嗒」组合震
        com.example.s1client.util.Haptic.success(context)
        onShutter?.invoke()
        resetAnimator?.cancel()
        resetAnimator = ValueAnimator.ofFloat(angle, BASE_ANGLE).apply {
            duration = 300L
            interpolator = DecelerateInterpolator()   // HTML 0.3s ease-out
            addUpdateListener {
                angle = it.animatedValue as Float
                invalidate()
            }
            start()
        }
        onRefresh?.invoke()
    }

    /** 未达标回弹：500ms 弹性（HTML 0.5s cubic-bezier(0.34,1.56,0.64,1)） */
    private fun springBack() {
        busy = true
        resetAnimator?.cancel()
        resetAnimator = ValueAnimator.ofFloat(angle, BASE_ANGLE).apply {
            duration = 500L
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener {
                angle = it.animatedValue as Float
                invalidate()
            }
            start()
        }
        postDelayed({ busy = false }, 300)
        invalidate()
    }

    // ————————— 权威回写入口（HomeFeed update lambda 调用） —————————

    /** 滚动隐现（Compose 侧滚动状态权威驱动；同值不动） */
    fun setRevealed(value: Boolean) {
        if (revealed == value) return
        revealed = value
        animate().cancel()
        if (value) {
            animate()
                .translationY(0f).alpha(1f)
                .setDuration(260L)
                .setInterpolator(OvershootInterpolator(1.1f))
                .start()
        } else {
            animate()
                .translationY(REVEAL_SHIFT_DP * d).alpha(0f)
                .setDuration(200L)
                .start()
        }
    }

    /** 刷新中回写（页面 isRefreshing 权威驱动；宪法 23.A.3——不回写=状态卡死） */
    fun setRefreshing(active: Boolean) {
        if (active) {
            exposing = true
            busy = true
            invalidate()
        } else if (exposing) {
            exposing = false
            // HTML 刷新完成：延迟 300ms 恢复提示并给一次完成震感（vibrate[8,30,8] ≈ light）
            postDelayed({
                busy = false
                com.example.s1client.util.Haptic.light(context)
                invalidate()
            }, 300)
            invalidate()
        }
    }

    override fun onDetachedFromWindow() {
        resetAnimator?.cancel()
        animate().cancel()
        super.onDetachedFromWindow()
    }
}
