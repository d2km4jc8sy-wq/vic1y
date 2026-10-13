package com.example.s1client

import android.content.Intent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.viewinterop.AndroidView
import com.example.s1client.ui.EdgePageBar
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
private val TopBarHeight = 50.dp

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
                        val r = FavoriteManager.toggleFavorite(tid)
                        val msg: String
                        val type: S1Notify.Type
                        when {
                            r.failed -> { msg = "操作失败：" + r.message; type = S1Notify.Type.ERROR }
                            r.isFavorited -> { msg = "已收藏"; type = S1Notify.Type.SUCCESS }
                            else -> { msg = "已取消收藏"; type = S1Notify.Type.SUCCESS }
                        }
                        S1Notify.show(act, msg, type)
                    }
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
                    undoBar = UndoBarData("已隐藏该帖", "撤销") { viewModel.unhidePost(post.tid) }
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
                            onJumpPage = { viewModel.jumpToPage(tab.fid, it) },
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

/** 单个版面的一页：瀑布流 + 下拉刷新 + 页码浮标 + 页码选择器 */
@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun ForumFeedPage(
    fid: String,
    coverStyle: Int,
    state: UiState,
    threadTypes: Map<String, String> = emptyMap(),
    selectedType: String? = null,
    expanded: Boolean = false,
    /** 已读 tid 集合：卡片标题据此降透明度 */
    readTids: Set<String> = emptySet(),
    onSelectType: (String?) -> Unit = {},
    onRequestCollapse: () -> Unit = {},
    onChipsTouch: (Boolean) -> Unit = {},
    dataSaver: Boolean,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onJumpPage: (Int) -> Unit,
    onAvatarClick: (Post) -> Unit,
    onMarkRead: (Post) -> Unit,
    onLongPressPost: (Post) -> Unit
) {
    var showPagePicker by remember(fid) { mutableStateOf(false) }
    // 细节 30：热帖筛选 —— 本地按回复数排序，不发额外请求
    var hotOnly by remember { mutableStateOf(false) }
    // 细节 14：绳子拉紧比例 → 背景微亮（"拉紧感"），松手回落
    var pullRatio by remember { mutableStateOf(0f) }
    // 贴边滚轮浮条的实例（Compose 里通过 AndroidView 持有，用于命令式 show/hide）
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val chipItems = remember(threadTypes) {
        listOf<Pair<String?, String>>(null to "全部") +
            threadTypes.toList().map { (id, name) -> id to name }
    }
    val showChips = chipItems.size > 1
    // 展开后若约 4 秒未操作则自动收起，避免遮挡看帖
    LaunchedEffect(expanded) {
        if (expanded) {
            delay(4000)
            onRequestCollapse()
        }
    }
    val refreshing = (state as? UiState.Success)?.isRefreshing == true
    val pullState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = onRefresh
    )

    when (state) {
        is UiState.Loading -> {
            // 骨架屏：双列占位卡 + 扫光，加载时先有「形」
            FeedSkeleton()
        }
        is UiState.Error -> {
            // 细节 13：失败"抖头" —— 卡片左右扌3 次 + HEAVY，错误从无声变明确
            val shake = androidx.compose.animation.core.Animatable(0f)
            LaunchedEffect(state.message) {
                com.example.s1client.util.Haptic.heavy(context)
                repeat(3) {
                    shake.animateTo(6f, androidx.compose.animation.core.tween(80))
                    shake.animateTo(-6f, androidx.compose.animation.core.tween(80))
                }
                shake.animateTo(0f, androidx.compose.animation.core.tween(80))
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer { translationX = shake.value }
                ) {
                    Text("加载失败: " + state.message, color = TextSub)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text("重试")
                    }
                }
            }
        }
        is UiState.Success -> {
            if (state.posts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("这个版块暂时没有帖子", color = TextSub)
                }
            } else {
                val gridState = rememberLazyStaggeredGridState()

                // 细节 12：滚动比例采样（回顶钮显隐 + 进度环）
                var scrollFraction by remember { mutableStateOf(0f) }
                LaunchedEffect(gridState, state.posts.size) {
                    snapshotFlow {
                        val li = gridState.layoutInfo
                        val idx = li.visibleItemsInfo.firstOrNull()?.index ?: 0
                        if (li.totalItemsCount > 0) idx / li.totalItemsCount.toFloat() else 0f
                    }.collect { scrollFraction = it }
                }

                LaunchedEffect(gridState, state.posts.size) {
                    snapshotFlow {
                        val layoutInfo = gridState.layoutInfo
                        val totalItems = layoutInfo.totalItemsCount
                        val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        totalItems > 0 && lastVisible >= totalItems - 4
                    }
                    .distinctUntilChanged()
                    .collect { isNearBottom ->
                        if (isNearBottom) {
                            // 瀑布流触底加载更多时给一下轻震，相当于翻到下一页的段落感
                            tickHaptic(context)
                            onLoadMore()
                        }
                    }
                }

                // 右下导航组可见性：滚过 8% 出现（替代旧的"滚动时出现、停 1.5s 消失"）

                // 绳子的显隐逻辑：滚动看内容时隐藏（不碍眼），停下来一会儿再浮现。
                // 但如果已经在「第一页 + 滚到顶部」，系统下拉刷新就够用了，绳子没必要出现
                // （翻到后面的页之后下拉被列表吃掉，那时才轮到这根绳子派上用场）。
                var showRope by remember { mutableStateOf(false) }
                LaunchedEffect(gridState, state.currentPage) {
                    snapshotFlow { gridState.isScrollInProgress }
                        .distinctUntilChanged()
                        .collect { isScrolling ->
                            if (isScrolling) {
                                showRope = false
                            } else {
                                delay(900)
                                val atTop = gridState.firstVisibleItemIndex == 0
                                        && gridState.firstVisibleItemScrollOffset == 0
                                // 方案 A（场景分时）：绳子只在"原生下拉失效"的场景出现——
                                // 翻页后或滚过 25%；第 1 页顶部附近只留回顶钮，重叠感消解
                                showRope = !(state.currentPage == 1 && atTop) &&
                                        (state.currentPage > 1 || scrollFraction > 0.25f)
                            }
                        }
                }

                // 到底弱提示：滚到最后一页底部时，给一下轻震 + 浮出「已经到底了」，
                // 和帖子详情页的到底反馈保持一致（s1er 也是越界/到底才给反馈，平时不打扰）。
                // reachedEnd 只在「从非到底切到到底」的那一刻触发一次，避免反复震动。
                val reachedEnd = state.totalPages > 0 &&
                        state.currentPage >= state.totalPages && !state.isLoadingMore
                LaunchedEffect(reachedEnd) {
                    if (reachedEnd) tickHaptic(context)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pullRefresh(pullState)
                        // 细节 14：绳子拉紧时整页微亮（"拉紧感"），纯绘制层零布局风险
                        .drawWithContent {
                            drawContent()
                            if (pullRatio > 0.01f) {
                                drawRect(androidx.compose.ui.graphics.Color.White.copy(alpha = pullRatio * 0.05f))
                            }
                        }
                ) {
                    // 分类筛选条：默认收起（不占高度、不挡视线）；点当前版面 tab 展开。
                    // 选中某分类即应用并自动收起；展开超 4 秒未操作也会自动收起。
                    AnimatedVisibility(
                        visible = expanded && showChips,
                        enter = expandVertically(animationSpec = tween(180)) + fadeIn(animationSpec = tween(120)),
                        exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(120)),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .zIndex(2f)
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                // 实体玻璃底：筛选条浮在卡片上时文字不再与内容打架
                                .background(GlassBar)
                                .padding(top = TopBarHeight, bottom = 4.dp)
                                // 手势隔离：触摸分类条期间通知外层禁用切版手势，
                                // 横滑分类时永远只滑分类，不会连带切换版面
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        onChipsTouch(true)
                                        do {
                                            val event = awaitPointerEvent()
                                        } while (event.changes.any { it.pressed })
                                        onChipsTouch(false)
                                    }
                                },
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 细节 30：热帖筛选项（永远第一位，本地计算不额外请求）
                            item(key = "hot") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(if (hotOnly) AccentPink else GlassFloat)
                                        .clickable { hotOnly = !hotOnly }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "热帖",
                                        color = if (hotOnly) Color.White else TextSub,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (hotOnly) FontWeight.Medium else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }
                            }
                            chipItems.forEach { (id, name) ->
                                item(key = id ?: "all") {
                                    val selected = selectedType == id
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(99.dp))
                                            .background(if (selected) AccentPink else GlassFloat)
                                            .clickable {
                                                onSelectType(id)
                                                onRequestCollapse()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = name,
                                            color = if (selected) Color.White else TextSub,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 10.dp,
                            end = 10.dp,
                            top = TopBarHeight + (if (expanded && showChips) 40.dp else 12.dp),
                            bottom = 60.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp
                    ) {
                        // key 用 tid，追加/换页时 Compose 能复用已有 item，避免整列重建
                        items(
                            if (hotOnly) {
                                val hot = state.posts.filter { (it.replies ?: 0) >= 50 }
                                if (hot.isNotEmpty()) hot.sortedByDescending { it.replies ?: 0 }
                                else state.posts.sortedByDescending { it.replies ?: 0 }.take(20)
                            } else state.posts,
                            key = { it.tid }
                        ) { post ->
                            XhsPostCard(
                                post = post,
                                coverStyle = coverStyle,
                                dataSaver = dataSaver,
                                read = post.tid in readTids,
                                onAvatarClick = { onAvatarClick(post) },
                                onMarkRead = { onMarkRead(post) },
                                onLongPress = { onLongPressPost(post) }
                            )
                        }
                    }

                    // 绳子下拉刷新（顶部右侧）。
                    // 系统下拉刷新只在列表滚到顶时才生效，翻了几页后就拉不动了；
                    // 这根绳子是独立控件，任何时候都能拉下来 —— 拉到底 = 回到板块第一页并刷新。
                    AndroidView(
                        factory = { ctx ->
                            val doRefresh = onRefresh
                            RopeRefreshView(ctx).apply {
                                alpha = RopeRefreshView.IDLE_ALPHA
                                // 控件 04：刷新珠子注入主题三色（原版三色追逐）
                                this.orbitColors = com.example.s1client.ui.theme.s1PaletteFor(
                                    com.example.s1client.util.ThemeManager.currentTheme
                                ).flowCoverColors.take(3).map { it.toArgb() }
                                // 细节 14：拉力比例上报给页面（背景微亮）
                                this.onPullRatio = { pullRatio = it }
                                this.onRefresh = {
                                    // 瀑布流是不断往下延伸的，刷新必须回到第一页，
                                    // 顺便把列表滚回顶部，否则数据换了但视口还停在半空中
                                    scope.launch { gridState.animateScrollToItem(0) }
                                    doRefresh()
                                }
                            }
                        },
                        update = { view ->
                            view.visibility =
                                if (showRope) android.view.View.VISIBLE else android.view.View.GONE
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = TopBarHeight, end = 16.dp)
                            .size(width = 60.dp, height = 260.dp)
                            .zIndex(2f)
                    )

                    // 页码面板：从页码胶囊向上展开（实心玻璃不透字、右对齐）——
                    // 替代左缘半透明滚轮条（可读性差、位置与右缘导航轴割裂）
                    // 关闭途径：点面板外遮罩（遮罩吞掉一切手势，滑动不再穿透到瀑布流）/
                    // 再点页码胶囊。不再"滚动即消失"——选页时背景滚动不再打断。
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showPagePicker,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 遮罩：吞掉一切手势（tap 关闭 / drag 不再穿透到瀑布流）
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.10f))
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        var moved = false
                                        while (true) {
                                            val e = awaitPointerEvent()
                                            e.changes.forEach { it.consume() }
                                            if (e.changes.any { it.positionChange() != Offset.Zero }) moved = true
                                            if (e.changes.all { !it.pressed }) break
                                        }
                                        if (!moved) showPagePicker = false
                                    }
                                }
                        )
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showPagePicker,
                        enter = fadeIn() + androidx.compose.animation.expandVertically(
                            expandFrom = Alignment.Bottom
                        ),
                        exit = fadeOut() + androidx.compose.animation.shrinkVertically(
                            shrinkTowards = Alignment.Bottom
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 90.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardBg)
                                .border(1.dp, GlassFloatLine, RoundedCornerShape(14.dp))
                                .padding(10.dp)
                        ) {
                            Column(modifier = Modifier.width(162.dp)) {
                                Text(
                                    text = "共 " + (if (state.totalPages > 0) state.totalPages.toString() else "?") + " 页 · 点选跳转",
                                    color = TextSub,
                                    fontSize = 10.5.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                )
                                if (state.totalPages > 0) {
                                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 320.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(state.totalPages) { p0 ->
                                            val p = p0 + 1
                                            val on = p == state.currentPage
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (on) AccentPink else GlassFloat)
                                                    .clickable {
                                                        runCatching { com.example.s1client.util.Haptic.selection(context) }
                                                        showPagePicker = false
                                                        onJumpPage(p)
                                                    }
                                                    .padding(vertical = 7.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    p.toString(),
                                                    color = if (on) Color.White else TextSub,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (on) FontWeight.Medium else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 页码胶囊：回顶已合并进绳子把手（单击回顶 + 外环进度），右下只留这一件
                    androidx.compose.animation.AnimatedVisibility(
                        visible = scrollFraction > 0.08f,
                        enter = fadeIn() + androidx.compose.animation.slideInVertically { it / 3 },
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 14.dp)
                    ) {
                        // 页码胶囊：点按切换 / 长按呼出页码面板
                        // 外圈命中缓冲垫：点偏不点穿卡片
                        Box(
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { }
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .shadow(6.dp, RoundedCornerShape(S1Radius.pill))
                                    .clip(RoundedCornerShape(S1Radius.pill))
                                    .background(CardBg)
                                    .border(1.dp, GlassFloatLine, RoundedCornerShape(S1Radius.pill))
                                    .combinedClickable(
                                        onClick = { showPagePicker = !showPagePicker },
                                        onLongClick = {
                                            runCatching { com.example.s1client.util.Haptic.selection(context) }
                                            showPagePicker = true
                                        }
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${state.currentPage} / " +
                                            (if (state.totalPages > 0) state.totalPages.toString() else "?"),
                                    color = StrongIcon,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (state.isLoadingMore) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = HeartRed, modifier = Modifier.size(24.dp))
                        }
                    }

                    // 到底弱提示：位置、配色与加载圈一致，只是换成一行小字药丸，不挡版面
                    if (reachedEnd) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "已经到底了",
                                color = TextSub,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(S1Radius.pill))
                                    .background(GlassFloat)
                                    .border(1.dp, GlassFloatLine, RoundedCornerShape(S1Radius.pill))
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }
                    }

                    PullRefreshIndicator(
                        refreshing = refreshing,
                        state = pullState,
                        modifier = Modifier.align(Alignment.TopCenter),
                        backgroundColor = CardBg,
                        contentColor = AccentPink
                    )
                }

                // 跳页改由贴边滚轮浮条（EdgePageBar）承担，旧的页码弹窗不再使用
            }
        }
    }
}



@Composable
fun XhsCategoryTabs(
    tabs: List<ForumTab>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    selectedIndex: Int,
    filterActiveForActive: Boolean = false,
    onTabClick: (Int) -> Unit,
    onHammerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 控件 09：滑动胶囊 Tab —— 胶囊实测选中项坐标后弹性滑过去，文字颜色跟随渐变。
        // （原下划线样式是用户早期拍板；本版按新指令改为胶囊，如要回退找我）
        val s1c = LocalS1Colors.current
        val tabRects = remember { mutableStateMapOf<Int, androidx.compose.ui.geometry.Rect>() }
        Box(Modifier.weight(1f)) {
            val selRect = tabRects[selectedIndex]
            if (selRect != null) {
                val d = LocalDensity.current
                val pillX by animateDpAsState(
                    // 左右各延 8dp，让胶囊包住文字而不是卡着文字边
                    targetValue = with(d) { selRect.left.toInt().toDp() } - 8.dp,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = 0.6f,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                    ),
                    label = "tabPillX"
                )
                val pillW by animateDpAsState(
                    targetValue = with(d) { selRect.width.toInt().toDp() } + 16.dp,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = 0.85f,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    ),
                    label = "tabPillW"
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = pillX)
                        .width(pillW)
                        .height(26.dp)
                        .clip(RoundedCornerShape(99.dp))
                        // 单色 accent 填充（渐变在深色主题下发脏，全局去彩色的延伸）
                        .background(s1c.accent)
                )
            }
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 14.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(tabs.size) { index ->
                    val isSelected = index == selectedIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .onGloballyPositioned { tabRects[index] = it.boundsInParent() }
                            .clickable { onTabClick(index) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tabs[index].name,
                                color = if (isSelected) Color.White else TextSub,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                // 版面名长短不一（「自由搏击区」6 字、「炉石传说」4 字），
                                // 宽度上限留够 5 个汉字，窄了就出省略号，绝不出现半个字。
                                modifier = Modifier.widthIn(max = 78.dp),
                                overflow = TextOverflow.Ellipsis
                            )
                            // 该版面已按分类筛选且筛选条收起时，名字旁点个小箭头提示
                            if (isSelected && filterActiveForActive) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "已按分类筛选",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 分隔线 + 主题 + 锤子：固定在右侧不随 Tab 滚动
        // 外面套一层与 Tab 同构的 Column（图标 + 下划线占位）：
        // 单个图标单独居中的话会沉下去约 4dp，看起来和版面文字不在一条线上
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(GlassFloatLine)
                )
                Box(
                    modifier = Modifier
                        .padding(start = 2.dp, end = 12.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .clickable { onHammerClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Hardware,
                        contentDescription = "板块管理",
                        tint = TextSub,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(7.5.dp))
        }
    }
}

/**
 * 锤子菜单：只留「板块管理」。
 * 省流模式 / 吐抹布（黑名单）已移到「我的 → 齿轮」面板，锤子不再当杂物抽屉。
 */
@Composable
private fun FeedSkeleton(modifier: Modifier = Modifier) {
    val s1 = LocalS1Colors.current
    val transition = rememberInfiniteTransition(label = "skeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "skeletonSweep"
    )
    // 扫光位置：整块占位区从左到右循环（宽度按常见屏宽估算即可，视觉上够用）
    val sweep = -360f + progress * (360f + 1200f)
    val shimmer = Brush.linearGradient(
        colors = listOf(
            s1.textSub.copy(alpha = 0.09f),
            s1.textSub.copy(alpha = 0.22f),
            s1.textSub.copy(alpha = 0.09f)
        ),
        start = Offset(sweep, 0f),
        end = Offset(sweep + 320f, 320f)
    )

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 两列错落高度，贴近真实瀑布流的节奏
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkeletonCard(shimmer, 196.dp)
            SkeletonCard(shimmer, 150.dp)
            SkeletonCard(shimmer, 178.dp)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkeletonCard(shimmer, 150.dp)
            SkeletonCard(shimmer, 196.dp)
            SkeletonCard(shimmer, 140.dp)
        }
    }
}

/** 一张占位卡：封面块 + 两行文字条，全部走同一个扫光笔刷 */
@Composable
private fun SkeletonCard(brush: Brush, height: Dp) {
    val s1 = LocalS1Colors.current
    Card(
        modifier = Modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = s1.cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(10.dp)
                    .background(brush, RoundedCornerShape(10.dp))
            )
            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(13.dp)
                        .background(brush, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(11.dp)
                        .background(brush, RoundedCornerShape(4.dp))
                )
            }
        }
    }
}
