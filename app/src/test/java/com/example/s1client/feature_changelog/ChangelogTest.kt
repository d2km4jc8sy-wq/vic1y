package com.example.s1client.feature_changelog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 更新日志内置数据的契约（阶段二④配套）。
 * 锚点：最新版本在最上、版本唯一、每条目至少一行说明——
 * 发布时在此数据头部追加新条目，漏写/乱序会被这里拦下。
 */
class ChangelogTest {

    @Test
    fun `条目非空且版本唯一`() {
        val entries = ChangelogData.entries
        assertTrue(entries.isNotEmpty())
        assertEquals(entries.size, entries.map { it.version }.distinct().size)
    }

    @Test
    fun `最新版本在最上`() {
        // 头部条目 = 当前发布版本（1.1.0 起语义化版本）
        assertEquals("1.1.0", ChangelogData.entries.first().version)
    }

    @Test
    fun `每条目至少一行说明且无空行`() {
        ChangelogData.entries.forEach { e ->
            assertTrue("版本 ${e.version} 缺少说明行", e.lines.isNotEmpty())
            e.lines.forEach { l -> assertTrue("版本 ${e.version} 有空白说明行", l.isNotBlank()) }
        }
    }
}
