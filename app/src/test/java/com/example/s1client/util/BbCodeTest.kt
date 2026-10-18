package com.example.s1client.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * BBCode 编辑辅助回归测试。
 *
 * 为什么锁这里：发帖编辑器里加粗、引用、插入链接全靠这三个函数。
 * 它们算错不会崩，只会让光标跳到错误位置、或多套一层标记——
 * 用户会以为是自己点错了，而且这类问题只能在真机上一遍遍试。
 */
class BbCodeTest {

    private fun value(text: String, selStart: Int = text.length, selEnd: Int = selStart) =
        TextFieldValue(text, TextRange(selStart, selEnd))

    // ===== wrap =====

    @Test
    fun `有选区时用标记包裹选中内容`() {
        val out = BbCode.wrap(value("abc", 0, 1), "[b]", "[/b]")
        assertEquals("[b]a[/b]bc", out.text)
    }

    @Test
    fun `包裹后光标落在标记之后`() {
        val out = BbCode.wrap(value("abc", 0, 1), "[b]", "[/b]")
        // "[b]" 3 字 + 选中 "a" 1 字 + "[/b]" 4 字
        assertEquals(8, out.selection.start)
        assertEquals(8, out.selection.end)
    }

    @Test
    fun `无选区时插入成对标记且光标停在中间`() {
        val out = BbCode.wrap(value("abc", 1, 1), "[b]", "[/b]")
        assertEquals("a[b][/b]bc", out.text)
        // 光标应落在 "[b]" 之后、[/b] 之前
        assertEquals(4, out.selection.start)
    }

    @Test
    fun `选中全文时前后包裹正确`() {
        val out = BbCode.wrap(value("abc", 0, 3), "[quote]", "[/quote]")
        assertEquals("[quote]abc[/quote]", out.text)
    }

    @Test
    fun `选区越界时被夹到文本长度内`() {
        val out = BbCode.wrap(value("abc", 99, 99), "[b]", "[/b]")
        assertEquals("abc[b][/b]", out.text)
    }

    // ===== insert =====

    @Test
    fun `在光标处插入片段`() {
        val out = BbCode.insert(value("abc", 1, 1), "X")
        assertEquals("aXbc", out.text)
        assertEquals(2, out.selection.start)
    }

    @Test
    fun `插入时替换选中的内容`() {
        val out = BbCode.insert(value("abc", 0, 2), "X")
        assertEquals("Xc", out.text)
    }

    @Test
    fun `插入空串不改变文本`() {
        val out = BbCode.insert(value("abc", 2, 2), "")
        assertEquals("abc", out.text)
        assertEquals(2, out.selection.start)
    }

    // ===== autoLink =====

    @Test
    fun `粘贴裸链接自动包成 url 标记`() {
        val out = BbCode.autoLink(value("看这个 "), value("看这个 https://example.com"))
        assertEquals("看这个 [url]https://example.com[/url]", out.text)
    }

    @Test
    fun `逐字输入不触发自动链接`() {
        val out = BbCode.autoLink(value("看这个 h"), value("看这个 ht"))
        assertEquals("看这个 ht", out.text)
    }

    @Test
    fun `粘贴非链接文本不处理`() {
        val out = BbCode.autoLink(value("你好"), value("你好世界"))
        assertEquals("你好世界", out.text)
    }

    @Test
    fun `已经在 url 标记内不重复包裹`() {
        val out = BbCode.autoLink(value("[url]"), value("[url]https://example.com"))
        assertEquals("[url]https://example.com", out.text)
    }

    @Test
    fun `删除操作不处理`() {
        val out = BbCode.autoLink(value("abc"), value("ab"))
        assertEquals("ab", out.text)
    }

    @Test
    fun `链接插在已有内容中间时前后都被保留`() {
        val out = BbCode.autoLink(value("前后"), value("前https://a.com后"))
        assertEquals("前[url]https://a.com[/url]后", out.text)
    }
}
