package com.example.s1client.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 触达区外扩数学的契约（阶段四 P15，2026-10-17）。
 * 锚点：向四周外扩 halo → 裁剪到父容器边界内；完全出界返回空矩形。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TouchDelegateRectTest {

    @Test
    fun `外扩向四周各加halo`() {
        val r = TouchDelegateHelper.expandRect(100, 100, 122, 122, 8, 1080, 2344)
        assertEquals(92, r.left)
        assertEquals(92, r.top)
        assertEquals(130, r.right)
        assertEquals(130, r.bottom)
    }

    @Test
    fun `越出父容器左边界被裁剪`() {
        // 按钮贴着左缘（left=2），外扩 8 → -6 → 裁剪到 0
        val r = TouchDelegateHelper.expandRect(0, 100, 22, 122, 8, 1080, 2344)
        assertEquals(0, r.left)
        assertEquals(30, r.right)
    }

    @Test
    fun `越出父容器上边界被裁剪`() {
        val r = TouchDelegateHelper.expandRect(100, 0, 122, 22, 8, 1080, 2344)
        assertEquals(0, r.top)
        assertEquals(30, r.bottom)
    }

    @Test
    fun `完全出界返回空矩形`() {
        val r = TouchDelegateHelper.expandRect(5000, 5000, 5022, 5022, 8, 1080, 2344)
        assertTrue(r.isEmpty)
    }

    @Test
    fun `零halo等于原边界`() {
        val r = TouchDelegateHelper.expandRect(100, 100, 122, 122, 0, 1080, 2344)
        assertEquals(100, r.left)
        assertEquals(122, r.right)
    }

    // ———— 最近邻路由（2026-10-17 防误触整改）————

    private fun noteRects(): List<android.graphics.Rect> {
        // 模拟便签四钮：22dp 视觉、间距 8dp、外扩 8dp → 38dp 触达，相邻重叠 8dp
        val mk = { l: Int, t: Int -> android.graphics.Rect(l, t, l + 38, t + 38) }
        return listOf(mk(0, 0), mk(30, 0), mk(60, 0), mk(90, 0))
    }

    @Test
    fun `重叠带归中心更近的钮`() {
        val rects = noteRects()
        // 钮0 触达 [0,38)，钮1 触达 [30,68)：重叠带 [30,38)。点 x=33 在两者内，
        // 中心 19 vs 49 → 离 19 更近（距离 14² < 16²）→ 归钮0
        assertEquals(0, TouchDelegateHelper.nearestHit(rects, 33, 19))
        // 点 x=36：中心距 17 vs 13 → 归钮1（想点右钮不再误触左钮）
        assertEquals(1, TouchDelegateHelper.nearestHit(rects, 36, 19))
    }

    @Test
    fun `仅在单钮触达区内归该钮`() {
        val rects = noteRects()
        assertEquals(0, TouchDelegateHelper.nearestHit(rects, 5, 5))
        assertEquals(3, TouchDelegateHelper.nearestHit(rects, 120, 19))
    }

    @Test
    fun `全部未命中返回负一`() {
        val rects = noteRects()
        assertEquals(-1, TouchDelegateHelper.nearestHit(rects, 500, 500))
    }

    @Test
    fun `正中点并列归先注册者`() {
        val rects = noteRects()
        // x=29 为钮0(0-38)与钮1(30-68)重叠带内？x=29 只在钮0 内（38>29>30 不成立）
        // 真正的几何并列点：两 38 宽钮中心 19 与 49 的中点 x=34（在 30..38 重叠带内）
        assertEquals(0, TouchDelegateHelper.nearestHit(rects, 34, 19))
    }

    @Test
    fun `钮内任意点都归自己即使边缘远离中心`() {
        val rects = noteRects()
        // 单独一个矩形时，矩形内任何点都命中（不受中心距离影响）
        assertEquals(0, TouchDelegateHelper.nearestHit(rects.subList(0, 1), 37, 37))
    }
}
