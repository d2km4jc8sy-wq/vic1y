package com.example.s1client.util

import android.content.Context
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.util.HtmlParser
import com.example.s1client.util.NoticeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 站内「提醒」未读状态管理。
 *
 * - 通知（回复/@/系统）：Discuz 通知页本身不暴露稳定的「未读/已读」标记，
 *   这里采用「已看锚点」方案 —— 用户打开提醒中心并加载到的通知 id 记为已读，
 *   之后再次拉取时，凡是 id 不在已读集合里的都算未读。锚点持久化在 SharedPreferences，跨进程保留。
 *   首次启动时把当前所有通知记为已读（建立基线），避免一进来就显示一堆历史未读（如「15」）。
 * - 私信：直接复用 HtmlParser.parsePmList 的 isNew 标记（服务端真实的未读状态）；
 *   同时维护一份客户端已看基线，首次启动一并清零，点开会话后也记入基线，使红点 / 角标可消失。
 *
 * 底栏「提醒」图标据此显示未读数（红点/数字），真有未读才变状态。
 */
object ReminderUnread {
    private const val PREFS = "s1_reminder_unread_v2"
    private const val KEY_SEEN = "seen_notice_ids"
    private const val KEY_SEEN_PM = "seen_pm_uids"
    private const val MAX_SEEN = 500

    /** 已读过的通知 id 集合 */
    fun loadSeenIds(context: Context): LinkedHashSet<String> {
        val set = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_SEEN, emptySet()) ?: emptySet()
        return LinkedHashSet(set)
    }

    /** 已看过的私信对方 uid 集合 */
    fun loadSeenPmUids(context: Context): LinkedHashSet<String> {
        val set = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_SEEN_PM, emptySet()) ?: emptySet()
        return LinkedHashSet(set)
    }

    /** 把一批通知 id 标记为已读并持久化 */
    fun markSeen(context: Context, ids: Collection<String>) {
        val filtered = ids.filter { it.isNotBlank() }
        if (filtered.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cur = loadSeenIds(context)
        if (cur.containsAll(filtered)) return
        cur.addAll(filtered)
        val toSave = if (cur.size > MAX_SEEN) cur.toList().takeLast(MAX_SEEN).toSet() else cur
        prefs.edit().putStringSet(KEY_SEEN, toSave).apply()
    }

    /** 把一个私信会话标记为已看（点开会话后调用），红点 / 角标随即消失 */
    fun markPmSeen(context: Context, uid: String) {
        if (uid.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cur = loadSeenPmUids(context)
        if (cur.contains(uid)) return
        cur.add(uid)
        prefs.edit().putStringSet(KEY_SEEN_PM, cur).apply()
    }

    /**
     * 计算当前未读数：未看通知数 + 未读私信数。
     * 首次运行（没有任何基线）时把当前所有通知 / 私信记为已读，返回 0，
     * 这样不会一进来就显示一堆历史未读（如「15」）。
     * 网络失败或没登录时返回 0（不打扰、不报错）。
     */
    suspend fun computeUnread(context: Context): Int = withContext(Dispatchers.IO) {
        try {
            // 通知：和提醒中心「全部」页一致，分别拉三个分类再合并，保证 id 对齐
            val noticeUrls = listOf(
                "https://stage1st.com/2b/home.php?mod=space&do=notice&view=mypost",
                "https://stage1st.com/2b/home.php?mod=space&do=notice&view=interactive",
                "https://stage1st.com/2b/home.php?mod=space&do=notice&view=system"
            )
            val notices = noticeUrls.flatMap { url ->
                NoticeParser.parse(RetrofitClient.fetchHtml(url))
            }
            val pmList = HtmlParser.parsePmList(
                RetrofitClient.fetchHtml("https://stage1st.com/2b/home.php?mod=space&do=pm")
            )

            val seen = loadSeenIds(context)
            val seenPm = loadSeenPmUids(context)

            // 首次运行：建立基线，把当前所有通知 / 私信记为已读，避免历史未读刷屏
            if (seen.isEmpty() && seenPm.isEmpty()) {
                markSeen(context, notices.mapNotNull { it.id.takeIf { id -> id.isNotBlank() } })
                markPmSeenAll(context, pmList.mapNotNull { it.touid.takeIf { u -> u.isNotBlank() } })
                return@withContext 0
            }

            val unreadNotice = notices.count { it.id.isNotBlank() && !seen.contains(it.id) }
            val pmUnread = pmList.count { it.isNew && it.touid.isNotBlank() && !seenPm.contains(it.touid) }
            unreadNotice + pmUnread
        } catch (e: Exception) {
            0
        }
    }

    private fun markPmSeenAll(context: Context, uids: Collection<String>) {
        if (uids.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cur = loadSeenPmUids(context)
        cur.addAll(uids)
        prefs.edit().putStringSet(KEY_SEEN_PM, cur).apply()
    }
}
