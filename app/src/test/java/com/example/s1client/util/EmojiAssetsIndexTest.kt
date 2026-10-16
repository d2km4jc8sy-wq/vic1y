package com.example.s1client.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * EmojiAssetsIndex 的纯函数部分（URL 解析 + 索引查找）。
 * 期望行为锚点：CDN 表情地址 → 本地 assets 路径的映射规则，
 * 与 PostAdapter 时代的 localEmojiUrl 逐字一致（不改行为，只换实现）。
 */
class EmojiAssetsIndexTest {

    @Test
    fun `CDN表情地址解析出分类与文件名`() {
        val (cat, file) = EmojiAssetsIndex.parseCdnUrl(
            "https://static.stage1st.com/image/smiley/face2017/206.gif"
        )!!
        assertEquals("face2017", cat)
        assertEquals("206.gif", file)
    }

    @Test
    fun `带查询参数的地址仍可解析`() {
        val (cat, file) = EmojiAssetsIndex.parseCdnUrl(
            "https://static.stage1st.com/image/smiley/carton2017/092.gif?v=2"
        )!!
        assertEquals("carton2017", cat)
        assertEquals("092.gif", file)
    }

    @Test
    fun `非表情CDN地址返回null`() {
        assertNull(EmojiAssetsIndex.parseCdnUrl("https://avatar.stage1st.com/000/00/01/00000002_avatar_middle.jpg"))
        assertNull(EmojiAssetsIndex.parseCdnUrl("https://stage1st.com/2b/forum.php"))
        assertNull(EmojiAssetsIndex.parseCdnUrl("file:///android_asset/emoticon/face2017/206.gif"))
    }

    @Test
    fun `本地命中返回assets路径`() {
        val index = mapOf("face2017" to setOf("206.gif", "209.gif"))
        assertEquals(
            "file:///android_asset/emoticon/face2017/206.gif",
            EmojiAssetsIndex.resolveFrom("face2017", "206.gif", index)
        )
    }

    @Test
    fun `分类缺失或文件缺失返回null`() {
        val index = mapOf("face2017" to setOf("206.gif"))
        assertNull(EmojiAssetsIndex.resolveFrom("noSuchCat", "206.gif", index))
        assertNull(EmojiAssetsIndex.resolveFrom("face2017", "404.gif", index))
    }
}
