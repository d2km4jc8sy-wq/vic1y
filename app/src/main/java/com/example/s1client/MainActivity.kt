package com.example.s1client

import android.content.Intent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.viewinterop.AndroidView
import com.example.s1client.ui.RopeRefreshView
import com.example.s1client.data.store.BlacklistManager
import com.example.s1client.util.Haptic
import com.example.s1client.util.S1Notify
import com.example.s1client.data.store.BlockedAuthorManager
import android.app.Activity
import com.example.s1client.ui.pressScale
import com.example.s1client.ui.theme.LocalS1Colors
import com.example.s1client.ui.theme.S1Theme
import com.example.s1client.util.ThemeManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.data.model.ProfileData
import com.example.s1client.data.store.FavoriteManager
import com.example.s1client.util.JsonParser
import com.example.s1client.util.avatarUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import android.util.Log
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.s1client.util.applyS1Theme
import com.example.s1client.util.setupS1EdgeToEdge
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import com.example.s1client.ui.design.AccentCyan
import com.example.s1client.ui.design.AccentPink
import com.example.s1client.ui.design.S1Radius
import com.example.s1client.ui.design.BgColor
import com.example.s1client.ui.design.BgColorBottom
import com.example.s1client.ui.design.CardBg
import com.example.s1client.ui.design.ChipBg
import com.example.s1client.ui.design.FlowCoverColors
import com.example.s1client.ui.design.GlassBar
import com.example.s1client.ui.design.GlassCard
import com.example.s1client.ui.design.GlassDialog
import com.example.s1client.ui.design.GlassDialogLine
import com.example.s1client.ui.design.GlassFloat
import com.example.s1client.ui.design.GlassFloatLine
import com.example.s1client.ui.design.GlassLine
import com.example.s1client.ui.design.HeartRed
import com.example.s1client.ui.design.OnAccent
import com.example.s1client.ui.design.OnSnackbar
import com.example.s1client.ui.design.SnackbarBg
import com.example.s1client.ui.design.StrongIcon
import com.example.s1client.ui.design.TextMain
import com.example.s1client.ui.design.TextSub


// 颜色统一走 Compose 主题调色板（ui/theme/S1Theme.kt），跟随「设置 → 选择主题」。
// 保留这些顶层名字是 @Composable 委托，调用点都在组合中；禁止再写死色值。

/** 顶部导航为浮层，内容按这个高度预留，避免隐藏时反复重排 */
internal val TopBarHeight = 50.dp

/** 轻点震动（系统段落感 EFFECT_TICK），用于翻页/反馈 */
internal fun tickHaptic(context: android.content.Context) {
    runCatching {
        val v = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            ?: return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            v.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_TICK))
        } else if (v.hasAmplitudeControl()) {
            v.vibrate(android.os.VibrationEffect.createOneShot(12, 160))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(12)
        }
    }
}

class MainActivity : ComponentActivity() {
    private val postViewModel: PostViewModel by viewModels()
    /** 每次回到首页 +1，触发未读提醒数重新拉取（驱动底栏「提醒」图标变状态） */
    private val homeResumeSignal = androidx.compose.runtime.mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        // 宪法 11：返回键二次确认退出，杜绝"无提示闪退"的错觉
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            private var lastBack = 0L
            override fun handleOnBackPressed() {
                val now = System.currentTimeMillis()
                if (now - lastBack < 2000) {
                    finish()
                } else {
                    lastBack = now
                    com.example.s1client.util.S1Notify.show(
                        this@MainActivity, "再按一次退出应用", com.example.s1client.util.S1Notify.Type.INFO
                    )
                }
            }
        })
        applyS1Theme()
        super.onCreate(savedInstanceState)
        // 首页底部是玻璃底栏：导航栏要填「玻璃底栏压在页面底上」的同色，
        // 否则浅色主题下导航栏区域会是一条与底栏不同的白带（MIUI 上必现）
        setupS1EdgeToEdge(hasGlassBottomBar = true)
        RetrofitClient.init(this)
        com.example.s1client.data.cache.ForumDataCache.init(this)
        setContent {
            S1Theme {
                XhsHomeScreen(postViewModel, homeResumeSignal)
            }
        }
        registerDebugThreadProbe()
    }

    /**
     * 仅 debug 构建：用一条广播在首页就能真实跑一遍「帖子详情页的数据加载」，
     * 把结果打进 logcat。
     *
     * 为什么需要：MIUI 禁止 `adb shell input tap/keyevent`（INJECT_EVENTS 被拒），
     * 既点不进帖子、也唤醒不了屏幕，截图验证走不通；而详情页 Activity 未导出，
     * `am start` 也起不来。有了这个探针，只要一条 `am broadcast` 就能验证
     * 「进帖子还会不会加载失败」——不必再靠猜。
     *
     * release 构建里直接返回，什么都不做。
     */
    private fun registerDebugThreadProbe() {
        if (!BuildConfig.DEBUG) return
        val probe = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                val tid = intent?.getStringExtra("tid").orEmpty()
                val page = intent?.getIntExtra("page", 1) ?: 1
                if (tid.isBlank()) return
                lifecycleScope.launch {
                    val t0 = System.currentTimeMillis()
                    val posts = withContext(Dispatchers.IO) {
                        try {
                            RetrofitClient.api.getThreadDetail(tid = tid, page = page)
                        } catch (e: Exception) {
                            Log.w("S1Probe", "probe request failed: ${e.javaClass.simpleName}: ${e.message}")
                            null
                        }
                    }
                    if (posts == null) {
                        Log.e("S1Probe", "probe tid=$tid page=$page FAILED (see above)")
                        return@launch
                    }
                    val v = posts.variables
                    val list = v?.postList
                    val replies = v?.thread?.replies?.toIntOrNull()
                    val ppp = v?.thread?.ppp?.toIntOrNull()
                    val total = com.example.s1client.util.Paging.totalPages(replies, ppp)
                    Log.i(
                        "S1Probe",
                        "probe OK tid=$tid page=$page 耗时=${System.currentTimeMillis() - t0}ms " +
                            "楼层数=${list?.size} replies=$replies ppp=$ppp 总页数=$total " +
                            "formhash=${if (v?.formhash.isNullOrBlank()) "空" else "有"}"
                    )
                }
            }
        }
        val filter = android.content.IntentFilter("com.example.s1client.PROBE_THREAD")
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(probe, filter, android.content.Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(probe, filter)
        }
    }

    override fun onResume() {
        super.onResume()
        homeResumeSignal.value++
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun XhsHomeScreen(viewModel: PostViewModel, resumeSignal: androidx.compose.runtime.State<Int>) {
    // 主题变化 → 系统栏同步（首页底部是玻璃底栏，所以传 true）。
    // 必须放在这里而不是主题选择器里：选择器是 BottomSheet，拿不到 Activity。
    com.example.s1client.ui.S1SystemBarSync(hasGlassBottomBar = true)
    val context = LocalContext.current
    val tabs by viewModel.tabs.collectAsState()
    val feeds by viewModel.feeds.collectAsState()
    val threadTypeMaps by viewModel.threadTypeMaps.collectAsState()
    val typeFilters by viewModel.typeFilters.collectAsState()
    val scope = rememberCoroutineScope()

    var showHammerMenu by remember { mutableStateOf(false) }
    /** 顶栏调色板图标 → 主题选择器（点选即生效，不用离开瀑布流） */
    var showForumManage by remember { mutableStateOf(false) }
    var showBlockedManager by remember { mutableStateOf(false) }
    /** 长按帖子 → Peek 预览面板（标题 + 正文摘要 + 收藏/分享/隐藏） */
    var peekPost by remember { mutableStateOf<Post?>(null) }

    /** 底部「撤销」条：破坏性操作（隐藏/塞抹布）先做再给 5 秒撤回窗口 */
    var undoBar by remember { mutableStateOf<UndoBarData?>(null) }
    /** 首页塞抹布二次确认的目标帖（作者） */
    val allForums by viewModel.allForums.collectAsState()
    val dataSaver by viewModel.dataSaver.collectAsState()
    val blockedAuthors by viewModel.blockedAuthors.collectAsState()
    val readTids by viewModel.readTids.collectAsState()
    // 底栏「提醒」未读数：拉到后驱动图标变状态（红点 + 数字）
    var unreadCount by remember { mutableStateOf(0) }
    var fetchingUnread by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val tabListState = rememberLazyListState()
    // 分类筛选条展开状态（按 fid 记忆；默认收起）
    var filterExpanded by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    /** 分类条被手指触摸期间为 true：临时禁用左右切版，手势冲突的开关 */
    val chipsTouchActive = remember { mutableStateOf(false) }
    val activeFid = tabs.getOrNull(pagerState.currentPage)?.fid
    val activeHasFilter = activeFid?.let { (threadTypeMaps[it]?.size ?: 0) > 0 } ?: false
    val activeFilterApplied = activeFid?.let { typeFilters[it] != null } ?: false
    fun toggleFilter(fid: String) {
        filterExpanded = filterExpanded.toMutableMap().apply {
            this[fid] = !(this[fid] ?: false)
        }
    }
    fun collapseFilter(fid: String) {
        filterExpanded = filterExpanded.toMutableMap().apply { put(fid, false) }
    }
    // 本次进程全场锁定的封面风格（冷启动/切主题时在 ThemeManager 重新随机）
    // 用当前主题做 key：主题即时切换后这里会重新取一次新风格，不用重建页面
    val themeIndexNow = ThemeManager.currentTheme
    val sessionCoverStyle = remember(themeIndexNow) { ThemeManager.coverStyleForCurrent() }

    // 退出确认：第一次按返回显示提示，2 秒内再按才真正退出
    var exitHintVisible by remember { mutableStateOf(false) }
    var lastBackPress by remember { mutableStateOf(0L) }
    BackHandler(
        enabled = !showHammerMenu && !showForumManage && !showBlockedManager && peekPost == null
    ) {
        val now = System.currentTimeMillis()
        if (now - lastBackPress < 2000L) {
            (context as? ComponentActivity)?.finish()
        } else {
            lastBackPress = now
            exitHintVisible = true
            // 第一次按返回时给一下轻震，和翻页/退出反馈一致
            tickHaptic(context)
        }
    }
    LaunchedEffect(exitHintVisible) {
        if (exitHintVisible) {
            delay(2000)
            exitHintVisible = false
        }
    }

    // 仅「用户手指滑动」切板块时给段落感震动；点击 Tab 跳转不震（用户要求）
    var prevPage by remember { mutableStateOf(pagerState.currentPage) }
    var isUserSwiping by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState) {
        pagerState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> isUserSwiping = true
                is DragInteraction.Stop,
                is DragInteraction.Cancel -> isUserSwiping = false
            }
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != prevPage) {
            prevPage = pagerState.currentPage
            if (isUserSwiping) tickHaptic(context)
        }
    }

    // 进入 / 回到首页时拉取未读提醒数（通知未看 + 私信未读），驱动底栏「提醒」状态
    LaunchedEffect(resumeSignal.value) {
        if (!ForumDataCache.isLoggedIn()) {
            unreadCount = 0
            return@LaunchedEffect
        }
        if (fetchingUnread) return@LaunchedEffect
        fetchingUnread = true
        try {
            unreadCount = com.example.s1client.util.ReminderUnread.computeUnread(context)
        } catch (_: Exception) {
            // 网络失败不打扰，保留上次的未读数
        } finally {
            fetchingUnread = false
        }
    }


    // 头像点击统一走帖子详情那套用户卡片（见 onAvatarClick），
    // 首页不再另开一套界面

    // 进入 / 滑到某版面时确保它有数据（已缓存则直接复用，不重复请求）
    LaunchedEffect(tabs, pagerState.currentPage) {
        tabs.getOrNull(pagerState.currentPage)?.let { viewModel.ensureLoaded(it.fid) }
    }

    // 板块管理里删掉当前页之后的版面时，把页码收回合法范围，避免 Pager 越界
    LaunchedEffect(tabs.size) {
        if (tabs.isNotEmpty() && pagerState.currentPage > tabs.lastIndex) {
            pagerState.scrollToPage(tabs.lastIndex)
        }
    }

    // 滑动切页时把对应 Tab 滚进可视区
    LaunchedEffect(pagerState.currentPage, tabs.size) {
        if (tabs.isNotEmpty()) {
            tabListState.animateScrollToItem(pagerState.currentPage.coerceIn(0, tabs.lastIndex))
        }
    }


    if (showHammerMenu) {
        HammerMenuDialog(
            onDismiss = { showHammerMenu = false },
            onManage = {
                showHammerMenu = false
                showForumManage = true
            }
        )
    }

    if (showForumManage) {
        ForumManageDialog(
            currentTabs = tabs,
            allForums = allForums,
            onDismiss = { showForumManage = false },
            onApply = { list ->
                showForumManage = false
                viewModel.setTabs(list)
                S1Notify.show(context as Activity, "板块已更新", S1Notify.Type.SUCCESS)
            }
        )
    }

    peekPost?.let { post ->
        val act = context as Activity
        PostPeekDialog(
            post = post,
            viewModel = viewModel,
            onDismiss = { peekPost = null },
            onFavorite = {
                peekPost = null
                if (!ForumDataCache.isLoggedIn()) {
                    S1Notify.show(act, "请先登录", S1Notify.Type.INFO)
                } else {
                    val tid = post.tid
                    scope.launch {
                        // Peek 单向收藏（只收不取消），取消去收藏页手动
                        val r = FavoriteManager.addFavorite(tid)
                        val msg: String
                        val type: S1Notify.Type
                        when {
                            r.failed -> { msg = "操作失败：" + r.message; type = S1Notify.Type.ERROR }
                            r.message == "已在收藏中" -> { msg = "已在收藏中 · 取消请到收藏页手动操作"; type = S1Notify.Type.INFO }
                            else -> { msg = "已收藏"; type = S1Notify.Type.SUCCESS }
                        }
                        S1Notify.show(act, msg, type)
                    }
                }
            },
            onUndoFavorite = {
                scope.launch {
                    val r = FavoriteManager.cancelFavorite(post.tid)
                    if (!r.failed) S1Notify.show(act, "已取消收藏", S1Notify.Type.SUCCESS)
                }
            },
            onShare = {
                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, post.title ?: "")
                    putExtra(
                        android.content.Intent.EXTRA_TEXT,
                        "https://stage1st.com/2b/thread-" + post.tid + "-1-1.html"
                    )
                }
                context.startActivity(android.content.Intent.createChooser(send, "分享帖子"))
                peekPost = null
            },
            onHidePost = {
                peekPost = null
                if (post.tid.isNotBlank()) {
                    Haptic.medium(context)
                    viewModel.hidePost(
                        post.tid,
                        post.title ?: "",
                        post.authorName ?: "",
                        post.timeText ?: ""
                    )
                }
            },
            onOpen = {
                peekPost = null
                if (post.tid.isNotBlank()) {
                    viewModel.markRead(post.tid, post.title ?: "")
                    context.startActivity(
                        android.content.Intent(context, ThreadDetailActivity::class.java).apply {
                            putExtra("tid", post.tid)
                            putExtra("subject", post.title ?: "")
                        }
                    )
                }
            }
        )
    }

    if (showBlockedManager) {
        BlockedAuthorsDialog(
            blocked = blockedAuthors,
            onDismiss = { showBlockedManager = false },
            onUnblock = { uid -> viewModel.unblockAuthor(uid) },
            onClearAll = {
                blockedAuthors.keys.toList().forEach { viewModel.unblockAuthor(it) }
            }
        )
    }

    // 撤销条：SnackbarHost 放 Scaffold 里，自动浮在底栏之上
    val undoHostState = remember { SnackbarHostState() }
    LaunchedEffect(undoBar) {
        val u = undoBar ?: return@LaunchedEffect
        val result = undoHostState.showSnackbar(
            message = u.message,
            actionLabel = u.actionLabel,
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) u.onUndo()
        if (undoBar === u) undoBar = null
    }

    Scaffold(
        containerColor = BgColor,
        snackbarHost = {
            SnackbarHost(hostState = undoHostState) { data ->
                val s1 = LocalS1Colors.current
                // 自绘撤销条：跟随主题色（snackbarBg/onSnackbar 配色早就备好，这里第一次用上）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .background(s1.snackbarBg, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = data.visuals.message,
                        color = s1.onSnackbar,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (data.visuals.actionLabel != null) {
                        Text(
                            text = data.visuals.actionLabel ?: "",
                            color = s1.accent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { data.performAction() }
                                .padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            XhsBottomNav(
                loggedIn = ForumDataCache.isLoggedIn(),
                onHome = { scope.launch { pagerState.animateScrollToPage(0) } },
                onSearch = {
                    context.startActivity(Intent(context, SearchActivity::class.java))
                },
                onCompose = {
                    if (!ForumDataCache.isLoggedIn()) {
                        S1Notify.show(context as Activity, "请先登录")
                    } else {
                        val cur = tabs.getOrNull(pagerState.currentPage)
                        context.startActivity(
                            Intent(context, PostEditorActivity::class.java).apply {
                                putExtra("fid", cur?.fid ?: "")
                                putExtra("forumName", cur?.name ?: "")
                            }
                        )
                    }
                },
                onReminder = {
                    if (ForumDataCache.isLoggedIn()) {
                        context.startActivity(Intent(context, NoticeCenterActivity::class.java))
                    } else {
                        S1Notify.show(context as Activity, "请先登录")
                    }
                },
                onMine = {
                    // 未登录跳登录页，登录后跳 MineActivity
                    val target = if (ForumDataCache.isLoggedIn()) MineActivity::class.java
                    else LoginActivity::class.java
                    context.startActivity(Intent(context, target))
                },
                unreadCount = unreadCount
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Brush.verticalGradient(listOf(BgColor, BgColorBottom)))
        ) {
            Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
                // 内容占满全屏：顶部预留高度改由瀑布流的 contentPadding 承担，
                // 这样导航收起时内容不动、也不会留下空白带
                HorizontalPager(
                    state = pagerState,
                    // 分类条被触摸期间禁用切版手势，避免「滑分类却切了版面」
                    userScrollEnabled = !chipsTouchActive.value,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val tab = tabs.getOrNull(page)
                    if (tab != null) {
                        ForumFeedPage(
                            fid = tab.fid,
                            coverStyle = sessionCoverStyle,
                            state = feeds[tab.fid] ?: UiState.Loading,
                            threadTypes = threadTypeMaps[tab.fid] ?: emptyMap(),
                            selectedType = typeFilters[tab.fid],
                            expanded = filterExpanded[tab.fid] ?: false,
                            readTids = readTids,
                            onSelectType = { viewModel.setTypeFilter(tab.fid, it) },
                            onRequestCollapse = { collapseFilter(tab.fid) },
                            onChipsTouch = { chipsTouchActive.value = it },
                            dataSaver = dataSaver,
                            onRetry = { viewModel.refresh(tab.fid) },
                            onRefresh = {
                                // 下拉刷新触发（松手越过阈值）时给一下轻震，和绳子/翻页反馈一致
                                tickHaptic(context)
                                viewModel.pullRefresh(tab.fid)
                            },
                            onLoadMore = { viewModel.loadMore(tab.fid) },
                            onJumpPage = { target ->
                                viewModel.jumpToPage(tab.fid, target)
                                // 原型规格 7：跳转「落点确认」反馈
                                S1Notify.show(context, "已跳转到第 $target 页", S1Notify.Type.SUCCESS)
                            },
                            onAvatarClick = { post ->
                                val uid = post.authorId
                                if (!uid.isNullOrBlank()) {
                                    (context as? androidx.activity.ComponentActivity)?.let {
                                        com.example.s1client.ui.showUserCardDialog(
                                            it, uid, post.authorName ?: "匿名",
                                            onBlocked = { bUid, bName ->
                                                undoBar = UndoBarData(
                                                    "已对「$bName」塞抹布", "撤销"
                                                ) { viewModel.unblockAuthor(bUid) }
                                            }
                                        )
                                    }
                                }
                            },
                            onMarkRead = { viewModel.markRead(it.tid, it.title ?: "") },
                            onLongPressPost = { post -> peekPost = post }
                        )
                    }
                }

                // drawBehind 闭包不是组合上下文，颜色要在组合期先取出来
                val topBarLine = GlassFloatLine
                // 顶部导航常驻（不再随滑动隐藏）
                XhsCategoryTabs(
                    tabs = tabs,
                    listState = tabListState,
                    selectedIndex = pagerState.currentPage,
                    filterActiveForActive = activeFilterApplied,
                    onTabClick = { index ->
                        if (index == pagerState.currentPage) {
                            // 点当前版面：展开/收起分类筛选条（仅该版面有子分类时）
                            if (activeHasFilter) activeFid?.let { toggleFilter(it) }
                        } else {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                    },
                    onHammerClick = { showHammerMenu = true },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(TopBarHeight)
                        .background(GlassBar)
                        // 玻璃底边：一条细亮线，把导航栏和内容分开
                        .drawBehind {
                            drawLine(
                                color = topBarLine,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        .zIndex(1f)
                )

                // 退出确认提示：第一次按返回时浮现，2 秒后自动淡去
                androidx.compose.animation.AnimatedVisibility(
                    visible = exitHintVisible,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(400)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text("再按一次退出程序", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
