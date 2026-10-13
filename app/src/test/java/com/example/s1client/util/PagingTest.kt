package com.example.s1client.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 分页边界回归测试。
 *
 * 背景：曾出现「翻页只能到 n-1 页、最后一页滑不过去」的线上问题，
 * 根因是 ViewPager2 的 itemCount 只跟「已加载页数」走、总页数已知时没有预留槽位。
 * 这里锁住总页数/楼层换算的算法口径，防止同类 off-by-one 再出现。
 */
class PagingTest {

    // ===== totalPages =====

    @Test
    fun `刚好整除时页数正确`() {
        // 79 回复 + 楼主 = 80 楼，每页 40 → 恰好 2 页
        assertEquals(2, Paging.totalPages(replies = 79, perPage = 40))
    }

    @Test
    fun `有余数时向上取整`() {
        // 80 回复 + 楼主 = 81 楼 → 第 3 页只有 1 楼
        assertEquals(3, Paging.totalPages(replies = 80, perPage = 40))
    }

    @Test
    fun `单页帖子为一页`() {
        assertEquals(1, Paging.totalPages(replies = 0, perPage = 40))
    }

    @Test
    fun `正好卡在页边界前一页`() {
        // 39 回复 + 楼主 = 40 楼 = 1 页
        assertEquals(1, Paging.totalPages(replies = 39, perPage = 40))
    }

    @Test
    fun `每页数非法时回落到默认值`() {
        // perPage 为 0 / 负数 / null 都应按 40 计算
        assertEquals(2, Paging.totalPages(replies = 79, perPage = 0))
        assertEquals(2, Paging.totalPages(replies = 79, perPage = -5))
        assertEquals(2, Paging.totalPages(replies = 79, perPage = null))
    }

    @Test
    fun `回复数缺失或非法时返回兜底页数`() {
        assertEquals(1, Paging.totalPages(replies = null, perPage = 40, fallbackTotal = 1))
        assertEquals(3, Paging.totalPages(replies = null, perPage = 40, fallbackTotal = 3))
        assertEquals(1, Paging.totalPages(replies = -1, perPage = 40, fallbackTotal = 1))
    }

    @Test
    fun `服务端每页数不是四十时按其口径计算`() {
        // 论坛把每页设成 20：99 回复 + 楼主 = 100 楼 → 5 页
        assertEquals(5, Paging.totalPages(replies = 99, perPage = 20))
    }

    // ===== locateFloor =====

    @Test
    fun `楼层定位首页`() {
        assertEquals(1 to 0, Paging.locateFloor(floor = 1, perPage = 40))
    }

    @Test
    fun `楼层定位第二页首层`() {
        assertEquals(2 to 0, Paging.locateFloor(floor = 41, perPage = 40))
    }

    @Test
    fun `楼层定位第二页末层`() {
        assertEquals(2 to 39, Paging.locateFloor(floor = 80, perPage = 40))
    }

    @Test
    fun `楼层定位跨到第三页`() {
        assertEquals(3 to 0, Paging.locateFloor(floor = 81, perPage = 40))
    }

    @Test
    fun `非法楼层号被夹到第一页`() {
        assertEquals(1 to 0, Paging.locateFloor(floor = 0, perPage = 40))
        assertEquals(1 to 0, Paging.locateFloor(floor = -10, perPage = 40))
    }

    @Test
    fun `楼层定位与总页数口径一致`() {
        // 同一个楼层，定位到的页码不得超出总页数（这是「只能到 n-1 页」类 bug 的根因之一）
        val total = Paging.totalPages(replies = 100, perPage = 40)
        assertEquals(3, total)
        val (page, _) = Paging.locateFloor(floor = 101, perPage = 40)
        assertTrue("第 101 层应落在最后一页($total)内，实际 $page", page <= total)
    }

    // ===== pageInRange / morePagesToLoad =====

    @Test
    fun `页码范围判断`() {
        assertTrue(Paging.pageInRange(1, 5))
        assertTrue(Paging.pageInRange(5, 5))
        assertFalse(Paging.pageInRange(6, 5))
        assertFalse(Paging.pageInRange(0, 5))
        assertFalse(Paging.pageInRange(-1, 5))
    }

    @Test
    fun `总页数未知时保守放行`() {
        assertTrue(Paging.pageInRange(999, 0))
    }

    @Test
    fun `能否继续加载`() {
        // 关键回归：已加载到总页数时必须停，否则会一直请求不存在的页
        assertTrue(Paging.morePagesToLoad(4, 5))
        assertFalse(Paging.morePagesToLoad(5, 5))
        assertFalse(Paging.morePagesToLoad(6, 5))
        // 总页数未知时放行
        assertTrue(Paging.morePagesToLoad(3, 0))
    }
}
