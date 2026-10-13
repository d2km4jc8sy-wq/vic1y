package com.example.s1client.util

import android.util.Log
import com.example.s1client.data.model.PmListItem
import com.example.s1client.data.model.PmMessage
import org.jsoup.Jsoup

/** 私信解析：会话列表页 / 会话消息页（home.php?mod=space&do=pm 的 HTML） */
object PmParser {

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
                val touid = extractUidFromSpaceUrl(userLink.attr("href")) ?: continue
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
