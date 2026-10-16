package com.example.s1client

import com.example.s1client.util.ThemeManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 深色跟随系统的映射契约（阶段四 2026-10-17）。
 * 锚点：跟随模式下系统深色→Linear 墨蓝、浅色→焙茶午后；
 * 映射落点永远 ∈ VISIBLE_THEMES（绝不落到已隐藏的主题上）。
 */
class FollowSystemThemeTest {

    @Test
    fun `系统深色映射Linear墨蓝`() {
        assertEquals(ThemeManager.THEME_LINEAR, ThemeManager.mapFollowSystem(true))
    }

    @Test
    fun `系统浅色映射焙茶午后`() {
        assertEquals(ThemeManager.THEME_LIGHT, ThemeManager.mapFollowSystem(false))
    }

    @Test
    fun `映射结果总是可见主题`() {
        assertTrue(ThemeManager.VISIBLE_THEMES.contains(ThemeManager.mapFollowSystem(true)))
        assertTrue(ThemeManager.VISIBLE_THEMES.contains(ThemeManager.mapFollowSystem(false)))
    }
}
