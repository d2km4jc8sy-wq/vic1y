package com.example.s1client.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.TextView
import com.example.s1client.R
import kotlin.math.abs

/**
 * 贴边页码浮条（可复用组件）：窄滚轮选页 + 右侧总页数标注。
 *
 * 交互：
 * - 滚动滚轮选页，每跨一页轻震一下
 * - 长按（~400ms）= 确认跳转，触发一次明显重震
 * - 无操作 [autoHideDelayMs] 毫秒自动淡出；跳转后也淡出
 * - 再次呼出恢复上次停留的页数
 *
 * 用法：
 * ```
 * edgePageBar.onConfirm = { page -> ... }
 * edgePageBar.show(currentPage, totalPages)
 * ```
 */
class EdgePageBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val wheel: WheelPickerView
    private val tvTotal: TextView

    /** 长按确认：跳到选定页 */
    var onConfirm: ((Int) -> Unit)? = null

    /** 浮条自己消失时回调（超时/确认后），方便调用方同步状态 */
    var onDismiss: (() -> Unit)? = null

    /** 无操作自动消失的延时 */
    var autoHideDelayMs = 2500L

    private var totalPages = 1
    /** 上次浮条消失时停留的页数（下次呼出恢复） */
    private var lastStayPage = -1

    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hide() }
    private val longPressRunnable = Runnable { fireLongPress() }
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    private var longPressFired = false
    private var downX = 0f
    private var downY = 0f

    init {
        LayoutInflater.from(context).inflate(R.layout.view_edge_page_bar, this, true)
        wheel = findViewById(R.id.edgeWheel)
        tvTotal = findViewById(R.id.tvEdgeTotal)

        val accent = resolveAttr(R.attr.s1Accent)
        val secondary = resolveAttr(R.attr.s1TextSecondary)
        wheel.setTextColors(accent, secondary)
    }

    private fun resolveAttr(attr: Int): Int {
        val tv = TypedValue()
        context.theme.resolveAttribute(attr, tv, true)
        return tv.data
    }

    /** 展开浮条；优先恢复上次停留的页数 */
    fun show(current: Int, total: Int) {
        totalPages = total.coerceAtLeast(1)
        val start = if (lastStayPage in 1..totalPages) lastStayPage
                    else current.coerceIn(1, totalPages)
        tvTotal.text = "$totalPages"
        wheel.setup(1, totalPages, start)
        wheel.onValueChanged = { _, _ ->
            tickHaptic()
            resetHideTimer()
        }
        visibility = View.VISIBLE
        alpha = 0f
        animate().alpha(1f).setDuration(160).start()
        resetHideTimer()
    }

    /** 收起浮条；记住当前停留页供下次恢复 */
    fun hide() {
        handler.removeCallbacks(hideRunnable)
        handler.removeCallbacks(longPressRunnable)
        val wasVisible = visibility == VISIBLE
        if (wasVisible) lastStayPage = wheel.value
        if (!wasVisible) return
        animate().alpha(0f).setDuration(200).withEndAction {
            visibility = GONE
            onDismiss?.invoke()
        }.start()
    }

    private fun resetHideTimer() {
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, autoHideDelayMs)
    }

    /**
     * 长按检测放在事件分发层：只观察、不消费，滚轮照常响应滚动手势。
     * （如果在这里拦截事件，滚轮就滚不动了）
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                longPressFired = false
                downX = ev.rawX
                downY = ev.rawY
                // 关键：告诉外层滚动容器别抢这次手势。
                // 否则手指在滚轮上滑时，下面的瀑布流/列表会跟着一起滚（尤其在首页，
                // Compose 的滚动容器会截走滚轮边界外的滑动）
                parent?.requestDisallowInterceptTouchEvent(true)
                handler.postDelayed(longPressRunnable, 400L)
                resetHideTimer()
            }
            MotionEvent.ACTION_MOVE -> {
                if (!longPressFired &&
                    (abs(ev.rawX - downX) > touchSlop || abs(ev.rawY - downY) > touchSlop)) {
                    // 手指动了 → 判定为滚动滚轮，取消长按
                    handler.removeCallbacks(longPressRunnable)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                parent?.requestDisallowInterceptTouchEvent(false)
                resetHideTimer()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun fireLongPress() {
        if (longPressFired || visibility != VISIBLE) return
        longPressFired = true
        heavyHaptic()
        lastStayPage = wheel.value
        val page = wheel.value
        hide()
        onConfirm?.invoke(page)
    }

    // ============ 震动（两级） ============

    private var lastTickMs = 0L

    /** 翻页：轻脆 tick */
    private fun tickHaptic() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastTickMs < 30) return
        lastTickMs = now
        runCatching {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE)
                    as? android.os.Vibrator ?: return
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                v.vibrate(android.os.VibrationEffect.createPredefined(
                    android.os.VibrationEffect.EFFECT_TICK))
            } else if (v.hasAmplitudeControl()) {
                v.vibrate(android.os.VibrationEffect.createOneShot(10, 140))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(10)
            }
        }
    }

    /** 长按确认：明显更重的一下 */
    private fun heavyHaptic() {
        runCatching {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE)
                    as? android.os.Vibrator ?: return
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                v.vibrate(android.os.VibrationEffect.createPredefined(
                    android.os.VibrationEffect.EFFECT_HEAVY_CLICK))
            } else if (v.hasAmplitudeControl()) {
                v.vibrate(android.os.VibrationEffect.createOneShot(35, 230))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(35)
            }
        }
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }
}
