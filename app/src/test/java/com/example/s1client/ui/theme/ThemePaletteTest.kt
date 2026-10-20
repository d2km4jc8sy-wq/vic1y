package com.example.s1client.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.s1client.util.ThemeManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 主题机制测试：现存 5 套调色板的结构完整性 + 幻彩两板确定性/深浅映射。
 *
 * 2026-10-19 随主题清理同步：曜石玄夜/樱雾粉霜/朱砂红白/苔绿灰阶（索引 1-4）
 * 与幻彩 v1 极光家族（索引 5）均已删除；幻彩渐变 = 原糖果家族（索引 11）顶替名号并移到末位。
 * 现清单与 ThemeManager.VISIBLE_THEMES 同序：Red赤红/焙茶午后/Linear墨蓝/机器猫/幻彩渐变。
 *
 * 行为基线（2026-10-19 按实际实现校准）：
 * - 纯色封面主题（coverStyle=-2）：赤红/Linear/机器猫，flowCoverColors 只作点缀取色，无循环要求；
 * - 焙茶午后走随机流光风格 0..3，其 4 色带为取色序列、非循环构造，只断言非空；
 * - 幻彩渐变 coverStyle 固定 5（流光封面）：色带按「三色 + 首色收尾」构造，循环不变量成立。
 */
class ThemePaletteTest {

    private val allThemes = listOf(
        ThemeManager.THEME_CRIMSON to "Red赤红",
        ThemeManager.THEME_TEA to "焙茶午后",
        ThemeManager.THEME_LINEAR to "Linear墨蓝",
        ThemeManager.THEME_DORAEMON to "机器猫",
        ThemeManager.THEME_IRIDESCENT to "幻彩渐变"
    )

    /** 使用流动封面的主题：色带首尾同色不变量（瀑布流平铺依赖） */
    private val flowCoverThemes = listOf(
        ThemeManager.THEME_IRIDESCENT
    )

    /** 五套主题全量：关键字段非空、isDark 符合设计 */
    @Test
    fun `五套主题结构完整`() {
        for ((index, name) in allThemes) {
            val p = s1PaletteFor(index)
            assertTrue("$name accent 不能为透明黑", p.accent != Color.Transparent)
            assertTrue("$name textMain 不能为透明", p.textMain.alpha > 0f)
            assertTrue("$name cardBg 不能为透明", p.cardBg.alpha > 0f)
            assertTrue("$name 封面色带至少 2 色", p.flowCoverColors.size >= 2)
        }
        assertFalse("焙茶午后应为浅色", s1PaletteFor(ThemeManager.THEME_TEA).isDark)
        assertTrue("Red赤红应为深色", s1PaletteFor(ThemeManager.THEME_CRIMSON).isDark)
        assertTrue("Linear墨蓝应为深色", s1PaletteFor(ThemeManager.THEME_LINEAR).isDark)
        assertTrue("机器猫应为深色体系（红蓝大面 + 白字）", s1PaletteFor(ThemeManager.THEME_DORAEMON).isDark)
    }

    /** 流动封面主题：色带首尾同色不变量（幻彩 = 封面三色 + 首色收尾） */
    @Test
    fun `流动封面主题色带首尾同色`() {
        for (index in flowCoverThemes) {
            val band = s1PaletteFor(index).flowCoverColors
            assertEquals("主题 $index 色带首尾必须相同", band.first(), band.last())
            assertEquals("主题 $index 色带去重后应保留封面三色", 3, band.dropLast(1).distinct().size)
        }
    }

    /**
     * 幻彩两板确定性 + 深浅映射：
     * 0 夜市（深，亮品红）/ 1 镭射糖果（浅，洋红）。
     * seed 参数直通 iridescentPaletteIndex：同 seed 必产出同板；accent 逐板锁定，防改板时串号。
     */
    @Test
    fun `幻彩两板确定性与深浅映射`() {
        data class Expect(val seed: Int, val dark: Boolean, val accent: Color)
        val cases = listOf(
            Expect(0, true, Color(0xFFF44FD8)),
            // 2026-10-19 幻彩重写时糖果 accent 由 #D6339C 调至 #C2267F
            // （S1Theme.kt：白底上与白字上都更稳，4.36:1 → 5.0:1），期望值同步
            Expect(1, false, Color(0xFFC2267F))
        )
        for (c in cases) {
            val a = s1PaletteFor(ThemeManager.THEME_IRIDESCENT, c.seed)
            val b = s1PaletteFor(ThemeManager.THEME_IRIDESCENT, c.seed)
            assertEquals("同 seed 必须产出同一调色板（seed=${c.seed}）", a, b)
            assertEquals("seed=${c.seed} 深浅不符", c.dark, a.isDark)
            assertEquals("seed=${c.seed} accent 被意外改动", c.accent, a.accent)
            assertTrue("seed=${c.seed} 色带须完整", a.flowCoverColors.size >= 2)
        }
    }
}
