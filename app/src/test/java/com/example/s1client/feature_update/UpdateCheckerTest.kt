package com.example.s1client.feature_update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 检查更新的纯逻辑契约（PROBLEMS P9，2026-10-17）。
 * 锚点：分段数值比较（1.10 > 1.9）、非法段按 0、GitHub latest 解析、
 * 忽略此版语义在调用方（本文件锚定解析与比较）。
 */
class UpdateCheckerTest {

    @Test
    fun `分段数值比较_110大于19`() {
        assertTrue(UpdateChecker.isNewer("1.10", "1.9"))
        assertFalse(UpdateChecker.isNewer("1.9", "1.10"))
    }

    @Test
    fun `段数不足补零比较_相等不算更新`() {
        // 1.1.0 与 1.1 逐段相等 → 不是更新（补零语义）
        assertFalse(UpdateChecker.isNewer("1.1.0", "1.1"))
        assertFalse(UpdateChecker.isNewer("1.1", "1.1.0"))
        // 真正多出的段才更新
        assertTrue(UpdateChecker.isNewer("1.1.1", "1.1"))
    }

    @Test
    fun `相同版本不是更新`() {
        assertFalse(UpdateChecker.isNewer("1.1.0", "1.1.0"))
        assertFalse(UpdateChecker.isNewer("1.0", "1.0.0"))
    }

    @Test
    fun `非法段按0处理_相段相等不更新`() {
        // "1.beta" 段落解析为 [1, 0]，与 1.0 相等 → 不是更新
        assertFalse(UpdateChecker.isNewer("1.beta", "1.0"))
        assertFalse(UpdateChecker.isNewer("abc", "1.0"))
        // 按零补段后多出的段才更新
        assertTrue(UpdateChecker.isNewer("1.beta.1", "1.0"))
    }

    @Test
    fun `v前缀与空白容忍`() {
        assertTrue(UpdateChecker.isNewer("v1.2", "1.1"))
        assertTrue(UpdateChecker.isNewer(" 1.2 ", "1.1"))
    }

    @Test
    fun `GitHub_latest解析出版本_说明与apk链接`() {
        val body = """
            {
              "tag_name": "v1.2.0",
              "html_url": "https://github.com/x/y/releases/tag/v1.2.0",
              "body": "修复：评分负分；性能：滚动优化",
              "assets": [
                {"browser_download_url": "https://github.com/x/y/releases/download/v1.2.0/S1Client.apk"},
                {"browser_download_url": "https://github.com/x/y/releases/download/v1.2.0/README.md"}
              ]
            }
        """.trimIndent()
        val info = UpdateChecker.parseLatest(body)!!
        assertEquals("1.2.0", info.version)
        assertEquals("https://github.com/x/y/releases/download/v1.2.0/S1Client.apk", info.apkUrl)
        assertTrue(info.notes.contains("评分负分"))
        assertEquals("https://github.com/x/y/releases/tag/v1.2.0", info.pageUrl)
    }

    @Test
    fun `无tag或坏JSON返回null`() {
        assertNull(UpdateChecker.parseLatest("{\"message\":\"Not Found\"}"))
        assertNull(UpdateChecker.parseLatest("not json"))
    }
}
