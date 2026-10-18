package com.example.s1client.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 相机过片扳手刷新：纯逻辑单测（宪法 5——新功能必须同步加测试）。
 *
 * 2026-10-18 22:30 重做后基准全部换新（几何取自设计者《相机下拉.html》）：
 * 转轴 (58,50) 在机身内 / 球头半径 32 在轴右侧 / 角度 0→90 / 阈值 52 / 甩动 1.6 度每毫秒。
 *
 * **重点回归项**：`angleForPoint` 与 `ballPosForAngle` 必须互为逆运算——
 * 旧版把球头画在转轴左侧，向下转球头反而上升（用户反馈「违和感」），
 * 现在球头在右侧，90° 时应正好落在转轴正下方 (58,82)。
 */
class CameraLeverViewTest {

    // ——— 基准常量锁定 ———

    @Test
    fun `几何基准常量未被漂移`() {
        assertEquals(124f, CameraLeverView.VB_W, 0f)
        assertEquals(132f, CameraLeverView.VB_H, 0f)
        assertEquals(58f, CameraLeverView.PIVOT_X, 0f)
        assertEquals(50f, CameraLeverView.PIVOT_Y, 0f)
        assertEquals(44f, CameraLeverView.BALL_R, 0f)
        assertEquals(0f, CameraLeverView.BASE_ANGLE, 0f)
        assertEquals(90f, CameraLeverView.MAX_ANGLE, 0f)
        assertEquals(45f, CameraLeverView.THRESHOLD_ANGLE, 0f)
        assertEquals(1.2f, CameraLeverView.FLICK_DEG_PER_MS, 0f)
    }

    // ——— 角度 ↔ 球头坐标 互逆（跟手的核心） ———

    @Test
    fun `起始角时球头在转轴正右方`() {
        val b = CameraLeverView.ballPosForAngle(0f)
        assertEquals(102f, b.first, 1e-3f)  // 58 + 44（拨杆 23:15 加长后）
        assertEquals(50f, b.second, 1e-3f)
    }

    @Test
    fun `到底角时球头在转轴正下方——这就是修掉违和感的几何`() {
        val b = CameraLeverView.ballPosForAngle(90f)
        assertEquals(58f, b.first, 1e-3f)
        assertEquals(94f, b.second, 1e-3f)
        // 关键回归：球头 Y 必须大于转轴 Y（旧版球头在轴左侧时会小于）
        assertTrue(b.second > CameraLeverView.PIVOT_Y)
    }

    @Test
    fun `任意角度下坐标反算可还原角度`() {
        for (a in floatArrayOf(0f, 15f, 30f, 52f, 75f, 90f)) {
            val b = CameraLeverView.ballPosForAngle(a)
            val back = CameraLeverView.angleForPoint(b.first, b.second)
            assertEquals(a, back, 1e-2f)
        }
    }

    // ——— 跟手：atan2 反算 ———

    @Test
    fun `球头在手右侧时角度为零`() {
        assertEquals(0f, CameraLeverView.angleForPoint(120f, 50f), 1e-3f)
    }

    @Test
    fun `球头在正下方时角度为九十`() {
        assertEquals(90f, CameraLeverView.angleForPoint(58f, 120f), 1e-3f)
    }

    @Test
    fun `球头在斜下四十五度时角度为四十五`() {
        val r = CameraLeverView.BALL_R * 0.7071f
        val a = CameraLeverView.angleForPoint(58f + r, 50f + r)
        assertEquals(45f, a, 0.2f)
    }

    @Test
    fun `越过上限被钳制为九十`() {
        assertEquals(90f, CameraLeverView.angleForPoint(20f, 140f), 1e-3f)  // 落在轴左侧下半区
        assertEquals(90f, CameraLeverView.clampAngle(180f), 1e-3f)
    }

    @Test
    fun `越过下限被钳制为零`() {
        assertEquals(0f, CameraLeverView.angleForPoint(20f, 20f), 1e-3f)   // 落在轴左侧上半区
        assertEquals(0f, CameraLeverView.clampAngle(-45f), 1e-3f)
    }

    // ——— 触发阈值 ———

    @Test
    fun `低于阈值不触发`() {
        assertFalse(CameraLeverView.shouldFire(0f))
        assertFalse(CameraLeverView.shouldFire(44.9f))
    }

    @Test
    fun `达到阈值即触发`() {
        assertTrue(CameraLeverView.shouldFire(45f))
        assertTrue(CameraLeverView.shouldFire(90f))
    }

    // ——— 起手热区 ———

    @Test
    fun `按在球头上可起手`() {
        val b = CameraLeverView.ballPosForAngle(0f)
        assertTrue(CameraLeverView.hitTest(b.first, b.second, 0f))
    }

    @Test
    fun `按在转轴附近可起手`() {
        assertTrue(CameraLeverView.hitTest(58f, 50f, 0f))
    }

    @Test
    fun `热区外不拦截触摸`() {
        // 机身左下角远离转轴与球头
        assertFalse(CameraLeverView.hitTest(18f, 96f, 0f))
    }

    @Test
    fun `球头随角度移动后热区跟着走`() {
        val b = CameraLeverView.ballPosForAngle(90f)
        // 90° 时球头在正下方，按这里应命中
        assertTrue(CameraLeverView.hitTest(b.first, b.second, 90f))
        // 机身左下角离球头和转轴都远，两个热区都不该覆盖
        assertFalse(CameraLeverView.hitTest(18f, 96f, 90f))
        assertFalse(CameraLeverView.hitTest(18f, 96f, 0f))
    }

    // ——— 分档咔哒 ———

    @Test
    fun `分档从零起计`() {
        assertEquals(0, CameraLeverView.tickIndex(0f))
        assertEquals(0, CameraLeverView.tickIndex(14.9f))
    }

    @Test
    fun `跨档步进`() {
        assertEquals(1, CameraLeverView.tickIndex(15f))
        assertEquals(3, CameraLeverView.tickIndex(45f))
        assertEquals(6, CameraLeverView.tickIndex(90f))
    }

    // ——— 甩动阈值 ———

    @Test
    fun `甩动阈值与最小起角已登记`() {
        assertEquals(18f, CameraLeverView.FLICK_MIN_ANGLE, 0f)
        // 控件放大到 88dp 后，起手热区应达到「拇指不必精确命中」的量级
        val scale = CameraLeverView.VIEW_W_DP / CameraLeverView.VB_W
        assertTrue(scale * CameraLeverView.HIT_BALL > 26f)
        assertTrue(scale * CameraLeverView.HIT_AXIS > 42f)
    }
}