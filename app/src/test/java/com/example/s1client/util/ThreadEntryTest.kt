package com.example.s1client.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * ThreadEntry（详情页起始页判定）的边界用例。
 * 期望行为锚点：口径与 s1er resolveThreadInitialPage 一致——
 * 楼层 → 页的换算永远是 ((floor - 1) / ppp) + 1，ppp 无效时回退 40。
 */
class ThreadEntryTest {

    // ---------- guessFromFloor ----------

    @Test
    fun `无进度落第1页`() {
        assertEquals(1, ThreadEntry.guessFromFloor(0, 0))
        assertEquals(1, ThreadEntry.guessFromFloor(1, 40))
    }

    @Test
    fun `楼层换算整除时落在上一页末尾`() {
        // 第 40 楼在第 1 页（ppp=40），第 41 楼起在第 2 页
        assertEquals(1, ThreadEntry.guessFromFloor(40, 40))
        assertEquals(2, ThreadEntry.guessFromFloor(41, 40))
        assertEquals(3, ThreadEntry.guessFromFloor(81, 40))
    }

    @Test
    fun `ppp无效时回退默认40`() {
        assertEquals(2, ThreadEntry.guessFromFloor(41, 0))
        assertEquals(2, ThreadEntry.guessFromFloor(41, -5))
    }

    @Test
    fun `小ppp换算`() {
        assertEquals(2, ThreadEntry.guessFromFloor(41, 25))
        assertEquals(1, ThreadEntry.guessFromFloor(25, 25))
        assertEquals(2, ThreadEntry.guessFromFloor(26, 25))
    }

    @Test
    fun `超大楼层不溢出`() {
        assertEquals(250, ThreadEntry.guessFromFloor(10000, 40))
    }

    // ---------- correctionTarget ----------

    @Test
    fun `纠正命中_换到真实ppp对应的页`() {
        assertEquals(2, ThreadEntry.correctionTarget(
            startPage = 1, resumeFloor = 41, hasPendingPid = false,
            livePerPage = 40, totalPages = 5))
    }

    @Test
    fun `有待定位pid时不纠正`() {
        assertNull(ThreadEntry.correctionTarget(
            startPage = 1, resumeFloor = 41, hasPendingPid = true,
            livePerPage = 40, totalPages = 5))
    }

    @Test
    fun `无进度或ppp无效时不纠正`() {
        assertNull(ThreadEntry.correctionTarget(1, 1, false, 40, 5))
        assertNull(ThreadEntry.correctionTarget(1, 41, false, 0, 5))
        assertNull(ThreadEntry.correctionTarget(1, 41, false, -1, 5))
    }

    @Test
    fun `目标页与当前页相同则不纠正`() {
        assertNull(ThreadEntry.correctionTarget(2, 41, false, 40, 5))
    }

    @Test
    fun `目标页超出总页数则不纠正`() {
        assertNull(ThreadEntry.correctionTarget(1, 10000, false, 40, 3))
    }

    @Test
    fun `总页数未知按1兜底参与比较`() {
        // totalPages=0 → coerce 到 1；want=2 > 1 → 不纠正
        assertNull(ThreadEntry.correctionTarget(1, 41, false, 40, 0))
    }

    @Test
    fun `纠正到末页`() {
        // 100 楼 / ppp=40 → 第 3 页，恰好等于总页数 → 允许
        assertEquals(3, ThreadEntry.correctionTarget(
            startPage = 1, resumeFloor = 100, hasPendingPid = false,
            livePerPage = 40, totalPages = 3))
    }
}
