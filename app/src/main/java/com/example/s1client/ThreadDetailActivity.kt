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
import com.example.s1client.data.api.PollVoteRepository
import com.example.s1client.ui.PollCard
import androidx.recyclerview.widget.RecyclerView
import com.example.s1client.data.store.ReadingProgressStore
import com.example.s1client.util.PostLocator
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

    /** 投票数据（special==1 的投票帖才有）；合并本地已投记录后渲染 */
    private var threadPoll: com.example.s1client.data.model.ThreadPoll? = null

    /**
     * 第 1 页响应里顺带带回来的投票原始值（fetchOnePage 在 IO 线程写，主线程读并 apply）。
     * 见 [applyCapturedPoll]：不用再为投票单独发一次请求。
     */
    @Volatile internal var capturedPollSpecial: String? = null
    @Volatile internal var capturedPollJson: com.google.gson.JsonObject? = null

    /** 本地阅读进度：进来时恢复、滚动时节流保存（点击帖子回到之前的进度） */
    private var lastProgressSaveAt = 0L

    /**
     * 本地阅读进度读出来的**绝对楼层**（1-based，跨页累计）。
     *
     * 口径照 s1er 的 ReadingRecord：权威字段是楼层而不是页码 ——
     * 服务端的每页帖数(ppp)会变，只存页码的话换页数后落点会漂。
     * 打开后由 [tryScrollToTarget] 消费，用完清零。
     */
    private var resumeFloor: Int = 0

    /** 从本地进度里读出来的楼层 pid（楼层算不出下标时的兜底，用完即清） */
    private var resumeFloorPid: String? = null

    /** 待滚动定位的楼层 id；定位成功后立即清空，避免重复执行 */
    private var pendingScrollPid: String? = null

    /** 上次阅读时服务端自报的每页帖数（ppp）。进页面时还没拉到数据，
     *  先用它把"绝对楼层"换算成页，拿到真实 ppp 后再纠正一次。 */
    private var recordedPerPage: Int = 0

    /** 定位"滚 + 校验"的循环：见 [tryScrollToTarget] 里的说明 */
    private var scrollVerifyJob: kotlinx.coroutines.Job? = null

    /**
     * 首屏落定前**禁止写进度**。
     *
     * 必须有的闸门：ViewPager2 初始化时会先回调一次 `onPageSelected(0)`，
     * 那一刻如果照常保存，就会把"第 1 页第 1 楼"写进库 ——
     * 用户辛苦翻到第 8 页的进度会被自己的一次打开动作覆盖掉。
     */
    private var progressReady: Boolean = false
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
    /** 正在补拉的页码（按页去重，替代原来的 isLoading 闸门 —— 见 onPageMissing） */
    internal val missingPages = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()

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
        // 从提醒跳进来时带的楼层 id。**页号不在这里解析**：
        // 照 s1er，先在详情页里拿到 pid，再异步 locatePostPage → 直接拉那一页，
        // 这样不会有"先闪一下第 1 页再跳"的过程，失败也能明确报错。
        pendingScrollPid = intent.getStringExtra(EXTRA_PID)?.takeIf { it.isNotBlank() }
        // 阅读进度（点击帖子回到之前的进度）：读绝对楼层 + 当时的每页帖数
        if (pendingScrollPid == null) {
            ReadingProgressStore.get(this).load(tid)?.let { p ->
                resumeFloor = p.floor
                resumeFloorPid = p.floorPid
                recordedPerPage = p.perPage
                android.util.Log.d(
                    "S1Progress",
                    "load tid=$tid floor=${p.floor} page=${p.page} ppp=${p.perPage} pid=${p.floorPid}"
                )
            }
        }

        binding.btnBack.setOnClickListener { handleBack() }
        if (tid.isEmpty()) { finish(); return }

        binding.btnShareCancel.setOnClickListener { exitShareSelectMode() }
        binding.btnShareSelected.setOnClickListener { shareSelectedFloors() }

        // ===== 用 ViewPager2 内置手势处理水平翻页 =====
        binding.vpPages.isUserInputEnabled = true

        setupPageAdapter()
        setupHideAndMute()
        // v3.1：板块名按需加载（ForumDataCache.forumList 全 App 无赋值点，见 ensureForumTreeLoaded）
        ensureForumTreeLoaded()

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
                // 原型规格 7：跳转落点高亮（目标页首楼卡闪烁 1.6s 渐隐）
                if (pendingFlashPage == currentPage) {
                    pendingFlashPage = -1
                    binding.vpPages.post {
                        val innerRv = (0 until binding.vpPages.childCount)
                            .map { binding.vpPages.getChildAt(it) }
                            .filterIsInstance<RecyclerView>()
                            .firstOrNull()
                        val vh = innerRv?.findViewHolderForAdapterPosition(position)
                            as? PageAdapter.PageVH
                        val rv = vh?.itemView as? RecyclerView
                        val card = rv?.getChildAt(0)
                        if (card != null) flashCardView(card)
                    }
                }
                // 实验功能「转盘翻页」：每翻一档给一次 iPod 式 tick 触觉（默认关）
                if (com.example.s1client.data.store.SettingStore.dialPaging(this@ThreadDetailActivity)
                    && position > 0
                ) {
                    runCatching {
                        val v = getSystemService(android.content.Context.VIBRATOR_SERVICE)
                            as? android.os.Vibrator
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            v?.vibrate(android.os.VibrationEffect.createPredefined(
                                android.os.VibrationEffect.EFFECT_TICK))
                        } else @Suppress("DEPRECATION") v?.vibrate(12)
                    }
                }
                tryScrollToTarget()
                // 翻页本身就是"读到哪了"，必须立刻记一笔 ——
                // 只靠竖向滚动的 onScrolled 存进度的话，用户左右翻页后直接返回
                // 是**一次都没保存**，表现就是"阅读进度没实现"。
                saveProgressFromPage(currentPage)
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
     * 把当前页滚到目标位置 —— 两种目标二选一：
     *   · [pendingScrollPid]  —— 提醒跳转带来的 pid，**找不到要明确报错**（s1er 会提示
     *     "未找到目标楼层"；静默停在第 1 页就是用户说的"点了没反应"）
     *   · [resumeFloor]       —— 本地阅读进度里的绝对楼层（尽力而为，失败不打扰）
     *
     * 触发点：onPageSelected / setPage 之后 / 换页布局完成后（post + postDelayed）
     * 以及每次 onScrolled —— 换页后列表 attach 的时机不固定，靠时间点赌不可靠，
     * 挂到滚动回调上才是确定性的。
     */
    private fun tryScrollToTarget(finalAttempt: Boolean = false) {
        if (pendingScrollPid == null && resumeFloor <= 0) return
        // 正在"滚 + 校验"：别再插一轮，否则 onScrolled 会形成自激
        if (scrollVerifyJob?.isActive == true) return
        val posts = pageAdapter.getPage(currentPage) ?: return
        if (posts.isEmpty()) return

        val pid = pendingScrollPid
        val postIndex: Int
        if (pid != null) {
            val idx = posts.indexOfFirst { it.pid == pid }
            if (idx < 0) {
                // 页码可能不对（帖被删楼 / ppp 变化），最后一次仍找不到就如实告诉用户
                if (finalAttempt) {
                    pendingScrollPid = null
                    S1Notify.show(
                        this,
                        "未找到该楼层，已停在定位到的第 $currentPage 页",
                        S1Notify.Type.ERROR
                    )
                }
                return
            }
            postIndex = idx
        } else {
            val ppp = if (perPage > 0) perPage else PAGE_SIZE
            val base = (currentPage - 1) * ppp + 1          // 本页第一楼的绝对楼层
            postIndex = (resumeFloor - base).coerceIn(0, posts.size - 1)
        }

        val listIndex = pageAdapter.listIndexFromPostIndex(currentPage, postIndex)

        // ⚠️ 必须用 PageAdapter 按**页**记住的那个 RecyclerView。
        // 原来退化成 `binding.vpPages.getChildAt(0) as? RecyclerView` —— 那是
        // **ViewPager2 自己的内部 RecyclerView**（不是某一页的楼层列表），
        // 于是"定位成功、日志也打了 scroll、屏幕上却没动"。
        if (!pageAdapter.scrollToPositionInPage(currentPage, listIndex)) {
            return   // 该页还没绑好：onScrolled / post / postDelayed 三个钩子会再调进来
        }

        // —— 滚一次不够，要"滚 + 校验" ——
        // 紧随其后到货的 战斗力 / 评分 / 页码 等数据会触发 notifyItemChanged /
        // notifyDataSetChanged，把 RecyclerView 的锚点又 reset 回顶部 ——
        // 表现就是"日志说滚了、屏幕上还在开头"。所以这里重复几次，
        // 直到确认目标行真的停在视口顶端才收工。
        if (scrollVerifyJob?.isActive == true) return
        scrollVerifyJob = lifecycleScope.launch {
            repeat(6) { i ->
                kotlinx.coroutines.delay(if (i == 0) 260L else 320L)
                val rv = pageAdapter.rvForPage(currentPage) ?: return@launch
                val first = (rv.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager)
                    ?.findFirstVisibleItemPosition() ?: -1
                if (first == listIndex) {
                    android.util.Log.d(
                        "S1Locate",
                        "scroll ok page=$currentPage pid=$pid floor=$resumeFloor index=$listIndex " +
                            "after ${i + 1} checks"
                    )
                    pendingScrollPid = null
                    resumeFloor = 0
                    resumeFloorPid = null
                    return@launch
                }
                android.util.Log.d("S1Locate", "scroll retry#$i first=$first target=$listIndex")
                pageAdapter.scrollToPositionInPage(currentPage, listIndex)
            }
            // 反复滚不上也要把标志清掉，否则每次 onScrolled 都会再试一遍
            android.util.Log.w("S1Locate", "scroll give up page=$currentPage target=$listIndex")
            pendingScrollPid = null
            resumeFloor = 0
            resumeFloorPid = null
        }
    }

    /**
     * 投票帖解析。判定与字段照搬 s1er 的 `parsePoll`：
     * `thread.special == 1` 且 `Variables.special_poll` 存在才是投票帖。
     * 再把本地记住的已投选项合进去（API 不返回这个字段）。
     */
    /**
     * 把第 1 页响应里顺带回来的投票数据挂到列表上（主线程）。
     *
     * 判定照 s1er 的 parsePoll：`thread.special == 1` 且 `Variables.special_poll` 存在
     * 才是投票帖；再把本地记住的已投选项合并进去（API 不返回这个字段）。
     */
    private fun applyCapturedPoll() {
        val special = capturedPollSpecial
        val pollJson = capturedPollJson
        if (special == null && pollJson == null) return
        capturedPollSpecial = null
        capturedPollJson = null
        android.util.Log.d(
            "S1Poll",
            "apply tid=$tid special=$special hasPoll=${pollJson != null} keys=${pollJson?.keySet()}"
        )
        if (special?.trim() == "1" && pollJson != null) {
            val parsed = com.example.s1client.data.model.ThreadPoll
                .fromGson(pollJson)
                .withUserVotes(PollVoteRepository.remembered(tid))
            if (parsed.options.isNotEmpty()) {
                threadPoll = parsed
                // 投票卡是**楼层列表里的一行**（第 1 页主楼之后），不再塞顶层卡槽
                pageAdapter.setPoll(parsed)
            }
        }
    }

    /** 投票成功后重新拉一次（票数/百分比要更新） */
    private fun loadPollIfAny() {
        lifecycleScope.launch {
            try {
                // 投票数据是整个帖子的属性，与页码无关，固定拉第 1 页的响应
                // （用户可能从阅读进度直接恢复到第 N 页，那时 tid 的 poll 仍在 Variables 里）
                val r = withContext(Dispatchers.IO) {
                    RetrofitClient.api.getThreadDetail(tid = tid, page = 1)
                }
                val special = r.variables?.thread?.special
                val pollJson = r.variables?.specialPoll
                android.util.Log.d(
                    "S1Poll",
                    "tid=$tid special=$special hasPoll=${pollJson != null} " +
                        "keys=${pollJson?.keySet()}"
                )
                if (special?.trim() == "1" && pollJson != null) {
                    val parsed = com.example.s1client.data.model.ThreadPoll
                        .fromGson(pollJson)
                        .withUserVotes(PollVoteRepository.remembered(tid))
                    if (parsed.options.isNotEmpty()) {
                        threadPoll = parsed
                        // 投票卡是**楼层列表里的一行**（第 1 页主楼之后），不再塞顶层卡槽
                        pageAdapter.setPoll(parsed)
                    }
                }
            } catch (_: Exception) {
                // 投票数据缺失不影响看帖，静默跳过
            }
        }
    }

    /**
     * 提交投票。返回 null = 成功（PollCard 会把结果态切出来）。
     *
     * 成功后照 s1er 刷新一次投票数据（票数/百分比要更新），并把选择记进本地缓存 ——
     * Discuz 的 API **不返回"我投了哪项"**，只能本地记，否则下次进来又变成未投票。
     */
    private suspend fun submitPollVote(ids: List<String>): String? {
        val err = PollVoteRepository.votePoll(tid, ids)
        if (err == null) {
            S1Notify.show(this, "投票成功", S1Notify.Type.SUCCESS)
            loadPollIfAny()
        }
        return err
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
            onPageMissing = { page -> onPageMissing(page) },
            sectionProvider = { resolveSectionName() },
            onPollVote = { ids -> submitPollVote(ids) }
        )
        // 多选分享：把选中判定与切换回调注入 PageAdapter，供各页 PostAdapter 取用
        pageAdapter.shareIsSelected = { p -> p.pid != null && shareSelected.contains(p.pid) }
        pageAdapter.shareOnToggle = { p -> toggleShareFloor(p) }
        binding.vpPages.adapter = pageAdapter
    }

    // ============ 整帖隐藏（1楼菜单「隐藏该帖」）+ 本帖屏蔽实时刷新 ============
    internal fun setupHideAndMute() {
        // v3.1 第八轮：顶栏标题与主楼显示屏里的标题重复且截断不全，删除（仅保留作者名）
        binding.tvThreadTitle.visibility = View.GONE
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

    // ============ 板块名（主楼品牌行右侧标签）：fid → ForumDataCache 递归解析 ============
    /** fetchOnePage 会先把 thread.fid 写进 threadFid，之后每次绑定主楼都会现算一次 */
    internal fun resolveSectionName(): String {
        if (threadFid.isBlank()) return ""
        fun find(list: List<com.example.s1client.data.model.SubForum>): String? =
            list.firstOrNull { it.fid == threadFid }?.name
                ?: list.firstNotNullOfOrNull { f -> f.subList?.let { find(it) } }
        val name = find(ForumDataCache.forumList)?.takeIf { it.isNotBlank() }
        // 正常命中不打日志；解析不到才记一条，方便日后排查（缓存未加载 / fid 不在版面树里）
        if (name == null) {
            android.util.Log.d(
                "S1Section",
                "fid=$threadFid 未命中 缓存板块数=${ForumDataCache.forumList.size}"
            )
        }
        return name ?: ""
    }

    /**
     * v3.1：ForumDataCache.forumList 全 App 没有任何赋值点（首页只喂
     * PostViewModel 的 tab 流），详情页解析板块名恒不命中。
     * 这里按需拉一次 forumindex 喂进全局缓存，回来后重绑第 1 页主楼品牌行。
     * 与 fetchOnePage 的竞态无妨：先到的一方各自走 resolveSectionName / refreshSectionTag。
     */
    internal fun ensureForumTreeLoaded() {
        if (ForumDataCache.forumList.isNotEmpty()) return
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.getForumIndex()
                val raw = resp.variables?.forumList.orEmpty()
                if (raw.isNotEmpty()) {
                    ForumDataCache.forumList = raw
                    android.util.Log.d("S1Section", "forumindex 按需加载：顶层 ${raw.size} 组")
                    pageAdapter.refreshSectionTag()
                }
            } catch (e: Exception) {
                android.util.Log.w("S1Section", "forumindex 加载失败: ${e.message}")
            }
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
        // 声明在 try 外：finally 里要用它去释放"首屏页占位"
        var startPage = 0
        lifecycleScope.launch {
            try {
                // —— 起始页的判定顺序（照 s1er 的 resolveThreadInitialPage）——
                //   ① intent 显式指定 page
                //   ② 带 pid（提醒跳转）→ 先 locatePostPage 定位，失败明确报错
                //   ③ 本地阅读进度 → 由"绝对楼层 + 上次的每页帖数"推页
                //   ④ 都没有 → 第 1 页
                startPage = intent.getIntExtra("page", 0).takeIf { it >= 1 } ?: 0

                if (startPage == 0 && pendingScrollPid != null) {
                    val pid = pendingScrollPid!!
                    val p = PostLocator.locatePostPage(tid, pid)
                    android.util.Log.d("S1Locate", "open tid=$tid pid=$pid page=$p")
                    if (p == null) {
                        // 不静默回落成第 1 页 —— 用户需要知道"没定位到"
                        pendingScrollPid = null
                        toast("定位楼层失败，已回到第 1 页", S1Notify.Type.ERROR)
                    }
                    startPage = p ?: 1
                }
                android.util.Log.d(
                    "S1Progress",
                    "start tid=$tid explicit=${intent.getIntExtra("page", 0)} " +
                        "pid=$pendingScrollPid resumeFloor=$resumeFloor recPpp=$recordedPerPage"
                )
                if (startPage == 0) {
                    startPage = if (resumeFloor > 1) {
                        val ppp = if (recordedPerPage > 0) recordedPerPage else PAGE_SIZE
                        ((resumeFloor - 1) / ppp) + 1
                    } else 1
                }

                // 把首屏这一页也占住：ViewPager2 首次绑定会走"占位分支"并回调
                // onPageMissing(startPage)，不去重的话会并发再拉一次同一页，
                // 它返回后的 setPage → notifyItemChanged → 重绑，会把刚做好的定位滚动覆盖掉。
                missingPages.add(startPage)
                var posts = fetchOnePage(startPage)
                syncPageSocketsOnMain()
                // ⚠️ 上一版我重写本函数时把投票的首次加载漏掉了，
                // 结果是"投票卡完全不显示"（只有投过票才会去拉一次）。
                // 现在改从第 1 页的响应里顺带取，零额外请求。
                applyCapturedPoll()
                if (posts.isEmpty()) { toast("加载失败", S1Notify.Type.ERROR); return@launch }

                // —— 拿到真实 ppp 后纠正一次页码 ——
                // 上次的每页帖数可能和服务端现在的不一样，用旧值算出的页会偏。
                // s1er 也是在第一页返回后按 live ppp 重新校正目标页。
                if (resumeFloor > 1 && pendingScrollPid == null && perPage > 0) {
                    val want = ((resumeFloor - 1) / perPage) + 1
                    if (want != startPage && want <= totalPages.coerceAtLeast(1)) {
                        val p2 = fetchOnePage(want)
                        syncPageSocketsOnMain()
                        if (p2.isNotEmpty()) { posts = p2; startPage = want }
                    }
                }

                hasMore = morePagesToLoad()
                if (authorId.isNullOrEmpty()) authorId = posts.firstOrNull { it.first == "1" }?.authorId
                bindTopAuthor(posts.firstOrNull { it.first == "1" })

                val ratingsReady = preloadRatings(startPage)
                // 必须显式写页码：syncPageSocketsOnMain 已把 maxPage 推到总页数，
                // 用 addPage（写进 maxPage+1）会把内容写到错误的页码上
                pageAdapter.setPage(startPage, posts)
                loadedMaxPage = startPage
                currentPage = startPage
                progressReady = true
                // ⚠️ ViewPager2 的 currentItem 默认是 0（第 1 页）。不手动切过去的话
                // 它显示的是第 1 页空槽 → onPageMissing(1) 又把第 1 页拉一遍，
                // 表现就是"跳了页但停在开头"。
                if (startPage > 1) binding.vpPages.setCurrentItem(startPage - 1, false)

                tryScrollToTarget()
                binding.vpPages.post { tryScrollToTarget() }
                binding.vpPages.postDelayed({ tryScrollToTarget(finalAttempt = true) }, 600)

                updatePageIndicator()
                // 从阅读进度恢复到第 N 页时没有第 1 页的响应，兜底单独拉一次投票
                if (startPage > 1) loadPollIfAny()
                if (ratingsReady) ratingFetchedPages.add(startPage)
                pageAdapter.getPage(startPage)?.let { loadPowers(it) }
                if (hasMore) preloadPage(startPage + 1)
            } finally {
                missingPages.remove(startPage)
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
                // 预加载到的正是目标页时，立刻定位（用户还没翻过来）
                tryScrollToTarget()
                if (ratingsReady) ratingFetchedPages.add(page)
                pageAdapter.getPage(page)?.let { loadPowers(it) }
            } finally {
                isLoading = false
            }
        }
    }

    /** 跳转落点高亮：目标卡叠一层 accent 圆角 overlay，1.6s 渐隐后移除 */
    internal fun flashCardView(card: View) {
        val gd = android.graphics.drawable.GradientDrawable().apply {
            setColor(0x405E6AD2)
            cornerRadius = 12f * resources.displayMetrics.density
        }
        card.overlay.add(gd)
        card.postInvalidate()
        val anim = android.animation.ValueAnimator.ofInt(0x40, 0x00).apply {
            duration = 1600
            startDelay = 120
            addUpdateListener {
                val a = it.animatedValue as Int
                gd.setColor((a shl 24) or 0x5E6AD2)
                card.overlay.clear()
                card.overlay.add(gd)
                card.postInvalidate()
            }
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationEnd(a: android.animation.Animator) { card.overlay.clear() }
                override fun onAnimationStart(a: android.animation.Animator) {}
                override fun onAnimationCancel(a: android.animation.Animator) { card.overlay.clear() }
                override fun onAnimationRepeat(a: android.animation.Animator) {}
            })
        }
        anim.start()
    }

    /** 跳转落点高亮的目标页（1 基），-1 = 无 */
    internal var pendingFlashPage: Int = -1

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

    override fun onPause() {
        super.onPause()
        // 离开前落一次进度（照 s1er 的 `_flushProgressBeforeLeave`）。
        // 只靠滚动节流保存是不够的：用户翻完页直接返回时，节流窗口可能还没到。
        if (tid.isNotBlank()) saveProgressFromViewport()
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
            override fun onScrolled(r: RecyclerView, dx: Int, dy: Int) {
                // 列表 attach 的时机不固定，把"补定位"挂在滚动回调上是确定性的
                // （比纯靠 postDelayed 赌时间靠谱）
                tryScrollToTarget()
                checkEndFooter(r)
                saveProgressThrottled(r)
            }
        })
        // 末页内容一屏装得下、无需滚动时也能显示
        rv.post { checkEndFooter(rv) }
    }

    /**
     * 节流保存阅读进度（滚动时调用）。2 秒节流 —— 滚动监听每帧都回调，
     * 不节流会把 SQLite 写爆。
     */
    private fun saveProgressThrottled(rv: RecyclerView) {
        val now = System.currentTimeMillis()
        if (now - lastProgressSaveAt < 2000) return
        lastProgressSaveAt = now
        saveProgressFromViewport()
    }

    /**
     * 按**当前视口**保存：绝对楼层 = (页-1)*每页帖数 + 视口第一楼在本页的下标 + 1。
     * 口径照 s1er 的 ReadingRecord —— 权威字段是楼层而不是页码。
     */
    private fun saveProgressFromViewport() {
        val rv = attachedRv ?: return
        val lm = rv.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager ?: return
        val firstListIndex = lm.findFirstVisibleItemPosition()
        if (firstListIndex < 0) return
        // 列表下标 → 楼层下标（第 1 页的投票卡占了一行）
        val postIndex = pageAdapter.postIndexFromListIndex(currentPage, firstListIndex)
        saveProgress(currentPage, postIndex)
    }

    /**
     * 只按页码保存（翻页时调用）。
     * 这一刻新页的列表还没 layout，拿不到"视口第一楼"，就记成这一页的第一楼 ——
     * 比"什么都不存"强得多（用户左右翻页后直接返回时的唯一一次记录）。
     */
    private fun saveProgressFromPage(page: Int) {
        saveProgress(page, 0)
    }

    private fun saveProgress(page: Int, postIndexInPage: Int) {
        if (!progressReady) return
        if (tid.isBlank() || page < 1) return
        val ppp = if (perPage > 0) perPage else PAGE_SIZE
        val floor = (page - 1) * ppp + postIndexInPage + 1
        val pid = pageAdapter.getPage(page)?.getOrNull(postIndexInPage)?.pid
        android.util.Log.d("S1Progress", "save tid=$tid page=$page floor=$floor ppp=$ppp pid=$pid")
        ReadingProgressStore.get(this).save(
            tid = tid,
            page = page,
            floor = floor,
            perPage = ppp,
            floorPid = pid,
            scrollOffset = attachedRv?.computeVerticalScrollOffset() ?: 0
        )
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
