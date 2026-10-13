package com.example.s1client.ui

import android.util.Log
import android.util.SparseArray
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.s1client.data.model.Post
import com.example.s1client.util.RateInfo

/**
 * ViewPager2 的适配器。
 * 每个 item = VerticalRecyclerView（PostAdapter）。
 *
 * 垂直滑动由 VerticalRecyclerView 自己处理，避免被 ViewPager2 抢走。
 *
 * 页面按**页码**存放（而不是顺序 append）：跳到第 N 页时只需请求第 N 页一次，
 * 不必把中间的页逐页加载一遍（旧实现跳远页会串行拉取所有中间页，非常慢）。
 * 尚未加载到的页码先渲染成占位，滑到时由 [onPageMissing] 回调触发按需加载。
 */
class PageAdapter(
    private val onPageScroll: (position: Int, rv: RecyclerView, dy: Int) -> Unit,
    private val subject: String = "",
    private val tid: String = "",
    private val onPostAvatarClick: ((Post) -> Unit)? = null,
    private val onPostEdit: ((Post) -> Unit)? = null,
    private val onPostRateClick: ((Post) -> Unit)? = null,
    private val onPostQuoteClick: ((Post) -> Unit)? = null,
    /** 点收藏图标：回传被点的 Post */
    private val onPostFavoriteClick: ((Post) -> Unit)? = null,
    private val onPostMoreClick: ((Post) -> Unit)? = null,
    private val rateProvider: (String?) -> RateInfo? = { null },
    private val onPostRatingRowClick: ((Post) -> Unit)? = null,
    /** uid -> 战斗力（按 uid 缓存，来自 Profile API），取不到就隐藏 */
    private val powerProvider: (String?) -> String? = { null },
    /** 点独立引用卡片：传出被引楼号（字符串数字）用于同页/跨页定位跳转 */
    private val onPostQuoteJump: ((String?) -> Unit)? = null,
    /** pid -> 是否已被本地楼层收藏 */
    private val bookmarkProvider: (String?) -> Boolean = { false },
    /** 滑到尚未加载的页码时回调，交给 Activity 去请求这一页 */
    private val onPageMissing: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<PageAdapter.PageVH>() {

    /** 多选分享：是否处于多选模式（仅 applyShareSelectMode 内部可改，onBindViewHolder 读最新值） */
    var shareSelectMode: Boolean = false
        private set
    /** pid -> 是否已选（由 Activity 注入） */
    var shareIsSelected: (Post) -> Boolean = { false }
    /** 勾选/取消回调（由 Activity 注入） */
    var shareOnToggle: ((Post) -> Unit)? = null

    /** 页码(1-based) -> 该页楼层 */
    private val pages = SparseArray<List<Post>>()
    /** 已加载页码中的最大值，决定 ViewPager 一共有多少个位置 */
    private var maxPage = 0

    /**
     * 最多在内存里保留多少页楼层。跳页穿梭（1→9→50→3）时只留最近访问的，
     * 最久没碰的页会被摘掉内容（置为未加载），再次滑到该页时由 onPageMissing
     * 惰性重新拉取——释放内存但用户无感。
     */
    private val MAX_PAGES = 30
    /** 页面访问顺序（LRU）：队首是最久没碰过的，超出上限时淘汰它 */
    private val pageAccess = LinkedHashSet<Int>()

    /** 当前已创建的 PostAdapter（按 ViewPager 位置），用于评分行数据到达后的原地刷新 */
    private val liveAdapters = mutableMapOf<Int, PostAdapter>()
    /** 各页的 RecyclerView（按 ViewPager 位置），用于跳页后精确滚动到指定下标 */
    private val liveRvs = mutableMapOf<Int, RecyclerView>()

    /**
     * 只把「位置」扩到 [upTo] 页，不填内容 —— 为已知但尚未加载的页码预留槽位。
     *
     * 为什么必须有这个：[getItemCount] 返回的就是 maxPage，而 ViewPager2 的可滑动范围
     * 由 itemCount 决定。之前 maxPage 只在真正拿到数据时才 +1，于是最后一页（比如共 5 页
     * 只加载到第 4 页）的位置压根不存在，用户**物理上滑不到**，预加载链一断就永远卡在 n-1 页。
     *
     * 预留出来的空槽由 [onBindViewHolder] 的占位分支渲染，并由 onPageMissing 惰性拉取。
     */
    fun reserveUpTo(upTo: Int) {
        if (upTo <= maxPage) return
        val oldMax = maxPage
        maxPage = upTo
        if (oldMax == 0) notifyItemInserted(upTo - 1)
        else notifyItemRangeInserted(oldMax, upTo - oldMax)
    }

    /** 按页码写入一页：已存在则刷新，是新的最大页则插入，否则整体刷新 */
    fun setPage(page: Int, posts: List<Post>) {
        if (page < 1) return
        pages.put(page, posts)
        // 更新 LRU 访问序，并淘汰最久未访问、超出上限的页（释放内存）
        pageAccess.remove(page)
        pageAccess.add(page)
        while (pageAccess.size > MAX_PAGES) {
            // 不能淘汰当前最大页：它的槽位决定 itemCount，摘掉内容会让最后一页变成
            // 空占位（用户看到的是"最后一页什么都没有"）。往回找第一个可淘汰的。
            val victim = pageAccess.firstOrNull { it < maxPage }
                ?: break
            pageAccess.remove(victim)
            pages.remove(victim)
        }
        // 注意两件事，否则跳远页会闪：
        // 1) 填充中间空洞时 itemCount 没变，必须走局部刷新；
        //    用 notifyDataSetChanged() 会让 ViewPager2 整体重绑、正在看的页面跟着闪
        // 2) 页码跳跃增长时（1 -> 9）要用 notifyItemRangeInserted 补齐数量，
        //    只用 notifyItemInserted 会让 RecyclerView 认为只多了 1 个 item，触发一致性重排
        if (page > maxPage) {
            val oldMax = maxPage
            maxPage = page
            if (oldMax == 0) notifyItemInserted(page - 1)
            else notifyItemRangeInserted(oldMax, page - oldMax)
        } else {
            notifyItemChanged(page - 1)
        }
    }

    fun hasPage(page: Int): Boolean = pages.get(page) != null

    /** 取某一页的楼层，参数是**页码（1-based）** */
    fun getPage(page: Int): List<Post>? = pages.get(page)

    /** 已加载的最大页码 */
    fun getPageCount(): Int = maxPage

    /** 评分数据更新后重绑已有楼层（不重建 adapter，竖向滚动位置不变） */
    fun notifyRatingsChanged() {
        Log.d("S1FLICK", "REBIND(notifyRatingsChanged)")
        val adapters = liveAdapters.values.toList()
        for (a in adapters) a.notifyDataSetChanged()
    }

    /**
     * 楼层收藏后重绑。
     *
     * 图标现在**不再做实心/空心变化**（用户拍板：形状已经区隔了收藏楼层与收藏帖子，
     * 再叠状态切换只会让人盯着图标猜状态），所以这里已经没有视觉可刷新。
     * 保留函数是为了不动调用方；内部退化为空实现。
     */
    fun notifyBookmarksChanged() {
        // 预留：若将来恢复状态指示，在此调用 adapters 刷新
    }

    /**
     * 黑名单/本帖屏蔽状态变化后：失效各页 PostAdapter 的命中缓存并重绑，
     * 让已渲染楼层即时折楼/展开（滚动位置不变）。
     */
    fun notifyBlockStateChanged() {
        val adapters = liveAdapters.values.toList()
        for (a in adapters) {
            a.invalidateBlockCaches()
            a.notifyDataSetChanged()
        }
    }

    /**
     * 进入/退出多选分享模式：更新模式标志并刷新所有已加载页的选中态。
     * 新滑入的页会在 onBindViewHolder 里读到最新 shareSelectMode / 选中判定 / 回调。
     */
    fun applyShareSelectMode(mode: Boolean) {
        shareSelectMode = mode
        // 同步给所有已加载页的 PostAdapter：它的 shareSelectMode 是实例字段，
        // 仅在构造时拿过一次值，必须这里主动更新，否则重绑后仍在非多选分支。
        liveAdapters.values.forEach {
            it.shareSelectMode = mode
            it.notifyDataSetChanged()
        }
    }

    inner class PageVH(val rv: VerticalRecyclerView) : RecyclerView.ViewHolder(rv)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageVH {
        val density = parent.context.resources.displayMetrics.density
        val rv = VerticalRecyclerView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutManager = LinearLayoutManager(parent.context)
            // 底部留白按 dp 算：要能滚出玻璃底栏的高度（原固定 120px 在高分屏不够，
            // 最后一楼会被「说点什么」输入条盖住）
            setPadding(0, (12 * density).toInt(), 0, (92 * density).toInt())
            clipToPadding = false
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            itemAnimator = null
        }
        return PageVH(rv)
    }

    override fun onBindViewHolder(holder: PageVH, position: Int) {
        val page = position + 1
        val posts = pages.get(page)
        if (posts == null) {
            // 还没加载过的页码：先给空列表占位，同时让 Activity 去拉这一页
            holder.rv.adapter = PostAdapter(emptyList(), subject, tid,
                powerProvider = powerProvider, onPostQuoteJump = onPostQuoteJump,
                bookmarkProvider = bookmarkProvider,
                shareSelectMode = shareSelectMode, isShareSelected = shareIsSelected,
                onShareToggle = shareOnToggle)
            liveAdapters.remove(position)
            onPageMissing?.invoke(page)
            return
        }
        val pa = PostAdapter(
            posts, subject, tid, onPostAvatarClick, onPostEdit,
            onPostRateClick, onPostQuoteClick, onPostFavoriteClick, onPostMoreClick,
            rateProvider, onPostRatingRowClick, powerProvider, onPostQuoteJump,
            bookmarkProvider,
            shareSelectMode = shareSelectMode, isShareSelected = shareIsSelected,
            onShareToggle = shareOnToggle
        )
        holder.rv.adapter = pa
        liveAdapters[position] = pa
        liveRvs[position] = holder.rv

        holder.rv.clearOnScrollListeners()
        holder.rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                onPageScroll(position, rv, dy)
            }
        })
        holder.rv.post { onPageScroll(position, holder.rv, 0) }
    }

    override fun onViewRecycled(holder: PageVH) {
        val pa = holder.rv.adapter
        if (pa != null) liveAdapters.values.remove(pa)
        liveRvs.values.remove(holder.rv)
        super.onViewRecycled(holder)
    }

    /**
     * 跳页后让指定页（1-based）的列表滚到某下标。
     * 不依赖「当前可见页」的 attachedRv，避免跳页时 attachedRv 仍指向旧页而导致滚错页、够不到末楼。
     */
    fun scrollToPositionInPage(page: Int, index: Int) {
        liveRvs[page - 1]?.scrollToPosition(index)
    }

    override fun getItemCount() = maxPage
}
