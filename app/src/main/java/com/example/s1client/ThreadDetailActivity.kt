package com.example.s1client

import android.app.Dialog
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import android.util.Log
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.s1client.ui.theme.S1Theme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.data.model.Post
import com.example.s1client.databinding.ActivityThreadDetailBinding
import com.example.s1client.databinding.DialogReplySheetBinding
import com.example.s1client.databinding.DialogUserCardBinding
import com.example.s1client.ui.PageAdapter
import com.example.s1client.util.BbCode
import com.example.s1client.data.store.DraftStore
import com.example.s1client.data.store.FavoriteManager
import androidx.compose.ui.graphics.toArgb
import com.example.s1client.util.Haptic
import com.example.s1client.util.ImageUploader
import com.example.s1client.util.JsonParser
import com.example.s1client.util.S1Notify
import com.example.s1client.util.TimeUtil
import com.example.s1client.data.store.BlockedAuthorManager
import com.example.s1client.data.store.HiddenPostManager
import com.example.s1client.data.store.PostBookmarkManager
import com.example.s1client.data.store.ThreadMutedUsers
import com.example.s1client.data.store.ExpandedFloors
import com.example.s1client.data.store.HiddenFloors
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.s1client.ui.design.AccentPink
import com.example.s1client.ui.design.OnAccent
import com.example.s1client.ui.design.TextSub

class ThreadDetailActivity : BaseActivity() {

    companion object {
        /** 需要滚动定位到的楼层 id（从提醒跳转进来时用） */
        const val EXTRA_PID = "pid"
    }

    internal lateinit var binding: ActivityThreadDetailBinding
    internal var tid: String = ""
    internal var subject: String = ""
    internal var isFavorited: Boolean = false
    internal var currentPage: Int = 1

    /** 待滚动定位的楼层 id；定位成功后立即清空，避免重复执行 */
    private var pendingScrollPid: String? = null
    internal var loadedMaxPage: Int = 0
    internal var hasMore: Boolean = true
    internal var isLoading: Boolean = false
    /**
     * 「回帖后跳到自己新楼层」的专用标志。
     *
     * 不能复用 [isLoading]：jumpToNewReply 会连续翻最多几十页，整个过程占着 isLoading
     * 的话，期间用户在 OnPageChangeCallback 触发的预加载、gotoPage、跳楼层全被门禁挡掉，
     * 弱网下表现就是"整页假死、什么都点不动"。两者语义不同，各管各的。
     */
    internal var jumpLoading: Boolean = false

    internal var authorId: String? = null
    internal var isOnlyAuthor: Boolean = false
    internal var quotedPost: Post? = null
    internal var threadFid: String = ""

    internal var attachedRv: RecyclerView? = null
    /** 总页数：由 thread.replies + ppp 算出。
     *  @Volatile：它在 IO 线程写（fetchOnePage）、主线程读（UI 与翻页判断），必须保证可见性 */
    @Volatile
    internal var totalPages: Int = 1
    /** 实际每页楼层数：论坛返回 t.ppp；与 totalPages 用同一口径，避免跳页落错页。
     *  同样跨线程读写 → @Volatile */
    @Volatile
    internal var perPage: Int = 40

    // ===== 多选分享：选中态 + 已选 pid（按加入顺序，便于按楼层序拼接） =====
    internal var shareSelectMode = false
    internal val shareSelected = java.util.LinkedHashSet<String>()
    internal val SHARE_MAX = 10 // 对齐 s1er 的 shareMaxSelectedFloors

    // ===== 回帖面板的本地图片 =====
    /** 本次回帖上传成功的附件 aid，提交时要带上 attachnew[...] */
    internal var replyAids: MutableList<String> = mutableListOf()
    /** 上传成功后的回调：把 [attachimg] 插进回帖输入框；面板关闭时置空 */
    internal var onReplyImageUploaded: ((String) -> Unit)? = null

    // 用 Activity 级 launcher：面板 ComposeView 在独立 Dialog 窗口里，
    // 拿不到 ActivityResultRegistryOwner，用不到 rememberLauncherForActivityResult
    internal val replyImagePicker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val callback = onReplyImageUploaded
        if (uri == null || callback == null) return@registerForActivityResult
        toast("正在上传图片…")
        lifecycleScope.launch {
            runCatching {
                ImageUploader.upload(
                    this@ThreadDetailActivity, uri,
                    listOf(
                        ImageUploader.replyPage(tid, threadFid),
                        ImageUploader.newthreadPage(threadFid)
                    )
                )
            }
                .onSuccess { aid ->
                    replyAids.add(aid)
                    callback(aid)
                    toast("图片已插入")
                }
                .onFailure {
                    android.util.Log.e("S1Upload", "reply pick/upload failed", it)
                    toast("图片上传失败：" + (it.message ?: it.javaClass.simpleName))
                }
        }
    }

    internal lateinit var pageAdapter: PageAdapter
    internal val PAGE_SIZE = 40  // 每页最多 40 楼

    /** pid -> 评分聚合，来自网页版 viewthread HTML 的 ratelog（移动版 API 没有） */
    internal val rateMap = java.util.concurrent.ConcurrentHashMap<String, com.example.s1client.util.RateInfo>()
    /** 已经抓过评分的页码，避免重复请求。
     *  注意：loadFirstPage / 多个 preloadPage 会并发调用 loadRatings，
     *  这里必须是并发安全集合（普通 mutableSetOf 会 ConcurrentModificationException）。 */
    internal val ratingFetchedPages = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()

    /** 战斗力数据到达后防抖刷新（多条 uid 陆续返回，合并成一次列表重绑） */
    internal val powerRefreshRunnable = Runnable {
        Log.d("S1FLICK", "POWER_ARRIVED")
        pageAdapter.notifyRatingsChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 细节 02：页面转场触觉 —— 进详情页 LIGHT 一震（"进来了"），返回在 finish 处 SELECTION
        Haptic.light(this)
        binding = ActivityThreadDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdge()
        applyTopInset(binding.topBar)
        applyBottomInset(binding.bottomBar)
        applyBottomInset(binding.shareSelectBar)

        tid = intent.getStringExtra("tid") ?: ""
        subject = intent.getStringExtra("subject") ?: ""
        isFavorited = intent.getBooleanExtra("favorited", false)
        // 从提醒跳进来时带的楼层 id（PostLocator 已定位过页号）
        pendingScrollPid = intent.getStringExtra(EXTRA_PID)?.takeIf { it.isNotBlank() }

        binding.btnBack.setOnClickListener { handleBack() }
        if (tid.isEmpty()) { finish(); return }

        binding.btnShareCancel.setOnClickListener { exitShareSelectMode() }
        binding.btnShareSelected.setOnClickListener { shareSelectedFloors() }

        // ===== 用 ViewPager2 内置手势处理水平翻页 =====
        binding.vpPages.isUserInputEnabled = true

        setupPageAdapter()
        setupHideAndMute()

        // 控件 04：换页加载珠子注入主题三色（原版三色追逐）
        val orbitPalette = com.example.s1client.ui.theme.s1PaletteFor(
            com.example.s1client.util.ThemeManager.currentTheme
        )
        binding.progressBar.setDotColors(
            orbitPalette.flowCoverColors.take(3).map { it.toArgb() }
        )

        binding.vpPages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPage = position + 1
                tryScrollToPendingPid()
                hideEndFooter()
                updatePageIndicator()
                loadRatings(currentPage)
                if (currentPage + 1 > loadedMaxPage && morePagesToLoad() && !isLoading) {
                    preloadPage(currentPage + 1)
                }
            }
        })

        binding.btnReplyInput.setOnClickListener { showReplySheet() }
        binding.tvPageInfo.setOnClickListener { showPagePicker() }
        binding.btnFavorite.setOnClickListener { toggleFavorite() }
        binding.btnShare.setOnClickListener { shareCurrentThread() }
        binding.btnOnlyAuthor.setOnClickListener { toggleOnlyAuthor() }


        updateFavoriteButton()
        refreshFavoriteState()
        registerDebugProbe()
        loadFirstPage()
    }

    /**
     * 仅 debug 构建可用的自检入口：让 adb 能直接触发本页的数据加载。
     *
     * 背景：MIUI 默认禁止 `adb shell input tap`（INJECT_EVENTS 被拒），无法自动化点进
     * 帖子，于是「进帖子是否还加载失败」只能靠人肉验证。装了这个探针后，
     * 一条广播就能真实跑一遍 fetchOnePage —— 之前那种"改了但没法验证"的情况能避免。
     *
     * release 构建里 registerReceiver 返回 null，什么都不做。
     */
    internal fun registerDebugProbe() {
        if (!BuildConfig.DEBUG) return
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                val page = intent?.getIntExtra("page", 1) ?: 1
                Log.i("S1Probe", "probe load page=$page tid=$tid")
                lifecycleScope.launch {
                    val posts = fetchOnePage(page)
                    syncPageSocketsOnMain()
                    Log.i(
                        "S1Probe",
                        "probe done page=$page posts=${posts.size} totalPages=$totalPages " +
                            "perPage=$perPage adapterPages=${pageAdapter.getPageCount()} " +
                            "firstPid=${posts.firstOrNull()?.pid}"
                    )
                }
            }
        }
        val filter = android.content.IntentFilter("com.example.s1client.PROBE_THREAD")
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, android.content.Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(receiver, filter)
        }
    }

    /**
     * 进页面时拉一次真实收藏状态来点亮星星。
     *
     * 原来只依赖 Intent 的 `favorited` extra，但全项目没有一处 putExtra 传它，
     * 初始值恒为 false → 已收藏的帖也显示空心星。查询失败时保持原状（不误显示未收藏）。
     */
    internal fun refreshFavoriteState() {
        if (!ForumDataCache.isLoggedIn()) return
        lifecycleScope.launch {
            val fav = com.example.s1client.data.store.FavoriteManager.isFavorited(tid) ?: return@launch
            if (fav != isFavorited) {
                isFavorited = fav
                updateFavoriteButton()
            }
        }
    }

    /**
     * 滚动到指定楼层。**由 onPageSelected 触发**（彼时当前页列表已 attach）。
     * 找不到该 pid（比如帖子被删、或被已读楼层过滤掉）就静默放弃 ——
     * 停在正确的那一页本身就是主要收益，不必再弹提示打扰用户。
     */
    private fun tryScrollToPendingPid() {
        val pid = pendingScrollPid ?: return
        val rv = attachedRv ?: return
        // 从 PageAdapter 持有的当前页数据里反查楼层下标
        val posts = pageAdapter.getPage(currentPage) ?: return
        val target = posts.indexOfFirst { it.pid == pid }
        if (target >= 0) {
            pendingScrollPid = null
            rv.post { rv.smoothScrollToPosition(target) }
        }
    }

    internal fun setupPageAdapter() {
        pageAdapter = PageAdapter(
            onPageScroll = { position, rv, dy ->
                // 底部栏常驻，不再随滚动显隐；这里只记住当前页的列表用于回帖后定位
                if (position == currentPage - 1) {
                    attachedRv = rv
                    attachEndDetector(rv)
                }
            },
            subject = subject,
            tid = tid,
            onPostAvatarClick = { post -> showUserCard(post) },
            onPostEdit = { post -> openEditor(post) },
            onPostRateClick = { post -> showRateDialog(post) },
            onPostQuoteClick = { post ->
                quotedPost = post
                showReplySheet()
            },
            onPostFavoriteClick = { post -> togglePostBookmark(post) },
            onPostMoreClick = { post -> showPostMenu(post) },
            rateProvider = { pid -> pid?.let { rateMap[it] } },
            onPostRatingRowClick = { post -> showRatingDetail(post) },
            powerProvider = { uid -> com.example.s1client.data.store.PowerRepository.get(uid) },
            onPostQuoteJump = { floor -> floor?.toIntOrNull()?.let { goToFloor(it) } },
            bookmarkProvider = { pid -> com.example.s1client.data.store.PostBookmarkManager.isBookmarked(pid) },
            onPageMissing = { page -> onPageMissing(page) }
        )
        // 多选分享：把选中判定与切换回调注入 PageAdapter，供各页 PostAdapter 取用
        pageAdapter.shareIsSelected = { p -> p.pid != null && shareSelected.contains(p.pid) }
        pageAdapter.shareOnToggle = { p -> toggleShareFloor(p) }
        binding.vpPages.adapter = pageAdapter
    }

    // ============ 整帖隐藏（1楼菜单「隐藏该帖」）+ 本帖屏蔽实时刷新 ============
    internal fun setupHideAndMute() {
        binding.tvThreadTitle.text = subject
        binding.tvMuteBanner.setOnClickListener { showMuteManageDialog() }

        // 本帖屏蔽状态变化：刷新楼层折叠 + 提示条
        lifecycleScope.launch {
            ThreadMutedUsers.mutedFlow.collect {
                updateMuteBanner()
                pageAdapter.notifyBlockStateChanged()
            }
        }
        // 全局拉黑变化：已打开的帖子也要即时折楼
        lifecycleScope.launch {
            BlockedAuthorManager.blockedFlow.collect {
                pageAdapter.notifyBlockStateChanged()
            }
        }
        // 单楼屏蔽变化：屏蔽/取消某一楼，即时重绑让该层折叠/展开
        lifecycleScope.launch {
            HiddenFloors.flow.collect {
                pageAdapter.notifyBlockStateChanged()
            }
        }
        updateMuteBanner()
    }

    /** 长按标题隐藏整帖：二次确认后隐藏并退出详情页（列表已通过 StateFlow 即时移除） */
    internal fun showHideThreadDialog() {
        val titleHint = subject.takeIf { it.isNotBlank() } ?: "该帖"
        AlertDialog.Builder(this)
            .setTitle("隐藏该帖")
            .setMessage("确认隐藏「$titleHint」？\n隐藏后首页不再显示这条帖子，可在「我的 → 抹布贴」恢复。")
            .setPositiveButton("确认隐藏") { _, _ ->
                Haptic.heavy(this@ThreadDetailActivity)
                HiddenPostManager.hide(tid, subject, "", "")
                S1Notify.show(this, "已隐藏该帖", S1Notify.Type.SUCCESS)
                finish()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 本帖屏蔽提示条：有屏蔽则显示「本帖已屏蔽 N 人 · 点击管理」 */
    internal fun updateMuteBanner() {
        val muted = ThreadMutedUsers.mutedInThread(tid)
        if (muted.isEmpty()) {
            binding.tvMuteBanner.visibility = View.GONE
            return
        }
        binding.tvMuteBanner.visibility = View.VISIBLE
        binding.tvMuteBanner.text = "本帖已屏蔽 ${muted.size} 人 · 点击管理"
    }

    /** 管理本帖屏蔽的人：逐条恢复 / 全部恢复 */
    internal fun showMuteManageDialog() {
        val muted = ThreadMutedUsers.mutedInThread(tid).toList()
        if (muted.isEmpty()) { updateMuteBanner(); return }
        val names = muted.map { (_, n) -> n.ifBlank { "未知" } }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("本帖已屏蔽（${muted.size}）")
            .setItems(names) { _, which ->
                val (uid, _) = muted[which]
                ThreadMutedUsers.unmute(tid, uid)
                S1Notify.show(this, "已恢复该人回复", S1Notify.Type.SUCCESS)
                updateMuteBanner()
                pageAdapter.notifyBlockStateChanged()
            }
            .setPositiveButton("全部恢复") { _, _ ->
                ThreadMutedUsers.unmuteAll(tid)
                S1Notify.show(this, "已恢复全部", S1Notify.Type.SUCCESS)
                updateMuteBanner()
                pageAdapter.notifyBlockStateChanged()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ============ 顶栏的楼主信息（头像 + 昵称）============
    internal fun bindTopAuthor(post: Post?) {
        val name = post?.author ?: return
        binding.tvTopAuthor.text = name
        binding.tvTopAvatarLetter.text = name.firstOrNull()?.toString() ?: "?"
        binding.tvTopAvatarLetter.background =
            com.example.s1client.ui.PostAdapter.makeCircleGradient(name.hashCode())
        binding.ivTopAvatar.alpha = 0f
        post.authorId?.let { com.example.s1client.ui.PostAdapter.buildAvatarUrl(it) }?.let { url ->
            binding.ivTopAvatar.load(url) {
                listener(
                    onSuccess = { _, _ -> binding.ivTopAvatar.alpha = 1f },
                    onError = { _, _ -> binding.ivTopAvatar.alpha = 0f }
                )
            }
        }
        if (!post.authorId.isNullOrBlank()) {
            val open = View.OnClickListener { showUserCard(post) }
            binding.topAvatarArea.setOnClickListener(open)
            binding.tvTopAuthor.setOnClickListener(open)
        }
    }

    // ============ 用户卡片（与首页头像点击共用同一套界面）============
    internal fun showUserCard(post: Post) {
        val uid = post.authorId
        if (uid.isNullOrBlank()) { toast("无法获取用户信息"); return }
        com.example.s1client.ui.showUserCardDialog(this, uid, post.author ?: "未知")
    }

    // ============ 楼层菜单（仅行尾「⋯」；长按已改复制/选区）============
    /** 菜单动作：图标 + 文字标签；danger=true 用红色（屏蔽/隐藏/举报类） */
    internal data class MenuAction(
        val label: String,
        val icon: Int,
        val danger: Boolean = false,
        val run: () -> Unit
    )

    internal fun toggleOnlyAuthor() {
        if (authorId.isNullOrEmpty()) { toast("正在加载楼主信息..."); loadFirstPage(); return }
        isOnlyAuthor = !isOnlyAuthor
        updateOnlyAuthorButton()
        resetAndReload()
    }

    internal fun updateOnlyAuthorButton() {
        if (isOnlyAuthor) {
            binding.btnOnlyAuthor.text = "全部楼层"
            binding.btnOnlyAuthor.isSelected = true
            val tv = TypedValue()
            theme.resolveAttribute(com.example.s1client.R.attr.s1TextOnAccent, tv, true)
            binding.btnOnlyAuthor.setTextColor(tv.data)
        } else {
            binding.btnOnlyAuthor.text = "只看楼主"
            binding.btnOnlyAuthor.isSelected = false
            val tv = TypedValue()
            theme.resolveAttribute(com.example.s1client.R.attr.s1TextSecondary, tv, true)
            binding.btnOnlyAuthor.setTextColor(tv.data)
        }
    }

    internal fun resetAndReload() {
        loadedMaxPage = 0
        hasMore = true
        currentPage = 1
        isLoading = false
        setupPageAdapter()
        updatePageIndicator()
        loadFirstPage()
    }

    // ============ 收藏按钮（星标）============
    /**
     * 收藏帖子的星标。
     *
     * **固定实心星，不随收藏状态变化**（用户拍板）。
     * 楼层收藏已改用书签图标，两者形状不同、含义一目了然；
     * 若这里再叠"实心/空心"状态切换，用户就得盯着图标猜状态，反而更费解。
     * 收藏与否由点击后的 toast 明确告知。
     *
     * 保留这个函数是因为 toggle 后仍要统一重绘一次（例如恢复 tint）。
     */
    internal fun updateFavoriteButton() {
        binding.btnFavorite.setImageResource(R.drawable.ic_star)
        val tv = TypedValue()
        theme.resolveAttribute(com.example.s1client.R.attr.s1Accent, tv, true)
        binding.btnFavorite.imageTintList =
            android.content.res.ColorStateList.valueOf(tv.data)
    }

    // ============ 分享 ============
    internal fun shareCurrentThread() {
        val title = subject.ifBlank { "Stage1st 帖子" }
        val url = "https://stage1st.com/2b/thread-$tid-1-1.html"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title\n$url")
        }
        try {
            startActivity(Intent.createChooser(intent, "分享帖子"))
        } catch (e: Exception) {
            toast("无法分享")
        }
    }

    // ============ 多选分享（抄 s1er：进入多选模式 → 点楼层勾选 → 底部栏分享） ============
    /** 从某楼菜单进入多选模式：预选当前楼，显示底部分享条、隐藏原回复条 */
    internal fun loadFirstPage() {
        isLoading = true
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val posts = fetchOnePage(1)
                // fetchOnePage 刚算出的总页数要在主线程同步进 UI（预留槽位 + 刷新页码）
                syncPageSocketsOnMain()
                if (posts.isEmpty()) { toast("加载失败", S1Notify.Type.ERROR); return@launch }
                hasMore = morePagesToLoad()

                if (authorId.isNullOrEmpty()) {
                    val opPost = posts.firstOrNull { it.first == "1" }
                    authorId = opPost?.authorId
                }
                // 楼主头像/昵称放顶栏（小红书式），楼层里不再重复作者行
                bindTopAuthor(posts.firstOrNull { it.first == "1" })

                val ratingsReady = preloadRatings(1)
                // 必须显式写页码：syncPageSocketsOnMain 已把 maxPage 推到总页数，
                // 用 addPage（写进 maxPage+1）会把首页内容写到错误的页码上
                pageAdapter.setPage(1, posts)
                loadedMaxPage = 1
                updatePageIndicator()
                if (ratingsReady) ratingFetchedPages.add(1)
                pageAdapter.getPage(1)?.let { loadPowers(it) }
                if (hasMore) preloadPage(2)
            } finally {
                binding.progressBar.visibility = View.GONE
                isLoading = false
            }
        }
    }

    internal fun preloadPage(page: Int) {
        if (page <= loadedMaxPage || isLoading || !morePagesToLoad()) return
        isLoading = true
        lifecycleScope.launch {
            try {
                val posts = fetchOnePage(page)
                syncPageSocketsOnMain()
                // 空响应不锁死 hasMore（可能只是这次网络抖动），下次滚动会自然重试
                if (posts.isEmpty()) return@launch
                hasMore = morePagesToLoad()
                val ratingsReady = preloadRatings(page)
                // 必须用 setPage(page, ...) 而不是 addPage()：
                // addPage 是「写进 maxPage+1」，而 reserveUpTo 之后 maxPage 已经等于总页数，
                // 用 addPage 会把这一页的内容写到错误的页码上。
                pageAdapter.setPage(page, posts)
                loadedMaxPage = page
                if (ratingsReady) ratingFetchedPages.add(page)
                pageAdapter.getPage(page)?.let { loadPowers(it) }
            } finally {
                isLoading = false
            }
        }
    }

    internal fun gotoPage(page: Int) {
        if (page < 1) { toast("已经是第一页"); return }
        if (page == currentPage) return
        if (pageAdapter.hasPage(page)) {
            // 相邻页用平滑滚动，跳远页直接切（否则会掠过空白页造成闪烁）
            val smooth = kotlin.math.abs(page - currentPage) == 1
            binding.vpPages.setCurrentItem(page - 1, smooth)
            return
        }
        // 边界按「帖子总页数」判断，不能用 hasMore：hasMore 会在跳到最后一页
        // （楼层不满 40）时被置 false，导致之后往回跳也被当成"最后一页"而拒绝
        if (totalPages > 0 && page > totalPages) { toast("没有第 $page 页"); return }
        if (isLoading) { toast("正在加载，请稍后"); return }

        // 只请求目标页本身，不再把中间的页逐页拉一遍
        isLoading = true
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val posts = fetchOnePage(page)
                syncPageSocketsOnMain()
                if (posts.isEmpty()) { toast("加载失败", S1Notify.Type.ERROR); return@launch }
                hasMore = morePagesToLoad()
                val ratingsReady = preloadRatings(page)
                pageAdapter.setPage(page, posts)
                if (page > loadedMaxPage) loadedMaxPage = page
                if (ratingsReady) ratingFetchedPages.add(page)
                pageAdapter.getPage(page)?.let { loadPowers(it) }
                // 关键：跳远页不要用平滑滚动，否则会高速掠过中间没加载的空白页（视觉上就是连闪几下）
                binding.vpPages.setCurrentItem(page - 1, false)
            } finally {
                binding.progressBar.visibility = View.GONE
                isLoading = false
            }
        }
    }

    // ============ 编辑自己的帖子 ============
    internal var needReloadAfterEdit = false

        // ============ 细节 22：音量键翻页（阅读模式，前台接管退出即恢复） ============
        override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        when (keyCode) {
            android.view.KeyEvent.KEYCODE_VOLUME_UP -> {
                if (binding.vpPages.currentItem > 0) {
                    binding.vpPages.currentItem -= 1
                    Haptic.selection(this)
                }
                return true
            }
            android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val max = (binding.vpPages.adapter?.itemCount ?: 1) - 1
                if (binding.vpPages.currentItem < max) {
                    binding.vpPages.currentItem += 1
                    Haptic.selection(this)
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        binding.progressBar.refreshThemeColors()
        // 从编辑页回来要刷新，才能看到改后的内容
        if (needReloadAfterEdit) {
            needReloadAfterEdit = false
            resetAndReload()
        }
    }

        override fun onBackPressed() {
            if (shareSelectMode) {
                exitShareSelectMode()
                return
            }
            super.onBackPressed()
        }

    internal fun checkEndFooter(r: RecyclerView) {
        if (totalPages <= 0 || currentPage < totalPages) { hideEndFooter(); return }
        val lm = r.layoutManager as? LinearLayoutManager ?: return
        if (lm.findLastVisibleItemPosition() >= (r.adapter?.itemCount ?: 0) - 1) {
            showEndFooter()
        }
    }

    /** 给当前页 RecyclerView 挂滚动监听：末页滚到底 → 浮出「已经到底」 */
    internal fun attachEndDetector(rv: RecyclerView?) {
        rv ?: return
        if (endDetectorAttached[rv] == true) return
        endDetectorAttached[rv] = true
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(r: RecyclerView, dx: Int, dy: Int) = checkEndFooter(r)
        })
        // 末页内容一屏装得下、无需滚动时也能显示
        rv.post { checkEndFooter(rv) }
    }

    internal fun showEndFooter() {
        binding.tvEndFooter.visibility = View.VISIBLE
        binding.tvEndFooter.alpha = 0f
        binding.tvEndFooter.animate().alpha(1f).setDuration(200).start()
    }

    internal fun hideEndFooter() {
        if (binding.tvEndFooter.visibility == View.VISIBLE) {
            binding.tvEndFooter.animate().alpha(0f).setDuration(200)
                .withEndAction { binding.tvEndFooter.visibility = View.GONE }.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
