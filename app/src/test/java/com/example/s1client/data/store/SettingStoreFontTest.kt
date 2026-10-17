package com.example.s1client.data.store

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 自定义字体存储与应用的契约（PROBLEMS P10，2026-10-17）。
 * 锚点：字体文件+名字落盘 → customTypeface 可加载（Robolectric 影子不校验字体格式）；
 * removeCustomFont → 文件删除 + 名字清空 + Typeface 失效。
 * （importFont 的 contentResolver 路径不在本测试覆盖——文件直写等价覆盖存储契约。）
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingStoreFontTest {

    private fun seedImported(ctx: android.content.Context) {
        val f = SettingStore.fontFile(ctx)
        f.parentFile?.mkdirs()
        f.writeBytes(ByteArray(256) { (it % 251).toByte() })
        ctx.getSharedPreferences("s1_client_prefs", android.content.Context.MODE_PRIVATE)
            .edit().putString("custom_font_file", "font.ttf").apply()
    }

    @Test
    fun `字体文件与名字落盘后_Typeface可加载`() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        seedImported(ctx)
        assertEquals("font.ttf", SettingStore.customFontName(ctx))
        assertTrue(SettingStore.fontFile(ctx).exists())
        // Robolectric 影子 Typeface.createFromFile 不校验字节 → 非 null
        assertNotNull(SettingStore.customTypeface(ctx))
    }

    @Test
    fun `未导入时返回null`() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        SettingStore.removeCustomFont(ctx)
        assertNull(SettingStore.customFontName(ctx))
        assertNull(SettingStore.customTypeface(ctx))
    }

    @Test
    fun `removeCustomFont清文件与名字`() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        seedImported(ctx)
        SettingStore.removeCustomFont(ctx)
        assertFalse(SettingStore.fontFile(ctx).exists())
        assertNull(SettingStore.customFontName(ctx))
        assertNull(SettingStore.customTypeface(ctx))
    }
}
