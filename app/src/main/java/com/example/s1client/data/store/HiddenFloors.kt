package com.example.s1client.data.store

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 单楼屏蔽：在某个帖子（tid）内，单独隐藏某一层（pid），粒度比「本帖屏蔽此人」更细。
 *
 * 与 [BlockedAuthorManager]（全局拉黑）、[ThreadMutedUsers]（本帖屏蔽某人）、
 * [HiddenPostManager]（整帖隐藏）、[ExpandedFloors]（单楼展开）是五个**独立作用域**，互不冲突：
 *  - 全局拉黑：作者所有帖消失 + 任意帖内其楼层折叠
 *  - 本帖屏蔽此人：仅在该 tid 内折叠某人全部楼层
 *  - 屏蔽此楼：仅在该 tid 内折叠指定 pid 这一层（其余楼层不受影响）
 *  - 单楼展开：被上述任一方式折叠的楼层，手动「点击展开」后只恢复这一楼
 *
 * 折叠优先级：「全局拉黑 / 本帖屏蔽此人 / 屏蔽此楼 > 单楼展开（例外）」。
 * 即被隐藏的这一楼仍可点折叠提示「展开」来看原文，展开后由 [ExpandedFloors] 记住，重进依然展开。
 *
 * 数据按 tid|pid 持久化到本地，并用 StateFlow 暴露，详情页观察后即时重绑楼层。
 */
object HiddenFloors {

    private const val PREFS_NAME = "s1_hidden_floors"
    private const val KEY_HIDDEN = "hidden"

    private lateinit var prefs: SharedPreferences

    private val _hidden = MutableStateFlow<Set<String>>(emptySet())
    val flow: StateFlow<Set<String>> = _hidden.asStateFlow()

    fun init(app: Application) {
        prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _hidden.value = load()
    }

    private fun key(tid: String, pid: String) = "$tid|$pid"

    /** 这一楼（按 pid）是否已被单独屏蔽 */
    fun isHidden(tid: String?, pid: String?): Boolean {
        if (tid.isNullOrBlank() || pid.isNullOrBlank()) return false
        return _hidden.value.contains(key(tid, pid))
    }

    /** 屏蔽这一楼（按 pid 精确，只藏这一楼） */
    fun hide(tid: String?, pid: String?) {
        if (tid.isNullOrBlank() || pid.isNullOrBlank()) return
        val k = key(tid, pid)
        if (_hidden.value.contains(k)) return
        _hidden.value = _hidden.value + k
        save()
    }

    /** 取消屏蔽这一楼（恢复折叠判定） */
    fun unhide(tid: String?, pid: String?) {
        if (tid.isNullOrBlank() || pid.isNullOrBlank()) return
        val k = key(tid, pid)
        if (!_hidden.value.contains(k)) return
        _hidden.value = _hidden.value - k
        save()
    }

    private fun load(): Set<String> {
        val raw = prefs.getString(KEY_HIDDEN, null) ?: return emptySet()
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
        _hidden.value.forEach { arr.put(it) }
        prefs.edit().putString(KEY_HIDDEN, arr.toString()).apply()
    }
}
