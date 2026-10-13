package com.example.s1client.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/**
 * 「欢迎加入 stage1 N 天」的边界测试。
 *
 * 关键要求：按注册时间到**真实当前时间**算，而不是本地累加 ——
 * 累加会因改机时间、时区、跨夏令时算错。
 */
class JoinDaysTest {

    private fun cal(y: Int, mo: Int, d: Int, h: Int = 12): Calendar =
        Calendar.getInstance().apply {
            set(y, mo - 1, d, h, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

    // ===== daysSince =====

    @Test
    fun `注册当天为零天`() {
        assertEquals(0, JoinDays.daysSince("2024-05-01", cal(2024, 5, 1)))
    }

    @Test
    fun `注册当天不同时刻仍为零天`() {
        // 23:59 注册、当天 23:59 打开：仍是 0 天（不能因为时刻不同就进位）
        val reg = "2024-05-01 23:59:00"
        assertEquals(0, JoinDays.daysSince(reg, cal(2024, 5, 1, 23)))
    }

    @Test
    fun `跨过午夜才进位一天`() {
        // 5/1 23:59 注册，5/2 00:01 打开 → 满一天，1 天
        val reg = "2024-05-01 23:59:00"
        assertEquals(1, JoinDays.daysSince(reg, cal(2024, 5, 2, 0)))
    }

    @Test
    fun `满一天为一`() {
        assertEquals(1, JoinDays.daysSince("2024-05-01", cal(2024, 5, 2)))
    }

    @Test
    fun `跨月计算正确`() {
        assertEquals(32, JoinDays.daysSince("2024-05-01", cal(2024, 6, 2)))
    }

    @Test
    fun `跨年计算正确`() {
        // 2024-12-31 → 2025-01-01 恰好 1 天
        assertEquals(1, JoinDays.daysSince("2024-12-31", cal(2025, 1, 1)))
    }

    @Test
    fun `闰年二月底正确`() {
        // 2024 是闰年：2/28 → 3/1 为 2 天
        assertEquals(2, JoinDays.daysSince("2024-02-28", cal(2024, 3, 1)))
    }

    @Test
    fun `当前时间早于注册时间不会得到负数`() {
        // 时钟被改早 / 服务器返回未来日期时兜底为 0
        assertEquals(0, JoinDays.daysSince("2030-01-01", cal(2024, 5, 1)))
    }

    // ===== 各种日期写法 =====

    @Test
    fun `兼容多种日期分隔符`() {
        val now = cal(2024, 5, 11)
        assertEquals(10, JoinDays.daysSince("2024-05-01", now))
        assertEquals(10, JoinDays.daysSince("2024/05/01", now))
        assertEquals(10, JoinDays.daysSince("2024.05.01", now))
        assertEquals(10, JoinDays.daysSince("2024年5月1日", now))
        assertEquals(10, JoinDays.daysSince("2024-5-1", now))
        assertEquals(10, JoinDays.daysSince("2024-05-01 08:30:00", now))
    }

    @Test
    fun `无法解析时返回 null`() {
        val now = cal(2024, 5, 11)
        assertNull(JoinDays.daysSince("", now))
        assertNull(JoinDays.daysSince("未知", now))
        assertNull(JoinDays.daysSince("2024-13-45", now))
    }

    // ===== welcomeText =====

    @Test
    fun `欢迎文案格式正确`() {
        assertEquals(
            "欢迎加入 stage1 10 天",
            JoinDays.welcomeText("2024-05-01", cal(2024, 5, 11))
        )
    }

    @Test
    fun `解析失败时欢迎文案也返回 null 以便回退`() {
        assertNull(JoinDays.welcomeText("垃圾数据", cal(2024, 5, 11)))
    }
}
