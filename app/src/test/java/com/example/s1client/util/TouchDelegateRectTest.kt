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
}
