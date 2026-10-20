package com.example.s1client.util

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.TypedValue
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.graphics.toArgb
import com.example.s1client.R

/**
 * 系统栏图标是否用深色（即「浅色状态栏」模式）。
 *
 * 直接读当前主题的 s1BgPage 页面底色算感知亮度，亮底→深色图标、暗底→浅色图标。
 * 这样新增主题时连「系统栏图标明暗」开关都不用写，只填 s1* 颜色即可自动正确。
 */
fun Context.isS1LightSystemBarIcons(): Boolean {
    val tv = TypedValue()
    if (!theme.resolveAttribute(R.attr.s1BgPage, tv, true)) return true
    val c = tv.data
    val r = Color.red(c) / 255f
    val g = Color.green(c) / 255f
    val b = Color.blue(c) / 255f
    // sRGB 感知亮度（Rec.601）
    return 0.299f * r + 0.587f * g + 0.114f * b >= 0.5f
}

/**
 * Compose Activity（不继承 BaseActivity）统一主题入口 —— 第一步。
 *
 * 必须在 [ComponentActivity.onCreate] 的 super.onCreate **之前**调用：
 * 应用当前 XML 主题（windowBackground / 各 ?attr/s1* 默认值）。
 *
 * 注意：只 setTheme 不够。XML 主题里声明了 android:navigationBarColor，
 * PhoneWindow 在 attach 阶段（早于 onCreate）就已缓存该颜色，
 * 所以系统栏透明必须在 super.onCreate **之后**再强制设置一次，见 [setupS1EdgeToEdge]。
 */
fun ComponentActivity.applyS1Theme() {
    ThemeManager.init(this)
    setTheme(ThemeManager.getThemeRes())
}

/**
 * 把半透明的「玻璃底栏色」压在不透明底色上，得到它实际呈现的颜色（用于填系统导航栏）。
 * Compose 的 Color.red/green/blue 是 0..1 浮点，alpha 也是 0..1。
 */
private fun s1Composite(over: androidx.compose.ui.graphics.Color,
                        under: androidx.compose.ui.graphics.Color): Int {
    val a = over.alpha
    fun ch(o: Float, u: Float) = ((o * a + u * (1f - a)) * 255f + 0.5f).toInt().coerceIn(0, 255)
    return android.graphics.Color.argb(
        255,
        ch(over.red, under.red),
        ch(over.green, under.green),
        ch(over.blue, under.blue)
    )
}

/**
 * 导航栏底色 —— 这里有个**不能想当然**的坑：
 *
 * 直觉上应该设透明、让页面内容画到导航栏后面。但 MIUI（Android 13 实测）在
 * 「导航栏透明 + isAppearanceLightNavigationBars = true（浅色主题）」时，
 * 会自己叠一层约 90% 的近白底来衬手势条，而且完全不理会
 * isNavigationBarContrastEnforced = false —— 结果就是浅色主题下底部多一条白带
 * （深色主题因为叠的是深色，看不出来）。
 *
 * 所以这里反其道而行：填一个**不透明**的颜色。不透明就没有 scrim 可言，
 * 而这个颜色取「玻璃底栏压在页面底色上的实际颜色」，
 * 于是导航栏和它上面的玻璃底栏在视觉上连成一片，完全看不出接缝。
 */
fun ComponentActivity.applyS1NavigationBarColor(hasGlassBottomBar: Boolean) {
    val c = com.example.s1client.ui.theme.s1PaletteFor(ThemeManager.currentTheme)
    window.navigationBarColor = if (hasGlassBottomBar) {
        // 首页：玻璃底栏（半透明）压在 Scaffold 的 containerColor = bgTop 上
        s1Composite(c.glassBar, c.bgTop)
    } else {
        // 没有玻璃底栏的页面：底部就是页面渐变的收尾色
        c.bgBottom.toArgb()
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
}

/**
 * Compose Activity 统一主题入口 —— 第二步。
 *
 * 在 super.onCreate **之后**（setContent 前后均可）调用：
 * 1. edge-to-edge —— 内容绘制到系统栏后面；
 * 2. 系统栏图标深浅跟随主题 —— OLED 用浅色图标，午后红茶 / 粉色佳人用深色图标
 *    （androidx 的 enableEdgeToEdge 只认系统暗色模式，不认应用内主题，所以显式设置）。
 *
 * @param hasGlassBottomBar 该页底部是否有玻璃底栏（首页有）。决定导航栏填什么色，见
 *        [applyS1NavigationBarColor] 的说明。
 */
fun ComponentActivity.setupS1EdgeToEdge(hasGlassBottomBar: Boolean = false) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = Color.TRANSPARENT
    applyS1NavigationBarColor(hasGlassBottomBar)
    applyS1WindowBackground(com.example.s1client.ui.theme.s1PaletteFor(ThemeManager.currentTheme))
    // 图标明暗按当前主题底色亮度自动：亮底深色图标 / 暗底浅色图标
    val lightSystemBarIcons = isS1LightSystemBarIcons()
    WindowInsetsControllerCompat(window, window.decorView).apply {
        isAppearanceLightStatusBars = lightSystemBarIcons
        isAppearanceLightNavigationBars = lightSystemBarIcons
    }
}

/**
 * 系统栏「白边」修复（2026-10-19）。
 *
 * **症状**：从浅色主题切到彩色/深色主题（例如幻彩浅板 → 机器猫），顶部状态栏和底部手势区
 * 各留一条**上一个主题的浅色带**，看起来像整块界面被镶了一圈白边。
 *
 * **根因**：targetSdk 35 / Android 15 起 `statusBarColor` 与 `navigationBarColor` 已被系统忽略，
 * 系统栏就是透明的 —— 露出来的是 **Window 自己的背景**。而主题切换是即时生效、不重建 Activity，
 * windowBackground 还停在 onCreate 时那套 XML 主题上，于是两条边保持旧色。
 *
 * **修法**：按当前色板重刷窗口背景，用 bgTop → bgBottom 的竖向渐变，
 * 与页面底（Scaffold 的 containerColor / 各页自绘底）完全同色，两条边就接上了。
 */
private fun ComponentActivity.applyS1WindowBackground(c: com.example.s1client.ui.theme.S1Colors) {
    window.setBackgroundDrawable(
        android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(c.bgTop.toArgb(), c.bgBottom.toArgb())
        )
    )
}

/**
 * 主题「即时切换」后的系统栏同步 —— 因为不再重建 Activity，XML 主题属性还是旧的，
 * 必须直接按新调色板把系统栏刷一遍，否则会出现「App 已变色、系统栏还是旧色」的割裂。
 */
fun ComponentActivity.syncSystemBarsWithPalette(hasGlassBottomBar: Boolean = false) {
    val colors = com.example.s1client.ui.theme.s1PaletteFor(ThemeManager.currentTheme)
    window.statusBarColor = Color.TRANSPARENT
    applyS1NavigationBarColor(hasGlassBottomBar)
    // 关键（2026-10-19 白边修复）：Android 15+ 系统栏颜色已失效，露的是 Window 背景，
    // 不重刷这里就会留下上一个主题的浅色边。
    applyS1WindowBackground(colors)
    WindowInsetsControllerCompat(window, window.decorView).apply {
        isAppearanceLightStatusBars = !colors.isDark
        isAppearanceLightNavigationBars = !colors.isDark
    }
}
