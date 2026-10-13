package com.example.s1client.data.store

import android.content.Context

/**
 * 本地黑名单：仅保留「关键词折叠」。
 *
 * 说明：作者屏蔽（塞抹布）已由 [com.example.s1client.util.BlockedAuthorManager]
 * 统一接管（带 StateFlow、即时刷新列表）；整帖隐藏由
 * [com.example.s1client.util.HiddenPostManager] 接管；本帖屏蔽某人由
 * [com.example.s1client.util.ThreadMutedUsers] 接管。三者作用域互不冲突。
 * 本类只负责「命中关键词的楼层折叠」，属纯本地 SharedPreferences。
 */
object BlacklistManager {

    private const val PREF = "s1_blacklist"
    private const val KEY_WORDS = "blocked_words"     // 关键词，逗号分隔

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ==================== 关键词 ====================

    fun addWord(ctx: Context, word: String) {
        val w = word.trim()
        if (w.isBlank()) return
        val set = blockedWords(ctx).toMutableSet().apply { add(w) }
        prefs(ctx).edit().putString(KEY_WORDS, set.joinToString(",")).apply()
    }

    fun removeWord(ctx: Context, word: String) {
        val set = blockedWords(ctx).toMutableSet().apply { remove(word) }
        prefs(ctx).edit().putString(KEY_WORDS, set.joinToString(",")).apply()
    }

    fun blockedWords(ctx: Context): Set<String> {
        val raw = prefs(ctx).getString(KEY_WORDS, "") ?: ""
        return if (raw.isBlank()) emptySet()
        else raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    /** 文本是否命中屏蔽关键词（命中则折叠） */
    fun isContentBlocked(ctx: Context, text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val words = blockedWords(ctx)
        if (words.isEmpty()) return false
        return words.any { text.contains(it, ignoreCase = true) }
    }
}
