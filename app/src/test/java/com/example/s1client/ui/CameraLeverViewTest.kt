package com.example.s1client.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 相机过片扳手刷新：纯逻辑单测（宪法 5——新功能必须同步加测试）。
 *
 * 全部数值对照设计者《相机扳手.html》基准锁定（宪法 19 有原型）：
 * BASE -15° / MAX 75° / 阈值 55° / 行程 130 / 分档 16。
 * 常量被随手改动时这里先红，防止"HTML 基准漂移"回归。
 */
class CameraLeverViewTest {

    // ——— HTML 基准数值锁定锚点 ———

    @Test
    fun `HTML 基准常量未被漂移`() {
        assertEquals(-15f, CameraLeverView.BASE_ANGLE, 0f)
        assertEquals(75f, CameraLeverView.MAX_ANGLE, 0f)
        assertEquals(55f, CameraLeverView.THRESHOLD_ANGLE, 0f)
        assertEquals(130f, CameraLeverView.MAX_PULL_DP, 0f)
        assertEquals(16f, CameraLeverView.TICK_STEP_DP, 0f)
    }

    // ——— 拉动距离 → 扳手角度 ———

    @Test
    fun `零行程等于初始角`() {
        assertEquals(-15f, CameraLeverView.angleForPull(0f), 1e-4f)
    }

    @Test
    fun `满行程等于最大角`() {
        assertEquals(75f, CameraLeverView.angleForPull(130f), 1e-4f)
    }

    @Test
    fun `中点行程线性插值`() {
        // 65/130 = 50% → -15 + 0.5*90 = 30°
        assertEquals(30f, CameraLeverView.angleForPull(65f), 1e-4f)
    }

    @Test
    fun `反向拉动钳制为初始角`() {
        assertEquals(-15f, CameraLeverView.angleForPull(-40f), 1e-4f)
    }

    @Test
    fun `超行程钳制为最大角`() {
        assertEquals(75f, CameraLeverView.angleForPull(300f), 1e-4f)
    }

    // ——— 松手触发阈值 ———

    @Test
    fun `低于阈值不触发`() {
        assertFalse(CameraLeverView.shouldFire(54.9f))
        assertFalse(CameraLeverView.shouldFire(-15f))
    }

    @Test
    fun `达到阈值即触发`() {
        assertTrue(CameraLeverView.shouldFire(55f))
        assertTrue(CameraLeverView.shouldFire(75f))
    }

    /** 行程与阈值的联动：拉过 105dp（55° 触发线对应 ≥101.1dp）后松手必然可触发 */
    @Test
    fun `行程过阈值点后角度必达触发线`() {
        val angleAt = CameraLeverView.angleForPull(105f)
        assertEquals(57.69f, angleAt, 0.01f)
        assertTrue(CameraLeverView.shouldFire(angleAt))
    }

    // ——— 分档咔哒 ———

    @Test
    fun `分档从零起计`() {
        assertEquals(0, CameraLeverView.tickIndex(0f))
        assertEquals(0, CameraLeverView.tickIndex(15.99f))
    }

    @Test
    fun `跨档步进`() {
        assertEquals(1, CameraLeverView.tickIndex(16f))
        assertEquals(2, CameraLeverView.tickIndex(32f))
        assertEquals(8, CameraLeverView.tickIndex(130f))
    }
}
