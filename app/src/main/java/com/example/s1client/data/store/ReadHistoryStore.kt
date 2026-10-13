package com.example.s1client.data.store

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 浏览历史：记录看过的帖子（tid + 标题 + 时间），供「我的 → 历史」真页面使用。
 *
 * 只记 tid 是没法做列表的（标题查不回来），所以这里额外存标题与时间戳，
 * 用 JSON 数组按「最近浏览在前」的顺序保存，上限 [MAX] 条。
 */
object ReadHistoryStore {

    private const val PREF = "s1_read_history"
    private const val KEY = "log"
    private const val MAX = 300

    data class Entry(val tid: String, val title: String, val at: Long)

    fun add(context: Context, tid: String, title: String) {
        if (tid.isBlank()) return
        val list = list(context).toMutableList()
        list.removeAll { it.tid == tid }
        list.add(0, Entry(tid, title.ifBlank { "无标题" }, System.currentTimeMillis()))
        save(context, list.take(MAX))
    }

    fun list(context: Context): List<Entry> {
        val raw = prefs(context).getString(KEY, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = ArrayList<Entry>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val tid = o.optString("tid")
                if (tid.isBlank()) continue
                out.add(Entry(tid, o.optString("title", "无标题"), o.optLong("at")))
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun remove(context: Context, tid: String) {
        save(context, list(context).filterNot { it.tid == tid })
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY).apply()
    }

    private fun save(context: Context, entries: List<Entry>) {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(JSONObject().apply {
                put("tid", e.tid)
                put("title", e.title)
                put("at", e.at)
            })
        }
        prefs(context).edit().putString(KEY, arr.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
}
