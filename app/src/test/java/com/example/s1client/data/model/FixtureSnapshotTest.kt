package com.example.s1client.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 真实数据快照测试：fixtures/ 里的三个 JSON 是 2026-10-13 从 stage1st 公共接口
 * 原样抓取的（forumindex → forumdisplay(fid=135) → viewthread(首帖)），
 * 代表接口的"真实长相"而非手造样例。
 *
 * 这些测试的职责：任何模型字段改动若破坏了对真实响应的兼容，这里第一时间报警。
 */
class FixtureSnapshotTest {

    private val gson = Gson()

    private fun loadFixture(name: String): String {
        val stream = javaClass.classLoader!!.getResourceAsStream("fixtures/$name")
        assertNotNull("缺少 fixture: fixtures/$name（该文件不入库时测试自动失效的前提不成立）", stream)
        return stream!!.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    /** 论坛索引：版块列表与分类必须能从真实响应中完整还原 */
    @Test
    fun `forumindex 真实响应解析`() {
        val r = gson.fromJson(loadFixture("forumindex.json"), ForumIndexResponse::class.java)
        val v = r.variables
        assertNotNull(v)
        assertTrue("版块列表不应为空", (v?.forumList?.size ?: 0) > 0)
        assertTrue("分类列表不应为空", (v?.catList?.size ?: 0) > 0)
        assertTrue("formhash 游客也有", !v?.formhash.isNullOrBlank())
        // 游客身份：member_uid 为 "0" 或空
        val uid = v?.memberUid
        assertTrue("游客快照 uid 应为 0/空", uid == null || uid == "0" || uid.isEmpty())
        // SubForum 递归子版块结构存在
        val withSub = v?.forumList?.firstOrNull { !it.subList.isNullOrEmpty() }
        if (withSub != null) {
            assertTrue("子版块应有自己的 fid", !withSub.subList!!.first().fid.isNullOrBlank())
        }
    }

    /** 版块帖子列表：51 帖的真实列表，逐字段校验 Thread 的别名链 */
    @Test
    fun `forumdisplay 真实响应解析`() {
        val r = gson.fromJson(loadFixture("forumdisplay_fid135.json"), ThreadListResponse::class.java)
        val v = r.variables
        assertNotNull(v)
        val list = v?.threadList
        assertTrue("帖子列表不应为空", (list?.size ?: 0) >= 40)

        // 每帖必须有 tid 和标题（快照里全部帖子过一遍）
        for (t in list!!) {
            assertTrue("tid 不应为空", !t.tid.isNullOrBlank())
            assertNotNull("subject 不应为 null（无标题帖不符合 Discuz 结构）", t.subject)
        }

        // 版块主题总数（真实总页数的计算依据）
        assertTrue("forum.threads 应为数字", (v?.forum?.threads ?: "0").toIntOrNull() != null)

        // 帖子分类映射：types 是 typeid->名称 的 map
        v?.threadTypes?.types?.let { types ->
            assertTrue("分类 map 不应为空", types.isNotEmpty())
        }

        // 抽一帖校验别名链（dateline 四级回退的第一级真实存在）
        val first = list.first()
        assertNotNull("首帖 dateline 应有值", first.dateline)
    }

    /** 帖子详情：楼层列表与 Post 的别名链在真实数据上成立 */
    @Test
    fun `viewthread 真实响应解析`() {
        val r = gson.fromJson(loadFixture("viewthread_tid2206666.json"), ThreadDetailResponse::class.java)
        val v = r.variables
        assertNotNull(v)
        assertEquals("详情 tid 应与请求一致", "2206666", v?.thread?.tid)
        val posts = v?.postList
        assertTrue("楼层列表不应为空", (posts?.size ?: 0) > 0)

        // 首楼：first="1"，楼层号来自 position/floor 别名
        val first = posts!!.first()
        assertEquals("首页首楼 first 应为 1", "1", first.first)
        assertTrue("首楼应有楼层号", !first.position.isNullOrBlank())
        assertTrue("首楼应有时间", !first.dateline.isNullOrBlank())
        assertTrue("首楼应有正文", !first.message.isNullOrBlank())

        // 作者一致性：详情头与首楼作者相同
        assertEquals(v?.thread?.author, first.author)
    }
}
