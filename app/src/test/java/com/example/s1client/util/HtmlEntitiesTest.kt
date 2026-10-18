package com.example.s1client.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * HTML 实体反转义回归测试。
 *
 * 为什么锁这里：这个函数在模型边界统一调用一次，全 App 所有标题/摘要都过它。
 * 最危险的不是「没解开」，而是「解多了」——`&amp;lt;` 如果被解两层就成了真正的 `<`，
 * 用户本来只想显示字面量，结果被当成标签吃掉。
 * 正确顺序是 `&amp;` 最后解，这条测试就是钉住这个顺序的。
 */
class HtmlEntitiesTest {

    @Test
    fun `null 原样返回 null`() {
        assertNull(unescapeHtmlEntities(null))
    }

    @Test
    fun `空串原样返回空串`() {
        assertEquals("", unescapeHtmlEntities(""))
    }

    @Test
    fun `六种常见实体各自还原`() {
        assertEquals("<", unescapeHtmlEntities("&lt;"))
        assertEquals(">", unescapeHtmlEntities("&gt;"))
        assertEquals("\"", unescapeHtmlEntities("&quot;"))
        assertEquals("'", unescapeHtmlEntities("&#39;"))
        assertEquals("'", unescapeHtmlEntities("&apos;"))
        assertEquals(" ", unescapeHtmlEntities("&nbsp;"))
        assertEquals("&", unescapeHtmlEntities("&amp;"))
    }

    @Test
    fun `混合文本中被逐一还原`() {
        assertEquals("A&B <tag> \"x\"", unescapeHtmlEntities("A&amp;B &lt;tag&gt; &quot;x&quot;"))
    }

    @Test
    fun `双重编码只解一层`() {
        // &amp;lt; 应还原成字面量 &lt;，而不是 <
        assertEquals("&lt;", unescapeHtmlEntities("&amp;lt;"))
        assertEquals("&amp;", unescapeHtmlEntities("&amp;amp;"))
        assertEquals("&gt;", unescapeHtmlEntities("&amp;gt;"))
    }

    @Test
    fun `没有实体的文本原样返回`() {
        assertEquals("普通中文 & 符号", unescapeHtmlEntities("普通中文 & 符号"))
        assertEquals("no entities here", unescapeHtmlEntities("no entities here"))
    }

    @Test
    fun `未知实体保持原样`() {
        assertEquals("&unknown;", unescapeHtmlEntities("&unknown;"))
        assertEquals("&#12345;", unescapeHtmlEntities("&#12345;"))
    }

    @Test
    fun `连续实体不会互相干扰`() {
        assertEquals("&&", unescapeHtmlEntities("&amp;&amp;"))
        assertEquals("<>", unescapeHtmlEntities("&lt;&gt;"))
    }

    @Test
    fun `真实标题案例`() {
        // 来自小米 10 截图：卡片把 & 显示成了 &amp;
        assertEquals(
            "【集中楼】百台&泛泛合泥地摔跤专楼",
            unescapeHtmlEntities("【集中楼】百台&amp;泛泛合泥地摔跤专楼")
        )
    }
}
