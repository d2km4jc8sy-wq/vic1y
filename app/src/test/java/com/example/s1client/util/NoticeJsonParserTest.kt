package com.example.s1client.util

import com.example.s1client.data.model.NoticeJsonItem
import com.example.s1client.data.model.NoticeListResponse
import com.example.s1client.data.model.NoticeListVariables
import com.example.s1client.data.model.NoticeMessage
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 提醒列表 JSON（Mobile API v3 `module=mynotelist`）解析测试。
 *
 * 锁住两件关键事：
 *   ① **分页** —— 总页数 = ceil(count / perpage)，这是「能翻页取到全部提醒」的前提
 *      （原来只抓 HTML 且无分页，用户反馈过"提醒只显示第一页"）；
 *   ② **Gson 能吃下数字字段** —— 服务端 count/perpage 是数字，模型声明成 String?，
 *      实测 Gson 的 nextString() 能转换，这里锁死防止将来改模型时踩坑。
 */
class NoticeJsonParserTest {

    private fun resp(
        list: List<NoticeJsonItem>,
        count: String?,
        perpage: String?,
        page: String? = "1"
    ) = NoticeListResponse(
        version = "3",
        variables = NoticeListVariables(
            list = list, count = count, perpage = perpage, page = page, notice = null
        ),
        message = null
    )

    private fun item(
        id: String = "1",
        authorid: String? = "123",
        author: String? = "坛友A",
        note: String? = "<a href=\"forum.php?mod=redirect&goto=findpost&ptid=99&pid=88\">回复了您的帖子</a>",
        dateline: String? = "1700000000",
        isNew: String? = "1"
    ) = NoticeJsonItem(
        id = id, authorid = authorid, author = author,
        note = note, dateline = dateline, isNew = isNew
    )

    @Test
    fun `总页数按 count 与 perpage 向上取整`() {
        val p = NoticeJsonParser.parse(
            resp(listOf(item()), count = "45", perpage = "20"),
            NoticeJsonParser.FEED_MYPOST, 1
        )
        assertEquals(3, p.totalPages)   // ceil(45/20)
        assertEquals(1, p.currentPage)
    }

    @Test
    fun `刚好整除时不该多出一页`() {
        val p = NoticeJsonParser.parse(
            resp(listOf(item()), count = "40", perpage = "20"),
            NoticeJsonParser.FEED_MYPOST, 1
        )
        assertEquals(2, p.totalPages)
    }

    @Test
    fun `count 缺失时按本页是否满退化`() {
        // 本页满 20 条 → 假定后面还有一页
        val full = (1..20).map { item(id = "$it") }
        val p1 = NoticeJsonParser.parse(
            resp(full, count = null, perpage = "20"), NoticeJsonParser.FEED_MYPOST, 1
        )
        assertEquals(2, p1.totalPages)

        // 不满 → 就这一页
        val p2 = NoticeJsonParser.parse(
            resp(listOf(item()), count = null, perpage = "20"), NoticeJsonParser.FEED_MYPOST, 1
        )
        assertEquals(1, p2.totalPages)
    }

    @Test
    fun `note 去标签并提取 tid 与未读标记`() {
        val p = NoticeJsonParser.parse(
            resp(listOf(item()), count = "1", perpage = "20"), NoticeJsonParser.FEED_MYPOST, 1
        )
        val it = p.items.first()
        assertEquals("99", it.tid)
        assertTrue("正文应含提示语，实际=${it.content}", it.content.contains("回复了您的帖子"))
        assertFalse("正文不该残留标签，实际=${it.content}", it.content.contains("<a"))
        assertTrue("new=1 应判为未读", it.isNew)
        assertEquals("坛友A", it.author)
        assertEquals("1", it.id)
    }

    @Test
    fun `new 不是 1 时判为已读`() {
        val p = NoticeJsonParser.parse(
            resp(listOf(item(isNew = "0")), count = "1", perpage = "20"),
            NoticeJsonParser.FEED_MYPOST, 1
        )
        assertFalse(p.items.first().isNew)
    }

    @Test
    fun `作者为空时兜底成系统通知`() {
        val p = NoticeJsonParser.parse(
            resp(listOf(item(author = "  ", authorid = "0")), count = "1", perpage = "20"),
            NoticeJsonParser.FEED_SYSTEM, 1
        )
        assertEquals("系统通知", p.items.first().author)
        assertEquals("system", p.items.first().type)
    }

    @Test
    fun `未登录响应要能识别`() {
        val r = NoticeListResponse(
            "3", null, NoticeMessage("login_before_enter_home//1", "")
        )
        assertTrue(NoticeJsonParser.isLoginRequired(r))
        val ok = NoticeListResponse("3", null, NoticeMessage("do_success", ""))
        assertFalse(NoticeJsonParser.isLoginRequired(ok))
    }

    /** 服务端 count/perpage 是数字，模型是 String? —— 锁住 Gson 的转换行为 */
    @Test
    fun `Gson 能把 JSON 里的数字读进 String 字段`() {
        val json = """
            {"Version":"3","Variables":{"list":[{"id":"7","authorid":"5","author":"某人",
            "note":"<a href=\"forum.php?mod=redirect&goto=findpost&ptid=123&pid=9\">回复了您的帖子</a>",
            "dateline":"1700000000","new":"1"}],"count":30,"perpage":20,"page":2}}
        """.trimIndent()
        val r = Gson().fromJson(json, NoticeListResponse::class.java)
        assertEquals("30", r.variables?.count)
        assertEquals("20", r.variables?.perpage)

        val p = NoticeJsonParser.parse(r, NoticeJsonParser.FEED_MYPOST, 2)
        assertEquals(2, p.totalPages)      // ceil(30/20)
        assertEquals(2, p.currentPage)
        assertEquals("123", p.items.first().tid)
        assertTrue(p.items.first().isNew)
    }
}
