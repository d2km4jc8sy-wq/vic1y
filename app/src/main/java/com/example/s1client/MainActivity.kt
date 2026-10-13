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
private fun tickHaptic(context: android.content.Context) {
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
    var showThemeSheet by remember { mutableStateOf(false) }
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

    if (showThemeSheet) {
        com.example.s1client.ui.ThemePickerSheet(onDismiss = { showThemeSheet = false })
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
fun HammerMenuDialog(
    onDismiss: () -> Unit,
    onManage: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = GlassDialog,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassDialogLine),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "板块管理",
                    color = TextMain,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
                HammerMenuItem(Icons.Default.DashboardCustomize, "管理显示的板块", onManage)
            }
        }
    }
}

/**
 * 长按 Peek：居中卡片，标题 + 正文预览 + 收藏/分享/隐藏。
 * 目的：不用进帖就能判断值不值得读（列表接口不返回正文，这里现取一层）。
 * 已按用户要求去掉「复制标题」。
 */
/**
 * 精简版长按菜单（搜索页等没有 Peek 预览的场景用）。
 * 已按用户要求去掉「复制标题」——复制标题意义不大。
 */
@Composable
fun PostActionDialog(
    post: Post,
    onDismiss: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = GlassDialog,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassDialogLine),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = post.title ?: "无标题",
                    color = TextMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
                HammerMenuItem(Icons.Default.Star, "收藏", onFavorite)
                HammerMenuItem(Icons.Default.Share, "分享帖子", onShare)
            }
        }
    }
}

@Composable
fun PostPeekDialog(
    post: Post,
    viewModel: PostViewModel,
    onDismiss: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onHidePost: (() -> Unit)? = null,
    onOpen: () -> Unit
) {
    var preview by remember(post.tid) { mutableStateOf<String?>(null) }
    var loading by remember(post.tid) { mutableStateOf(true) }
    LaunchedEffect(post.tid) {
        loading = true
        preview = viewModel.fetchThreadPreview(post.tid)
        loading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = GlassDialog,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassDialogLine),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                // 标题
                Text(
                    text = post.title ?: "无标题",
                    color = TextMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOfNotNull(
                        post.authorName,
                        post.timeText.takeIf { it.isNotBlank() },
                        post.replies?.let { "$it 回复" }
                    ).joinToString(" · "),
                    color = TextSub,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.Divider(color = GlassDialogLine)
                Spacer(modifier = Modifier.height(12.dp))

                // 正文预览：加载中 / 成功 / 失败三态
                when {
                    loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = AccentPink
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("正在加载正文…", color = TextSub, fontSize = 13.sp)
                        }
                    }
                    preview.isNullOrBlank() -> {
                        Text(
                            "正文预览没取到，可以直接进帖看看。",
                            color = TextSub,
                            fontSize = 13.sp
                        )
                    }
                    else -> {
                        // 预览区高度有上限，内容多时内部滚动
                        Box(modifier = Modifier.heightIn(max = 180.dp)) {
                            val scroll = rememberScrollState()
                            Text(
                                text = preview ?: "",
                                color = TextMain,
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                modifier = Modifier.verticalScroll(scroll)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 三个操作：收藏 / 分享 / 隐藏
                Row(modifier = Modifier.fillMaxWidth()) {
                    PeekActionButton("收藏", Modifier.weight(1f), onFavorite)
                    Spacer(modifier = Modifier.width(8.dp))
                    PeekActionButton("分享", Modifier.weight(1f), onShare)
                    if (onHidePost != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        PeekActionButton("隐藏", Modifier.weight(1f), onHidePost)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                // 进帖：点整行
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("查看完整帖子", color = AccentPink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("→", color = AccentPink, fontSize = 14.sp)
                }
            }
        }
    }
}

/** Peek 里的操作按钮：竖杠风格，和顶栏文字一致 */
@Composable
private fun PeekActionButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(GlassDialogLine.copy(alpha = 0.35f))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/** 黑名单管理：没有这个入口，塞抹布就成了单向陷阱 */
@Composable
fun BlockedAuthorsDialog(
    blocked: Map<String, String>,
    onDismiss: () -> Unit,
    onUnblock: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = GlassDialog,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassDialogLine),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = "吐抹布（" + blocked.size + "）",
                    color = TextMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (blocked.isEmpty()) {
                    Text(
                        text = "还没有人被塞抹布",
                        color = TextSub,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                } else {
                    val entries = blocked.entries.toList()
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(entries.size) { index ->
                            val (uid, name) = entries[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        color = TextMain,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "UID " + uid,
                                        color = TextSub,
                                        fontSize = 10.sp
                                    )
                                }
                                Text(
                                    text = "吐抹布",
                                    color = AccentPink,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(99.dp))
                                        .clickable { onUnblock(uid) }
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "全部吐抹布",
                        color = TextSub,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .clickable { onClearAll() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HammerMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = TextSub, modifier = Modifier.size(19.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = TextMain, fontSize = 14.sp)
    }
}

/** 板块管理：竖向列表，长按拖动排序，点 - / + 增删，确定后一次性应用 */
@Composable
fun ForumManageDialog(
    currentTabs: List<ForumTab>,
    allForums: List<ForumTab>,
    onDismiss: () -> Unit,
    onApply: (List<ForumTab>) -> Unit
) {
    // 本地工作副本：拖动 / 增删都先改这里，点「确定」才落库，取消即丢弃
    var order by remember { mutableStateOf(currentTabs) }
    val addable = allForums.filter { f -> order.none { it.fid == f.fid } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = GlassDialog,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassDialogLine),
            modifier = Modifier.fillMaxWidth()
        ) {
            // 外层可滚动：小屏上也能滚到「确定」；两个内层列表都有 heightIn 上限，
            // 约束是有限值，不会触发「嵌套纵向滚动」的无限高度崩溃
            Column(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "板块管理",
                    color = TextMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Text(
                    text = "长按拖动排序，点 - / + 增删",
                    color = TextSub,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )

                // ===== 已添加（可拖动排序） =====
                Text(
                    text = "已添加（" + order.size + "）",
                    color = AccentPink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 6.dp)
                )
                Box(modifier = Modifier.heightIn(max = 200.dp)) {
                    ReorderableTabColumn(
                        tabs = order,
                        canRemove = order.size > 1,
                        onReorder = { from, to ->
                            if (from != to && from in order.indices && to in order.indices) {
                                val list = order.toMutableList()
                                list.add(to, list.removeAt(from))
                                order = list
                            }
                        },
                        onRemove = { fid -> order = order.filter { it.fid != fid } }
                    )
                }

                // ===== 可添加 =====
                Text(
                    text = "可添加（" + addable.size + "）",
                    color = TextSub,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 6.dp)
                )
                if (addable.isEmpty()) {
                    Text(
                        text = "没有更多板块了",
                        color = TextSub,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                        items(addable.size) { index ->
                            val f = addable[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { order = order + f }
                                    .padding(horizontal = 20.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = f.name,
                                    color = TextMain,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "添加",
                                    tint = AccentPink,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // ===== 取消 / 确定 =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 14.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "取消",
                        color = TextSub,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "确定",
                        color = OnAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(AccentPink)
                            .clickable { onApply(order) }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/** 「已添加」列表：长按某行后上下拖动即可换位 */
@Composable
private fun ReorderableTabColumn(
    tabs: List<ForumTab>,
    canRemove: Boolean,
    onReorder: (Int, Int) -> Unit,
    onRemove: (String) -> Unit
) {
    val rowHeight = 46.dp
    val rowHeightPx = with(LocalDensity.current) { rowHeight.toPx() }
    var draggingIndex by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        tabs.forEachIndexed { index, tab ->
            val isDragging = index == draggingIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .zIndex(if (isDragging) 1f else 0f)
                    // 拖动中用 translationY 位移；读的是 State，不触发重组
                    .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                    .background(if (isDragging) ChipBg else Color.Transparent)
                    .pointerInput(tabs) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingIndex = index
                                dragOffset = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                            },
                            onDragEnd = {
                                val target = (index + (dragOffset / rowHeightPx).roundToInt())
                                    .coerceIn(0, tabs.lastIndex)
                                draggingIndex = -1
                                dragOffset = 0f
                                if (target != index) onReorder(index, target)
                            },
                            onDragCancel = {
                                draggingIndex = -1
                                dragOffset = 0f
                            }
                        )
                    }
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "拖动排序",
                    tint = TextSub,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = tab.name,
                    color = TextMain,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "移除",
                    tint = if (canRemove) TextSub else TextSub.copy(alpha = 0.35f),
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable(enabled = canRemove) { onRemove(tab.fid) }
                )
            }
        }
    }
}

/**
 * 无图帖子的文字封面：
 * style -1  OLED 纯黑（曜石玄夜专用，黑底+琥珀金点缀）
 * style 0..4 五种风格，每种自动按主题明暗渲染深/浅两版：
 *           0 角落光晕 / 1 微网格+顶彩条 / 2 大引号 / 3 玻璃描边+顶高光 / 4 组合
 * style 5   幻彩渐变专用：低饱和浅色三段柔和渐变（Apple 风），色板来自当前主题
 */
private val CoverGlowDark = listOf(
    Color(0xFFB48CFF), Color(0xFFFFA08C), Color(0xFF8CC8FF), Color(0xFF8CFFB4)
)
private val CoverBarDark = listOf(
    Color(0xFF7C6AD6), Color(0xFFD67A6A), Color(0xFF6A9BD6), Color(0xFF6AD6A0)
)
private val CoverBaseDark = listOf(
    Color(0xFF1E1E24), Color(0xFF1C1B20), Color(0xFF1C1C1E), Color(0xFF1B1B22), Color(0xFF1E1E24)
)

/**
 * Linear 主题（coverStyle = -2）专用：
 * 卡片底几乎与页面底同色，靠 1px 描边分层；顶部一条 2dp 语义色条做点缀。
 * 色条按 tid 取 5 色之一（低饱和、与主色同族），保留"每张卡略有不同"的节奏。
 */
private val LinearCardBase = Color(0xFF0D0E10)
private val LinearCardLine = Color(0xFF191B1F)
private val LinearStripColors = listOf(
    Color(0xFF5E6AD2), Color(0xFF3E8E6E), Color(0xFFC79A3A),
    Color(0xFF9A5BB8), Color(0xFF5E8FBF)
)

// ————————— 幻彩流光封面（coverStyle = 5）—————————
//
// 构图定稿 = 方向 B「液态流光」（2026-10-13 用户选定，取代此前的
// 方向 2「两团光斑」与更早的 12 颗铺满方案）。
//
// 为什么换：12 颗的问题不是数量，而是**光斑会爬到文字底下压脏标题**；
// 两团光斑虽然文字干净，但整体偏静、看不出"流"字。
// B 方案把流动放在**底色本身** —— 三段渐变的色标沿 x 轴整体横移，
// 顶部叠一条缓慢横移的亮带；文字区由**底部白纱**托底，纱下的流动被削弱，
// 于是"光一直在动"与"文字始终干净"这两件事不再冲突。
//
// **"文字干净"仍是硬底线**，靠三条：
//   ① 底部白纱从 0.42h 起往上渗，标题无论几行都落在纱上；
//   ② 顶部亮带压在 0.30h 附近，够不到标题区；
//   ③ 四边提亮辉光把卡片"框住"，边界清晰。
//
// 色板（用户 2026-10-12 定，勿改）：幻彩色板 2 套（浅/深），每次进主题随机抽一套。
// 封面的三段渐变跟随色板（`s1.coverColors`）：
//   浅色板 = 粉调　深色板 = 紫调
// 下面三个常量只是**取不到色板时的兜底**，正常不会用到。

/** 兜底：封面上半主色（紫） */
private val FlowColorTop = Color(0xFFB569F6)
/** 兜底：封面下半主色（粉） */
private val FlowColorBottom = Color(0xFFFF7AB5)
/** 兜底：渐变中段过渡色 */
private val FlowColorMid = Color(0xFFC03DEA)

/** 封面标题用色：封面永远是亮底，所以固定深紫黑（不随主题明暗变） */
private val FlowCoverText = Color(0xFF2E1B38)

/**
 * 循环总时长（秒）。渐变色标沿 x 轴横移一个来回所用的时间。
 * 首版 60s 实测太慢、肉眼察觉不到动，改为 24s（慢但可辨，再快就晃眼）。
 */
private const val FLOW_CYCLE_SECONDS = 24f


@Composable
private fun TextCover(
    style: Int,
    seed: Int,
    title: String
) {
    val s1 = LocalS1Colors.current
    val isDark = s1.isDark
    // 从主题幻彩板取一个同色系柔光色（按 seed 轻微变化，但整体统一）
    val glow = s1.flowCoverColors.getOrElse(seed % s1.flowCoverColors.size) { s1.accent }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 156.dp)
            .background(s1.cardBg)
            .drawBehind {
                val w = size.width; val h = size.height
                // 左上主题同色柔光，极低透明度，营造"色块"质感而不抢戏
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glow.copy(alpha = if (isDark) 0.22f else 0.30f),
                            glow.copy(alpha = 0f)
                        ),
                        center = Offset(w * 0.18f, h * 0.12f),
                        radius = w * 0.95f
                    )
                )
                // 底部一道淡描边，增加层次
                drawLine(
                    glow.copy(alpha = if (isDark) 0.18f else 0.22f),
                    Offset(0f, h - 0.5f), Offset(w, h - 0.5f), 1f
                )
            }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            color = s1.textMain,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 14.dp)
        )
    }
}

/**
 * 瀑布流骨架屏：双列占位卡 + 一道缓慢扫过的高光。
 * 加载时先有「形」，不再是屏幕中间一个孤零零的转圈。
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

@Composable
fun XhsPostCard(
    post: Post,
    coverStyle: Int,
    dataSaver: Boolean,
    /** 已读过的帖子：标题降一档不透明度，刷过的一眼跳过 */
    read: Boolean = false,
    onAvatarClick: () -> Unit,
    onMarkRead: () -> Unit,
    onLongPress: () -> Unit
) {
    val title = post.title ?: "无标题"
    val tag = post.tag ?: "杂谈"
    val author = post.authorName ?: "匿名"
    val replies = post.replies ?: 0
    val timeText = post.timeText ?: ""
    val isHot = replies >= 300
    // 省流模式：不加载封面图，一律用风格文字封面
    val showImage = !dataSaver && !post.coverUrl.isNullOrBlank()
    val context = androidx.compose.ui.platform.LocalContext.current
    val s1 = LocalS1Colors.current
    // 按压缩放：interactionSource 与点击共用，按下才有反馈
    val pressSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    // 卡片圆角：Linear 收敛到 9dp，其余 14dp
    val corner = if (coverStyle == -2) 9f else 14f
    val cardShape = RoundedCornerShape(corner.dp)
    // 幻彩流光的流动相位：只在绘制阶段读 .value（每帧重绘、不重组），非幻彩主题不建动画
    val flowPhaseState: State<Float> = if (coverStyle == 5) {
        rememberInfiniteTransition(label = "iridescent")
            .animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween((FLOW_CYCLE_SECONDS * 1000).toInt(), easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "iridescentPhase"
            )
    } else {
        remember { mutableStateOf(0f) }
    }
    // 控件 01 · strange-cobra-12：五色描边带相位（0→1 每 0.75s，对应 translateX(-25%)）
    val cobraPhaseState: State<Float> = if (coverStyle == 5) {
        rememberInfiniteTransition(label = "cobra").animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(6400, easing = LinearEasing)),
            label = "cobraPhase"
        )
    } else {
        remember { mutableStateOf(0f) }
    }
    // 液态玻璃边框（方案 B）：整圈渐变描边 + 内侧顶部高光
    val gTop = s1.glassEdgeTop
    val gMid = s1.glassEdgeMid
    val gBot = s1.glassEdgeBottom
    val gInset = s1.glassInsetLight
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(pressSource)
            .combinedClickable(
                interactionSource = pressSource,
                indication = androidx.compose.material.ripple.rememberRipple(),
                onClick = {
                    if (post.tid.isNotBlank()) {
                        onMarkRead()
                        context.startActivity(
                            android.content.Intent(context, ThreadDetailActivity::class.java).apply {
                                putExtra("tid", post.tid)
                                putExtra("subject", post.title ?: "")
                            }
                        )
                    }
                },
                onLongClick = {
                    // 长按帖子给一下轻震，和翻页/退出反馈一致
                    tickHaptic(context)
                    onLongPress()
                }
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = when {
                coverStyle == 5 -> if (s1.isDark) Color(0xFF0B0B10) else Color(0xFFF5F4F8)
                coverStyle == -2 -> LinearCardBase
                else -> GlassCard
            }
        ),
        // 边框改为自绘的液态玻璃（见下方 Box 的 drawWithContent），此处不再用 BorderStroke
        border = null,
        // 投影：半透明卡底 + 高程制造立体感（Linear 去掉阴影，靠边框分层）
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (coverStyle == -2) 0.dp else 4.dp
        )
    ) {
      // 液态玻璃描边画在最外层 Box 上（不被裁剪，四边都能完整显示）；
      // 内容 Column 单独 clip 进卡片圆角 —— 顶部色条因此不再从圆角缝隙漏出（修复白边 bug）。
      // 注意：不要把 clip 和 drawWithContent 写在同一个 Modifier 链上，
      // 否则 clip 会把描边靠外的那一半也裁掉，看起来就"只有顶部有"。
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .then(
              if (coverStyle == 5) {
                  // 幻彩整卡外框（方案② 静态柔彩）：五色带 40% 环绕整卡（含 meta），
                  // 与设置页同语言；封面/meta 同面板直角拼接，内角弧不复存在
                  Modifier.drawWithContent {
                      drawContent()
                      val band = listOf(
                          Color(0xFF4FCF70), Color(0xFFFAD648), Color(0xFFA767E5),
                          Color(0xFF12BCFE), Color(0xFF44CE7B)
                      )
                      drawRoundRect(
                          brush = Brush.linearGradient(
                              band.map { it.copy(alpha = 0.4f) },
                              start = Offset.Zero,
                              end = Offset(size.width, size.height)
                          ),
                          cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner.dp.toPx()),
                          style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                      )
                  }
              } else if (gTop != null) {
                  Modifier.drawWithContent {
                      drawContent()
                      val w = size.width
                      val h = size.height
                      // 深色主题玻璃要"明显"：描边加粗 + 中段/底部提亮；浅色主题保持含蓄
                      val stroke = if (s1.isDark) 1.5.dp.toPx() else 1.1.dp.toPx()
                      // 整圈渐变描边：顶亮 → 中弱 → 底稍亮（模拟玻璃管折射）
                      drawRoundRect(
                          brush = Brush.verticalGradient(
                              colors = listOf(
                                  gTop,
                                  gMid ?: gTop,
                                  gBot ?: gMid ?: gTop
                              )
                          ),
                          cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner.dp.toPx()),
                          style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                      )
                      // 内侧顶部高光：玻璃的"上表面反射"
                      if (gInset != null) {
                          drawRoundRect(
                              brush = Brush.verticalGradient(
                                  colors = listOf(gInset, Color.Transparent),
                                  startY = 0f,
                                  endY = h * 0.34f
                              ),
                              cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner.dp.toPx()),
                              style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke * 0.9f)
                          )
                      }
                  }
              } else Modifier
          )
      ) {
        Column(
          modifier = Modifier
            .clip(cardShape)
            .defaultMinSize(minHeight = 96.dp)
        ) {
            val cover = post.coverUrl
            if (showImage && !cover.isNullOrBlank()) {
                // 有图：图片填满顶部，仅圆角上沿；不再压黑色遮罩
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = corner.dp, topEnd = corner.dp))
                ) {
                    AsyncImage(
                        model = cover,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(post.coverAspect ?: 1f)
                    )
                }
            }

            // 标题区：有图卡在封面下方（纯卡片底）；无图卡按主题类型区分封面：
            //  - 幻彩渐变（coverStyle=5）：高饱和流动幻彩渐变（深色封面，白字）
            //  - OLED（coverStyle=-1）：纯黑底 + 强调色描边
            //  - 普通主题（coverStyle=0..3）：4 套封面（深色莫兰迪 / 微网格 / 大引号 / 暗色玻璃），每次冷启动随机锁定一套
            if (showImage) {
                Text(
                    text = title,
                    color = if (read) TextMain.copy(alpha = 0.55f) else TextMain,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp)
                )
            } else {
                val seedAbs = kotlin.math.abs((post.tid ?: author).hashCode())
                // 普通主题：4 套封面（深色莫兰迪 / 微网格 / 大引号 / 暗色玻璃），每次冷启动随机锁定一套；
                // 每张卡片按 tid 取该风格下的 4 色之一，制造「同风格异色」的变化
                // 封面两套制（用户拍板）：莫兰迪/暗色玻璃删除，只留微网格与大引号
                val styleIdx = if (seedAbs % 2 == 0) 1 else 2
                val glowColor = CoverGlowDark.getOrElse(seedAbs % CoverGlowDark.size) { s1.accent }
                // -1(OLED) / -2(Linear) 都是深底封面，正文用白字；
                // 浅色主题（焙茶/樱雾/朱砂/苔绿）不再吃暗色封面 —— 黑板砸暖底是全页最大的风格断裂
                val isDarkCover = coverStyle != -1 && coverStyle != -2 && s1.isDark

                val coverModifier = when (coverStyle) {
                    5 -> {
                        // 幻彩封面：整卡外框在 Card 层（五色带 40% 环绕整卡），
                        // 这里补"内框线"——1dp 细线环封面块（上圆下方，与卡圆角/拼接处一致），
                        // 内外两层线都在，用户拍板双层都要
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 120.dp)
                            .drawBehind {
                                val band = listOf(
                                    Color(0xFF4FCF70), Color(0xFFFAD648), Color(0xFFA767E5),
                                    Color(0xFF12BCFE), Color(0xFF44CE7B)
                                )
                                val r = 10.dp.toPx()
                                val path = Path().apply {
                                    addRoundRect(
                                        androidx.compose.ui.geometry.RoundRect(
                                            androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height),
                                            topLeft = androidx.compose.ui.geometry.CornerRadius(r, r),
                                            topRight = androidx.compose.ui.geometry.CornerRadius(r, r),
                                            bottomRight = androidx.compose.ui.geometry.CornerRadius(0f, 0f),
                                            bottomLeft = androidx.compose.ui.geometry.CornerRadius(0f, 0f)
                                        )
                                    )
                                }
                                drawPath(
                                    path,
                                    brush = Brush.linearGradient(
                                        band.map { it.copy(alpha = 0.4f) },
                                        start = Offset.Zero,
                                        end = Offset(size.width, size.height)
                                    ),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                )
                            }
                    }
                    -1 -> {
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 120.dp)
                            .background(s1.cardBg)
                            .drawBehind {
                                val w = size.width; val h = size.height
                                drawLine(
                                    s1.accent.copy(alpha = 0.35f),
                                    Offset(0f, h - 0.5f), Offset(w, h - 0.5f), 1.5f
                                )
                            }
                    }
                    -2 -> {
                        // Linear：纯色底 + 右下角同色系径向微光
                        //
                        // 根治记录 —— 顶部装饰**全风格清零**：
                        // 这里的 2dp 顶部色条（LinearStripColors）是最后一处漏网的。
                        // 用户第三次反馈"顶部依旧存在这种 bug"，逐像素比对截图确认：
                        // 截图 y=66..73 的线是 (154,90,184)，与 LinearStripColors[3]=(154,91,184) 完全一致。
                        // 所以现在四套内置封面（莫兰迪/微网格/大引号/暗色玻璃）+ Linear 的顶部
                        // 一律不再画任何色条/白线/光晕 —— 顶部保持与封面本体同色，整张封面是统一的。
                        // 微光也一并挪到右下角（原来在 0.86w/0.10h，会贴着顶边泛色）。
                        val strip = LinearStripColors.getOrElse(seedAbs % LinearStripColors.size) { s1.accent }
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 120.dp)
                            .background(LinearCardBase)
                            .drawBehind {
                                val w = size.width; val h = size.height
                                // 柔和径向微光（比普通主题更淡，避免破坏克制感）
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            strip.copy(alpha = 0.13f),
                                            strip.copy(alpha = 0f)
                                        ),
                                        center = Offset(w * 0.86f, h * 0.90f),
                                        radius = w * 0.92f
                                    )
                                )
                            }
                    }
                    else -> if (!s1.isDark) {
                        // 浅色主题统一克制封面：cardBg 底 + 右下角主题色微光，
                        // 四套暗色封面设计保留给深色主题（全页协调性修复）
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 120.dp)
                            .background(androidx.compose.ui.graphics.lerp(s1.cardBg, s1.accent, 0.06f))
                            .drawBehind {
                                val w = size.width; val h = size.height
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            s1.accent.copy(alpha = 0.35f),
                                            s1.accent.copy(alpha = 0f)
                                        ),
                                        center = Offset(w * 0.86f, h * 0.90f),
                                        radius = w * 0.95f
                                    )
                                )
                            }
                    } else when (styleIdx) {
                        1 -> Modifier  // 微网格：单色底 + 极细网格
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 120.dp)
                            .background(CoverBaseDark[1])
                            .drawBehind {
                                val w = size.width; val h = size.height
                                val grid = Color.White.copy(alpha = 0.05f)
                                var gx = 0f
                                while (gx <= w) { drawLine(grid, Offset(gx, 0f), Offset(gx, h), 1f); gx += 12.dp.toPx() }
                                var gy = 0f
                                while (gy <= h) { drawLine(grid, Offset(0f, gy), Offset(w, gy), 1f); gy += 12.dp.toPx() }
                                // 原顶部那条 3dp 语义色条已移除：
                                // 整张封面是深色，顶部突然压一条高饱和色带，和"统一"的封面观感冲突
                                // （用户反馈：封面顶部出现色差不一样的白条）。只留网格。
                                // `barColor` 也一并不再计算 —— CoverBarDark 目前没有其他使用点了。
                            }
                        2 -> Modifier  // 大引号极简：纯深灰底 + 大引号 + 右下角版块名
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 120.dp)
                            .background(CoverBaseDark[2])
                        // 深色封面上顶着一条白线，就是用户说的"封面顶部色差不一样的白条"。
                        // 四套内置封面里它是唯一真正"白"的一条 —— 微网格是彩色条、
                        // 莫兰迪是彩色光晕，一并处理掉后四套封面顶部全部统一。
                        else -> Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 120.dp)
                            .background(CoverBaseDark[0])
                    }
                }

                // 幻彩封面（style 5）= strange-cobra 彩边 + 内层双色：
                // 深色变体白字 / 浅色变体深字（FlowCoverText）
                val baseTitleColor = if (coverStyle == 5) {
                    if (s1.isDark) Color.White else FlowCoverText
                } else if (isDarkCover) Color.White else s1.textMain
                // 已读：保持原色相，只降不透明度（不动封面、不动卡片底色）
                val titleColor = if (read) baseTitleColor.copy(alpha = baseTitleColor.alpha * 0.6f)
                                 else baseTitleColor

                Box(
                    modifier = coverModifier
                        // 左右下三边统一 12dp，与下方元信息行（start/end=12dp）严格对齐 ——
                        // 原来封面用 14dp、元信息用 12dp，差 2dp 没对齐，
                        // 就是那种"说不出的不协调感"的来源。top 保持 14dp（留给大引号水印）。
                        .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 12.dp)
                ) {
                    // 大引号风格：左上巨大引号
                    if (isDarkCover && styleIdx == 2) {
                        Text(
                            text = "“",
                            color = Color.White.copy(alpha = 0.07f),
                            fontSize = 92.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.TopStart).offset(x = (-2).dp, y = (-10).dp)
                        )
                    }
                    Text(
                        text = title,
                        color = titleColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 21.sp,
                        // 幻彩流光封面：标题居中（用户要求"自动在封面居中位置展示"）；
                        // 其余深色封面仍左下角，浅色封面仍居中
                        textAlign = if (coverStyle == 5 || !isDarkCover) TextAlign.Center else TextAlign.Start,
                        // 幻彩流光封面：标题加一层极淡的白色柔光阴影。
                        // 参考稿（Start / UIVERSE）的字都有微妙阴影勾边 ——
                        // 光斑偶尔蹭到文字边缘时，这层光能把文字"钉"在背景上
                        style = if (coverStyle == 5) {
                            androidx.compose.material3.LocalTextStyle.current.copy(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.White.copy(alpha = 0.75f),
                                    offset = Offset(0f, 1f),
                                    blurRadius = 3f
                                )
                            )
                        } else androidx.compose.material3.LocalTextStyle.current,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (coverStyle == 5 || !isDarkCover) Modifier.align(Alignment.Center)
                                else Modifier.align(Alignment.BottomStart)
                            )
                            .padding(
                                start = if (coverStyle == 5) 8.dp else 0.dp,
                                end = if (coverStyle == 5) 8.dp else 0.dp,
                                top = if (isDarkCover && coverStyle != 5) 6.dp else 0.dp,
                                bottom = 0.dp
                            )
                    )
                }
            }

            // 元信息第一行：作者（左）/ 回帖数（右）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val uid = post.authorId
                val avatarUrlStr = if (!uid.isNullOrBlank()) avatarUrl(uid, "small") else null
                val letterSeed = author.ifBlank { "?" }
                val avatarBrush = remember(letterSeed) { Brush.linearGradient(getAvatarGradient(letterSeed)) }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(avatarBrush)
                        .clickable { onAvatarClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letterSeed.firstOrNull()?.toString()?.uppercase() ?: "?",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (avatarUrlStr != null) {
                        AsyncImage(
                            model = avatarUrlStr,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = author,
                    color = TextSub,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "回复",
                    tint = if (isHot) s1.heart else TextSub,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = replies.toString(),
                    color = if (isHot) s1.heart else TextSub,
                    fontSize = 11.sp,
                    fontWeight = if (isHot) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            // 元信息第二行：分类（左，主题强调色胶囊）/ 时间（右）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (tag.isNotBlank()) {
                    // 分类标签：去掉原来的「强调粉胶囊底色」。
                    // 用户反馈那个粉色块和周围灰字明显不一致、看着不舒服 ——
                    // 现在改成与作者/时间同色的纯文字，整行元信息统一。
                    Text(
                        text = tag,
                        color = TextSub,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (timeText.isNotBlank()) {
                    Text(
                        text = timeText,
                        color = TextSub.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
      }  // ← 关闭液态玻璃描边 Box
    }  // ← 关闭 Card
}

@Composable
fun XhsBottomNav(
    loggedIn: Boolean = true,
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onCompose: () -> Unit,
    onReminder: () -> Unit,
    onMine: () -> Unit,
    unreadCount: Int = 0
) {
    // drawBehind 闭包不是组合上下文，颜色要在组合期先取出来
    val navBarLine = GlassFloatLine
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // 顺序很关键：background 必须在 navigationBarsPadding 之外层，
            // 才能把玻璃底铺满整个节点（含导航栏/手势条区域）。
            // 之前是「先 navigationBarsPadding 再 background」—— 那样背景只画在
            // 内缩后的内容区，导航栏那条高度是空的，只能靠 XML 的 opaque
            // navigationBarColor 去补，于是不同设备就出现了色带（小米上的「白边」）。
            .background(GlassBar)
            // 玻璃顶边
            .drawBehind {
                drawLine(
                    color = navBarLine,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            // 内容避开手势条：把这一行放到最后，只垫内容、不影响上面的背景
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 收紧上下留白：原来上下各 8dp，加上 item 自身的 4dp 与文字行高留白，
                // 实测文字下方空了 35dp（内容只有 18dp），底栏显得很空、占屏 10.6%。
                // 现在上 5dp / 下 3dp，配合 item 内边距与行高一起收，底栏降到 ~68dp。
                .padding(top = 5.dp, bottom = 3.dp),
            // 两格时用 SpaceEvenly（间距均匀、不至于贴到屏幕两端）；五格时 SpaceAround
            horizontalArrangement = if (loggedIn) Arrangement.SpaceAround else Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            XhsNavItem(R.drawable.ic_nav_home, "首页", onClick = onHome)
            // 未登录只留「首页」+「我的」两格：搜索/发帖/提醒本来就是登录后才有意义的功能，
            // 与其摆着点了弹提示，不如不出现（用户的设想）。
            if (loggedIn) {
                XhsNavItem(R.drawable.ic_nav_search, "搜索", onClick = onSearch)
                XhsNavItem(R.drawable.ic_nav_compose, "发帖", onClick = onCompose)
                XhsNavItem(
                    R.drawable.ic_nav_notice, "提醒",
                    badgeCount = unreadCount,
                    onClick = onReminder
                )
            }
            XhsNavItem(R.drawable.ic_nav_me, "我的", onClick = onMine)
        }
    }
}

@Composable
fun XhsNavItem(
    iconRes: Int,
    label: String,
    badgeCount: Int = 0,
    onClick: () -> Unit = {}
) {
    // 所有图标默认同一中性色；只有「提醒」在真有未读时变强调色（用户要求：仅提醒会翘动状态）
    val hasUnread = badgeCount > 0
    val tint = if (hasUnread) AccentPink else TextSub
    // 细节 03：底栏按压 morph 的状态
    val navPressSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val navPressed by navPressSource.collectIsPressedAsState()
    val navScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (navPressed) 1.15f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.5f,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "navScale"
    )
    val navCtx = androidx.compose.ui.platform.LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            // 细节 03：底栏按压 morph —— 按压缩放 + 弹性回弹 + SELECTION 震
            .graphicsLayer {
                scaleX = navScale
                scaleY = navScale
            }
            .clickable(
                interactionSource = navPressSource,
                indication = androidx.compose.material.ripple.rememberRipple()
            ) {
                tickHaptic(navCtx)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(23.dp)
            )
            if (hasUnread) {
                // 未读徽标：红色小胶囊 + 数字（超过 99 显示 99+）
                val badgeText = if (badgeCount > 99) "99+" else badgeCount.toString()
                Box(
                    modifier = Modifier
                        .offset(x = 9.dp, y = (-5).dp)
                        .height(15.dp)
                        .background(HeartRed, RoundedCornerShape(99.dp))
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 9.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            // 行高显式收紧：11sp 的默认行高会上下各留 3~4dp 空白，
            // 那部分空白直接变成底栏的"空"，也是底栏显高的元凶之一
            lineHeight = 12.sp,
            fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal
        )
    }
}


// 首字母头像的渐变色（和卡片调色板一致），同样提到顶层常量
private val AvatarGradients: List<List<Color>> = listOf(
    listOf(Color(0xFFFF9A9E), Color(0xFFFAD0C4)),
    listOf(Color(0xFFA1C4FD), Color(0xFFC2E9FB)),
    listOf(Color(0xFF84FAB0), Color(0xFF8FD3F4)),
    listOf(Color(0xFFFCCB90), Color(0xFFD57EEB)),
    listOf(Color(0xFFA6C0FE), Color(0xFFF68084)),
    listOf(Color(0xFFD4FC79), Color(0xFF96E6A1)),
    listOf(Color(0xFFE0C3FC), Color(0xFF8EC5FC)),
    listOf(Color(0xFFF6D365), Color(0xFFFDA085))
)

fun getAvatarGradient(seed: String): List<Color> =
    AvatarGradients[Math.floorMod(seed.hashCode(), AvatarGradients.size)]


// ================= 长按操作菜单 =================
@Composable
fun MopMenuDialog(
    username: String,
    onDismiss: () -> Unit,
    onPrivateMessage: () -> Unit,
    onReport: () -> Unit,
    onMop: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(GlassDialog)
                .border(1.dp, GlassDialogLine, RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            // 头部
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(username, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("选择操作", color = TextSub, fontSize = 11.sp)
            }
            Divider(color = GlassDialogLine)
            Spacer(modifier = Modifier.height(4.dp))

            // 私信
            MopMenuItem(
                icon = Icons.Default.Email,
                label = "发送私信",
                tint = TextSub,
                labelColor = TextMain,
                onClick = onPrivateMessage
            )
            // 举报
            MopMenuItem(
                icon = Icons.Default.Info,
                label = "举报",
                tint = TextSub,
                labelColor = TextMain,
                onClick = onReport
            )
            // 塞抹布
            MopMenuItem(
                icon = Icons.Default.Warning,
                label = "塞抹布",
                tint = Color(0xFFFF6B81),
                labelColor = Color(0xFFFF6B81),
                onClick = onMop
            )
        }
    }
}

@Composable
fun MopMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    labelColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = labelColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

/** 撤销条数据：message 提示文案，actionLabel 固定「撤销」，onUndo 点撤销时回调 */
private data class UndoBarData(
    val message: String,
    val actionLabel: String,
    val onUndo: () -> Unit
)
