package com.example.s1client.util

import com.example.s1client.data.model.NoticeJsonItem
import com.example.s1client.data.model.NoticeListResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil

/**
 * 提醒列表 JSON（Mobile API v3 `module=mynotelist`）的解析与分页。
 *
 * 基准实现：开源 s1er `api_service.dart` 的 `parseNoticeListJson` —— 包括
 * **总页数的算法**原样照搬，不自行发明：
 *   totalPages = count / perpage 向上取整；
 *   取不到 count 时退化为「本页满了就假定还有下一页」。
 *
 * 输出复用既有的 [NoticeItem]（HTML 解析器同款），这样 Activity 层不用改数据模型，
 * 两条路径（JSON / HTML 兜底）产出的行可以直接混排。
 */
object NoticeJsonParser {

    /** 一页提醒 + 分页信息 */
    data class NoticePage(
        val items: List<NoticeItem>,
        val currentPage: Int,
        val totalPages: Int
    )

    /** 未登录：Discuz 会回 `login_before_enter_home` / `to_login` 而不是列表 */
    fun isLoginRequired(resp: NoticeListResponse): Boolean {
        val v = resp.message?.messageval.orEmpty()
        return v.contains("login_before_enter_home") || v.contains("to_login")
    }

    /**
     * @param feed 本次请求的分类（mypost / interactive / system），只用于给条目打 type
     * @param page 本次请求的页码（服务端没回页码时以其为准）
     */
    fun parse(resp: NoticeListResponse, feed: String, page: Int): NoticePage {
        val raw = resp.variables?.list
        val items = if (raw == null) {
            emptyList()
        } else {
            raw.mapNotNull { it.toNoticeItem(feed) }
        }

        val currentPage = resp.variables?.page?.trim()?.toIntOrNull() ?: page
        val perPage = resp.variables?.perpage?.trim()?.toIntOrNull() ?: DEFAULT_PER_PAGE
        val count = resp.variables?.count?.trim()?.toIntOrNull()
        val totalPages = if (count != null && count > 0 && perPage > 0) {
            ceil(count.toDouble() / perPage).toInt()
        } else {
            // 没有总数时的退化估计（与 s1er 一致）：本页还满着，就假定后面还有一页
            if (items.size >= perPage) currentPage + 1 else currentPage
        }

        return NoticePage(
            items = items,
            currentPage = currentPage,
            totalPages = if (totalPages < 1) 1 else totalPages
        )
    }

    private fun NoticeJsonItem.toNoticeItem(feed: String): NoticeItem {
        val note = note.orEmpty()
        val text = stripHtml(note)
        // Discuz 的回复提醒里，note 通常是：
        //   <a href="home.php?mod=space&uid=579037">郁哲</a> 回复了您的帖子
        //   <a href="forum.php?mod=redirect&goto=findpost&ptid=2291675&pid=70368274">《动物园之星2》</a>
        // **第一个 href 是用户主页、没有 tid**。原来只取第一个，导致 tid 恒为 null、
        // 所有回复提醒都跳不动。现在遍历全部链接，优先挑能抠出 tid 的那个。
        val hrefs = allHrefs(note)
        val href = hrefs.firstOrNull { extractTid(it) != null } ?: hrefs.firstOrNull()
        val postLink = matchPostLink(note)
        return NoticeItem(
            avatar = avatarUrl(authorid),
            author = author?.trim()?.takeIf { it.isNotEmpty() } ?: SYSTEM_AUTHOR,
            content = text,
            time = formatTime(dateline),
            type = detectType(feed, text),
            targetUrl = href,
            tid = postLink.first.ifEmpty { extractTid(href ?: note).orEmpty() },
            pid = postLink.second.ifEmpty { null },
            id = id?.trim().orEmpty(),
            isNew = isNew?.trim() == "1"
        )
    }

    /**
     * 分类：优先信服务端 feed，帖子轨再按正文微调（口径照 s1er `_classifyNotice`）。
     * 取值沿用本项目既有集合：reply / at / system / other。
     */
    private fun detectType(feed: String, content: String): String = when (feed) {
        FEED_SYSTEM -> "system"
        FEED_INTERACTIVE -> "at"
        else -> when {
            content.contains("评分") -> "other"
            content.contains("回复") -> "reply"
            else -> "reply"
        }
    }

    /** 去掉 HTML 标签，留纯文本（note 里只有 <a> 之类简单标签，正则够用） */
    fun stripHtml(html: String): String =
        html.replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .trim()

    /** 取 note 里的**全部**链接。注意不能只取第一个 —— 见下方注释。 */
    private fun allHrefs(html: String): List<String> =
        Regex("""href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(html)
            .map { it.groupValues[1].replace("&amp;", "&").trim() }
            .filter { it.isNotBlank() }
            .toList()

    /**
     * 从链接里抠 tid。两种写法都见过，等号不能省：
     *   `forum.php?mod=redirect&goto=findpost&ptid=99&pid=88`
     *   `thread-123-1-1.html`
     */
    /**
     * 从 note 里抠帖子链接。**正则与字段顺序直接照搬开源 s1er**
     * （api_service.dart 的 `_findPostLinkRe` + `NoticeItem.fromJson` 逻辑），
     * 不另造轮子：
     *   r'goto=findpost&(?:amp;)?(?:ptid=(\d+)&(?:amp;)?pid=(\d+)|pid=(\d+)&(?:amp;)?ptid=(\d+))'
     * 它比"先抽 href 再找 tid"更可靠，因为 Discuz 的回复提醒里
     * **第一个 href 是用户主页、没有 tid**，而帖子链接是 findpost 那个；
     * 同时它兼容 ptid/pid 顺序颠倒与 `&amp;` 实体两种写法。
     * @return tid to pid（任一可能为空）
     */
    private fun matchPostLink(html: String): Pair<String, String> {
        val m = POST_LINK_RE.find(html)
            ?: return Regex("""thread-(\d+)""", RegexOption.IGNORE_CASE)
                .find(html)?.let { r -> r.groupValues[1] to "" }
            ?: return "" to ""
        val tid = m.groupValues[1].ifEmpty { m.groupValues[4] }
        val pid = m.groupValues[2].ifEmpty { m.groupValues[3] }
        return tid to pid
    }

    private val POST_LINK_RE = Regex(
        """goto=findpost&(?:amp;)?(?:ptid=(\d+)&(?:amp;)?pid=(\d+)|pid=(\d+)&(?:amp;)?ptid=(\d+))""",
        RegexOption.IGNORE_CASE
    )

    private fun extractTid(s: String): String? {
        Regex("""(?:ptid|tid)\s*=\s*(\d+)""", RegexOption.IGNORE_CASE)
            .find(s)?.groupValues?.get(1)?.let { return it }
        Regex("""thread-\s*(\d+)""", RegexOption.IGNORE_CASE)
            .find(s)?.groupValues?.get(1)?.let { return it }
        return null
    }

    /** 头像地址：与「我的」页身份卡同款（uid 补零分目录） */
    private fun avatarUrl(uid: String?): String? {
        val u = uid?.trim()?.toIntOrNull() ?: return null
        if (u <= 0) return null
        val p = String.format("%09d", u)
        return "https://avatar.stage1st.com/" +
            "${p.substring(0, 3)}/${p.substring(3, 5)}/" +
            "${p.substring(5, 7)}/${p.substring(7, 9)}_avatar_middle.jpg"
    }

    /** 时间戳（秒）→ "yyyy-MM-dd HH:mm"；解析不了就给空串，交给 UI 不显示 */
    private fun formatTime(sec: String?): String? {
        val s = sec?.trim()?.toLongOrNull() ?: return null
        if (s <= 0) return null
        return try {
            val f = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
            val cal = Calendar.getInstance()
            cal.timeInMillis = s * 1000
            f.format(cal.time)
        } catch (_: Exception) {
            null
        }
    }

    private const val DEFAULT_PER_PAGE = 20
    private const val SYSTEM_AUTHOR = "系统通知"

    /** 与 s1er NoticeFeed 对齐的分类取值 */
    const val FEED_MYPOST = "mypost"
    const val FEED_INTERACTIVE = "interactive"
    const val FEED_SYSTEM = "system"
}
