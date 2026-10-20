package com.example.s1client.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 相机过片扳手刷新：纯逻辑单测（宪法 5——新功能必须同步加测试）。
 *
 * **2026-10-19 L4 左手版基准换新**：行程 D6 = 静息 -30°（斜指右上）→ 拉满 +60°（斜指右下），
 * 阈值 = 行程一半（绝对角 15°）。转轴 (58,50) / 球头半径 44 / 咔哒步长 15°（按行程角）不变。
 *
 * **2026-10-20 方案 B**：画布 124×132 → 93×82（去空白，容器 = 图形），VIEW_W 104→78dp，
 * 新增 ORIGIN_X/Y 与 toVb()。行程 / 阈值 / 半径 / 热区未动 —— 回归重点是
 * 「画布必须刚好包住图形」与「触摸像素 → vb 的回加偏移」。
 *
 * **重点回归项**：`angleForPoint` 与 `ballPosForAngle` 必须互为逆运算；
 * 静息时球头必须在转轴**上方**（斜指右上）、拉满时在转轴**右下方**——这是左手版拇指下压弧线。
 */
class CameraLeverViewTest {

    // ——— 基准常量锁定 ———

    @Test
    fun `几何基准常量未被漂移`() {
        // 2026-10-20 方案 B：画布 124×132 → 93×82（去空白，容器=图形）。
        // 行程 / 转轴 / 半径 / 阈值不动，保证手感零变化。
        assertEquals(93f, CameraLeverView.VB_W, 0f)
        assertEquals(82f, CameraLeverView.VB_H, 0f)
        assertEquals(13f, CameraLeverView.ORIGIN_X, 0f)
        assertEquals(19f, CameraLeverView.ORIGIN_Y, 0f)
        assertEquals(58f, CameraLeverView.PIVOT_X, 0f)
        assertEquals(50f, CameraLeverView.PIVOT_Y, 0f)
        assertEquals(44f, CameraLeverView.BALL_R, 0f)
        assertEquals(-30f, CameraLeverView.BASE_ANGLE, 0f)
        assertEquals(60f, CameraLeverView.MAX_ANGLE, 0f)
        assertEquals(15f, CameraLeverView.THRESHOLD_ANGLE, 0f)
        assertEquals(1.2f, CameraLeverView.FLICK_DEG_PER_MS, 0f)
    }

    // ——— 画布收紧后，图形必须完整落在新 viewBox 内（不再被裁、也不留大片空白） ———

    @Test
    fun `新画布刚好包住全部图形`() {
        val ox = CameraLeverView.ORIGIN_X
        val oy = CameraLeverView.ORIGIN_Y
        val w = CameraLeverView.VB_W
        val h = CameraLeverView.VB_H
        // 机身四角（最靠左 / 最靠下的实体）
        assertTrue("机身左缘应在画布内", 14f - ox >= 0f)
        assertTrue("机身右缘应在画布内", 70f - ox <= w)
        assertTrue("机身底缘应在画布内", 100f - oy <= h)
        // 球头（-30° 静息位）含描边
        val b = CameraLeverView.ballPosForAngle(-30f)
        assertTrue("静息球头右缘应在画布内", b.first + 8.5f - ox <= w)
        assertTrue("静息球头上缘应在画布内", b.second - 8.5f - oy >= 0f)
        // 轨迹弧扫过的最大外接圆
        assertTrue("弧右缘应在画布内", 58f + CameraLeverView.BALL_R + 1f - ox <= w)
        assertTrue("弧下缘应在画布内", 50f + CameraLeverView.BALL_R + 1f - oy <= h)
        // 收紧有效：旧画布 124×132 的空白被去掉了
        assertTrue("画布宽应小于旧值", w < 124f)
        assertTrue("画布高应小于旧值", h < 132f)
        // 缩放比保持不变 → 机身视觉大小与旧版一致（78/93 = 104/124）
        val old = 104f / 124f
        val now = CameraLeverView.VIEW_W_DP / w
        assertEquals(old, now, 1e-3f)
    }

    // ——— 触摸映射：像素 → vb 必须回加原点偏移 ———

    @Test
    fun `触摸坐标回加画布原点偏移`() {
        val s = CameraLeverView.VIEW_W_DP / CameraLeverView.VB_W
        // 静息球头中心，反推它在新画布坐标里的位置，再正向映射回 vb
        val b = CameraLeverView.ballPosForAngle(-30f)
        val px = (b.first - CameraLeverView.ORIGIN_X) * s
        val py = (b.second - CameraLeverView.ORIGIN_Y) * s
        val (vx, vy) = CameraLeverView.toVb(px, py, s)
        assertEquals(b.first, vx, 1e-2f)
        assertEquals(b.second, vy, 1e-2f)
        // 画布左上角映射回旧 vb 原点
        val (zx, zy) = CameraLeverView.toVb(0f, 0f, s)
        assertEquals(CameraLeverView.ORIGIN_X, zx, 1e-3f)
        assertEquals(CameraLeverView.ORIGIN_Y, zy, 1e-3f)
    }

    @Test
    fun `新画布下球头跟手仍可命中`() {
        val s = CameraLeverView.VIEW_W_DP / CameraLeverView.VB_W
        // 静息球头按新画布像素坐标命中热区
        val b = CameraLeverView.ballPosForAngle(-30f)
        val (vx, vy) = CameraLeverView.toVb(
            (b.first - CameraLeverView.ORIGIN_X) * s,
            (b.second - CameraLeverView.ORIGIN_Y) * s,
            s
        )
        assertTrue(CameraLeverView.hitTest(vx, vy, -30f))
        // 转轴同样可命中（拇指不必精确命中）
        assertTrue(CameraLeverView.hitTest(CameraLeverView.PIVOT_X, CameraLeverView.PIVOT_Y, -30f))
    }

    // ——— 角度 ↔ 球头坐标 互逆（跟手的核心） ———

    @Test
    fun `静息角时球头在转轴右上`() {
        val b = CameraLeverView.ballPosForAngle(-30f)
        assertEquals(96.105f, b.first, 1e-2f)   // 58 + 44·cos30°
        assertEquals(28f, b.second, 1e-2f)      // 50 − 44·sin30°
        // 静息在轴上方——左手拇指贴缘上抬即达
        assertTrue(b.second < CameraLeverView.PIVOT_Y)
    }

    @Test
    fun `拉满角时球头在转轴右下方——压杆行程的落点`() {
        val b = CameraLeverView.ballPosForAngle(60f)
        assertEquals(80f, b.first, 1e-2f)       // 58 + 44·cos60°
        assertEquals(88.105f, b.second, 1e-2f)  // 50 + 44·sin60°
        assertTrue(b.second > CameraLeverView.PIVOT_Y)
    }

    @Test
    fun `任意角度下坐标反算可还原角度`() {
        for (a in floatArrayOf(-30f, -15f, 0f, 15f, 30f, 45f, 60f)) {
            val b = CameraLeverView.ballPosForAngle(a)
            val back = CameraLeverView.angleForPoint(b.first, b.second)
            assertEquals(a, back, 1e-2f)
        }
    }

    // ——— 跟手：atan2 反算 ———

    @Test
    fun `球头在正右方时角度为零`() {
        assertEquals(0f, CameraLeverView.angleForPoint(120f, 50f), 1e-3f)
    }

    @Test
    fun `球头在正下方时钳制为六十`() {
        assertEquals(60f, CameraLeverView.angleForPoint(58f, 120f), 1e-3f)
    }

    @Test
    fun `球头在行程中点时角度为十五`() {
        val r = CameraLeverView.BALL_R
        val a = CameraLeverView.angleForPoint(
            58f + r * Math.cos(Math.toRadians(15.0)).toFloat(),
            50f + r * Math.sin(Math.toRadians(15.0)).toFloat()
        )
        assertEquals(15f, a, 0.2f)
    }

    @Test
    fun `越过上限被钳制为六十`() {
        assertEquals(60f, CameraLeverView.angleForPoint(20f, 140f), 1e-3f)  // 落在轴左侧下半区
        assertEquals(60f, CameraLeverView.clampAngle(180f), 1e-3f)
    }

    @Test
    fun `越过下限被钳制为负三十`() {
        assertEquals(-30f, CameraLeverView.angleForPoint(20f, 20f), 1e-3f)  // 落在轴左侧上半区
        assertEquals(-30f, CameraLeverView.clampAngle(-45f), 1e-3f)
    }

    // ——— 触发阈值 ———

    @Test
    fun `低于阈值不触发`() {
        assertFalse(CameraLeverView.shouldFire(-30f))
        assertFalse(CameraLeverView.shouldFire(14.9f))
    }

    @Test
    fun `达到阈值即触发`() {
        assertTrue(CameraLeverView.shouldFire(15f))
        assertTrue(CameraLeverView.shouldFire(60f))
    }

    // ——— 起手热区 ———

    @Test
    fun `按在静息球头上可起手`() {
        val b = CameraLeverView.ballPosForAngle(-30f)
        assertTrue(CameraLeverView.hitTest(b.first, b.second, -30f))
    }

    @Test
    fun `按在转轴附近可起手`() {
        assertTrue(CameraLeverView.hitTest(58f, 50f, -30f))
    }

    @Test
    fun `热区外不拦截触摸`() {
        // 机身左下角远离转轴与静息球头
        assertFalse(CameraLeverView.hitTest(18f, 96f, -30f))
    }

    @Test
    fun `球头随角度移动后热区跟着走`() {
        val b = CameraLeverView.ballPosForAngle(60f)
        // 拉满时球头在右下方，按这里应命中
        assertTrue(CameraLeverView.hitTest(b.first, b.second, 60f))
        // 机身左下角离球头和转轴都远，两个热区都不该覆盖
        assertFalse(CameraLeverView.hitTest(18f, 96f, 60f))
        assertFalse(CameraLeverView.hitTest(18f, 96f, -30f))
    }

    // ——— 分档咔哒（按行程角：静息 = 0 档） ———

    @Test
    fun `分档从静息起计`() {
        assertEquals(0, CameraLeverView.tickIndex(-30f))
        assertEquals(0, CameraLeverView.tickIndex(-15.1f))
    }

    @Test
    fun `跨档步进`() {
        assertEquals(1, CameraLeverView.tickIndex(-15f))
        assertEquals(3, CameraLeverView.tickIndex(15f))
        assertEquals(6, CameraLeverView.tickIndex(60f))
    }

    // ——— 甩动阈值 ———

    @Test
    fun `甩动阈值与最小起角已登记`() {
        assertEquals(18f, CameraLeverView.FLICK_MIN_ANGLE, 0f)
        // 控件 78dp 宽，起手热区应达到「拇指不必精确命中」的量级
        val scale = CameraLeverView.VIEW_W_DP / CameraLeverView.VB_W
        assertTrue(scale * CameraLeverView.HIT_BALL > 26f)
        assertTrue(scale * CameraLeverView.HIT_AXIS > 42f)
    }
}
