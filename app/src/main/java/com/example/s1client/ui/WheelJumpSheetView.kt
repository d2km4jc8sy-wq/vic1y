package com.example.s1client.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs
import androidx.compose.ui.graphics.toArgb

/**
 * 滚轮跳转面板（View 体系，方案 01 移植版 · 2026-10-15 定稿）。
 *
 * 结构（代码布局，无 XML）：
 * ```
 * LinearLayout(vertical)
 * ├─ grab 手柄
 * ├─ header：标题 tvTitle ｜ 状态 chip tvChip（滚轮停稳后长按 / 可长按跳转）
 * └─ FrameLayout
 *     ├─ band 中央选中带（软底色 + 描边）
 *     ├─ WheelPickerView（复用现有，自带 onSettled 停稳回调）
 *     └─ HoldJumpView 长按进度按钮（覆盖选中带：按住 1s 从左到右充满）
 * ```
 *
 * 交互：线性自由滑动 → 停稳（IDLE + 吸附）→「武装」→ 按住选中带 1000ms
 * （进度走满 + 震感逐级爬升）→ 跳转 [onJump]；松手中断；未停稳点按 → 提示。
 *
 * 首页（页码）与详情页（楼层）共用，数据源由 [setup] 决定。
 */
class WheelJumpSheetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    var onJump: ((Int) -> Unit)? = null
    var onRequestClose: (() -> Unit)? = null

    /** 无操作自动关闭（人类逻辑：停止操作后应自己收起） */
    var autoCloseMs: Long = 4000L
    private val autoCloseHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val autoCloseRunnable = Runnable { onRequestClose?.invoke() }
    private fun resetAutoClose() {
        autoCloseHandler.removeCallbacks(autoCloseRunnable)
        autoCloseHandler.postDelayed(autoCloseRunnable, autoCloseMs)
    }

    private val dp = resources.displayMetrics.density
    private var accentColor = 0xFF5E6AD2.toInt()
    private var accentSoft = 0x295E6AD2
    private var titleColor = 0xFFEDEEF2.toInt()
    private var subColor = 0xFF8A8F98.toInt()

    private var bandView: View? = null
    private lateinit var tvChip: TextView
    private lateinit var wheel: WheelPickerView
    private lateinit var holdView: HoldJumpView
    private var tvTitle: TextView? = null
    private var tvBrand: TextView? = null

    /** 票头金字：STAGE1 */
    private var brandColor = 0xFFFFD76E.toInt()



    /**
     * 色源架构修正（2026-10-15）：不再走 Context attr（主题切换不 recreate 会滞后），
     * 改由 Compose 层把 LocalS1Colors.current 的具体色值直接传入，重组即刷新。
     */
    fun setS1Colors(p: com.example.s1client.ui.theme.S1Colors) {
        val paper = s1SheetPaper(p)
        val title = s1SheetTitle(p)
        val sub = s1SheetSub(p)
        val brand = s1SheetBrand(p)
        val bandFill = s1SheetBandFill(p)
        val bandStroke = s1SheetBandStroke(p)
        brandColor = brand
        val gd = android.graphics.drawable.GradientDrawable().apply {
            setColor(paper)
            cornerRadii = floatArrayOf(
                22 * dp, 22 * dp,   // 顶左
                22 * dp, 22 * dp,   // 顶右
                0f, 0f,             // 底右（贴底直角，无缝）
                0f, 0f              // 底左
            )
        }
        background = gd
        titleColor = title
        subColor = sub
        tvTitle?.setTextColor(title)
        tvBrand?.setTextColor(brand)
        bandView?.background = roundedStroke(bandFill, bandStroke, 10f)
        holdView.setAccent(bandStroke, bandFill)
        wheel.setTextColors(bandStroke, sub)
        if (holdView.armed) {
            tvChip?.setTextColor(bandStroke)
            tvChip?.background = roundedStroke(bandFill, bandStroke, 99f)
        } else {
            tvChip?.setTextColor(sub)
        }
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(0xF2141517.toInt())
        val pad = (10 * dp).toInt()
        setPadding(0, pad, 0, (22 * dp).toInt())

        // 票据版顶部：打孔条（撕票孔，透出下层遮罩）→ grab → 撕裂虚线（左右半圆缺口）
        // grab 手柄
        addView(View(context).apply {
            layoutParams = LayoutParams((34 * dp).toInt(), (4 * dp).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            background = rounded(0x1FFFFFFF, 99f)
        })

        // header：STAGE1 金字票头 + 标题 + 状态 chip
        val header = android.widget.RelativeLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding((16 * dp).toInt(), (8 * dp).toInt(), (16 * dp).toInt(), (10 * dp).toInt())
        }
        val brandId = View.generateViewId()
        val brand = TextView(context).apply {
            id = brandId
            text = "STAGE1"
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            layoutParams = android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply { addRule(android.widget.RelativeLayout.ALIGN_PARENT_START) }
        }
        tvTitle = TextView(context).apply {
            text = "跳转到页"
            textSize = 12.5f
            setTextColor(titleColor)
            layoutParams = android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply { addRule(android.widget.RelativeLayout.END_OF, brandId) ; marginStart = (7 * dp).toInt() }
        }
        tvBrand = brand
        header.addView(brand)
        header.addView(tvTitle)
        tvChip = TextView(context).apply {
            text = "滚轮停稳后长按"
            textSize = 9.5f
            setTextColor(subColor)
            background = rounded(0x00000000, 99f)
            setPadding((9 * dp).toInt(), (3 * dp).toInt(), (9 * dp).toInt(), (3 * dp).toInt())
            layoutParams = android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply { addRule(android.widget.RelativeLayout.ALIGN_PARENT_END) }
        }
        header.addView(tvChip)
        addView(header)

        // 滚轮区：选中带 + WheelPickerView + HoldJumpView
        val wheelWrap = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (200 * dp).toInt())
        }
        bandView = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, (44 * dp).toInt(), Gravity.CENTER)
            background = roundedStroke(accentSoft, accentColor, 10f)
        }
        wheelWrap.addView(bandView)
        wheel = WheelPickerView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        wheelWrap.addView(wheel)
        holdView = HoldJumpView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, (44 * dp).toInt(), Gravity.CENTER)
        }
        wheelWrap.addView(holdView)
        addView(wheelWrap)
    }

    /** 外部主题变化后刷新强调色（可选调用） */
    fun setAccent(color: Int, soft: Int) {
        accentColor = color
        accentSoft = soft
        holdView.setAccent(color, soft)
        (wheelWrap() ?: return)
    }

    private fun wheelWrap(): FrameLayout? = getChildAt(childCount - 1) as? FrameLayout

    /**
     * 配置数据源与回调。
     * @param title 面板标题（首页「跳转到页」/ 详情页「跳转到楼层」）
     */
    fun setup(title: String, min: Int, max: Int, initial: Int, accent: Int, accentSoftColor: Int) {
        tvTitle?.text = title
        resetAutoClose()
        accentColor = accent
        accentSoft = accentSoftColor
        holdView.setAccent(accent, accentSoftColor)
        wheel.setup(min, max, initial)
        wheel.onValueChanged = { _, speed ->
            disarm()
            // 滚轮跨档 tick：档位感（用户定稿：中央选中带滚动时加震感）
            runCatching {
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    v?.vibrate(android.os.VibrationEffect.createPredefined(
                        android.os.VibrationEffect.EFFECT_TICK))
                } else @Suppress("DEPRECATION") v?.vibrate(10)
            }
            resetAutoClose()
        }
        wheel.onSettled = { v ->
            arm()
            resetAutoClose()
            // 原型规格 6：滚到顶/底给边界提示
            when (v) {
                wheel.minValue -> flashInfo("已到顶部 · ${wheel.minValue}")
                wheel.maxValue -> flashInfo("已到底部 · ${wheel.maxValue}")
            }
        }
        holdView.onTapWhileUnstable = {
            flashChip("滚轮还在滚动，停稳后才能长按")
            resetAutoClose()
        }
        holdView.onHoldStart = {
            resetAutoClose()
        }
        holdView.onHoldComplete = {
            tvChip?.text = "已跳转"
            onJump?.invoke(wheel.value)
        }
        // scrollToPosition 是即时布局（无滚动事件、不触发 onSettled），
        // 等 post 布局稳定后重新武装，避免初始 armed 被 onValueChanged 抢先 disarm
        post { arm() }
    }

    private fun arm() {
        holdView.armed = true
        tvChip?.text = "可长按跳转"
        tvChip?.setTextColor(accentColor)
        tvChip?.background = roundedStroke(accentSoft, accentColor, 99f)
    }

    private fun disarm() {
        holdView.armed = false
        tvChip?.text = "滚轮停稳后长按"
        tvChip?.setTextColor(subColor)
        tvChip?.background = rounded(0x00000000, 99f)
    }

    /** 中性提示（边界等）：accent 色 1.2s */
    private fun flashInfo(msg: String) {
        val keep = tvChip?.text
        val keepArmed = holdView.armed
        tvChip?.text = msg
        tvChip?.setTextColor(accentColor)
        tvChip?.background = roundedStroke(accentSoft, accentColor, 99f)
        holdView.armed = false
        val h = chipFlashT ?: android.os.Handler(android.os.Looper.getMainLooper()).also { chipFlashT = it }
        chipRestoreRunnable?.let(h::removeCallbacks)
        val r = Runnable {
            tvChip?.text = keep
            holdView.armed = keepArmed
            if (holdView.armed) {
                tvChip?.setTextColor(accentColor)
                tvChip?.background = roundedStroke(accentSoft, accentColor, 99f)
            } else {
                tvChip?.setTextColor(subColor)
                tvChip?.background = rounded(0x00000000, 99f)
            }
        }
        chipRestoreRunnable = r
        h.postDelayed(r, 1200)
    }

    private var chipFlashT: android.os.Handler? = null

    /** 提示闪回任务（2026-10-16 泄漏修复）：闭包持有 tvChip/holdView 即整棵 View 树，
     *  detach 后仍会跑完才释放 → 整张 Sheet 连同 Activity 被 Handler 挂住。
     *  改为具名 Runnable + [cancelPendingTimers] 统一取消。 */
    private var chipRestoreRunnable: Runnable? = null

    private fun flashChip(msg: String) {
        val keep = tvChip?.text
        tvChip?.text = msg
        tvChip?.setTextColor(0xFFE5484D.toInt())
        val h = chipFlashT ?: android.os.Handler(android.os.Looper.getMainLooper()).also { chipFlashT = it }
        chipRestoreRunnable?.let(h::removeCallbacks)
        val r = Runnable {
            tvChip?.text = keep
            if (holdView.armed) tvChip?.setTextColor(accentColor) else tvChip?.setTextColor(subColor)
        }
        chipRestoreRunnable = r
        h.postDelayed(r, 900)
    }

    /** View detach 时取消所有待执行定时任务（防 Handler 持有 View 树泄漏） */
    private fun cancelPendingTimers() {
        autoCloseHandler.removeCallbacks(autoCloseRunnable)
        val r = chipRestoreRunnable
        if (r != null) {
            chipFlashT?.removeCallbacks(r)
            chipRestoreRunnable = null
        }
    }

    override fun onDetachedFromWindow() {
        cancelPendingTimers()
        super.onDetachedFromWindow()
    }

    /** 滚轮当前值 */
    fun currentValue(): Int = wheel.value

    private var lastSetupTitle = ""
    private var lastSetupMax = -1
    private var lastSetupInitial = -1

    /** 数据源变化时重设（totalPages 异步到达）；同参幂等 */
    fun ensureSetup(title: String, min: Int, max: Int, initial: Int, accent: Int, accentSoftColor: Int) {
        if (lastSetupMax == max && lastSetupInitial == initial && lastSetupTitle == title) return
        lastSetupTitle = title
        lastSetupMax = max
        lastSetupInitial = initial
        setup(title, min, max, initial, accent, accentSoftColor)
    }

    private fun rounded(color: Int, r: Float) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color)
        cornerRadius = r * dp
    }

    /**
     * 主题自适应配色（宪法 18 条：禁止写死）。
     * 由调用方 resolve 主题 attr 后传入。
     */
    fun setThemeColors(
        bg: Int, title: Int, sub: Int,
        bandFill: Int, bandStroke: Int,
        wheelAccent: Int, wheelSecondary: Int
    ) {
        val gd = android.graphics.drawable.GradientDrawable().apply {
            setColor(bg)
            cornerRadii = floatArrayOf(22 * dp, 22 * dp, 22 * dp, 22 * dp, 0f, 0f, 0f, 0f) // 仅顶部圆角
        }
        background = gd
        titleColor = title
        subColor = sub
        tvTitle?.setTextColor(title)
        bandView?.background = roundedStroke(bandFill, bandStroke, 10f)
        holdView.setAccent(bandStroke, bandFill)
        wheel.setTextColors(wheelAccent, wheelSecondary)
        if (holdView.armed) {
            tvChip?.setTextColor(bandStroke)
            tvChip?.background = roundedStroke(bandFill, bandStroke, 99f)
        } else {
            tvChip?.setTextColor(sub)
        }
    }

    private fun roundedStroke(fill: Int, stroke: Int, r: Float) =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(fill)
            cornerRadius = r * dp
            setStroke((1 * dp).toInt(), stroke)
        }

    /**
     * 「按住 1s 才跳转」进度按钮：覆盖在滚轮选中带上（移植自交付包 HoldJumpButton，
     * 融合 S1 蓄力震感：220/440/660ms tick、1000ms 满震）。
     */
    inner class HoldJumpView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null
    ) : View(context, attrs) {

        var onHoldComplete: (() -> Unit)? = null
        var onTapWhileUnstable: (() -> Unit)? = null
        var onHoldStart: (() -> Unit)? = null

        var armed: Boolean = false

        private val holdMs = 1000L
        private val radius = 10f * dp
        private val strokeW = 1.5f * dp

        private val paintRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeW
        }
        private val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        private val rect = RectF()
        private var holding = false
        private var progress = 0f
        private var startMs = 0L

        fun setAccent(color: Int, soft: Int) {
            paintRing.color = color
            paintFill.color = soft
        }

        private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator

        private fun vib(ms: Long, amp: Int) {
            runCatching {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
                    vibrator?.hasAmplitudeControl() == true
                ) vibrator.vibrate(android.os.VibrationEffect.createOneShot(ms, amp))
                else {
                    @Suppress("DEPRECATION")
                    val vv = vibrator
                    vv?.vibrate(ms.toInt().coerceAtMost(60).toLong())
                }
            }
        }

        private val frameRunnable = object : Runnable {
            override fun run() {
                if (!holding) return
                val elapsed = System.currentTimeMillis() - startMs
                progress = (elapsed.toFloat() / holdMs).coerceIn(0f, 1f)
                // 震感爬升（S1 蓄力灵魂融合）
                when {
                    elapsed >= 660 && elapsed < 662 + 16 -> vib(30, 180)
                    elapsed >= 440 && elapsed < 442 + 16 -> vib(30, 140)
                    elapsed >= 220 && elapsed < 222 + 16 -> vib(30, 80)
                }
                if (progress >= 1f) {
                    holding = false
                    vib(45, 255) // 满震
                    onHoldComplete?.invoke()
                    postInvalidate()
                    return
                }
                postInvalidate()
                postOnAnimation(this)
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    if (!armed) {
                        onTapWhileUnstable?.invoke()
                        return true
                    }
                    if (holding) return true
                    holding = true
                    startMs = System.currentTimeMillis()
                    progress = 0f
                    onHoldStart?.invoke()
                    postOnAnimation(frameRunnable)
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (holding) {
                        holding = false
                        progress = 0f
                        postInvalidate()
                    }
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        override fun onDraw(canvas: Canvas) {
            val pad = strokeW
            rect.set(pad, pad, width - pad, height - pad)
            if (holding) {
                val fillW = rect.width() * progress
                canvas.save()
                canvas.clipRect(rect.left, rect.top, rect.left + fillW, rect.bottom)
                canvas.drawRoundRect(rect, radius, radius, paintFill)
                canvas.drawRoundRect(rect, radius, radius, paintRing)
                canvas.restore()
            }
        }
    }
}


/** 派生色辅助：向白混合 */
private fun sheetMixWhite(r: Float, g: Float, b: Float, f: Float): Triple<Float, Float, Float> = Triple(
    r + (1f - r) * f, g + (1f - g) * f, b + (1f - b) * f)

/** 跳转 Sheet 主题派生色（宪法 18 条：零写死，全从 S1Colors 取） */
fun s1SheetPaper(p: com.example.s1client.ui.theme.S1Colors): Int = p.glassDialog.toArgb()
fun s1SheetTitle(p: com.example.s1client.ui.theme.S1Colors): Int = p.textMain.toArgb()
fun s1SheetSub(p: com.example.s1client.ui.theme.S1Colors): Int = p.textSub.toArgb()
fun s1SheetBrand(p: com.example.s1client.ui.theme.S1Colors): Int = p.accent.toArgb()
fun s1SheetBandFill(p: com.example.s1client.ui.theme.S1Colors): Int =
    p.accent.copy(alpha = 0.16f).toArgb()
fun s1SheetBandStroke(p: com.example.s1client.ui.theme.S1Colors): Int =
    sheetMixWhite(p.accent.red, p.accent.green, p.accent.blue, 0.25f).let { (r, g, b) ->
        androidx.compose.ui.graphics.Color(r, g, b).toArgb()
    }
