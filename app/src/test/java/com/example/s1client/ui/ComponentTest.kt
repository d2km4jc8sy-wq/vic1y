package com.example.s1client.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import com.example.s1client.ui.design.TextMain
import com.example.s1client.ui.theme.LocalS1Colors
import com.example.s1client.ui.theme.S1Theme
import com.example.s1client.ui.theme.s1PaletteFor
import com.example.s1client.util.ThemeManager
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 组件测试：关键交互组件在浅/深两套主题下渲染正常、基本交互正常。
 * 主题注入走 CompositionLocalProvider（currentTheme setter 私有，见 PROBLEMS P1）。
 * Robolectric LEGACY 图形模式（本机 NATIVE 不可用，见 PROGRESS 4.2）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class ComponentTest {

    @get:Rule
    val compose = createComposeRule()

    /** 开关组件：渲染 + 单击切换状态 */
    @Test
    fun `开关组件渲染与切换`() {
        var state = false
        compose.setContent {
            S1Theme {
                SunMoonSwitch(
                    checked = state,
                    onCheckedChange = { state = it }
                )
            }
        }
        compose.onNode(hasClickAction()).assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue("单击后状态应翻转", state) }
    }

    /** 设计 token 双轨：浅/深调色板下 textMain 必须不同（token 双主题生效的最低证明） */
    @Test
    fun `设计token随调色板变化`() {
        var lightColor: Color? = null
        var darkColor: Color? = null
        compose.setContent {
            CompositionLocalProvider(LocalS1Colors provides s1PaletteFor(ThemeManager.THEME_TEA)) {
                lightColor = TextMain
            }
            CompositionLocalProvider(LocalS1Colors provides s1PaletteFor(ThemeManager.THEME_LINEAR)) {
                darkColor = TextMain
            }
        }
        compose.waitForIdle()
        assertNotNull("浅色调色板未捕获", lightColor)
        assertNotNull("深色调色板未捕获", darkColor)
        assertTrue(
            "浅/深调色板的 textMain 应不同",
            lightColor != darkColor
        )
    }

    /** 深色调色板（OLED）下开关组件渲染不崩溃 */
    @Test
    fun `开关组件深色调色板渲染`() {
        compose.setContent {
            CompositionLocalProvider(
                LocalS1Colors provides s1PaletteFor(ThemeManager.THEME_LINEAR)
            ) {
                SunMoonSwitch(checked = true, onCheckedChange = {})
            }
        }
        compose.onNode(hasClickAction()).assertIsDisplayed()
    }
}
