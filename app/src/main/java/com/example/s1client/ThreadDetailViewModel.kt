package com.example.s1client

import androidx.lifecycle.ViewModel
import com.example.s1client.util.RateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 帖子详情页的状态宿主（阶段二 ThreadDetailViewModel，2026-10-16）。
 *
 * 收编的是**跨线程 / 数据类**字段（原 Activity 上的 @Volatile 与并发容器）：
 * 页元数据、评分聚合、投票原始值、页级去重闸门。Activity 保留的只有
 * 主线程限定的视图编排状态（currentPage / isLoading / 滚动重试 / 弹层引用等）。
 *
 * 线程安全升级：原 totalPages/perPage 用 @Volatile 保证可见性、
 * rateMap 用 ConcurrentHashMap——现在统一由 MutableStateFlow 承载，
 * 写入走 [updateMeta] 的 CAS 原子更新；Activity 侧通过同形委托属性访问，
 * 四个 Actions 扩展文件零改动。
 *
 * 不放进本 VM 的：tid/subject（不可变入口参数）、附在 PageAdapter 上的页内容
 * （适配器是渲染层，页数据经 setPage 单向流入）。
 */
class ThreadDetailViewModel : ViewModel() {

    /** 页元数据：分页判定与「只看作者」过滤所需的最小集合 */
    data class Meta(
        /** 总页数：由 thread.replies + ppp 算出（IO 线程写、主线程读） */
        val totalPages: Int = 1,
        /** 实际每页楼层数：论坛返回 t.ppp；与 totalPages 同口径，避免跳页落错页 */
        val perPage: Int = 40,
        /** 已加载的最大页码 */
        val loadedMaxPage: Int = 0,
        /** 顺序往下还能不能继续加载（按 totalPages 判定，非楼层数） */
        val hasMore: Boolean = true,
        /** 楼主 uid / 「只看某作者」的目标 uid（fetchOnePage 在 IO 线程读） */
        val authorId: String? = null,
        val isOnlyAuthor: Boolean = false,
        /** 帖子所属版块 fid（fetchOnePage 在 IO 线程写，发帖选版面用） */
        val threadFid: String = ""
    )

    private val _meta = MutableStateFlow(Meta())
    val meta: StateFlow<Meta> = _meta.asStateFlow()

    /** 原子更新页元数据（多字段并发写不丢更新） */
    fun updateMeta(transform: (Meta) -> Meta) {
        _meta.update(transform)
    }

    /** pid -> 评分聚合（来自网页版 viewthread HTML 的 ratelog；移动版 API 没有） */
    private val ratingsBackend = LinkedHashMap<String, RateInfo>()
    private val _ratings = MutableStateFlow<Map<String, RateInfo>>(emptyMap())
    val ratings: StateFlow<Map<String, RateInfo>> = _ratings.asStateFlow()

    /** 评分聚合的写入面：保持原 rateMap 的 MutableMap 使用习惯（真身 LinkedHashMap，写后同步快照到 StateFlow） */
    val ratingsMap: MutableMap<String, RateInfo> = object : AbstractMutableMap<String, RateInfo>() {
        override fun put(key: String, value: RateInfo): RateInfo? {
            val old = ratingsBackend.put(key, value)
            _ratings.value = ratingsBackend.toMap()
            return old
        }

        override val size: Int
            get() = ratingsBackend.size

        override fun get(key: String): RateInfo? = ratingsBackend[key]

        override val entries: MutableSet<MutableMap.MutableEntry<String, RateInfo>>
            get() = ratingsBackend.entries
    }

    /**
     * 投票原始值：fetchOnePage 在 IO 线程顺手捕获（page==1 响应里的 special 字段），
     * 主线程 applyCapturedPoll 消费后即清。语义与原 @Volatile 字段一致。
     */
    @Volatile
    var capturedPollSpecial: String? = null

    @Volatile
    var capturedPollJson: com.google.gson.JsonObject? = null

    /** 页级请求去重闸门：同一页并发只发一次（调用方全部在主线程协程，容器用并发集合兜底） */
    val inFlightPages: MutableSet<Int> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    /** 已抓过评分的页码去重（loadRatings / submitRate 刷新用） */
    val ratingsFetchedPages: MutableSet<Int> = java.util.concurrent.ConcurrentHashMap.newKeySet()
}
