package com.example.s1client.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RatingParser 对真实页面结构的契约测试（阶段二，2026-10-16）。
 *
 * fixture：游客态实抓（2026-10-16 快照），采集与形态分析见 notes_fixtures.md。
 * 服务端会把 mobile=2 302 回桌面模板（mobile=no），与源码注释一致，
 * 所以 fixture 覆盖的是 parse() 的桌面 `<tr>` 兜底分支——正是线上实际命中的分支。
 *
 * 上游改版时，这里的失败会直接指认是 RatingParser 的哪条契约断了。
 */
class RatingParserContractTest {

    private fun fixture(name: String): String =
        javaClass.classLoader!!.getResourceAsStream("fixtures/$name")!!
            .readBytes().toString(Charsets.UTF_8)

    /** viewthread 第 1 页（tid=2290331，归墟·汉密尔顿巡演帖），7 个楼层带 ratelog */
    private val viewthread: String by lazy { fixture("viewthread_p1_guest.html") }

    @Test
    fun `viewthread解析出全部7个带评分楼层`() {
        val map = RatingParser.parse(viewthread)
        assertEquals(
            listOf(
                "70275730", "70275778", "70275819", "70275826",
                "70275889", "70276343", "70276711"
            ),
            map.keys.sorted()
        )
    }

    @Test
    fun `聚合数取自块头参与人数而非行数`() {
        val map = RatingParser.parse(viewthread)
        // pid 70275730：块头「参与人数 2」；总分为两条分值代数和（+1 与 -1 → 0）
        val info = map["70275730"]
        assertNotNull(info)
        assertEquals(2, info!!.count)
        assertEquals(0, info.total)
    }

    @Test
    fun `桌面分支现状_负分单元格td-xg1被跳过_只收xi1正分`() {
        // ⚠️ 固化现状而非理想行为：td.xi1 ×7 / td.xg1 ×6，parse() 只取 xi1，
        // 因此 7 个楼层的 entries 合计应为 7（每块 1 条正分）。
        // 若未来修复为收录负分，此断言应同步改为 13 并更新本注释（见 PROBLEMS P7）。
        val map = RatingParser.parse(viewthread)
        assertEquals(7, map.values.sumOf { it.entries.size })
        val first = map["70275730"]!!
        assertEquals(1, first.entries.size)
        assertEquals(1, first.entries[0].amount)
        assertEquals("firewings0607", first.entries[0].name)
    }

    @Test
    fun `理由文本被保留`() {
        val map = RatingParser.parse(viewthread)
        val first = map["70275730"]!!
        assertEquals("来必看", first.entries[0].reason)
    }

    @Test
    fun `viewratings明细页解析出两条含负分的完整记录`() {
        val info = RatingParser.parseViewRatings(fixture("viewratings_guest.html"), "70275730")
        assertNotNull(info)
        assertEquals(2, info!!.entries.size)
        assertEquals(0, info.total)
        // 列序「分值/用户名/时间/理由」，分值形如「战斗力 +1 鹅」
        val byName = info.entries.associateBy { it.name }
        assertEquals(setOf("firewings0607", "长门改二"), byName.keys)
        assertEquals(1, byName["firewings0607"]!!.amount)
        assertEquals(-1, byName["长门改二"]!!.amount)
        assertEquals("来必看", byName["firewings0607"]!!.reason)
        assertEquals("思路广", byName["长门改二"]!!.reason)
        assertTrue(byName["firewings0607"]!!.uid.isNotBlank())
    }
}
