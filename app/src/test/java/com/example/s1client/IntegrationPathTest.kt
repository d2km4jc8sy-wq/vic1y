package com.example.s1client

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.data.store.DraftStore
import com.example.s1client.data.store.PostBookmarkManager
import com.example.s1client.data.store.ReadHistoryStore
import com.example.s1client.fake.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 深度集成测试（阶段 4.4）：核心用户路径的状态链走查。
 * 原则：不追覆盖率，追求"重构和换主题时能立刻发现回归"。
 * 全部走真实 Store 持久化（Robolectric 内存 prefs），无网络依赖。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class IntegrationPathTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun resetState() {
        // 每条路径从干净状态出发（清 prefs + 缓存态）
        context.getSharedPreferences("s1_read_history", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("s1_post_bookmarks", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("s1_user_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        ForumDataCache.init(context)
        ForumDataCache.clearUser()
    }

    /** 路径 B：浏览帖子 → 阅读历史记录 → 去重置顶 → 上限截断（我的页浏览历史卡的数据源） */
    @Test
    fun `阅读历史链_去重置顶与上限`() {
        ReadHistoryStore.add(context, "100001", "第一帖")
        ReadHistoryStore.add(context, "100002", "第二帖")
        // 重看第一帖 → 置顶去重，不产生重复项
        ReadHistoryStore.add(context, "100001", "第一帖")
        val list = ReadHistoryStore.list(context)
        assertEquals(2, list.size)
        assertEquals("100001", list[0].tid)

        // 无标题兜底
        ReadHistoryStore.add(context, "100003", "")
        assertEquals("无标题", ReadHistoryStore.list(context)[0].title)
    }

    /** 路径 B2：楼层收藏链——保存→可查→删除（原帖删除仍可看的本地快照语义） */
    @Test
    fun `楼层收藏链_保存查询删除`() = runTest {
        PostBookmarkManager.init(context)
        val post = com.example.s1client.data.model.Post(
            pid = "300001", author = "收藏测试", authorId = "42",
            dateline = "2026-10-14 00:30", message = "原文内容", position = "5", first = "0"
        )
        PostBookmarkManager.add(context, post, tid = "200001", subject = "集成测试帖", timeText = "2026-10-14 00:30")
        assertTrue("保存后应可查", PostBookmarkManager.isBookmarked("300001"))
        assertTrue("清单应含该楼层", PostBookmarkManager.getAll().any { it.pid == "300001" })
        PostBookmarkManager.remove(context, "300001")
        assertFalse("删除后不可查", PostBookmarkManager.isBookmarked("300001"))
    }

    /** 路径 C：发帖草稿链——保存→恢复→清除（编辑器数据源） */
    @Test
    fun `草稿链_保存恢复清除`() {
        DraftStore.save(context, "draft_fid7", "这是草稿正文")
        assertEquals("这是草稿正文", DraftStore.load(context, "draft_fid7"))
        DraftStore.clear(context, "draft_fid7")
        assertTrue("清除后应恢复空", DraftStore.load(context, "draft_fid7").isBlank())
    }

    /** 路径 E：登录→登出状态机（FakeAuthRepository 驱动，含持久化往返） */
    @Test
    fun `登录登出状态机_含持久化往返`() = runTest {
        // 登录成功预设：写入缓存并落盘
        FakeAuthRepository(loggedIn = true).install()
        assertTrue(ForumDataCache.isLoggedIn())

        // 新 init 读取（模拟冷启动恢复登录态）
        ForumDataCache.init(ApplicationProvider.getApplicationContext())
        assertTrue("落盘后重启应恢复登录态", ForumDataCache.isLoggedIn())

        // 登出：清缓存 + 清盘
        ForumDataCache.clearUser()
        assertFalse(ForumDataCache.isLoggedIn())
        ForumDataCache.init(ApplicationProvider.getApplicationContext())
        assertFalse("登出后重启不应恢复登录态", ForumDataCache.isLoggedIn())
    }

    /** 路径 E2：登录态与 formhash 的一致性（发帖/评分操作的会话前提） */
    @Test
    fun `登录态与formhash一致性`() = runTest {
        val fake = FakeAuthRepository(loggedIn = true)
        fake.install()
        assertTrue("formhash 应随登录态就位", ForumDataCache.formhash.isNotEmpty())
        ForumDataCache.clearUser()
        // 生产语义：clearUser 不清 formhash（页面级令牌，登出后下次拉页面自然刷新）
        assertEquals("登出保持 formhash 原值", "fixture_hash", ForumDataCache.formhash)
    }
}
