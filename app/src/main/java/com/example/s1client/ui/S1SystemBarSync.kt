package com.example.s1client.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.s1client.util.ThemeManager
import com.example.s1client.util.syncSystemBarsWithPalette

/**
 * 主题一变，就把系统栏（状态栏图标明暗 / 导航栏底色）刷成新配色。
 *
 * **为什么放在宿主页面里，而不是主题选择器里：**
 * 选择器是 `ModalBottomSheet`，它内部有自己的窗口与 ContextThemeWrapper，
 * 里面的 `LocalContext.current` 并不是 Activity —— `as? ComponentActivity` 会得到 null，
 * 于是同步代码一次都没执行过。症状就是：换完主题后底部残留一条**上一个主题的导航栏色条**
 * （因为导航栏色还停留在进 App 时 setupS1EdgeToEdge 设的那个值，而 Compose 画的内容已经变了）。
 *
 * 现在改成在宿主页面里读 `ThemeManager.currentTheme`（可观察状态）来触发同步：
 * 不依赖任何 Context 的强转，任何入口换主题都能生效。
 *
 * @param hasGlassBottomBar 该页底部是否有玻璃底栏（首页有），决定导航栏填哪种色。
 */
@Composable
fun S1SystemBarSync(hasGlassBottomBar: Boolean = false) {
    val context = LocalContext.current
    // 读一次可观察状态：主题变化 → 这里重组 → LaunchedEffect 重新执行
    val themeIndex = ThemeManager.currentTheme
    LaunchedEffect(themeIndex) {
        (context as? ComponentActivity)?.syncSystemBarsWithPalette(hasGlassBottomBar)
    }
}
