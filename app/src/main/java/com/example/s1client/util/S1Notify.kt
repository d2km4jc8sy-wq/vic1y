package com.example.s1client.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.example.s1client.R

/**
 * 统一风格化通知（替代裸 Toast）：玻璃质感圆角卡片 + 前导色点 + 语义色 + 自动配对震动。
 *
 * 对标 s1er 的 S1SnackBar（浮动、语义色、配对震动）。全 App 走这一套，
 * 不再出现黑底小字原生 Toast。Activity 子类可直接用 [com.example.s1client.BaseActivity.toast]；
 * Compose / 非 Activity 上下文用 [show]。
 *
 * - INFO  → 次级文字色点，配对 selection 轻震
 * - SUCCESS → 强调色点，配对 medium 震（写入/收藏成功）
 * - ERROR → 红心色点，配对 heavy 震（失败/破坏确认）
 */
object S1Notify {

    enum class Type { INFO, SUCCESS, ERROR }

    private var current: android.view.View? = null

    /**
     * 统一入口，接收任意 Context：自动解析出所属 Activity 的窗口。
     * 若拿不到窗口（如纯 ApplicationContext）则静默跳过，绝不崩溃。
     */
    fun show(context: Context, message: String, type: Type = Type.INFO, silent: Boolean = false) {
        val activity = context.findActivity() ?: return
        show(activity, message, type, silent)
    }

    fun show(activity: Activity, message: String, type: Type = Type.INFO, silent: Boolean = false) {
        // 配对震动（silent=true 时由调用方自行精细控制，避免双重震动）
        if (!silent) {
            when (type) {
                Type.SUCCESS -> Haptic.success(activity)  // 哒哒—嗒 组合震
                Type.ERROR -> Haptic.error(activity)      // 三连重震
                Type.INFO -> Haptic.selection(activity)
            }
        }

        val dm = activity.resources.displayMetrics
        val density = dm.density

        val bgColor = resolve(activity, R.attr.s1BgCard, Color.DKGRAY)
        val lineColor = resolve(activity, R.attr.s1BorderCard, Color.GRAY)
        val textColor = resolve(activity, R.attr.s1TextBody, Color.WHITE)
        val dotColor = when (type) {
            Type.SUCCESS -> resolve(activity, R.attr.s1Accent, Color.CYAN)
            Type.ERROR -> resolve(activity, android.R.attr.colorError, 0xFFFF2C55.toInt())
            Type.INFO -> resolve(activity, R.attr.s1TextSecondary, Color.GRAY)
        }

        val dot = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(dotColor)
        }
        val dotSize = (8 * density).toInt()
        dot.setBounds(0, 0, dotSize, dotSize)

        val bg = GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = 14 * density
            setStroke((1 * density).toInt(), lineColor)
        }

        val tv = TextView(activity).apply {
            text = message
            setTextColor(textColor)
            textSize = 13f
            background = bg
            setCompoundDrawables(dot, null, null, null)
            compoundDrawablePadding = (8 * density).toInt()
            setPadding(
                (14 * density).toInt(),
                (10 * density).toInt(),
                (14 * density).toInt(),
                (10 * density).toInt()
            )
        }

        val root = activity.window.decorView as? ViewGroup ?: return
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            // 抬高到导航栏 + 底部操作区之上
            bottomMargin = ((72 + navBarHeight(activity)) * density).toInt()
        }

        // 先移除上一个，避免堆叠
        (current?.parent as? ViewGroup)?.removeView(current)
        current = tv
        root.addView(tv, lp)

        // 铃铛来件（2026-10-19 加餐）：机器猫主题限定——通知条如铃铛衰减摆动进场。
        // 震感沿用上方按 Type 的原配，不叠加。仅机器猫主题生效（其他主题零变化）。
        if (ThemeManager.currentTheme == ThemeManager.THEME_DORAEMON) {
            android.animation.ObjectAnimator.ofFloat(tv, "rotation", 0f, 10f, -8f, 4f, 0f).apply {
                duration = 750
                start()
            }
        }

        tv.alpha = 0f
        tv.animate().alpha(1f).setDuration(180).start()
        tv.postDelayed({
            tv.animate().alpha(0f).setDuration(250).withEndAction {
                (tv.parent as? ViewGroup)?.removeView(tv)
                if (current == tv) current = null
            }
        }, 1700)
    }

    private fun navBarHeight(ctx: Activity): Int {
        val id = ctx.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (id > 0) ctx.resources.getDimensionPixelSize(id) / ctx.resources.displayMetrics.density.toInt() else 0
    }

    private fun resolve(ctx: Activity, attrId: Int, fallback: Int): Int {
        val tv = TypedValue()
        return if (ctx.theme.resolveAttribute(attrId, tv, true)) tv.data else fallback
    }

    /** 从任意 Context 向上找到宿主 Activity（支持 ContextWrapper 链）。 */
    private fun Context.findActivity(): Activity? {
        var ctx: Context? = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
