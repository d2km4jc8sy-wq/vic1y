package com.example.s1client.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 模型解析快照测试：锁定 Discuz 接口的"暗知识"——字段别名、键序、类型兜底。
 * 这些映射是从真实抓包逆向出来的，改坏任何一个，UI 上就是日期丢失/楼层错乱这类
 * "说不清哪里不对"的问题。改任何模型字段前先跑这里。
 */
class ModelParseTest {

    private val gson = Gson()

    /** Thread.dateline 的 alternate 键序：dateline > dbdateline > lastpost > lastpost_dateline */
    @Test
    fun `Thread dateline 三级别名回退`() {
        val t1 = gson.fromJson("""{"tid":"1","dateline":"2026-10-13 12:00"}""", Thread::class.java)
        assertEquals("2026-10-13 12:00", t1.dateline)

        val t2 = gson.fromJson("""{"tid":"1","dbdateline":"1697170000"}""", Thread::class.java)
        assertEquals("1697170000", t2.dateline)

        val t3 = gson.fromJson("""{"tid":"1","lastpost":"1697170001"}""", Thread::class.java)
        assertEquals("1697170001", t3.dateline)

        val t4 = gson.fromJson("""{"tid":"1","lastpost_dateline":"1697170002"}""", Thread::class.java)
        assertEquals("1697170002", t4.dateline)
    }

    /** Thread.typeId/typename 的双别名 */
    @Test
    fun `Thread typeId 别名映射`() {
        val t = gson.fromJson("""{"typeid":"37","typename":"求助"}""", Thread::class.java)
        assertEquals("37", t.typeId)
        assertEquals("求助", t.typeName)
    }

    /** Post.position：部分接口给 position，部分给 floor / postposition */
    @Test
    fun `Post 楼层双别名`() {
        val p1 = gson.fromJson("""{"pid":"9","position":"3"}""", Post::class.java)
        assertEquals("3", p1.position)
        val p2 = gson.fromJson("""{"pid":"9","floor":"7"}""", Post::class.java)
        assertEquals("7", p2.position)
    }

    /** attachmentImagePreviewList：服务端可能回空数组占位，不能炸 */
    @Test
    fun `Thread 附件预览空数组兜底`() {
        val t = gson.fromJson("""{"tid":"1","attachmentImagePreviewList":[]}""", Thread::class.java)
        assertTrue(t.attachmentImagePreviewList?.isEmpty() ?: false)
        val t2 = gson.fromJson("""{"tid":"1"}""", Thread::class.java)
        assertEquals(null, t2.attachmentImagePreviewList)
    }

    /** MessageInfo：Discuz 通用消息体（messageval 代码 + messagestr 文案） */
    @Test
    fun `MessageInfo 通用消息体`() {
        val m = gson.fromJson(
            """{"messageval":"post_newreply_succeed","messagestr":"回复发布成功"}""",
            MessageInfo::class.java
        )
        assertEquals("post_newreply_succeed", m.messageVal)
        assertEquals("回复发布成功", m.messageStr)
    }

    /** 登录响应：group 嵌套 + Message 错误原因 */
    @Test
    fun `LoginResponse 组与错误消息`() {
        val r = gson.fromJson(
            """{"Variables":{"member_uid":"229182","member_username":"tester",
                "groupid":"17","group":{"groupid":"17","grouptitle":"正式会员"}},
                "Message":{"messageval":"login_succeed","messagestr":"登录成功"}}""",
            LoginResponse::class.java
        )
        assertEquals("tester", r.variables?.memberUsername)
        assertEquals("正式会员", r.variables?.group?.groupTitle)
        assertEquals("login_succeed", r.message?.messageVal)
    }

    /** 私信列表项：isNew 是解析层算好的布尔（a.new 标记），不是接口直出 */
    @Test
    fun `PmListItem 基本映射`() {
        val p = gson.fromJson(
            """{"plid":"88","touid":"100","username":"医生狼多",
                "lastMessage":"你好","lastTime":"昨天 20:00","msgCount":"3","isNew":true}""",
            PmListItem::class.java
        )
        assertEquals("88", p.plid)
        assertTrue(p.isNew)
        assertEquals("3", p.msgCount)
    }
}
