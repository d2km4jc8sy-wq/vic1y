package com.example.s1client.util

import com.example.s1client.data.model.CreditItem
import com.example.s1client.data.model.ProfileData
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

object JsonParser {

    fun parseProfile(space: JsonElement?, extCredits: JsonElement?, fallbackUid: String): ProfileData {
        if (space == null || !space.isJsonObject) return ProfileData(uid = fallbackUid)

        val obj = space.asJsonObject

        val uid = getString(obj, "uid", "member_uid") ?: fallbackUid
        val username = getString(obj, "username", "member_username", "name") ?: ""

        // 用户组优先从 group.grouptitle
        var groupName = ""
        obj.get("group")?.let { g ->
            if (g.isJsonObject) {
                groupName = getString(g.asJsonObject, "grouptitle", "groupname") ?: ""
            }
        }
        if (groupName.isEmpty()) {
            groupName = getString(obj, "groupname", "grouptitle") ?: ""
        }

        val friends = getString(obj, "friends", "friendnum") ?: "0"
        val threads = getString(obj, "threads", "threadcount") ?: "0"
        val posts = getString(obj, "posts", "postcount", "replycount") ?: "0"
        val oltime = getString(obj, "oltime", "onlinetime") ?: ""
        val regDate = getString(obj, "regdate", "registerdate") ?: ""
        val lastVisit = getString(obj, "lastvisit", "lastvisitdate") ?: ""
        val lastActivity = getString(obj, "lastactivity", "lastactivedate") ?: ""
        val lastPost = getString(obj, "lastpost", "lastpostdate") ?: ""
        val customStatus = getString(obj, "customstatus", "status") ?: ""
        val credits = getString(obj, "credits", "credit") ?: ""

        val creditItems = parseExtCredits(extCredits, obj)

        return ProfileData(
            uid = uid, username = username, groupName = groupName,
            friends = friends, threads = threads, posts = posts,
            oltime = oltime,
            regDate = formatTimestamp(regDate),
            lastVisit = formatTimestamp(lastVisit),
            lastActivity = formatTimestamp(lastActivity),
            lastPost = formatTimestamp(lastPost),
            customStatus = customStatus, credits = credits, creditItems = creditItems
        )
    }

    private fun parseExtCredits(extCredits: JsonElement?, spaceObj: JsonObject): List<CreditItem> {
        val result = mutableListOf<CreditItem>()

        if (extCredits != null && extCredits.isJsonObject) {
            val creditsObj = extCredits.asJsonObject
            for ((key, v) in creditsObj.entrySet()) {
                if (!v.isJsonObject) continue
                val o = v.asJsonObject
                val title = getString(o, "title", "name") ?: "积分$key"
                val unit = getString(o, "unit") ?: ""
                val value = getString(spaceObj, "extcredits$key") ?: "0"
                result.add(CreditItem(title, value, unit))
            }
        }

        if (extCredits != null && extCredits.isJsonArray) {
            return parseCreditArray(extCredits.asJsonArray)
        }

        return result
    }

    private fun parseCreditArray(arr: JsonArray): List<CreditItem> {
        val result = mutableListOf<CreditItem>()
        for (el in arr) {
            if (!el.isJsonObject) continue
            val o = el.asJsonObject
            val title = getString(o, "title", "name") ?: continue
            val value = getString(o, "value", "credit") ?: "0"
            val unit = getString(o, "unit") ?: ""
            result.add(CreditItem(title, value, unit))
        }
        return result
    }

    private fun getString(obj: JsonObject, vararg keys: String): String? {
        for (k in keys) {
            val el = obj.get(k) ?: continue
            if (el.isJsonNull) continue
            if (el.isJsonPrimitive) {
                val s = el.asString
                if (s.isNotEmpty() && s != "null") return s
            }
        }
        return null
    }

    private fun formatTimestamp(raw: String): String {
        if (raw.isBlank()) return ""
        val ts = raw.toLongOrNull() ?: return raw
        if (ts < 1000000000L) return raw
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(ts * 1000))
        } catch (e: Exception) { raw }
    }
}
