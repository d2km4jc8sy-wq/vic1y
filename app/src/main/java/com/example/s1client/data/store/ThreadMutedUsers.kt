package com.example.s1client.data.store

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 本帖屏蔽：只在某一个帖子（tid）内，折叠某位用户（uid）的楼层回复。
 *
 * 与 [BlockedAuthorManager]（全局拉黑，作用于所有帖）和 [HiddenPostManager]（整帖隐藏）
 * 是三个**独立作用域**，互不冲突：
 *  - 全局拉黑：该作者所有帖从列表消失 + 其楼层在任意帖内折叠
 *  - 本帖屏蔽：仅在该 tid 内折叠某人楼层，不影响列表、不影响其他帖
 *  - 整帖隐藏：该 tid 从列表消失
 *
 * 数据用 StateFlow 暴露，详情页观察后即时重绑楼层（折楼/展开立刻生效）。
 * key 形如 "tid|uid"，value 为昵称（用于恢复页展示）。
 */
object ThreadMutedUsers {

    private const val PREFS_NAME = "s1_thread_mutes"
    private const val KEY_MUTED = "muted"

    private lateinit var prefs: SharedPreferences

    private val _muted = MutableStateFlow<Map<String, String>>(emptyMap())
    val mutedFlow: StateFlow<Map<String, String>> = _muted.asStateFlow()

    fun init(app: Application) {
        prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _muted.value = load()
    }

    private fun key(tid: String, uid: String) = "$tid|$uid"

    /** 在本帖屏蔽某人 */
    fun mute(tid: String, uid: String, name: String) {
        if (tid.isBlank() || uid.isBlank()) return
        val next = _muted.value.toMutableMap()
        next[key(tid, uid)] = name.ifBlank { uid }
        _muted.value = next
        save(next)
    }

    /** 解除本帖内某人的屏蔽 */
    fun unmute(tid: String, uid: String) {
        if (tid.isBlank() || uid.isBlank()) return
        val next = _muted.value.toMutableMap().apply { remove(key(tid, uid)) }
        _muted.value = next
        save(next)
    }

    /** 清空本帖内所有屏蔽 */
    fun unmuteAll(tid: String) {
        if (tid.isBlank()) return
        val prefix = "$tid|"
        val next = _muted.value.filterKeys { !it.startsWith(prefix) }
        _muted.value = next
        save(next)
    }

    fun isMuted(tid: String?, uid: String?): Boolean {
        if (tid.isNullOrBlank() || uid.isNullOrBlank()) return false
        return _muted.value.containsKey(key(tid, uid))
    }

    /** 本帖内被屏蔽的人：uid -> 昵称 */
    fun mutedInThread(tid: String?): Map<String, String> {
        if (tid.isNullOrBlank()) return emptyMap()
        val prefix = "$tid|"
        return _muted.value.filterKeys { it.startsWith(prefix) }
            .mapKeys { it.key.removePrefix(prefix) }
    }

    private fun load(): Map<String, String> {
        val raw = prefs.getString(KEY_MUTED, null) ?: return emptyMap()
        if (raw.isBlank()) return emptyMap()
        return try {
            val arr = org.json.JSONArray(raw)
            val map = linkedMapOf<String, String>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val k = obj.optString("k", "")
                val v = obj.optString("v", "")
                if (k.isNotBlank()) map[k] = v
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun save(map: Map<String, String>) {
        val arr = org.json.JSONArray()
        map.forEach { (k, v) ->
            arr.put(org.json.JSONObject().apply { put("k", k); put("v", v) })
        }
        prefs.edit().putString(KEY_MUTED, arr.toString()).apply()
    }
}
