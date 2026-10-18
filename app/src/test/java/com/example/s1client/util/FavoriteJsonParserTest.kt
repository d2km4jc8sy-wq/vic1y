package com.example.s1client.util

import com.example.s1client.data.model.FavoriteJsonItem
import com.example.s1client.data.model.FavoriteListResponse
import com.example.s1client.data.model.FavoriteMessage
import com.example.s1client.data.model.FavoriteVariables
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 收藏列表 JSON 解析回归测试。
 *
 * 为什么锁这里：收藏页全靠这个接口，字段取错不会崩——
 * 只会让列表变空、标题带 HTML 标签、或者分页永远只有一页。
 * 总页数算法是照搬 s1er 的（ceil(count / perpage)），这里把口径钉住，防止有人「优化」成别的算法。
 */
class FavoriteJsonParserTest {

    private fun item(
        tid: String? = "100",
        id: String? = null,
        idtype: String? = "thread",
        title: String? = "标题",
        subject: String? = null,
        replies: String? = "5",
        dateline: String? = "1700000000",
        dbdateline: String? = null
    ) = FavoriteJsonItem(
        favid = "1", idtype = idtype, tid = tid, id = id,
        title = title, subject = subject, replies = replies,
        dateline = dateline, dbdateline = dbdateline
    )

    private fun resp(
        list: List<FavoriteJsonItem>?,
        perpage: String? = "20",
        count: String? = "0",
        messageval: String? = null
    ) = FavoriteListResponse(
        version = "1",
        variables = FavoriteVariables(list = list, perpage = perpage, count = count),
        message = messageval?.let { FavoriteMessage(it, "") }
    )

    // ===== 空数据 =====

    @Test
    fun `variables 为 null 时返回空页`() {
        val out = FavoriteJsonParser.parse(FavoriteListResponse("1", null, null), page = 1)
        assertTrue(out.items.isEmpty())
        assertEquals(1, out.currentPage)
        assertEquals(1, out.totalPages)
    }

    @Test
    fun `list 为 null 时返回空页`() {
        val out = FavoriteJsonParser.parse(resp(null), page = 1)
        assertTrue(out.items.isEmpty())
    }

    // ===== 字段映射 =====

    @Test
    fun `正常条目映射出标题与楼层数`() {
        val out = FavoriteJsonParser.parse(resp(listOf(item())), page = 1)
        assertEquals(1, out.items.size)
        assertEquals("100", out.items[0].tid)
        assertEquals("标题", out.items[0].subject)
        assertEquals("5", out.items[0].replies)
    }

    @Test
    fun `tid 为空时用 id 兜底`() {
        val out = FavoriteJsonParser.parse(resp(listOf(item(tid = null, id = "777"))), page = 1)
        assertEquals("777", out.items[0].tid)
    }

    @Test
    fun `tid 与 id 都为空时跳过该条`() {
        val out = FavoriteJsonParser.parse(resp(listOf(item(tid = null, id = null))), page = 1)
        assertTrue(out.items.isEmpty())
    }

    @Test
    fun `标题为空时跳过该条`() {
        val out = FavoriteJsonParser.parse(resp(listOf(item(title = null, subject = null))), page = 1)
        assertTrue(out.items.isEmpty())
    }

    @Test
    fun `subject 在 title 缺失时顶上`() {
        val out = FavoriteJsonParser.parse(resp(listOf(item(title = null, subject = "副标题"))), page = 1)
        assertEquals("副标题", out.items[0].subject)
    }

    @Test
    fun `标题里的 HTML 标签被剥离`() {
        val out = FavoriteJsonParser.parse(resp(listOf(item(title = "<b>加粗</b>标题"))), page = 1)
        assertEquals("加粗标题", out.items[0].subject)
    }

    @Test
    fun `标题里的实体被还原且不双重解码`() {
        val out = FavoriteJsonParser.parse(resp(listOf(item(title = "百台&amp;泛泛"))), page = 1)
        assertEquals("百台&泛泛", out.items[0].subject)
    }

    @Test
    fun `版块收藏被过滤掉`() {
        val list = listOf(item(tid = "1", idtype = "forum"), item(tid = "2", idtype = "thread"))
        val out = FavoriteJsonParser.parse(resp(list), page = 1)
        assertEquals(1, out.items.size)
        assertEquals("2", out.items[0].tid)
    }

    @Test
    fun `idtype 缺失时按帖子处理`() {
        val out = FavoriteJsonParser.parse(resp(listOf(item(idtype = null))), page = 1)
        assertEquals(1, out.items.size)
    }

    // ===== 分页口径 =====

    @Test
    fun `count 与 perpage 决定总页数`() {
        // 45 条 / 每页 20 → 3 页（向上取整）
        val out = FavoriteJsonParser.parse(resp(listOf(item()), perpage = "20", count = "45"), page = 1)
        assertEquals(3, out.totalPages)
    }

    @Test
    fun `刚好整除时不额外加页`() {
        val out = FavoriteJsonParser.parse(resp(listOf(item()), perpage = "20", count = "40"), page = 1)
        assertEquals(2, out.totalPages)
    }

    @Test
    fun `count 缺失但本页已满时假定还有下一页`() {
        val items = (1..20).map { item(tid = "$it") }
        val out = FavoriteJsonParser.parse(resp(items, perpage = "20", count = null), page = 2)
        assertEquals(3, out.totalPages)
    }

    @Test
    fun `count 缺失且本页不满时当前页即末页`() {
        val items = (1..5).map { item(tid = "$it") }
        val out = FavoriteJsonParser.parse(resp(items, perpage = "20", count = null), page = 2)
        assertEquals(2, out.totalPages)
    }

    @Test
    fun `perpage 非法时回落到默认二十`() {
        val items = (1..20).map { item(tid = "$it") }
        val out = FavoriteJsonParser.parse(resp(items, perpage = "abc", count = null), page = 1)
        assertEquals(2, out.totalPages)
    }

    @Test
    fun `总页数至少为一`() {
        val out = FavoriteJsonParser.parse(resp(listOf(item()), perpage = "20", count = "0"), page = 1)
        assertTrue(out.totalPages >= 1)
    }

    // ===== 登录失效识别 =====

    @Test
    fun `login_before_enter_home 判为需要登录`() {
        assertTrue(FavoriteJsonParser.isLoginRequired(resp(null, messageval = "login_before_enter_home")))
    }

    @Test
    fun `to_login 判为需要登录`() {
        assertTrue(FavoriteJsonParser.isLoginRequired(resp(null, messageval = "to_login")))
    }

    @Test
    fun `其他提示不判为需要登录`() {
        assertFalse(FavoriteJsonParser.isLoginRequired(resp(null, messageval = "favorite_does_not_exist")))
        assertFalse(FavoriteJsonParser.isLoginRequired(resp(null, messageval = null)))
    }
}
