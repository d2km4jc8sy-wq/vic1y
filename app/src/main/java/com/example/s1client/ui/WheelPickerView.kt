package com.example.s1client.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

/**
 * 滚轮选择器（iPhone 那种滚动手感）：竖直滚动、居中吸附、中间项放大高亮、上下淡出。
 *
 * 回调分两段：
 * - [onValueChanged]：滚动过程中当前值变化，带瞬时滚动速度（用于按速度调震感）
 * - [onSettled]：停下来（松手/惯性结束）后的最终值，此时才提交跳转
 */
class WheelPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {

    var minValue: Int = 1
        private set
    var maxValue: Int = 1
        private set

    /** 当前滚到的值（1-based） */
    var value: Int = 1
        private set

    /** 滚动中当前值变化：(值, 瞬时速度 px/ms) */
    var onValueChanged: ((Int, Float) -> Unit)? = null

    /** 滚动停止后的最终值 */
    var onSettled: ((Int) -> Unit)? = null

    private val itemHeightDp = 44f
    private var itemHeightPx = 0
    private var lastY = 0
    private var lastT = 0L
    private var lastValue = -1

    private inner class WheelAdapter : RecyclerView.Adapter<WheelVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WheelVH {
            val tv = TextView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, itemHeightPx)
                gravity = android.view.Gravity.CENTER
                textSize = 18f
                setSingleLine()
            }
            return WheelVH(tv)
        }
        override fun onBindViewHolder(holder: WheelVH, position: Int) {
            holder.tv.text = (minValue + position).toString()
        }
        override fun getItemCount(): Int = maxValue - minValue + 1
    }

    private inner class WheelVH(val tv: TextView) : RecyclerView.ViewHolder(tv)

    fun setup(min: Int, max: Int, initial: Int) {
        minValue = min
        maxValue = max
        value = initial.coerceIn(min, max)
        lastValue = value
        itemHeightPx = (itemHeightDp * resources.displayMetrics.density).toInt()
        layoutManager = LinearLayoutManager(context)
        adapter = WheelAdapter()
        // 不用 LinearSnapHelper：它在大速度 fling 时会返回"无吸附目标"，
        // 导致滚轮停在两个数字正中间。改为滚动停止(IDLE)时自己平滑吸附（见下方监听器）
        overScrollMode = OVER_SCROLL_NEVER
        // 首尾项也要能滚到正中间，所以上下各垫半个可视高度
        post {
            val pad = (height / 2) - (itemHeightPx / 2)
            if (pad > 0) {
                setPadding(0, pad, 0, pad)
                clipToPadding = false
            }
            scrollToPosition(value - minValue)
            post { refreshVisual() }
        }
        // 关键修复：每次 setup 先清掉旧监听器再挂新的。
        // 之前重复 addOnScrollListener，第二次展开后同一手势会触发多次回调，
        // 值回调和震动全部错乱。
        clearOnScrollListeners()
        addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                refreshVisual()
                val now = android.os.SystemClock.elapsedRealtime()
                val dt = (now - lastT).coerceAtLeast(1)
                val speed = abs(dy).toFloat() / dt   // px/ms
                lastT = now
                val v = centerValue()
                if (v in minValue..maxValue && v != lastValue) {
                    lastValue = v
                    value = v
                    onValueChanged?.invoke(v, speed)
                }
            }
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState == SCROLL_STATE_IDLE) {
                    // 先吸附到最近数字的正中间（最多滑半格，保证数字永远对齐细线）
                    snapToCenter()
                    val v = centerValue()
                    if (v in minValue..maxValue) {
                        value = v
                        lastValue = v
                        onSettled?.invoke(v)
                    }
                }
            }
        })
    }

    /** 当前最靠近中心的那一项对应的值 */
    private fun centerValue(): Int {
        val center = height / 2
        var closest = NO_POSITION
        var minDist = Int.MAX_VALUE
        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            val childCenter = (child.top + child.bottom) / 2
            val d = abs(childCenter - center)
            if (d < minDist) {
                minDist = d
                closest = getChildAdapterPosition(child)
            }
        }
        return if (closest == NO_POSITION) value else minValue + closest
    }

    /**
     * 滚动停止后平滑吸附到最近数字的正中间。
     * LinearSnapHelper 在快速甩动时会放弃吸附（停在两个数字中间），所以自己来：
     * 找到最靠近中心的 child，把它滚到正中（最多半格距离，必收敛）。
     */
    private fun snapToCenter() {
        val center = height / 2
        var best: View? = null
        var bestDist = Int.MAX_VALUE
        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            val childCenter = (child.top + child.bottom) / 2
            val d = abs(childCenter - center)
            if (d < bestDist) {
                bestDist = d
                best = child
            }
        }
        best?.let {
            val childCenter = (it.top + it.bottom) / 2
            val diff = childCenter - center
            if (abs(diff) > 2) smoothScrollBy(0, diff)
        }
    }

    /**
     * 扁平化视觉：不做缩放和立体效果，仅靠颜色/透明度区分选中项
     * （上下细横线由布局里的两条 1dp 线承担）
     */
    private fun refreshVisual() {
        val center = height / 2
        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            val childCenter = (child.top + child.bottom) / 2
            val dist = abs(childCenter - center).toFloat() / (itemHeightPx * 2f)
            val r = dist.coerceIn(0f, 1f)
            child.alpha = 1f - 0.62f * r
            val tv = (child as? ViewGroup)?.getChildAt(0) as? TextView ?: child as? TextView
            tv?.setTextColor(textColorFor(r))
        }
    }

    private var accentColor: Int = 0xFF000000.toInt()
    private var normalColor: Int = 0xFF888888.toInt()

    fun setTextColors(accent: Int, normal: Int) {
        accentColor = accent
        normalColor = normal
    }

    private fun textColorFor(r: Float): Int = if (r < 0.35f) accentColor else normalColor
}
