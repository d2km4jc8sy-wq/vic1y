package com.example.s1client.data.model

import android.content.Context

object ForumDataCache {
    var forumList: List<SubForum> = emptyList()
    var catList: List<ForumCategory> = emptyList()
    var formhash: String = ""
    var memberUid: String = "0"
    var memberUsername: String = ""
    var memberCredits: String = "0"
    var groupTitle: String = ""
    var groupId: String = ""
    var memberRegDate: String = ""
    var memberSignature: String = ""

    private const val PREF = "s1_user_prefs"
    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            loadUser()
        }
    }

    private fun loadUser() {
        val ctx = appContext ?: return
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        memberUid = sp.getString("memberUid", "0") ?: "0"
        memberUsername = sp.getString("memberUsername", "") ?: ""
        memberCredits = sp.getString("memberCredits", "0") ?: "0"
        groupTitle = sp.getString("groupTitle", "") ?: ""
        groupId = sp.getString("groupId", "") ?: ""
        memberRegDate = sp.getString("memberRegDate", "") ?: ""
        memberSignature = sp.getString("memberSignature", "") ?: ""
    }

    fun saveUser() {
        val ctx = appContext ?: return
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("memberUid", memberUid)
            .putString("memberUsername", memberUsername)
            .putString("memberCredits", memberCredits)
            .putString("groupTitle", groupTitle)
            .putString("groupId", groupId)
            .putString("memberRegDate", memberRegDate)
            .putString("memberSignature", memberSignature)
            .apply()
    }

    fun clearUser() {
        memberUid = "0"
        memberUsername = ""
        memberCredits = "0"
        groupTitle = ""
        groupId = ""
        memberRegDate = ""
        memberSignature = ""
        val ctx = appContext ?: return
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = memberUid != "0" && memberUid.isNotEmpty()

    fun getCategoryOfForum(fid: String?): ForumCategory? {
        if (fid == null) return null
        return catList.firstOrNull { it.forumIds?.contains(fid) == true }
    }

    fun getForumsInCategory(cat: ForumCategory): List<SubForum> {
        val ids = cat.forumIds ?: return emptyList()
        return ids.mapNotNull { id -> forumList.firstOrNull { it.fid == id } }
    }

    fun getAllForums(): List<SubForum> {
        val result = mutableListOf<SubForum>()
        for (cat in catList) {
            result.addAll(getForumsInCategory(cat))
        }
        return result
    }

    fun findForum(fid: String?): SubForum? {
        if (fid == null) return null
        for (f in forumList) {
            if (f.fid == fid) return f
            f.subList?.let { subs ->
                for (sub in subs) {
                    if (sub.fid == fid) return sub
                }
            }
        }
        return null
    }
}
