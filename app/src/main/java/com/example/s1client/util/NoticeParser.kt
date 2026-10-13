package com.example.s1client.util

import org.jsoup.Jsoup

/**
 * 论坛「提醒 / 通知」页（home.php?mod=space&do=notice）的解析。
 *
 * 该页是带登录态 cookie 的 HTML，由 NoticeCenterActivity 用 RetrofitClient.fetchHtml 拉取，
 * 这里只负责把列表项抽成结构化数据。Discuz 的提醒页 HTML 不同版本 class 名略有差异，
 * 解析做了多重兜底（ul.m_notice_list / .m_notice_list / 退化到所有带 ntc_l 的 li）。
 */
data class NoticeItem(
    val avatar: String?,
    val author: String?,
    val content: String,
    val time: String?,
    /** reply=回复我 / at=@我 / system=系统消息 / other=其他 */
    val type: String,
    /** 指向来源（帖子或用户空间）的原始 href */
    val targetUrl: String?,
    /** 从 targetUrl 里抠出的 tid（能跳帖）；没有则为 null */
    val tid: String?,
    /** 通知唯一 id（来自 dl 的 notice 属性），用于本地已读锚点 */
    val id: String = ""
)

object NoticeParser {

    /** 没登录时接口会返回登录页，而不是通知列表 */
    fun isLoginPage(html: String): Boolean =
        html.contains("用户登录") && !html.contains("m_notice_list")

    /**
     * 解析通知列表。
     * 真实结构（Discuz X3.5）：每条通知是一个 <dl notice="id">（位于 <div class="nts"> 内），
     * 头像在 dd.avt > a > img，时间在 dt span.xg1，正文在 dd.ntc_body，
     * 来源链接形如 forum.php?mod=redirect&goto=findpost&ptid=xxx（含可跳转的 tid）。
     *
     * @param defaultType 已知分类时直接赋予（回复我/@我/系统），空字符串则按内容逐条推断。
     */
    fun parse(html: String, defaultType: String = ""): List<NoticeItem> {
        val doc = Jsoup.parse(html)
        val items = doc.select("div.nts > dl, dl[notice]")
        if (items.isEmpty()) return emptyList()

        val out = ArrayList<NoticeItem>(items.size)
        for (dl in items) {
            val avatar = dl.selectFirst("dd.avt img, dd[class*=avt] img, img")
                ?.attr("src")?.takeIf { it.isNotBlank() }
            val ntc = dl.selectFirst("dd.ntc_body") ?: dl
            val author = ntc.selectFirst("a")?.text()?.takeIf { it.isNotBlank() }
            val content = (ntc.text() ?: "").trim()
            val time = dl.selectFirst("dt span.xg1, span.xg1")?.text()?.trim()
            val targetA = ntc.selectFirst(
                "a[href*=ptid=], a[href*=tid=], a[href*=thread-], " +
                    "a[href*=goto=findpost], a[href*=mod=viewthread]"
            )
            val targetUrl = targetA?.attr("href")?.takeIf { it.isNotBlank() }
            val tid = targetUrl?.let { extractTid(it) }
            val type = if (defaultType.isNotEmpty()) defaultType else detectType(tid, content)
            val id = dl.attr("notice").takeIf { it.isNotBlank() } ?: tid ?: content.hashCode().toString()
            out.add(NoticeItem(avatar, author, content, time, type, targetUrl, tid, id))
        }
        return out
    }

    private fun detectType(tid: String?, content: String): String {
        return if (tid == null) {
            "system"
        } else if (content.contains("回复") || content.contains("评论") || content.contains("点评")) {
            "reply"
        } else {
            "at"
        }
    }

    /** 从各种形态的链接里抠 tid：tid=123 / ptid=123 / thread-123-... */
    fun extractTid(url: String): String? {
        return Regex("tid=(\\d+)").find(url)?.groupValues?.getOrNull(1)
            ?: Regex("ptid=(\\d+)").find(url)?.groupValues?.getOrNull(1)
            ?: Regex("thread-(\\d+)").find(url)?.groupValues?.getOrNull(1)
    }

    /** 把相对/协议相对头像地址补成绝对地址 */
    fun resolveUrl(src: String?): String? {
        if (src.isNullOrBlank()) return null
        return when {
            src.startsWith("//") -> "https:$src"
            src.startsWith("/") -> "https://stage1st.com$src"
            src.startsWith("http") -> src
            else -> "https://stage1st.com/2b/$src"
        }
    }
}
