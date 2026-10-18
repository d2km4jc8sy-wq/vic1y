package com.example.s1client.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 私信解析回归测试。
 *
 * 为什么锁这里：私信列表的「未读红点」全靠 `a.new` 这一个选择器。
 * 历史上曾拿 `cur1` 样式类当未读标记，结果整列全标了红点——
 * 这种错误界面不报错，只是所有会话看起来都像没读。
 */
class PmParserTest {

    private fun item(
        plid: String,
        uid: String,
        name: String,
        last: String,
        time: String,
        count: String = "",
        unread: Boolean = false
    ): String = buildString {
        append("""<dl id="pmlist_$plid">""")
        append("""<dt><a href="space-uid-$uid.html">$name</a></dt>""")
        append("""<dd class="ptm pm_c">$last</dd>""")
        append("""<span class="xg1">$time</span>""")
        if (unread) append("""<a class="new" href="#">未读</a>""")
        if (count.isNotEmpty()) append("""<span class="z">$count</span>""")
        append("""</dl>""")
    }

    // ===== parsePmList =====

    @Test
    fun `空 HTML 返回空列表`() {
        assertTrue(PmParser.parsePmList("").isEmpty())
        assertTrue(PmParser.parsePmList("   ").isEmpty())
    }

    @Test
    fun `解析单条私信的关键字段`() {
        val html = item("1001", "2002", "张三", "最近聊了什么", "2026-10-18", "共 3 条")
        val list = PmParser.parsePmList(html)
        assertEquals(1, list.size)
        val pm = list[0]
        assertEquals("1001", pm.plid)
        assertEquals("2002", pm.touid)
        assertEquals("张三", pm.username)
        assertEquals("最近聊了什么", pm.lastMessage)
        assertEquals("2026-10-18", pm.lastTime)
        assertEquals("3", pm.msgCount)
    }

    @Test
    fun `有 a new 标记时判为未读`() {
        val html = item("1", "2", "李四", "hi", "10-18", unread = true)
        assertTrue(PmParser.parsePmList(html)[0].isNew)
    }

    @Test
    fun `没有 a new 标记时判为已读`() {
        val html = item("1", "2", "李四", "hi", "10-18", unread = false)
        assertFalse(PmParser.parsePmList(html)[0].isNew)
    }

    @Test
    fun `共N条解析出数量`() {
        val html = item("1", "2", "王五", "hi", "10-18", count = "共 12 条")
        assertEquals("12", PmParser.parsePmList(html)[0].msgCount)
    }

    @Test
    fun `缺少条数时默认为零`() {
        val html = item("1", "2", "王五", "hi", "10-18")
        assertEquals("0", PmParser.parsePmList(html)[0].msgCount)
    }

    @Test
    fun `多条私信按文档顺序返回`() {
        val html = item("1", "11", "甲", "a", "t1") + item("2", "22", "乙", "b", "t2")
        val list = PmParser.parsePmList(html)
        assertEquals(2, list.size)
        assertEquals("甲", list[0].username)
        assertEquals("乙", list[1].username)
    }

    @Test
    fun `缺少用户链接的条目被跳过`() {
        val bad = """<dl id="pmlist_9"><dd class="ptm pm_c">没有用户链接</dd></dl>"""
        assertTrue(PmParser.parsePmList(bad).isEmpty())
    }

    // ===== parsePmConversation =====

    private fun message(id: String, author: String, body: String, time: String): String =
        """<dl id="pmlist_$id"><dt><span class="xw1">$author</span><span class="xg1">$time</span></dt>""" +
            """<dd class="ptm">$body</dd></dl>"""

    @Test
    fun `会话空 HTML 返回空列表`() {
        assertTrue(PmParser.parsePmConversation("").isEmpty())
    }

    @Test
    fun `解析会话消息正文与时间`() {
        val html = "<div id=\"pm_ul\" class=\"xld\">" + message("1", "张三", "你好", "10:30") + "</div>"
        val list = PmParser.parsePmConversation(html)
        assertEquals(1, list.size)
        assertEquals("你好", list[0].message)
        assertEquals("10:30", list[0].dateline)
    }

    @Test
    fun `作者为您的消息标记为本人`() {
        val html = "<div id=\"pm_ul\">" + message("1", "您", "我发的", "10:30") + "</div>"
        assertTrue(PmParser.parsePmConversation(html)[0].isSelf)
    }

    @Test
    fun `他人消息不标记为本人`() {
        val html = "<div id=\"pm_ul\">" + message("1", "张三", "他发的", "10:30") + "</div>"
        assertFalse(PmParser.parsePmConversation(html)[0].isSelf)
    }

    @Test
    fun `正文为空的消息被丢弃`() {
        val html = "<div id=\"pm_ul\">" + message("1", "张三", "", "10:30") + "</div>"
        assertTrue(PmParser.parsePmConversation(html).isEmpty())
    }

    @Test
    fun `多条会话按顺序返回`() {
        val html = "<div id=\"pm_ul\">" +
            message("1", "张三", "第一条", "10:00") +
            message("2", "您", "第二条", "10:05") +
            "</div>"
        val list = PmParser.parsePmConversation(html)
        assertEquals(2, list.size)
        assertEquals("第一条", list[0].message)
        assertEquals("第二条", list[1].message)
    }
}
