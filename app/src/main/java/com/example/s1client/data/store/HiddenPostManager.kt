package com.example.s1client.data.store

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * 被隐藏帖子的一条记录。
 */
data class HiddenPostInfo(
    val tid: String,
    val title: String,
    val author: String,
    val time: String
)

/**
 * 全局「不看该帖」隐藏帖子管理。
 *
 * 被隐藏的帖子信息持久化在此，首页信息流加载时过滤掉 tid；
 * 「我的 → 抹布贴」可查看标题并恢复。
 */
object HiddenPostManager {

    private const val PREFS_NAME = "s1_hidden_posts"
    private const val KEY_HIDDEN_V2 = "hidden_posts_v2"

    private lateinit var prefs: SharedPreferences

    private val _hiddenPosts = MutableStateFlow<List<HiddenPostInfo>>(emptyList())
    val hiddenPostsFlow: StateFlow<List<HiddenPostInfo>> = _hiddenPosts.asStateFlow()

    private val _hiddenTids = MutableStateFlow<Set<String>>(emptySet())
    val hiddenTidsFlow: StateFlow<Set<String>> = _hiddenTids.asStateFlow()

    fun init(app: Application) {
        prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val list = load()
        _hiddenPosts.value = list
        _hiddenTids.value = list.map { it.tid }.toSet()
    }

    /** 隐藏指定帖子 */
    fun hide(tid: String, title: String = "", author: String = "", time: String = "") {
        if (tid.isBlank()) return
        if (_hiddenTids.value.contains(tid)) return
        val next = _hiddenPosts.value + HiddenPostInfo(
            tid = tid,
            title = title.ifBlank { "帖子 $tid" },
            author = author,
            time = time
        )
        _hiddenPosts.value = next
        _hiddenTids.value = next.map { it.tid }.toSet()
        save(next)
    }

    /** 恢复（取消隐藏）指定帖子 */
    fun unhide(tid: String) {
        val next = _hiddenPosts.value.filter { it.tid != tid }
        _hiddenPosts.value = next
        _hiddenTids.value = next.map { it.tid }.toSet()
        save(next)
    }

    /** 清空所有隐藏 */
    fun unhideAll() {
        _hiddenPosts.value = emptyList()
        _hiddenTids.value = emptySet()
        save(emptyList())
    }

    fun isHidden(tid: String?): Boolean =
        !tid.isNullOrBlank() && tid in _hiddenTids.value

    fun allHidden(): List<HiddenPostInfo> = _hiddenPosts.value

    fun hiddenTids(): Set<String> = _hiddenTids.value

    private fun load(): List<HiddenPostInfo> {
        val raw = prefs.getString(KEY_HIDDEN_V2, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                HiddenPostInfo(
                    tid = obj.optString("tid", ""),
                    title = obj.optString("title", ""),
                    author = obj.optString("author", ""),
                    time = obj.optString("time", "")
                )
            }.filter { it.tid.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun save(list: List<HiddenPostInfo>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("tid", it.tid)
                put("title", it.title)
                put("author", it.author)
                put("time", it.time)
            })
        }
        prefs.edit().putString(KEY_HIDDEN_V2, arr.toString()).apply()
    }
}
