package com.example.s1client

import com.example.s1client.util.PostLocator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 楼层定位：Location 头的页号解析是纯函数，最适合单测覆盖。
 * 真实请求部分（OkHttp / 重定向）需要设备环境，交给真机验收。
 */
class PostLocatorTest {

    @Test fun `thread-路径带页号`() {
        assertEquals(3, PostLocator.parsePageFromLocation(
            "thread-2291675-3-1.html#pid70368274"))
    }

    @Test fun `forum-php-查询串带page`() {
        assertEquals(7, PostLocator.parsePageFromLocation(
            "forum.php?mod=viewthread&tid=2291675&page=7"))
    }

    @Test fun `相对协议写法`() {
        assertEquals(2, PostLocator.parsePageFromLocation(
            "//stage1st.com/2b/thread-2291675-2-1.html"))
    }

    @Test fun `无页号但跳到帖子本体视为第1页`() {
        assertEquals(1, PostLocator.parsePageFromLocation(
            "forum.php?mod=viewthread&tid=2291675"))
    }

    @Test fun `纯thread路径无页号也是第1页`() {
        assertEquals(1, PostLocator.parsePageFromLocation(
            "thread-2291675-1-1.html"))
    }

    @Test fun `空值返回null`() {
        assertNull(PostLocator.parsePageFromLocation(null))
        assertNull(PostLocator.parsePageFromLocation(""))
    }

    @Test fun `非帖子链接返回null`() {
        assertNull(PostLocator.parsePageFromLocation("home.php?mod=space&uid=123"))
        assertNull(PostLocator.parsePageFromLocation("forum.php?mod=forumdisplay&fid=45"))
    }
}
