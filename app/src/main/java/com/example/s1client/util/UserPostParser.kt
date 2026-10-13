package com.example.s1client.util

import android.util.Log
import com.example.s1client.data.model.ForumFavoriteItem
import com.example.s1client.data.model.UserPostItem
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/** 用户空间页解析：主题列表 / 回帖列表（table 结构）+ 收藏列表（ul/li 结构） */
object UserPostParser {

    fun parseThreadList(html: String): List<UserPostItem> = parseTableList(html, false)
    fun parseReplyList(html: String): List<UserPostItem> = parseTableList(html, true)

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
                if (HTML_NAV_WORDS.any { subject == it || subject.contains(it) }) continue

                var time = ""
                var p = link.parent()
                var depth = 0
                while (p != null && depth < 3) {
                    val t = extractDateFromText(p.text())
                    if (t.isNotEmpty()) { time = t; break }
                    p = p.parent()
                    depth++
                }

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

    /**
     * 版块收藏列表（R5 新增）。
     * 对标开源 s1er：收藏三 Tab 同走 home.php?do=favorite HTML，版块轨 = 同 URL 加 &type=forum&mobile=2。
     * 页面是 ul/li 结构：每个 li 一个收藏块，块内含版块链接 forumdisplay&fid=xxx。
     * 判定规则照搬 s1er：块内 viewthread 链接出现在 forumdisplay 之前 → 是帖子轨（跳过），
     * 否则有 forumdisplay 链接 → 版块轨。
     */
    fun parseFavoriteForumList(html: String): List<ForumFavoriteItem> {
        if (html.isBlank()) return emptyList()
        if (html.contains("您还没有添加任何收藏")) return emptyList()
        return try {
            val doc: Document = Jsoup.parse(html)
            // 列表区域：findbox ul / threadlist_box / threadlist，都找不到就整页盲扫（对标 s1er 的三级回退）
            val region: Element = doc.select("div.findbox ul").first()
                ?: doc.select("div.threadlist_box").first()
                ?: doc.select("div.threadlist").first()
                ?: doc.body()

            val result = mutableListOf<ForumFavoriteItem>()
            val seenFids = mutableSetOf<String>()

            for (li in region.select("li")) {
                // 判定用整块 HTML 的小写形式（对标 s1er 的 block 文本比较）
                val block = li.outerHtml().lowercase()
                val forumIdx = block.indexOf("forumdisplay")
                val threadIdx = block.indexOf("viewthread")
                // 帖子轨判定：有 viewthread 且出现得更早（或没有 forumdisplay）→ 跳过
                if (threadIdx >= 0 && (forumIdx < 0 || threadIdx < forumIdx)) continue
                if (forumIdx < 0) continue

                val forumLink = li.selectFirst("a[href*=forumdisplay]") ?: continue
                val href = forumLink.attr("href")
                val fid = Regex("""forumdisplay&(?:amp;)?fid=(\d+)""", RegexOption.IGNORE_CASE)
                    .find(href)?.groupValues?.get(1) ?: continue
                if (!seenFids.add(fid)) continue

                // 标题：优先 <em>，退回链接文本
                val title = (li.selectFirst("em")?.text()?.trim()
                    ?: forumLink.text().trim()).ifBlank { "版块 #$fid" }

                // 收藏时间：span.mtime（YYYY-MM-DD）
                val dateline = li.selectFirst("span.mtime")?.text()?.trim().orEmpty()

                // favid：取消收藏链接 op=delete&favid=xxx
                val favid = li.selectFirst("a[href*=op=delete]")
                    ?.attr("href")?.let { h ->
                        Regex("""favid=(\d+)""", RegexOption.IGNORE_CASE).find(h)?.groupValues?.get(1)
                    }.orEmpty()

                result.add(ForumFavoriteItem(fid = fid, title = title, favid = favid, dateline = dateline))
            }

            Log.d("S1HTML", "解析版块收藏: ${result.size} 条")
            result
        } catch (e: Exception) {
            Log.e("S1HTML", "解析版块收藏失败", e)
            emptyList()
        }
    }

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
                if (HTML_NAV_WORDS.any { subject == it }) continue
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
}
