package com.example.s1client

import com.example.s1client.util.PostLocator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 楼层定位：页号解析是纯函数，最适合单测覆盖。
 * 真实请求部分（OkHttp / 重定向）需要设备环境，交给真机验收。
 *
 * 用例口径照 s1er 的 `pageFromFindpostLocation`：
 * page 参数 > 伪静态路径 > page<1 归一为 1 > 同 tid 的帖子链接触发视为 1 > 其它 null。
 */
class PostLocatorTest {

    @Test fun `thread-路径带页号`() {
        assertEquals(3, PostLocator.pageFromFindpostLocation(
            "thread-2291675-3-1.html#pid70368274", "2291675"))
    }

    @Test fun `forum-php-查询串带page`() {
        assertEquals(7, PostLocator.pageFromFindpostLocation(
            "forum.php?mod=viewthread&tid=2291675&page=7", "2291675"))
    }

    /** 真机实测的形态：301 Location 就是这种相对 URL + #pid 锚点 */
    @Test fun `真机实测的Location形态`() {
        assertEquals(1, PostLocator.pageFromFindpostLocation(
            "forum.php?mod=viewthread&tid=2291675&page=1&mobile=2#pid70368274", "2291675"))
    }

    @Test fun `page为0归一为第1页`() {
        assertEquals(1, PostLocator.pageFromFindpostLocation(
            "forum.php?mod=viewthread&tid=2291675&page=0", "2291675"))
    }

    @Test fun `相对协议写法`() {
        assertEquals(2, PostLocator.pageFromFindpostLocation(
            "//stage1st.com/2b/thread-2291675-2-1.html", "2291675"))
    }

    @Test fun `无页号但跳到帖子本体视为第1页`() {
        assertEquals(1, PostLocator.pageFromFindpostLocation(
            "forum.php?mod=viewthread&tid=2291675", "2291675"))
    }

    @Test fun `纯thread路径无页号也是第1页`() {
        assertEquals(1, PostLocator.pageFromFindpostLocation(
            "thread-2291675-1-1.html", "2291675"))
    }

    /**
     * 判定顺序回归（照 s1er）：**有明确页号就先信页号**，
     * tid 校验只在"完全没有 page"时才起作用（那时才需要它来兜底成第 1 页）。
     */
    @Test fun `tid不一致的边界`() {
        // 无 page、tid 对不上 → 不许当成第 1 页（否则会静默跳错帖）
        assertNull(PostLocator.pageFromFindpostLocation(
            "forum.php?mod=viewthread&tid=9999999", "2291675"))
        // 路径里带了页号 → 按 s1er 的 `fromQuery ?? fromPath` 优先取页号
        assertEquals(1, PostLocator.pageFromFindpostLocation(
            "thread-2291675-1-1.html", "2291675"))
    }

    @Test fun `空值返回null`() {
        assertNull(PostLocator.pageFromFindpostLocation(null, "2291675"))
        assertNull(PostLocator.pageFromFindpostLocation("", "2291675"))
    }

    @Test fun `非帖子链接返回null`() {
        assertNull(PostLocator.pageFromFindpostLocation("home.php?mod=space&uid=123", "2291675"))
        assertNull(PostLocator.pageFromFindpostLocation(
            "forum.php?mod=forumdisplay&fid=45", "2291675"))
    }

    // —— 第三级：HTML 兜底 ——

    @Test fun `html取div-pg里的页码`() {
        val html = """<div class="pgt"><div class="pg"><strong>5</strong></div></div>"""
        assertEquals(5, PostLocator.pageFromViewthreadHtml(html))
    }

    @Test fun `html只有pgt但看得出是读帖页则为第1页`() {
        assertEquals(1, PostLocator.pageFromViewthreadHtml("""<div id="postlist">x</div>"""))
        assertEquals(1, PostLocator.pageFromViewthreadHtml("""<div class="pg_viewthread">"""))
    }

    @Test fun `html完全不像帖子返回null`() {
        assertNull(PostLocator.pageFromViewthreadHtml(""))
        assertNull(PostLocator.pageFromViewthreadHtml("<html><body>登录</body></html>"))
    }
}
