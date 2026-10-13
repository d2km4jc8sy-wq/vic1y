package com.example.s1client

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.s1client.data.api.ApiResult
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.api.safeApiCall
import com.example.s1client.data.model.SubForum
import com.example.s1client.util.BlockedAuthorManager
import com.example.s1client.util.DataSaverStore
import com.example.s1client.util.ReadHistoryStore
import com.example.s1client.util.HiddenPostManager
import com.example.s1client.util.TimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class UiState {
    object Loading : UiState()
    data class Success(
        val posts: List<Post>,
        val isLoadingMore: Boolean = false,
        val isRefreshing: Boolean = false,
        val currentPage: Int = 1,
        /** 版块总页数；0 表示还没拿到真实数据（不要再写死成 500） */
        val totalPages: Int = 0
    ) : UiState()
    data class Error(val message: String) : UiState()
}

/** 首页顶部的一个版面 Tab */
data class ForumTab(val fid: String, val name: String)

/**
 * 首页信息流。
 *
 * 每个版面各自持有一份列表状态并缓存在内存里，左右滑动切版面时切回不会重新请求。
 * 需要释放时调用 [clearCache]。
 */
class PostViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("s1_home_tabs", Context.MODE_PRIVATE)

    /** 接口未返回版面表时使用的兜底（与 stage1st.com 在线核对过的真实版面） */
    private val fallbackTabs = listOf(
        ForumTab("4", "游戏论坛"),
        ForumTab("6", "动漫论坛"),
        ForumTab("48", "影视论坛"),
        ForumTab("51", "ＰC数码"),
        ForumTab("135", "手游页游"),
        ForumTab("136", "模玩手办"),
        ForumTab("144", "欧美动漫"),
        ForumTab("157", "归墟")
    )

    /** 服务端提供的全部可选版面，供「增加板块」挑选 */
    private val _allForums = MutableStateFlow(fallbackTabs)
    val allForums: StateFlow<List<ForumTab>> = _allForums.asStateFlow()

    /** 用户当前显示的 Tab（可增删、可调序，落 SharedPreferences 持久化） */
    private val _tabs = MutableStateFlow(loadSavedTabs() ?: fallbackTabs)
    val tabs: StateFlow<List<ForumTab>> = _tabs.asStateFlow()

    /** 用户是否手动配置过 Tab；配置过就不再被服务端返回的版面表覆盖 */
    private var customized: Boolean = prefs.contains(KEY_TABS)

    /** fid -> 该版面的列表状态 */
    private val _feeds = MutableStateFlow<Map<String, UiState>>(emptyMap())
    val feeds: StateFlow<Map<String, UiState>> = _feeds.asStateFlow()

    /** fid -> 该版面的子分类映射（id -> 名称），首次拉取时缓存，用于分类筛选条 */
    private val _threadTypeMaps = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    val threadTypeMaps: StateFlow<Map<String, Map<String, String>>> = _threadTypeMaps.asStateFlow()

    /** fid -> 当前选中的分类筛选（null 表示「全部」） */
    private val _typeFilters = MutableStateFlow<Map<String, String?>>(emptyMap())
    val typeFilters: StateFlow<Map<String, String?>> = _typeFilters.asStateFlow()

    /** 把当前筛选换算成接口参数：filter=typeid & typeid=xxx（不带则为 null） */
    private fun typeQuery(fid: String): Pair<String?, String?> {
        val tid = _typeFilters.value[fid]
        return (if (!tid.isNullOrBlank()) "typeid" else null) to tid
    }

    /** 正在请求中的版面，避免同一版面重复发请求 */
    private val inFlight = mutableSetOf<String>()

    /**
     * 单版面在内存里保留的最大帖子数。超过后丢掉最旧的，防止重度用户无限下滑把内存撑爆。
     * PAGE_SIZE(40) 下约 15 页，足够浏览；触发点都在列表底部，丢最旧的不影响当前可视区，
     * 且首页用 key={it.tid} 渲染，头部裁切不会引起滚动跳动。
     */
    private val MAX_FEED_POSTS = 600

    /** 省流模式：卡片不加载封面图，只显示渐变文字封面。
     *  状态放在全局 DataSaverStore，和「我的」齿轮面板的开关共用，改一处两边同步。 */
    val dataSaver: StateFlow<Boolean> = DataSaverStore.enabled

    /** 已读帖子的 tid */
    private val _readTids = MutableStateFlow(prefs.getStringSet(KEY_READ, emptySet()).orEmpty().toSet())
    val readTids: StateFlow<Set<String>> = _readTids.asStateFlow()

    /** 被「塞抹布」的作者：uid -> 显示名 */
    private val _blockedAuthors = MutableStateFlow(BlockedAuthorManager.allBlocked())
    val blockedAuthors: StateFlow<Map<String, String>> = _blockedAuthors.asStateFlow()

    /** 被「不看该帖」隐藏的帖子 tid */
    private val _hiddenPosts = MutableStateFlow(HiddenPostManager.hiddenTids())
    val hiddenPosts: StateFlow<Set<String>> = _hiddenPosts.asStateFlow()

    init {
        // 省流开关的持久化值读进全局 Store（「我的」页面也要读同一份）
        DataSaverStore.init(getApplication())
        refreshForumList()
        ensureLoaded(_tabs.value.firstOrNull()?.fid ?: fallbackTabs.first().fid)

        // 监听全局 Manager 变化，同步到当前 ViewModel 并实时过滤信息流
        viewModelScope.launch {
            BlockedAuthorManager.blockedFlow.collect { _blockedAuthors.value = it; refilterFeeds() }
        }
        viewModelScope.launch {
            HiddenPostManager.hiddenTidsFlow.collect { _hiddenPosts.value = it; refilterFeeds() }
        }
    }

    // ==================== Tab 配置 ====================

    /** 板块管理「确定」：一次性应用新的 Tab 列表（顺序 + 增删）。
     *  空列表不应用，避免把顶部导航删空。 */
    fun setTabs(list: List<ForumTab>) {
        if (list.isEmpty()) return
        applyTabs(list)
    }

    private fun applyTabs(list: List<ForumTab>) {
        customized = true
        _tabs.value = list
        prefs.edit()
            .putString(KEY_TABS, list.joinToString(",") { "${it.fid}|${it.name}" })
            .apply()
    }

    private fun loadSavedTabs(): List<ForumTab>? {
        val raw = prefs.getString(KEY_TABS, null) ?: return null
        val list = raw.split(",").mapNotNull { item ->
            val i = item.indexOf('|')
            if (i <= 0 || i == item.length - 1) null
            else ForumTab(item.substring(0, i), item.substring(i + 1))
        }
        return list.ifEmpty { null }
    }

    /**
     * 长按 Peek 预览用：取楼主第一层正文的**纯文本**摘要。
     * 列表接口不返回正文，所以这里现取一层；失败返回 null（UI 走降级文案）。
     */
    suspend fun fetchThreadPreview(tid: String): String? = withContext(Dispatchers.IO) {
        try {
            val r = RetrofitClient.api.getThreadDetail(tid = tid, page = 1)
            val msg = r.variables?.postList?.firstOrNull()?.message
                ?: return@withContext null
            val plain = android.text.Html
                .fromHtml(msg, android.text.Html.FROM_HTML_MODE_COMPACT)
                .toString()
            val cleaned = plain
                .replace(Regex("[\\[［]图片[\\]］]"), "")
                .replace('\u00A0', ' ')
                .replace(Regex("\\s+"), " ")
                .trim()
            if (cleaned.isBlank()) null
            else if (cleaned.length > PREVIEW_MAX) cleaned.take(PREVIEW_MAX) + "…"
            else cleaned
        } catch (e: Exception) {
            android.util.Log.w("S1Peek", "预览加载失败: ${e.message}")
            null
        }
    }

    /** 重新拉取版面表（登录状态变化后调用）。成功则更新可选列表；
     *  用户没手动配置过时，Tabs 跟随服务端的一级版面。 */
    fun refreshForumList() {
        viewModelScope.launch {
            when (val r = safeApiCall { RetrofitClient.api.getForumIndex() }) {
                is ApiResult.Success -> {
                    val raw = r.data.variables?.forumList.orEmpty()
                    // 一级版面：默认顶部栏只放这些，避免一次塞进几十个子版块
                    val topLevel = raw
                        .filter { !it.fid.isNullOrBlank() && !it.name.isNullOrBlank() }
                        .map { ForumTab(it.fid!!, it.name!!) }
                    // 全部版面（含子版块）：供「板块管理 → 可添加」挑选
                    val all = flattenForumTabs(raw)
                    if (all.isNotEmpty()) {
                        _allForums.value = all
                        if (!customized) _tabs.value = topLevel.ifEmpty { all }
                    }
                }
                is ApiResult.Failure -> { /* 静默失败，保留旧值 */ }
            }
        }
    }

    /** 首次进入某版面时加载第 1 页；已缓存则直接复用 */
    fun ensureLoaded(fid: String) {
        if (_feeds.value.containsKey(fid) || !inFlight.add(fid)) return
        _feeds.value = _feeds.value + (fid to UiState.Loading)

        viewModelScope.launch {
            loadFirstPage(fid)
            inFlight.remove(fid)
        }
    }

    /** 下拉刷新：丢掉该版面缓存重新加载第 1 页 */
    fun refresh(fid: String) {
        if (!inFlight.add(fid)) return
        _feeds.value = _feeds.value - fid

        viewModelScope.launch {
            loadFirstPage(fid)
            inFlight.remove(fid)
        }
    }

    /** 下拉刷新：保留当前列表（不闪白），只标记刷新中，重新拉第 1 页。
     *  Discuz 的 forumdisplay 默认就是按最后回复时间倒序，所以刷新完即“按回帖时间重排”。 */
    fun pullRefresh(fid: String) {
        val cur = _feeds.value[fid] as? UiState.Success
        if (cur == null) {
            refresh(fid)
            return
        }
        if (cur.isRefreshing || !inFlight.add(fid)) return
        setFeed(fid, cur.copy(isRefreshing = true))

        viewModelScope.launch {
            val (filter, typeId) = typeQuery(fid)
            when (val r = safeApiCall {
                RetrofitClient.api.getThreadList(fid = fid, page = 1, filter = filter, typeId = typeId)
            }) {
                is ApiResult.Success -> {
                    val vars = r.data.variables
                    val threads = vars?.threadList.orEmpty()
                    val types = vars?.threadTypes?.types.orEmpty()
                    setFeed(fid, UiState.Success(
                        posts = threads.map { it.toPost(nameOf(fid), types) }.filterActive(),
                        currentPage = 1,
                        totalPages = realTotalPages(vars, cur.totalPages)
                    ))
                }
                is ApiResult.Failure -> setFeed(fid, cur.copy(isRefreshing = false))
            }
            inFlight.remove(fid)
        }
    }

    /** 无限下拉：加载下一页追加到该版面列表末尾 */
    fun loadMore(fid: String) {
        val cur = _feeds.value[fid] as? UiState.Success ?: return
        if (cur.isLoadingMore || !inFlight.add(fid)) return
        setFeed(fid, cur.copy(isLoadingMore = true))

        viewModelScope.launch {
            val nextPage = cur.currentPage + 1
            val (filter, typeId) = typeQuery(fid)
            when (val r = safeApiCall {
                RetrofitClient.api.getThreadList(fid = fid, page = nextPage, filter = filter, typeId = typeId)
            }) {
                is ApiResult.Success -> {
                    val vars = r.data.variables
                    val types = vars?.threadTypes?.types.orEmpty()
                    val existing = cur.posts.map { it.tid }.toSet()
                    val added = vars?.threadList.orEmpty()
                        .map { it.toPost(nameOf(fid), types) }.filterActive()
                        .filter { it.tid !in existing }
                    setFeed(fid, UiState.Success(
                        posts = capPosts(cur.posts + added),
                        isLoadingMore = false,
                        currentPage = nextPage,
                        totalPages = realTotalPages(vars, cur.totalPages)
                    ))
                }
                is ApiResult.Failure -> setFeed(fid, cur.copy(isLoadingMore = false))
            }
            inFlight.remove(fid)
        }
    }

    /** 跳页：清空该版面列表，加载目标页 */
    fun jumpToPage(fid: String, targetPage: Int) {
        val cur = _feeds.value[fid] as? UiState.Success ?: return
        // totalPages 为 0 表示还没拿到真实数据，此时不限制跳页上限
        if (targetPage < 1 ||
            (cur.totalPages > 0 && targetPage > cur.totalPages) ||
            !inFlight.add(fid)) return
        setFeed(fid, UiState.Loading)

        viewModelScope.launch {
            val (filter, typeId) = typeQuery(fid)
            when (val r = safeApiCall {
                RetrofitClient.api.getThreadList(fid = fid, page = targetPage, filter = filter, typeId = typeId)
            }) {
                is ApiResult.Success -> {
                    val vars = r.data.variables
                    val threads = vars?.threadList.orEmpty()
                    val types = vars?.threadTypes?.types.orEmpty()
                    setFeed(fid, UiState.Success(
                        posts = threads.map { it.toPost(nameOf(fid), types) }.filterActive(),
                        currentPage = targetPage,
                        totalPages = realTotalPages(vars, cur.totalPages)
                    ))
                }
                is ApiResult.Failure -> setFeed(fid, UiState.Error(r.error.userMessage))
            }
            inFlight.remove(fid)
        }
    }

    /** 释放所有已缓存的版面数据 */
    fun clearCache() {
        _feeds.value = emptyMap()
    }

    // ==================== 省流 / 已读 / 屏蔽 ====================

    fun setDataSaver(enabled: Boolean) {
        DataSaverStore.set(getApplication(), enabled)
    }

    /**
     * 标记帖子已读（点开卡片时调用），同时把「tid + 标题」写进浏览历史。
     * 注意：已读过的帖子不会重复写历史（本来就只在第一次点开时记一次），
     * 但标题可能变，所以这里仍然按 tid 去重后刷新标题。
     */
    fun markRead(tid: String, title: String = "") {
        if (tid.isBlank()) return
        if (tid !in _readTids.value) {
            val next = _readTids.value + tid
            _readTids.value = next
            prefs.edit().putStringSet(KEY_READ, next).apply()
        }
        ReadHistoryStore.add(getApplication(), tid, title)
    }

    /** 塞抹布 */
    fun blockAuthor(authorId: String, authorName: String) {
        BlockedAuthorManager.block(authorId, authorName)
    }

    /** 吐抹布 */
    fun unblockAuthor(authorId: String) {
        BlockedAuthorManager.unblock(authorId)
    }

    /** 不看该帖：隐藏单条帖子 */
    fun hidePost(tid: String, title: String = "", author: String = "", time: String = "") {
        HiddenPostManager.hide(tid, title, author, time)
    }

    /** 恢复显示被隐藏的帖子 */
    fun unhidePost(tid: String) {
        HiddenPostManager.unhide(tid)
    }

    /** 切换分类筛选：更新选中态并重新拉取该版面第一页 */
    fun setTypeFilter(fid: String, typeId: String?) {
        if (_typeFilters.value[fid] == typeId) return
        _typeFilters.value = _typeFilters.value + (fid to typeId)
        viewModelScope.launch {
            inFlight.remove(fid)
            loadFirstPage(fid)
            inFlight.remove(fid)
        }
    }

    private fun refilterFeeds() {
        _feeds.value = _feeds.value.mapValues { (_, st) ->
            if (st is UiState.Success) st.copy(posts = st.posts.filterActive()) else st
        }
    }

    private fun List<Post>.filterActive(): List<Post> {
        val blocked = _blockedAuthors.value
        val hidden = _hiddenPosts.value
        return filter {
            (it.authorId.isNullOrBlank() || !blocked.containsKey(it.authorId)) &&
                it.tid !in hidden
        }
    }

    private suspend fun loadFirstPage(fid: String) {
        val (filter, typeId) = typeQuery(fid)
        when (val r = safeApiCall {
            RetrofitClient.api.getThreadList(fid = fid, page = 1, filter = filter, typeId = typeId)
        }) {
            is ApiResult.Success -> {
                val vars = r.data.variables
                val threads = vars?.threadList.orEmpty()
                val types = vars?.threadTypes?.types.orEmpty()
                if (types.isNotEmpty()) {
                    _threadTypeMaps.value = _threadTypeMaps.value + (fid to types)
                }
                setFeed(fid, if (threads.isEmpty()) {
                    UiState.Error(if (typeId.isNullOrBlank()) "这个版块暂时没有帖子" else "该分类下暂无帖子")
                } else {
                    UiState.Success(
                        posts = threads.map { it.toPost(nameOf(fid), types) }.filterActive(),
                        currentPage = 1,
                        totalPages = realTotalPages(vars, 0)
                    )
                })
            }
            is ApiResult.Failure -> setFeed(fid, UiState.Error(r.error.userMessage))
        }
    }

    private fun nameOf(fid: String): String =
        _tabs.value.firstOrNull { it.fid == fid }?.name ?: "杂谈"

    /**
     * 真实总页数 = 版块主题总数 / 每页条数。
     * 数据来自 forumdisplay 响应的 Variables.forum.threads 和 Variables.tpp
     * （实测动漫论坛：threads=189738、tpp=50）。拿不到时沿用旧值。
     */
    private fun realTotalPages(
        vars: com.example.s1client.data.model.ThreadListVariables?,
        fallback: Int
    ): Int {
        val threads = vars?.forum?.threads?.toIntOrNull() ?: return fallback
        if (threads <= 0) return fallback
        val tpp = vars.tpp?.toIntOrNull()?.takeIf { it > 0 } ?: 50
        return ((threads + tpp - 1) / tpp).coerceAtLeast(1)
    }

    private fun setFeed(fid: String, state: UiState) {
        _feeds.value = _feeds.value + (fid to state)
    }

    /** 帖子数超过上限时只保留最新的 MAX_FEED_POSTS 条，丢弃最旧的一批（无限下滑的兜底） */
    private fun capPosts(posts: List<Post>): List<Post> =
        if (posts.size > MAX_FEED_POSTS) posts.takeLast(MAX_FEED_POSTS) else posts
}

/** 长按 Peek 预览正文的最大字数 */
private const val PREVIEW_MAX = 300

private const val KEY_TABS = "home_tabs"
private const val KEY_READ = "read_tids"

/** 递归展开版面（含子版块）；顺序保持父在前、子紧随其后，按 fid 去重。
 *  首页板块管理与发帖选版面共用。 */
internal fun flattenForumTabs(list: List<SubForum>): List<ForumTab> {
    val out = ArrayList<ForumTab>()
    val seen = HashSet<String>()
    fun walk(items: List<SubForum>) {
        for (f in items) {
            val fid = f.fid
            val name = f.name
            if (!fid.isNullOrBlank() && !name.isNullOrBlank() && seen.add(fid)) {
                out.add(ForumTab(fid, name))
            }
            f.subList?.let { if (it.isNotEmpty()) walk(it) }
        }
    }
    walk(list)
    return out
}

internal fun com.example.s1client.data.model.Thread.toPost(
    forumName: String,
    typeNames: Map<String, String>
): Post {
    val (cover, aspect) = firstImageInfo()
    // 帖子分类：接口只给 typeid，名称要从 threadtypes 映射表查；
    // 查不到再退到 typename 字段，最后退到版面名。
    val category = typeNames[typeId?.trim().orEmpty()]
        ?: typeName?.takeIf { it.isNotBlank() }
        ?: forumName
    // 带图判断：能取到封面，或接口说这个帖子有附件
    val hasImage = cover != null || (attachment?.toIntOrNull() ?: 0) > 0
    return Post(
        id = tid?.toIntOrNull() ?: 0,
        title = subject ?: "无标题",
        coverUrl = cover,
        coverAspect = aspect,
        tag = category,
        authorName = author ?: "匿名",
        authorId = authorId,
        likes = 0,
        replies = replies?.toIntOrNull() ?: 0,
        tid = tid ?: "",
        typeId = typeId,
        timeText = TimeUtil.formatRelative(dateline),
        views = views?.toIntOrNull() ?: 0,
        hasImage = hasImage
    )
}

/**
 * 从 attachmentImagePreviewList 取第一张图片附件。
 * 返回 (图片绝对 URL, 宽高比) —— 无图时均为 null。
 * 宽高比限制在 0.75~1.6，避免极端长图/宽图把瀑布流撑坏。
 */
private fun com.example.s1client.data.model.Thread.firstImageInfo(): Pair<String?, Float?> {
    val obj = attachmentImagePreviewList
        ?.firstOrNull { it.isJsonObject }
        ?.asJsonObject
        ?: return null to null

    val isImage = obj.get("isimage")
    if (isImage != null && isImage.isJsonPrimitive && isImage.asInt == 0) return null to null

    val att = obj.get("attachment")
    val path = if (att != null && att.isJsonPrimitive) att.asString else null

    val w = obj.get("width")?.takeIf { it.isJsonPrimitive }?.asFloat
    val h = obj.get("height")?.takeIf { it.isJsonPrimitive }?.asFloat
    val aspect = if (w != null && h != null && w > 0f && h > 0f) {
        (w / h).coerceIn(0.75f, 1.6f)
    } else {
        null
    }

    return com.example.s1client.util.attachmentUrl(path) to aspect
}
