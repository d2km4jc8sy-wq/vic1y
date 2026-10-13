package com.example.s1client.util

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 单楼展开：被全局拉黑 / 本帖屏蔽的楼层，用户手动「点击展开」后只恢复这一楼，
 * 该人其余楼层仍保持折叠。
 *
 * 与 [BlockedAuthorManager]（全局拉黑）、[ThreadMutedUsers]（本帖屏蔽）、
 * [HiddenPostManager]（整帖隐藏）互不冲突：折叠优先级是「全局拉黑 / 本帖屏蔽 > 单楼展开（例外）」。
 *
 * 状态按 tid|pid 持久化到本地，重开帖子 / 重进详情页依然有效；同时用 StateFlow 暴露，
 * 配合详情页的 notifyBlockStateChanged 即时重绑。
 */
object ExpandedFloors {

    private const val PREFS_NAME = "s1_expanded_floors"
    private const val KEY_EXPANDED = "expanded"

    private lateinit var prefs: SharedPreferences

    private val _expanded = MutableStateFlow<Set<String>>(emptySet())
    val flow: StateFlow<Set<String>> = _expanded.asStateFlow()

    fun init(app: Application) {
        prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _expanded.value = load()
    }

    private fun key(tid: String, pid: String) = "$tid|$pid"

    /** 这一楼（按 pid）是否已被单独展开 */
    fun isExpanded(tid: String?, pid: String?): Boolean {
        if (tid.isNullOrBlank() || pid.isNullOrBlank()) return false
        return _expanded.value.contains(key(tid, pid))
    }

    /** 单独展开这一楼（按 pid 精确，只恢复这一楼） */
    fun expand(tid: String?, pid: String?) {
        if (tid.isNullOrBlank() || pid.isNullOrBlank()) return
        val k = key(tid, pid)
        if (_expanded.value.contains(k)) return
        _expanded.value = _expanded.value + k
        save()
    }

    /** 收回这一楼的单独展开（恢复折叠） */
    fun collapse(tid: String?, pid: String?) {
        if (tid.isNullOrBlank() || pid.isNullOrBlank()) return
        val k = key(tid, pid)
        if (!_expanded.value.contains(k)) return
        _expanded.value = _expanded.value - k
        save()
    }

    private fun load(): Set<String> {
        val raw = prefs.getString(KEY_EXPANDED, null) ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return try {
            val arr = org.json.JSONArray(raw)
            val set = linkedSetOf<String>()
            for (i in 0 until arr.length()) {
                val s = arr.optString(i, "")
                if (s.isNotBlank()) set.add(s)
            }
            set
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun save() {
        val arr = org.json.JSONArray()
        _expanded.value.forEach { arr.put(it) }
        prefs.edit().putString(KEY_EXPANDED, arr.toString()).apply()
    }
}
