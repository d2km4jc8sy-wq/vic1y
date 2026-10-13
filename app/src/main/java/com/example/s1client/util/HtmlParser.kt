package com.example.s1client.util

import android.util.Log
import com.example.s1client.data.model.FriendItem
import com.example.s1client.data.model.PmListItem
import com.example.s1client.data.model.PmMessage
import com.example.s1client.data.model.Thread
import com.example.s1client.data.model.UserPostItem
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object HtmlParser {

    private val NAV_WORDS = setOf(
        "查看资料", "去串个门", "打个招呼", "发送消息", "互动", "管理",
        "首页", "论坛", "登录", "注册", "返回", "查看", "删除", "备注",
        "好友分组", "更多", "下一页", "上一页", "收听TA", "取消收听",
        "我的", "设置", "消息", "提醒", "退出", "积分", "用户组",
        "全部收藏", "帖子", "版块", "归档", "收藏"
    )

    // ==================== 好友列表 ====================

    fun parseFriendList(html: String): List<FriendItem> {
        if (html.isBlank()) return emptyList()
        return try {
            val doc = Jsoup.parse(html)
            val result = mutableListOf<FriendItem>()
            val seenUids = mutableSetOf<String>()

            val lis = doc.select("li[id^=friend_]")
            if (lis.isNotEmpty()) {
                for (li in lis) {
                    val uid = li.id().removePrefix("friend_").removeSuffix("_li")
                    if (uid.isEmpty() || !uid.all { it.isDigit() }) continue
                    val h4 = li.selectFirst("h4") ?: continue
                    var username = ""
                    for (link in h4.select("a[href^=space-uid-]")) {
                        val text = link.text().trim()
                        if (text.isNotEmpty() && !text.startsWith("热度")) {
                            username = text; break
                        }
                    }
                    if (username.isEmpty()) continue
                    val note = li.selectFirst("p.maxh")?.text()?.trim() ?: ""
                    if (seenUids.contains(uid)) continue
                    seenUids.add(uid)
                    result.add(FriendItem(uid, username, note))
                }
                if (result.isNotEmpty()) return result
            }

            val links = doc.select("a[href*=space-uid-]")
            for (link in links) {
                val href = link.attr("href")
                val uid = extractUidFromSpaceUrl(href) ?: continue
                if (uid == "0") continue
                val text = link.text().trim()
                if (text.isEmpty() || text.length > 30) continue
                if (text.all { it.isDigit() }) continue
                if (NAV_WORDS.any { text.contains(it) }) continue
                if (seenUids.contains(uid)) continue
                seenUids.add(uid)
                result.add(FriendItem(uid, text, ""))
            }
            result
        } catch (e: Exception) {
            Log.e("S1HTML", "解析好友失败", e); emptyList()
        }
    }

    private fun extractUidFromSpaceUrl(href: String): String? {
        Regex("""space-uid-(\d+)""").find(href)?.groupValues?.get(1)?.let { return it }
        Regex("""[?&]uid=(\d+)""").find(href)?.groupValues?.get(1)?.let { return it }
        return null
    }

    // ==================== 主题/回帖列表 ====================

    fun parseThreadList(html: String): List<UserPostItem> = parseTableList(html, false)
    fun parseReplyList(html: String): List<UserPostItem> = parseTableList(html, true)

    // ==================== 帖子搜索 ====================

    /**
     * 解析 Discuz search.php 搜索结果页（ul/li.pbw 结构）
     */
    fun parseSearchResult(html: String): List<Thread> {
        if (html.isBlank()) return emptyList()
        return try {
            val doc = Jsoup.parse(html)
            val result = mutableListOf<Thread>()
            val seenTids = mutableSetOf<String>()

            val items = doc.select("div#threadlist li.pbw")
            Log.d("S1HTML", "搜索结果 li 数量: ${items.size}")

            for (li in items) {
                val tid = li.id().takeIf { it.isNotBlank() && it.all { c -> c.isDigit() } }
                    ?: continue
                if (seenTids.contains(tid)) continue

                val titleLink = li.selectFirst("h3 a") ?: continue
                val subject = titleLink.text().trim()
                if (subject.isEmpty()) continue

                // replies / views：从 p.xg1 "5 个回复 - 553 次查看"
                val xg1 = li.selectFirst("p.xg1")?.text() ?: ""
                val replies = Regex("""(\d+)\s*个回复""").find(xg1)?.groupValues?.get(1) ?: "0"
                val views = Regex("""(\d+)\s*次查看""").find(xg1)?.groupValues?.get(1) ?: "0"

                // 时间 / 作者 / 版块：从最后一个带 space-uid 的 p 里取
                var dateline = ""
                var author = ""
                var authorId = ""
                var forum = ""

                val metaP = li.select("p").lastOrNull {
                    it.selectFirst("a[href^=space-uid-]") != null
                }
                if (metaP != null) {
                    metaP.selectFirst("span")?.text()?.trim()?.let { dateline = it }
                    metaP.selectFirst("a[href^=space-uid-]")?.let { link ->
                        author = link.text().trim()
                        authorId = Regex("""space-uid-(\d+)""")
                            .find(link.attr("href"))?.groupValues?.get(1) ?: ""
                    }
                    metaP.selectFirst("a.xi1")?.text()?.trim()?.let { forum = it }
                }

                seenTids.add(tid)
                result.add(Thread(
                    tid = tid,
                    subject = subject,
                    author = author,
                    authorId = authorId,
                    replies = replies,
                    views = views,
                    attachmentImagePreviewList = null,
                    dateline = dateline,
                    displayOrder = null,
                    typeId = null,
                    typeName = forum.takeIf { it.isNotBlank() }
                ))
            }

            Log.d("S1HTML", "搜索解析结果: ${result.size} 条")
            result
        } catch (e: Exception) {
            Log.e("S1HTML", "解析搜索失败", e); emptyList()
        }
    }

    // ==================== 收藏列表（ul/li 结构）====================

    /**
     * 收藏页是 ul/li 结构，不是 table。
     * 扫描所有帖子链接（thread-xxx / viewthread?tid=xxx），过滤导航词。
     * 时间从父元素文本里提取。
     */
    fun parseFavoriteList(html: String): List<UserPostItem> {
        if (html.isBlank()) return emptyList()
        return try {
            val doc = Jsoup.parse(html)
            val result = mutableListOf<UserPostItem>()
            val seenTids = mutableSetOf<String>()

            // 收藏页的所有帖子链接
            val links = doc.select("a[href*=thread-], a[href*=viewthread]")
            Log.d("S1HTML", "收藏页找到链接: ${links.size} 个")

            for (link in links) {
                val href = link.attr("href")
                val tid = extractTid(href) ?: continue
                if (seenTids.contains(tid)) continue

                val subject = link.text().trim()
                if (subject.isEmpty() || subject.length < 2) continue
                if (subject.all { it.isDigit() }) continue
                if (subject.length > 100) continue
                if (NAV_WORDS.any { subject == it || subject.contains(it) }) continue

                // 时间：从 link 的父级 / 祖父级里找日期
                var time = ""
                var p = link.parent()
                var depth = 0
                while (p != null && depth < 3) {
                    val t = extractDateFromText(p.text())
                    if (t.isNotEmpty()) { time = t; break }
                    p = p.parent()
                    depth++
                }

                // 版块：链接文本里的 [xxx]
                val forumRegex = Regex("""\[([^\]]{2,12})\]""")
                val forum = forumRegex.find(subject)?.groupValues?.get(1) ?: ""

                seenTids.add(tid)
                result.add(UserPostItem(tid, subject, "", time, forum))
            }

            Log.d("S1HTML", "解析收藏: ${result.size} 条")
            result
        } catch (e: Exception) {
            Log.e("S1HTML", "解析收藏失败", e); emptyList()
        }
    }

    private fun extractDateFromText(text: String): String {
        // 匹配 "2026-5-28 22:40" 或 "2026-05-28" 之类
        val m = Regex("""\d{4}-\d{1,2}-\d{1,2}(\s+\d{1,2}:\d{2})?""").find(text)
        return m?.value ?: ""
    }

    // ==================== 主题/回帖通用 table 解析 ====================

    private fun parseTableList(html: String, isReply: Boolean): List<UserPostItem> {
        if (html.isBlank()) return emptyList()
        return try {
            val doc = Jsoup.parse(html)
            val result = mutableListOf<UserPostItem>()
            val seenKeys = mutableSetOf<String>()

            val rows = doc.select("tr")
            Log.d("S1HTML", "遍历 tr: ${rows.size} 行 (isReply=$isReply)")

            for (row in rows) {
                val th = row.selectFirst("th") ?: continue
                val titleLink = if (isReply) {
                    th.selectFirst("a[href*=ptid]")
                        ?: th.selectFirst("a[href^=thread-]")
                        ?: th.selectFirst("a")
                } else {
                    th.selectFirst("a[href^=thread-]")
                        ?: th.selectFirst("a[href*=viewthread]")
                        ?: th.selectFirst("a")
                } ?: continue

                val href = titleLink.attr("href")
                val subject = titleLink.text().trim()
                if (subject.isEmpty() || subject.length < 2) continue
                if (subject.all { it.isDigit() }) continue
                if (subject.length > 100) continue
                if (NAV_WORDS.any { subject == it }) continue
                if (subject == "标题" || subject == "版块/圈子"
                    || subject == "最后发帖" || subject == "最后发表") continue

                val tid = extractTid(href) ?: extractTidFromRow(row) ?: ""
                val time = row.selectFirst("td.by em")?.text()?.trim() ?: ""
                val forum = row.selectFirst("td a.xg1")?.text()?.trim() ?: ""

                // 盲抓「回复 / 查看」列。Discuz 空间页通常是
                //   <td class="num"><a class="xi2">324</a><em>536304</em></td>
                // 结构对不上就留空，列表项会自动不显示这两个胶囊。
                val numCell = row.selectFirst("td.num")
                val numParts = numCell?.select("a, em")
                    ?.map { it.text().trim() }
                    ?.filter { it.isNotEmpty() }
                    .orEmpty()
                val replies = numParts.getOrNull(0)?.let { Regex("""\d+""").find(it)?.value }.orEmpty()
                val views = numParts.getOrNull(1)?.let { Regex("""\d+""").find(it)?.value }.orEmpty()

                var message = ""
                if (isReply) {
                    val nextRow = row.nextElementSibling()
                    if (nextRow != null && nextRow.tagName() == "tr") {
                        val summaryCell = nextRow.selectFirst("td[colspan]")
                        if (summaryCell != null) {
                            val summaryLink = summaryCell.selectFirst("a[href*=ptid]")
                            message = summaryLink?.text()?.trim() ?: ""
                            if (message.length > 100) message = message.take(100) + "..."
                        }
                    }
                }

                val key = "$tid|${subject.take(30)}"
                if (seenKeys.contains(key)) continue
                seenKeys.add(key)

                result.add(UserPostItem(tid, subject, message, time, forum, replies, views))
            }

            Log.d("S1HTML", "解析结果: ${result.size} 条 (isReply=$isReply)")
            result
        } catch (e: Exception) {
            Log.e("S1HTML", "解析失败", e); emptyList()
        }
    }

    private fun extractTid(href: String): String? {
        Regex("""thread-(\d+)""").find(href)?.groupValues?.get(1)?.let { return it }
        Regex("""p?tid[=\-](\d+)""").find(href)?.groupValues?.get(1)?.let { return it }
        Regex("""viewthread[^"]*?tid[=\-](\d+)""").find(href)?.groupValues?.get(1)?.let { return it }
        return null
    }

    private fun extractTidFromRow(row: Element): String? {
        row.selectFirst("td.icn a[href*=tid]")?.let {
            extractTid(it.attr("href"))?.let { tid -> return tid }
        }
        return null
    }

    // ==================== 私信 ====================

    /** 解析私信列表页 */
    fun parsePmList(html: String): List<PmListItem> {
        if (html.isBlank()) return emptyList()
        return try {
            val doc = Jsoup.parse(html)
            val result = mutableListOf<PmListItem>()
            val items = doc.select("dl[id^=pmlist_]")
            Log.d("S1HTML", "私信列表 dl 数量: " + items.size)
            for (dl in items) {
                val plid = dl.id().removePrefix("pmlist_")
                val userLink = dl.select("a[href^=space-uid-]")
                    .firstOrNull { it.text().isNotBlank() } ?: continue
                val touid = Regex("""space-uid-(\d+)""").find(userLink.attr("href"))
                    ?.groupValues?.get(1) ?: continue
                val username = userLink.text().trim()
                val lastMessage = dl.selectFirst("dd.ptm.pm_c")?.text()?.trim() ?: ""
                val lastTime = dl.selectFirst("span.xg1")?.text()?.trim() ?: ""
                // 未读只认 Discuz 的 a.new 标记；cur1 是样式类不是未读态（曾导致全列表误标红点）
                val isNew = dl.selectFirst("a.new") != null
                val countText = dl.selectFirst("span.z")?.text()?.trim() ?: ""
                val msgCount = Regex("""共\s*(\d+)\s*条""").find(countText)
                    ?.groupValues?.get(1) ?: "0"
                result.add(PmListItem(plid, touid, username, lastMessage, lastTime, msgCount, isNew))
            }
            Log.d("S1HTML", "解析私信: " + result.size + " 条")
            result
        } catch (e: Exception) {
            Log.e("S1HTML", "解析私信失败", e); emptyList()
        }
    }

    /** 解析会话消息（每行一个 dl） */
    fun parsePmConversation(html: String): List<PmMessage> {
        if (html.isBlank()) return emptyList()
        return try {
            val doc = Jsoup.parse(html)
            val messages = mutableListOf<PmMessage>()
            val nodes = doc.select("div#pm_ul.xld dl, div#pm_ul dl")
            Log.d("S1HTML", "会话消息 dl 数量: " + nodes.size)
            for (node in nodes) {
                val pmid = node.id().removePrefix("pmlist_")
                val authorEl = node.selectFirst("span.xw1, span.xg1")
                val author = authorEl?.text()?.trim() ?: ""
                val isSelf = author == "您" || author.contains("您")
                val msgEl = node.selectFirst("dd.ptm")
                val message = msgEl?.html()?.trim() ?: ""
                val dateline = node.selectFirst("span.xg1")?.text()?.trim() ?: ""
                if (message.isEmpty()) continue
                messages.add(PmMessage(pmid, author, message, dateline, isSelf))
            }
            Log.d("S1HTML", "解析会话消息: " + messages.size + " 条")
            messages
        } catch (e: Exception) {
            Log.e("S1HTML", "解析会话失败", e); emptyList()
        }
    }

}
