package com.example.s1client.ui.theme

import com.example.s1client.util.ThemeManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 主题机制测试：7 套调色板的结构完整性 + 幻彩变体确定性。
 * 换主题/改调色板后的回归第一道防线（此前主题系统 0 测试覆盖）。
 *
 * 行为基线说明（2026-10-13 按实际实现校准）：
 * - Linear 墨蓝 coverStyle=-2（纯色封面），不使用流动色带，故其 flowCoverColors
 *   不满足"首尾同色"循环不变量——该不变量只约束使用流动封面的 5 套主题。
 * - 幻彩深浅变体的 Android 层差异在 Theme_S1Client_IridescentDark 样式切换
 *   （ThemeManager:141），palette 层两 seed 差异点为封面色板。
 */
class ThemePaletteTest {

    private val allThemes = listOf(
        ThemeManager.THEME_LIGHT to "焙茶午后",
        ThemeManager.THEME_OLED to "曜石玄夜",
        ThemeManager.THEME_PINK to "樱雾粉霜",
        ThemeManager.THEME_FC to "朱砂红白",
        ThemeManager.THEME_RP5 to "苔绿灰阶",
        ThemeManager.THEME_IRIDESCENT to "幻彩渐变",
        ThemeManager.THEME_LINEAR to "Linear墨蓝"
    )

    /** 使用流动封面的主题：色带首尾同色不变量必须成立 */
    private val flowCoverThemes = listOf(
        ThemeManager.THEME_LIGHT,
        ThemeManager.THEME_PINK,
        ThemeManager.THEME_FC,
        ThemeManager.THEME_RP5,
        ThemeManager.THEME_IRIDESCENT
    )

    /** 七套主题全量：关键字段非空、isDark 符合设计 */
    @Test
    fun `七套主题结构完整`() {
        for ((index, name) in allThemes) {
            val p = s1PaletteFor(index)
            assertTrue("$name accent 不能为透明黑", p.accent != androidx.compose.ui.graphics.Color.Transparent)
            assertTrue("$name textMain 不能为透明", p.textMain.alpha > 0f)
            assertTrue("$name cardBg 不能为透明", p.cardBg.alpha > 0f)
            assertNotNull("$name flowCoverColors 不应为 null", p.flowCoverColors)
            assertTrue("$name 封面色带至少 2 色", p.flowCoverColors.size >= 2)
        }
        assertFalse("焙茶午后应为浅色", s1PaletteFor(ThemeManager.THEME_LIGHT).isDark)
        assertTrue("曜石玄夜应为深色", s1PaletteFor(ThemeManager.THEME_OLED).isDark)
        assertFalse("樱雾粉霜应为浅色", s1PaletteFor(ThemeManager.THEME_PINK).isDark)
        assertTrue("Linear墨蓝应为深色", s1PaletteFor(ThemeManager.THEME_LINEAR).isDark)
    }

    /** 流动封面主题：色带首尾同色不变量（瀑布流平铺依赖） */
    @Test
    fun `流动封面主题色带首尾同色`() {
        for (index in flowCoverThemes) {
            val band = s1PaletteFor(index).flowCoverColors
            assertEquals("主题 $index 色带首尾必须相同", band.first(), band.last())
        }
    }


    /** 幻彩主题：同一 seed 结果确定；两 seed 均产出完整调色板 */
    @Test
    fun `幻彩变体确定性`() {
        val seed0a = s1PaletteFor(ThemeManager.THEME_IRIDESCENT, 0)
        val seed0b = s1PaletteFor(ThemeManager.THEME_IRIDESCENT, 0)
        assertEquals("同 seed 必须产出同一调色板", seed0a, seed0b)

        val seed1 = s1PaletteFor(ThemeManager.THEME_IRIDESCENT, 1)
        assertTrue("seed1 色带也须完整", seed1.flowCoverColors.size >= 2)
        assertTrue("seed1 accent 不能为透明黑",
            seed1.accent != androidx.compose.ui.graphics.Color.Transparent)
    }
}
