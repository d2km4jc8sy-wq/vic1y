package com.example.s1client.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 小黑屋解析测试。
 *
 * 核心验证点：S1 返回的 `data` 是**裸数字键对象**（`{579096:{...}}`），
 * 这**不是合法 JSON**，不处理必然解析失败 —— s1er 在 Dart 侧专门写了
 * `ensureJson` 兜这个坑。这里锁住该行为。
 */
class DarkRoomParserTest {

    /** 从真实接口抓到的响应样本（已截断） */
    private val realSample = """{"message":"1|79003","data":{579096:{"cid":"79088","uid":"579096","operatorid":"178545","operator":"巴マミ","action":"禁止发言","reason":"去治肾，别来了","dateline":"2026-10-12 16:51","username":"精卵结合","groupexpiry":"永不过期"},579209:{"cid":"79087","uid":"579209","operatorid":"464256","operator":"活久见","action":"禁止发言","reason":"又发病了是吧","dateline":"2026-10-12 15:59","username":"惑司","groupexpiry":"2027-1-10 15:59"}}}"""

    // ===== 裸数字键：核心场景 =====

    @Test
    fun `能解析裸数字键的真实响应`() {
        val p = DarkRoomParser.parse(realSample)
        assertEquals(2, p.items.size)
        assertEquals("79088", p.items[0].cid)
        assertEquals("精卵结合", p.items[0].username)
    }

    @Test
    fun `标准化后是合法 JSON`() {
        val fixed = DarkRoomParser.normalizeNumericKeys(realSample)
        // 数字键已加引号
        assertTrue(fixed.contains("\"579096\":"))
        // 原来的非法写法已消失
        assertFalse(fixed.contains("{579096:"))
        // 并且能直接被 Gson 解析
        com.google.gson.JsonParser.parseString(fixed)
    }

    @Test
    fun `标准 JSON 不被破坏`() {
        val normal = """{"message":"1|100","data":[{"cid":"1","uid":"2","username":"a"}]}"""
        val p = DarkRoomParser.parse(normal)
        assertEquals(1, p.items.size)
        assertEquals("a", p.items[0].username)
    }

    @Test
    fun `逗号后的裸数字键也要处理`() {
        // 真实响应里 {579096:{...},579209:{...} 的第二个键在逗号之后，
        // 只处理 '{' 会漏掉第二条及之后的条目（早前真实踩过）。
        val p = DarkRoomParser.parse(realSample)
        assertEquals("解析出全部 2 条，不能漏掉逗号后的第二条", 2, p.items.size)
        assertEquals("79087", p.items[1].cid)
        assertEquals("惑司", p.items[1].username)
    }

    @Test
    fun `字符串内容里的花括号数字不被误改`() {
        val s = """{"message":"1|5","data":{1:{"reason":"他说{123 是这样","cid":"9"}}}"""
        val fixed = DarkRoomParser.normalizeNumericKeys(s)
        // reason 内部的 {123 必须原样保留
        assertTrue(fixed.contains("{123 是这样"))
        // 只有顶层数字键被加引号
        assertTrue(fixed.contains("\"1\":"))
    }

    // ===== 游标 / 分页 =====

    @Test
    fun `解析出下一页游标`() {
        val p = DarkRoomParser.parse(realSample)
        assertEquals("1", p.dataExist)
        assertEquals("79003", p.nextCursor)
        assertTrue(p.hasMore)
    }

    @Test
    fun `dataExist 为 0 时到底`() {
        val p = DarkRoomParser.parse("""{"message":"0|","data":{1:{"cid":"1"}}}""")
        assertFalse(p.hasMore)
    }

    @Test
    fun `游标与请求相同则不判还有更多`() {
        // 防止服务端回同一个游标导致死循环翻页
        val p = DarkRoomParser.parse("""{"message":"1|79003","data":{1:{"cid":"1"}}}""", requestCursor = "79003")
        assertFalse(p.hasMore)
    }

    // ===== 字段 =====

    @Test
    fun `处罚字段完整`() {
        val p = DarkRoomParser.parse(realSample)
        val e = p.items[0]
        assertEquals("579096", e.uid)
        assertEquals("178545", e.operatorId)
        assertEquals("巴マミ", e.operatorName)
        assertEquals("禁止发言", e.action)
        assertEquals("去治肾，别来了", e.reason)
        assertEquals("2026-10-12 16:51", e.dateline)
        assertTrue(e.neverExpires)
    }

    @Test
    fun `有到期时间的不是永不过期`() {
        val p = DarkRoomParser.parse(realSample)
        assertFalse(p.items[1].neverExpires)
        assertEquals("2027-1-10 15:59", p.items[1].groupExpiry)
    }

    @Test
    fun `缺字段时用空串不崩`() {
        val p = DarkRoomParser.parse("""{"message":"1|2","data":{1:{"cid":"7"}}}""")
        assertEquals("7", p.items[0].cid)
        assertEquals("", p.items[0].username)
        assertEquals("", p.items[0].action)
    }

    // ===== 异常输入 =====

    @Test
    fun `空响应`() {
        val p = DarkRoomParser.parse("")
        assertTrue(p.items.isEmpty())
        assertFalse(p.parseError.isEmpty())
    }

    @Test
    fun `HTML 页面也不崩`() {
        val p = DarkRoomParser.parse("<html><body>登录</body></html>")
        assertTrue(p.items.isEmpty())
    }

    @Test
    fun `未登录能识别`() {
        assertTrue(DarkRoomParser.parse("""{"error":"to_login"}""").loginRequired)
        assertTrue(DarkRoomParser.parse("""<a href="to_login">请登录</a>""").loginRequired)
    }

    @Test
    fun `data 为空对象`() {
        val p = DarkRoomParser.parse("""{"message":"0|","data":{}}""")
        assertTrue(p.items.isEmpty())
        assertFalse(p.hasMore)
    }
}
