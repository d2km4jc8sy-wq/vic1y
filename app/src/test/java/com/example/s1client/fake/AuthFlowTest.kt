package com.example.s1client.fake

import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.util.PmParser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P3 验证：用 FakeAuthRepository（MockK）预置登录态后，
 * 需要登录的功能（私信列表解析管线）在测试中可完整走通。
 * 全部 Mock 逻辑在测试目录，生产代码零改动。
 */
class AuthFlowTest {

    /** 登录成功预设：状态写入 + isLoggedIn 语义 */
    @Test
    fun `登录成功预设`() = runTest {
        val fake = FakeAuthRepository(loggedIn = true)
        fake.install()

        assertTrue("应处于登录态", ForumDataCache.isLoggedIn())
        assertEquals("fixture_user", ForumDataCache.memberUsername)
        assertEquals("229182", ForumDataCache.memberUid)
    }

    /** 登录失败预设：clearUser 语义 */
    @Test
    fun `登录失败预设`() = runTest {
        val fake = FakeAuthRepository(loggedIn = false)
        fake.install()

        assertFalse("应处于未登录态", ForumDataCache.isLoggedIn())
        assertEquals("0", ForumDataCache.memberUid)
    }

    /** MockK stub 的 S1Api.login 端点按预设返回成功响应 */
    @Test
    fun `login端点stub返回成功`() = runTest {
        val fake = FakeAuthRepository(loggedIn = true)
        fake.stubLoginEndpoint()

        val resp = fake.mockS1Api.login(
            username = "fixture_user", password = "pw", formhash = "h"
        )
        assertEquals("login_succeed", resp.message?.messageVal)
        assertEquals("fixture_user", resp.variables?.memberUsername)
    }

    /**
     * 登录态下私信管线走通：FakeAuthRepository 预置登录 →
     * PmParser 解析 pmlist HTML（含 a.new 未读标记——cur1 不是未读的暗知识用例）。
     */
    @Test
    fun `登录态下私信列表解析管线`() = runTest {
        val fake = FakeAuthRepository(loggedIn = true)
        fake.install()

        val html = """
        <div id="pm">
          <dl id="pmlist_66">
            <dt><a href="space-uid-100" class="new">医生狼多</a></dt>
            <dd class="ptm pm_c">最近消息内容</dd>
            <span class="xg1">2026-10-13 20:00</span>
            <span class="z">共 3 条</span>
          </dl>
          <dl id="pmlist_67">
            <dt><a href="space-uid-101">已读用户</a></dt>
            <dd class="ptm pm_c">另一条</dd>
            <span class="xg1">2026-10-12 09:00</span>
            <span class="z">共 1 条</span>
          </dl>
        </div>
        """.trimIndent()

        val list = PmParser.parsePmList(html)
        assertEquals(2, list.size)
        assertEquals("66", list[0].plid)
        assertEquals("100", list[0].touid)
        assertTrue("a.new 标记 → 未读", list[0].isNew)
        assertFalse("无 a.new → 已读", list[1].isNew)
        assertEquals("3", list[0].msgCount)

        // 登录态贯穿：解析发生在已登录的 ForumDataCache 上下文中
        assertTrue(ForumDataCache.isLoggedIn())
    }
}
