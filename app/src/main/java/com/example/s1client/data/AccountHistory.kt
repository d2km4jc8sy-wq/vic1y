package com.example.s1client.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * 历史登录账号（仿 QQ/微信 多账号切换）。
 * 与 ForumDataCache（当前登录态）相互独立——退出登录时只清 ForumDataCache，历史账号保留。
 * 头像不落盘，显示时由 uid 通过 avatarUrl(uid) 动态加载。
 */
data class HistoryAccount(val uid: String, val username: String, val ts: Long)

object AccountHistory {
    private const val PREF = "s1_account_history"
    private const val KEY = "accounts"
    private const val MAX = 5

    private fun sp(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** 登录成功时调用：按 uid 去重、置顶、最多保留 MAX 个 */
    fun add(ctx: Context, uid: String, username: String) {
        if (uid.isBlank() || uid == "0") return
        val list = getAll(ctx).toMutableList()
        list.removeIf { it.uid == uid }
        list.add(0, HistoryAccount(uid, username, System.currentTimeMillis()))
        val arr = JSONArray()
        list.take(MAX).forEach { a ->
            arr.put(JSONObject().apply {
                put("uid", a.uid)
                put("username", a.username)
                put("ts", a.ts)
            })
        }
        sp(ctx).edit().putString(KEY, arr.toString()).apply()
    }

    fun getAll(ctx: Context): List<HistoryAccount> {
        val raw = sp(ctx).getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                HistoryAccount(o.getString("uid"), o.optString("username"), o.optLong("ts"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clear(ctx: Context) {
        sp(ctx).edit().remove(KEY).apply()
    }
}
