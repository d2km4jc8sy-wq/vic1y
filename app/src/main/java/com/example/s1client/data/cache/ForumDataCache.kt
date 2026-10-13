package com.example.s1client.data.cache

import android.content.Context
import com.example.s1client.data.model.ForumCategory
import com.example.s1client.data.model.SubForum
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局数据缓存。
 *
 * P1 重构（2026-10-13）：内部实现改为 StateFlow，对外 API（属性读写的语法、
 * 持久化触发时机）逐字保留——功能代码照旧 `ForumDataCache.memberUid = x` 直读写。
 * 需要响应式订阅的地方（阶段 3 后续）可观察对应的 `*Flow` 属性。
 *
 * 持久化语义不变：user 字段的落盘只发生在显式 saveUser()/clearUser() 调用，
 * setter 本身不落盘（与重构前一致）。
 */
object ForumDataCache {

    // ---- 内部 StateFlow 存储 ----
    private val _forumList = MutableStateFlow<List<SubForum>>(emptyList())
    private val _catList = MutableStateFlow<List<ForumCategory>>(emptyList())
    private val _formhash = MutableStateFlow("")
    private val _memberUid = MutableStateFlow("0")
    private val _memberUsername = MutableStateFlow("")
    private val _memberCredits = MutableStateFlow("0")
    private val _groupTitle = MutableStateFlow("")
    private val _groupId = MutableStateFlow("")
    private val _memberRegDate = MutableStateFlow("")
    private val _memberSignature = MutableStateFlow("")

    // ---- 对外属性（语法与重构前一致）----

    var forumList: List<SubForum>
        get() = _forumList.value
        set(value) { _forumList.value = value }

    var catList: List<ForumCategory>
        get() = _catList.value
        set(value) { _catList.value = value }

    var formhash: String
        get() = _formhash.value
        set(value) { _formhash.value = value }

    var memberUid: String
        get() = _memberUid.value
        set(value) { _memberUid.value = value }

    var memberUsername: String
        get() = _memberUsername.value
        set(value) { _memberUsername.value = value }

    var memberCredits: String
        get() = _memberCredits.value
        set(value) { _memberCredits.value = value }

    var groupTitle: String
        get() = _groupTitle.value
        set(value) { _groupTitle.value = value }

    var groupId: String
        get() = _groupId.value
        set(value) { _groupId.value = value }

    var memberRegDate: String
        get() = _memberRegDate.value
        set(value) { _memberRegDate.value = value }

    var memberSignature: String
        get() = _memberSignature.value
        set(value) { _memberSignature.value = value }

    // ---- 响应式只读出口（新增，供阶段 3 后续订阅；不影响既有调用点）----
    val forumListFlow: StateFlow<List<SubForum>> get() = _forumList.asStateFlow()
    val memberUidFlow: StateFlow<String> get() = _memberUid.asStateFlow()
    val memberUsernameFlow: StateFlow<String> get() = _memberUsername.asStateFlow()

    // ---- 持久化（语义不变）----

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

    // ---- 查询助手（不变）----

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
