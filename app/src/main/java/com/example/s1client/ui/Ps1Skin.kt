package com.example.s1client.ui

import android.content.res.Resources
import com.example.s1client.R
import com.example.s1client.util.ThemeManager

/**
 * PS1 皮肤（帖子详情）明暗资源选择。
 *
 * 决策：皮肤色号不进 attrs/themes，避免改动主题系统；
 * 运行时按 [ThemeManager.currentTheme] 判明暗，在两套固定 drawable 之间切换。
 *
 * 浅色主题：焙茶午后 / 樱雾粉霜 / 苔绿灰阶，以及幻彩渐变抽到浅紫粉板的那次；
 * 其余（Linear 墨蓝、朱砂红白、幻彩深板）按深色皮肤。
 */
object Ps1Skin {

    fun isLight(): Boolean = when (ThemeManager.currentTheme) {
        ThemeManager.THEME_LIGHT,
        ThemeManager.THEME_PINK,
        ThemeManager.THEME_RP5 -> true
        ThemeManager.THEME_IRIDESCENT -> ThemeManager.iridescentSeed != 1
        else -> false
    }

    /** 主楼金属机身 */
    fun bodyRes(): Int =
        if (isLight()) R.drawable.ps_body_light else R.drawable.ps_body_dark

    /** 主楼显示屏 */
    fun screenRes(): Int =
        if (isLight()) R.drawable.ps_screen_light else R.drawable.ps_screen_dark

    /**
     * 回复楼手柄便签（票据面）。
     *
     * 细节轮 3（2026-10-16）：**完全对齐项目既有的票据定义**——
     * 首页长按预览卡（PostPeekDialog）的 TicketPalette：深票纸（主题底色压暗 45%）
     * + 四色荧光晕染 + 票面亮白字。此前两轮做的「88% 半透明浅色纸」是方向错误：
     * 半透明只会让便签与页面底色混成灰蒙，既无票据质感，又牺牲正文对比度。
     *
     * 幻彩主题 → [TicketNight] 紫粉夜光板；其他主题 → 从该主题色板派生。
     * 运行时构造 GradientDrawable（不能进 drawable XML，因为色值是运行时派生的）。
     */
    /**
 * 回复楼便签纸 = **磨砂玻璃膜**（细节轮4，2026-10-16 方案 A）。
 *
 * 背景：完整 TicketPalette 深票纸在 Peek 卡（独立弹层 + scrim）里冲击力强，
 * 但搬到内容流的便签上色差过大 —— 深色块插在浅色正文中间非常扎眼（用户反馈）。
 * iOS 27 的做法值得参考：磨砂玻璃的质感来自「半透明材质 + 降饱和 + 细描边」，
 * **不靠明度差**，色差极小就能有层次。
 *
 * 因此便签不再用深票纸，改为：主题卡片色低透明度（~14%）+ 亮白细描边，
 * 文字用**主题正文色**（跟随明暗自动适配）—— 一套代码通吃全部主题，幻彩也不例外。
 * 票据感由**结构**承载（右侧齿口咬边 / 亮白虚线 stamp / 引用卡压边），不由浓色承载。
 */
fun ticketDrawable(): android.graphics.drawable.Drawable {
        val s1 = com.example.s1client.ui.theme.s1PaletteFor(ThemeManager.currentTheme)
        // 磨砂膜：取主题 accent 极淡的一层（比 bgCard 更贴"玻璃膜带一点主题味"）
        // 2026-10-18：页面底改成红色后，半透明膜会把红透上来（卡片变酒红，成了第三种颜色）。
        // 改用不透明的卡片色，红只留在卡片之间的缝里；描边也换成中性亮线，不再带主题色。
        val film = s1.cardBg
        val edge = s1.textMain.copy(alpha = 0.10f)
        val d = Resources.getSystem().displayMetrics.density
        return android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(film.toArgb(), film.toArgb())
        ).apply {
            cornerRadius = 2 * d
            setStroke(d.toInt().coerceAtLeast(1), edge.toArgb())
        }
    }

    private fun androidx.compose.ui.graphics.Color.toArgb(): Int = android.graphics.Color.argb(
        (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
    )

    /** 票面主文字色（深票纸上恒亮白） */
    fun ticketInkColor(): Int {
        val s1 = com.example.s1client.ui.theme.s1PaletteFor(ThemeManager.currentTheme)
        return ticketPaletteFor(s1).onTicket.toArgb()
    }

    /** 票面次文字色（时间/楼层号等） */
    fun ticketSubInkColor(): Int {
        val s1 = com.example.s1client.ui.theme.s1PaletteFor(ThemeManager.currentTheme)
        return ticketPaletteFor(s1).onTicketSub.toArgb()
    }

    /**
     * 便签右上角倾斜评分 stamp 虚线框。
     * 细节轮4：便签改磨砂玻璃膜（浅底）后，亮白虚线不可见 → 回到随明暗切换
     * （浅色皮肤用浅底上的米褐虚线，深色皮肤用暗底上的暗虚线）。
     */
    fun noteStampRes(): Int =
        if (isLight()) R.drawable.ps_stamp_light else R.drawable.ps_stamp_dark

    /** 主楼操作带左侧评分 chip 虚线框（主楼仍是浅色 CRT 屏，随明暗切换） */
    fun chipRes(): Int =
        if (isLight()) R.drawable.ps_score_chip_light else R.drawable.ps_score_chip_dark

    /** 加分绿（与设计稿一致：加绿减红） */
    const val SCORE_UP = 0xFF00FF88.toInt()

    /** 扣分红 */
    const val SCORE_DOWN = 0xFFFF3355.toInt()

    /** 无评分时的中性灰（抽屉大数字） */
    const val SCORE_NEUTRAL = 0xFFE0E0E0.toInt()
}
