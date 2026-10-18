package com.example.s1client.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 时间格式化回归测试。
 *
 * 为什么锁这里：列表卡片、楼层时间、阅读历史全都过这几个函数，
 * 一旦口径变化（比如把「一周内」改成「三天内」），全 App 的时间显示会一起变，
 * 而这种错误编译能过、界面不报错，只能靠测试发现。
 *
 * 测试内统一用「当前时间减去偏移」构造输入，边界值留 2 秒余量，
 * 避免用例执行时的秒级漂移导致偶发失败。
 */
class TimeUtilTest {

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1000
    private fun ago(seconds: Long): String = (nowSeconds() - seconds).toString()

    // ===== formatRelative：列表用相对时间 =====

    @Test
    fun `空值与非法输入返回空串`() {
        assertEquals("", TimeUtil.formatRelative(null))
        assertEquals("", TimeUtil.formatRelative(""))
        assertEquals("", TimeUtil.formatRelative("   "))
        assertEquals("", TimeUtil.formatRelative("abc"))
        assertEquals("", TimeUtil.formatRelative("0"))
        assertEquals("", TimeUtil.formatRelative("-1"))
    }

    @Test
    fun `未来时间视为异常返回空串`() {
        assertEquals("", TimeUtil.formatRelative((nowSeconds() + 3600).toString()))
    }

    @Test
    fun `一分钟内显示刚刚`() {
        assertEquals("刚刚", TimeUtil.formatRelative(ago(3)))
        assertEquals("刚刚", TimeUtil.formatRelative(ago(40)))
    }

    @Test
    fun `满一分钟后显示分钟前`() {
        assertEquals("1分钟前", TimeUtil.formatRelative(ago(90)))
        assertEquals("2分钟前", TimeUtil.formatRelative(ago(150)))
        assertEquals("59分钟前", TimeUtil.formatRelative(ago(3590)))
    }

    @Test
    fun `满一小时后显示小时前`() {
        assertEquals("1小时前", TimeUtil.formatRelative(ago(3700)))
        assertEquals("2小时前", TimeUtil.formatRelative(ago(7300)))
        assertEquals("23小时前", TimeUtil.formatRelative(ago(86300)))
    }

    @Test
    fun `满一天后显示天前`() {
        assertEquals("1天前", TimeUtil.formatRelative(ago(86500)))
        assertEquals("6天前", TimeUtil.formatRelative(ago(86400L * 6 + 100)))
    }

    @Test
    fun `超过一周显示日期而不是天数`() {
        val out = TimeUtil.formatRelative(ago(86400L * 10))
        assertTrue("应为 yyyy-MM-dd，实际：$out", Regex("""\d{4}-\d{2}-\d{2}""").matches(out))
    }

    @Test
    fun `时间戳前后空白被忽略`() {
        val ts = ago(120)
        assertEquals(TimeUtil.formatRelative(ts), TimeUtil.formatRelative("  $ts  "))
    }

    // ===== formatDate：精确到日 =====

    @Test
    fun `formatDate 空值返回空串`() {
        assertEquals("", TimeUtil.formatDate(null))
        assertEquals("", TimeUtil.formatDate(""))
        assertEquals("", TimeUtil.formatDate("xyz"))
        assertEquals("", TimeUtil.formatDate("0"))
    }

    @Test
    fun `formatDate 输出年月日`() {
        val out = TimeUtil.formatDate("1700000000")
        assertEquals("2023-11-15", out)
    }

    // ===== formatDateTime：精确到分 =====

    @Test
    fun `formatDateTime 空值返回空串`() {
        assertEquals("", TimeUtil.formatDateTime(null))
        assertEquals("", TimeUtil.formatDateTime("abc"))
        assertEquals("", TimeUtil.formatDateTime("0"))
    }

    @Test
    fun `formatDateTime 输出年月日时分`() {
        val out = TimeUtil.formatDateTime("1700000000")
        assertTrue("应为 yyyy-MM-dd HH:mm，实际：$out", Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}""").matches(out))
    }

    // ===== formatSmart：详情页楼层时间 =====

    @Test
    fun `formatSmart 空值返回空串`() {
        assertEquals("", TimeUtil.formatSmart(null))
        assertEquals("", TimeUtil.formatSmart("   "))
    }

    @Test
    fun `formatSmart 对久远楼层的日期串原样返回`() {
        // 服务端对老楼层直接返回 "2016-11-11 22:37" 这种串，不能当时间戳解析
        assertEquals("2016-11-11 22:37", TimeUtil.formatSmart("2016-11-11 22:37"))
    }

    @Test
    fun `formatSmart 对时间戳走相对时间`() {
        assertEquals("刚刚", TimeUtil.formatSmart(ago(5)))
        assertEquals("2小时前", TimeUtil.formatSmart(ago(7300)))
    }

    @Test
    fun `formatSmart 未来时间戳返回空串`() {
        assertEquals("", TimeUtil.formatSmart((nowSeconds() + 600).toString()))
    }
}
