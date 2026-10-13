package com.example.s1client.util

import android.util.Log
import com.example.s1client.data.model.Thread
import org.jsoup.Jsoup

/** Discuz search.php 搜索结果页解析（ul/li.pbw 结构） */
object SearchParser {

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
                        authorId = extractUidFromSpaceUrl(link.attr("href")) ?: ""
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
}
