package com.example.s1client.util

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局「塞抹布」作者管理。
 *
 * 由于用户卡片 [com.example.s1client.ui.showUserCardDialog] 是顶层函数，无法直接拿到
 * ViewModel；但屏蔽作者本质上是一份全局持久化数据，所以抽成单例 Manager。
 *
 * 与 [com.example.s1client.PostViewModel] 共用同一个 SharedPreferences 文件和 key，
 * 保证旧版数据无缝兼容。PostViewModel 监听 [blockedFlow] 即可同步 UI 过滤。
 */
object BlockedAuthorManager {

    private const val PREFS_NAME = "s1_home_tabs"
    private const val KEY_BLOCKED = "blocked_authors"

    private lateinit var prefs: SharedPreferences

    private val _blocked = MutableStateFlow<Map<String, String>>(emptyMap())
    val blockedFlow: StateFlow<Map<String, String>> = _blocked.asStateFlow()

    fun init(app: Application) {
        prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _blocked.value = load()
    }

    /** 塞抹布：屏蔽某位作者 */
    fun block(uid: String, name: String) {
        if (uid.isBlank()) return
        val next = _blocked.value.toMutableMap()
        next[uid] = name.ifBlank { uid }
        _blocked.value = next
        save(next)
    }

    /** 吐抹布：解除屏蔽 */
    fun unblock(uid: String) {
        if (uid.isBlank()) return
        val next = _blocked.value.toMutableMap().apply { remove(uid) }
        _blocked.value = next
        save(next)
    }

    fun isBlocked(uid: String?): Boolean =
        !uid.isNullOrBlank() && _blocked.value.containsKey(uid)

    fun allBlocked(): Map<String, String> = _blocked.value

    private fun load(): Map<String, String> {
        val raw = when (val stored = prefs.all[KEY_BLOCKED]) {
            null -> return emptyMap()
            is String -> stored
            is Set<*> -> stored.filterIsInstance<String>().joinToString("\n")
            else -> return emptyMap()
        }
        if (raw.isBlank()) return emptyMap()
        return raw.split("\n").mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val i = line.indexOf('|')
            if (i <= 0) line to line else line.substring(0, i) to line.substring(i + 1)
        }.toMap()
    }

    private fun save(map: Map<String, String>) {
        prefs.edit()
            .putString(KEY_BLOCKED, map.entries.joinToString("\n") { "${it.key}|${it.value}" })
            .apply()
    }
}
