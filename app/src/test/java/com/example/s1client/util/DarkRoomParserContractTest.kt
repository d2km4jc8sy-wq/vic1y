package com.example.s1client.util

import com.example.s1client.data.model.DarkRoomEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DarkRoomParser 对真实接口的契约测试（阶段二，2026-10-16）。
 *
 * fixture：游客态实抓 `forum.php?mod=misc&action=showdarkroom&ajaxdata=json`
 * （2026-10-16 快照，application/json，无登录墙）。
 * 关键契约：服务端返回**裸数字键的坏 JSON**（`"data":{258876:{...}}`），
 * 必须先过 normalizeNumericKeys 才能交给 Gson——这是小黑屋解析的生死线。
 * 上游改版时，这里的失败直接指认 DarkRoomParser 契约断裂。
 */
class DarkRoomParserContractTest {

    private val body: String by lazy {
        javaClass.classLoader!!.getResourceAsStream("fixtures/darkroom_guest.html")!!
            .readBytes().toString(Charsets.UTF_8)
    }

    @Test
    fun `解析出47条记录且外壳字段齐全`() {
        val page = DarkRoomParser.parse(body)
        assertEquals(47, page.items.size)
        assertEquals("1", page.dataExist)
        assertEquals("79021", page.nextCursor)
        assertTrue(page.hasMore)
        assertFalse(page.loginRequired)
        assertEquals("", page.parseError)
    }

    @Test
    fun `动作列全部为禁止发言`() {
        val page = DarkRoomParser.parse(body)
        assertTrue(page.items.all { it.action == "禁止发言" })
    }

    @Test
    fun `处罚人去重后为6人`() {
        val page = DarkRoomParser.parse(body)
        assertEquals(6, page.items.map { it.operatorName }.distinct().size)
    }

    @Test
    fun `永久封禁边界样本被正确识别`() {
        val page = DarkRoomParser.parse(body)
        val never = page.items.filter { it.neverExpires }
        assertTrue("至少应识别出 7 条永不过期（实测快照）", never.size >= 7)
        val sample = page.items.firstOrNull { it.cid == "79091" }
        if (sample != null) {
            assertEquals("kankanaaaa", sample.username)
            assertTrue(sample.neverExpires)
        }
    }

    @Test
    fun `裸数字键坏JSON被规范化后无丢记录`() {
        // normalizeNumericKeys 的直接契约：data 下裸数字键补引号后条数不变
        val normalized = DarkRoomParser.normalizeNumericKeys(body)
        val rawCount = Regex("\"(cid)\"").findAll(body).count()
        val fixed = DarkRoomParser.parse(normalized)
        assertEquals(rawCount, fixed.items.size)
    }

    @Test
    fun `条目字段非默认值`() {
        val page = DarkRoomParser.parse(body)
        val sample: DarkRoomEntry = page.items.first()
        assertTrue(sample.cid.isNotBlank())
        assertTrue(sample.username.isNotBlank())
        assertTrue(sample.dateline.isNotBlank())
    }
}
