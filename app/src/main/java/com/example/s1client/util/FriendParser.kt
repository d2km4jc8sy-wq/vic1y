package com.example.s1client.util

import android.util.Log
import com.example.s1client.data.model.FriendItem
import org.jsoup.Jsoup

/** 好友列表页解析（Discuz space 好友页，两套结构自适应） */
object FriendParser {

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
                if (HTML_NAV_WORDS.any { text.contains(it) }) continue
                if (seenUids.contains(uid)) continue
                seenUids.add(uid)
                result.add(FriendItem(uid, text, ""))
            }
            result
        } catch (e: Exception) {
            Log.e("S1HTML", "解析好友失败", e); emptyList()
        }
    }
}
