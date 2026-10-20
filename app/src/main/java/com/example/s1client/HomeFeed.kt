package com.example.s1client

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.example.s1client.ui.TicketPullView
import com.example.s1client.ui.ticketPaletteFor
import com.example.s1client.ui.CameraLeverView
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
import androidx.compose.material.icons.outlined.PhotoCamera
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
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
import com.example.s1client.ui.design.AccentPink
import com.example.s1client.ui.design.S1Radius
import com.example.s1client.ui.design.BgColor
import com.example.s1client.ui.design.BgColorBottom
import com.example.s1client.ui.design.CardBg
import com.example.s1client.ui.design.ChipBg
import com.example.s1client.ui.design.FlowCoverColors
import com.example.s1client.ui.design.GlassBar
import com.example.s1client.ui.design.FeedGap
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

// ================= 瀑布流信息流（分页/绳子刷新/骨架屏） =================
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
    /** 头像独立加载（三档策略算好的布尔；false=字母头像兜底不联网） */
    avatarAutoLoad: Boolean,
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
                                .padding(top = TopBarHeight, bottom = 3.dp)
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
                            horizontalArrangement = Arrangement.spacedBy(21.dp),
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
                                        .padding(horizontal = 13.dp, vertical = 5.dp)
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
                                            .padding(horizontal = 13.dp, vertical = 5.dp)
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
                        // 滚轮呼出时物理禁滚（模态）：事件即使泄漏到列表也无视觉后果
                        userScrollEnabled = !showPagePicker,
                        // 列缝底色：机器猫用中性灰把红蓝切开，其余主题默认同页面底
                        modifier = Modifier.fillMaxSize().background(FeedGap),
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
                                avatarAutoLoad = avatarAutoLoad,
                                read = post.tid in readTids,
                                onAvatarClick = { onAvatarClick(post) },
                                onMarkRead = { onMarkRead(post) },
                                onLongPress = { onLongPressPost(post) },
                                interactionsLocked = showPagePicker
                            )
                        }
                    }

                    // 下拉刷新（顶部右侧）：默认「抽票」实现。
                    // 方案可切换：PULL_IMPL = 0 抽票据 / 1 相机过片扳手（实验开关）/ 2 原绳子
                    // —— 三个实现接口一致，改这里一个数字即可切换。
                    // 2026-10-18 实验区上架（拍板 1）：相机扳手由设置页「实验功能」开关控制，
                    // 关闭（默认）= 0 走现状票夹，零行为变化。
                    val PULL_IMPL =
                        if (com.example.s1client.data.store.SettingStore.cameraLeverRefresh(context)) 1
                        else 0
                    // 绳子下拉刷新（顶部右侧）。
                    // 系统下拉刷新只在列表滚到顶时才生效，翻了几页后就拉不动了；
                    // 这根绳子是独立控件，任何时候都能拉下来 —— 拉到底 = 回到板块第一页并刷新。
                    val pullAlpha by animateFloatAsState(
                        targetValue = if (gridState.isScrollInProgress) 0f else 1f,
                        // 非对称节奏（FAB 惯例）：滚动手一停**不立刻**弹回，延迟 350ms 再
                        // 280ms 淡入——否则停顿瞬间票夹闪现，过渡生硬；淡出保持 120ms 快隐
                        animationSpec = if (gridState.isScrollInProgress)
                            tween(durationMillis = 120) else
                            tween(durationMillis = 280, delayMillis = 350)
                    )
                    // 实验功能（相机扳手 PULL_IMPL=1）隐现联动。
                    // 2026-10-18 设计者**推翻原拍板 5** 的隐现时序，新需求三条：
                    //   ① 首页静止时不显示；
                    //   ② 滚动越过视口 8% 阈值后才允许现身（口径对齐票夹 ticketRevealed
                    //      —— 2026-10-17 设计者定：常驻会压住右列第一张卡、抢触摸）；
                    //   ③ 停手后延迟 400ms 再浮现（原 200ms，设计者反馈偏急）。
                    // 因此去掉原「进场 600ms 后无条件现身一次」——那正是「首页静止时却显示」的来源。
                    // 与票夹的 ticketRevealed 独立——相机模式下两者互斥（key 重建，不会同屏）。
                    var leverRevealed by remember { mutableStateOf(false) }
                    LaunchedEffect(gridState, PULL_IMPL) {
                        if (PULL_IMPL != 1) return@LaunchedEffect
                        snapshotFlow { gridState.isScrollInProgress }
                            .distinctUntilChanged()
                            .collect { scrolling ->
                                if (scrolling) {
                                    leverRevealed = false
                                } else {
                                    // 停手 400ms 后再判断：滚过阈值才现身
                                    delay(400L)
                                    val reached = gridState.firstVisibleItemIndex > 0 ||
                                            gridState.firstVisibleItemScrollOffset >
                                            (0.08f * android.content.res.Resources.getSystem()
                                        .displayMetrics.heightPixels).toInt()
                                    leverRevealed = reached
                                }
                            }
                    }
                    // 快门刷新遮罩 alpha（相机扳手 PULL_IMPL=1；refreshing 权威驱动，
                    // 宪法 23.A.3）。轻量版（拍板 3 · 2026-10-18 设计者选"补个轻量版"）：
                    // HTML 原样 = 压暗 85% + blur 10px；去模糊、压暗降至 60%，
                    // 保留 📷 脉冲 + 「快门曝光中...」；300ms 淡入淡出（HTML 0.3s）。
                    // 白闪已按设计者 2026-10-18 反馈去掉（刺眼），快门瞬间改由
                    // 「扳手回位 + 相机退场 + 遮罩淡入」三段式表达。
                    val maskAlpha = remember { Animatable(0f) }
                    // R13 修复（审计报告 v2 2026-10-18 第 9 轮）：原来 refreshing 为 true 时
                    // 只 animateTo(1f) 没有任何退场兜底 —— 一旦 isRefreshing 因请求异常未复位
                    // （ViewModel 状态机漏一支 / 进程被打断），遮罩会永久停在 60% 压暗盖住整个首页，
                    // 且无取消入口。加 10s 上限强制退场：超时只退遮罩（视觉兜底），
                    // refreshing 状态本身不变，仍由状态机自行收敛。
                    // 注：10s 是防御性下限，只负责把遮罩退掉，不改 refreshing 状态本身。
                    val REFRESH_MASK_TIMEOUT_MS = 10_000L
                    LaunchedEffect(refreshing) {
                        if (refreshing) {
                            maskAlpha.animateTo(1f, tween(300))
                            delay(REFRESH_MASK_TIMEOUT_MS)
                            maskAlpha.animateTo(0f, tween(300))
                        } else {
                            maskAlpha.animateTo(0f, tween(300))
                        }
                    }
                    // 遮罩内容只在 >0.01 时参与组合（derivedStateOf：阈值翻转才重组，不跟帧）
                    val maskVisible by remember {
                        androidx.compose.runtime.derivedStateOf { maskAlpha.value > 0.01f }
                    }
                    // 📷 图标脉冲（HTML rmPulse 1s：scale 1↔1.15 + alpha 0.8↔1）
                    val shutterPulse = rememberInfiniteTransition(label = "shutterPulse")
                    val pulseScale by shutterPulse.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse
                        ),
                        label = "pulseScale"
                    )
                    val pulseAlpha by shutterPulse.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )

                    // 桥接戏票档浓票纸（宪法20）：在 composable 上下文算好 palette，再喂给 update lambda
                    val s1PullPalette = ticketPaletteFor(LocalS1Colors.current)
                    // 2026-10-17（用户反馈）：票夹默认隐藏——常驻票夹在首页首屏压住右列
                    // 第一张卡、抢走下方触摸且观感差。改为「滚动超过视口 ~8% 才现身」
                    // （参考绳子下拉 ~8% 阈值；derivedStateOf 只在布尔翻转时重组）。
                    // 现身后的触摸拦截由 TicketPullView.hitTest 收紧（不再下探 180dp）。
                    val ticketRevealed by remember {
                        androidx.compose.runtime.derivedStateOf {
                            gridState.firstVisibleItemIndex > 0 ||
                                    gridState.firstVisibleItemScrollOffset >
                                    (0.08f * android.content.res.Resources.getSystem()
                                        .displayMetrics.heightPixels).toInt()
                        }
                    }
                    // P4 首次提示的本地记忆已随提示一起删掉（2026-10-19）
                    // key(开关)：AndroidView 的 factory 只跑一次，切换 PULL_IMPL 换的是
                    // 不同 View 类型，必须靠 key 强制重建实例（拍板 1：设置改秒切）。
                    androidx.compose.runtime.key(PULL_IMPL) {
                    AndroidView(
                        factory = { ctx ->
                            val doRefresh = onRefresh
                            when (PULL_IMPL) {
                                0 -> TicketPullView(ctx).apply {
                                    alpha = TicketPullView.IDLE_ALPHA
                                    this.onPullRatio = { pullRatio = it }
                                    this.onRefresh = {
                                        scope.launch { gridState.animateScrollToItem(0) }
                                        doRefresh()
                                    }
                                }
                                // 1 = 相机过片扳手（实验开关，HTML 原型移植，2026-10-18 设计者直派）
                                1 -> CameraLeverView(ctx).apply {
                                    this.onRefresh = {
                                        // 同绳子语义：刷新必须回到第一页，并把列表滚回顶部，
                                        // 否则数据换了但视口还停在半空中
                                        scope.launch { gridState.animateScrollToItem(0) }
                                        doRefresh()
                                    }
                                }
                                // 2 = 原绳子
                                else -> RopeRefreshView(ctx).apply {
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
                            }
                        },
                        update = { view ->
                            // 2026-10-17：票夹改为「滚动超过视口 ~8% 才现身」——默认位完全让路
                            // 给卡片（不画、不拦截触摸）；现身后的滚动淡出仍走 pullAlpha。
                            // INVISIBLE = 不绘制且不参与触摸，彻底解决「下方帖子难点进」。
                            // 相机扳手例外（2026-10-18）：隐现动画在 View 内 alpha/位移自理，
                            // 走 INVISIBLE 会在首页顶部把它永久藏掉（ticketRevealed 仅票夹语义）。
                            view.visibility = when {
                                view is CameraLeverView -> android.view.View.VISIBLE
                                ticketRevealed -> android.view.View.VISIBLE
                                else -> android.view.View.INVISIBLE
                            }
                            // 相机扳手分支：revealed（滚动隐现）与 refreshing（曝光中）权威回写
                            //（宪法 23.A.3：页面状态驱动控件，控件不自猜）
                            (view as? CameraLeverView)?.let { v ->
                                // 曝光期间相机退场（HTML：遮罩出场时组件隐藏），
                                // 刷新结束随 leverRevealed 重浮现——扳手回位→相机退场→遮罩→回归
                                v.setRevealed(leverRevealed && !refreshing)
                                v.setRefreshing(refreshing)
                            }
                            // 桥接戏票档浓票纸（宪法20）：每次重组按当前主题注入 palette
                            (view as? TicketPullView)?.let { v ->
                                // 刷新态由 HomeFeed 状态权威驱动（2026-10-16 根因修复：
                                // 旧版从不回写，控件内部 refreshing 触发一次后永远 true，
                                // 票面文字被条码顶掉 = 用户看到的"只显示一次就隐藏"）
                                v.refreshing = refreshing
                                val pal = s1PullPalette
                                v.applyPalette(
                                    pal.paperInt(),
                                    intArrayOf(pal.glowInt(0), pal.glowInt(1), pal.glowInt(2), pal.glowInt(3)),
                                    pal.onTicketInt(),
                                    pal.accentInt()
                                )
                            }
                        },
                        modifier = if (PULL_IMPL == 1)
                            // 相机扳手（2026-10-19）：挂载点镜像到左缘（用户指定），版面零改动——
                            // feed contentPadding 不变、卡片全宽，浮层行为与原右缘版完全一致（滚到顶浮现、滚动隐藏）。
                            // 下移 26% 避开侧边返回手势区的做法保留（左缘同理）。
                            // 行程仍为 D6（-30°→+60°）+ P1~P5 手感增强。
                            //
                            // 2026-10-20 方案 B：start 30→10dp。旧画布左边空 13vb，
                            // 导致机身左缘实际落在 41.7dp、比卡片边距远 31.7dp，像块贴纸。
                            // 控件画布已收紧到图形外框（VB 93×82 / VIEW_W 78dp），
                            // 现在机身左缘 ≈10dp，与卡片 contentPadding start 对齐。
                            //
                            // 同日：top 26% → 23%。26% 时相机整体骑在第二张卡顶边上
                            // （实测机身底缘 310.7dp = 左列卡缝 310.7dp，分毫不差）。
                            // 上移 23dp 后整机落进第一张卡内部，不再骑缝。
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(top = (LocalConfiguration.current.screenHeightDp * 0.23f).dp, start = 10.dp)
                                .size(width = CameraLeverView.VIEW_W_DP.dp, height = CameraLeverView.VIEW_H_DP.dp)
                                .zIndex(2f)
                        else
                            // 票夹（现状）：水平定位链 end 2dp + 控件内 clipRightGap 8dp =
                            // 票纸右缘距屏 10dp，与瀑布流 contentPadding end 严格对齐（外框贴合）
                            Modifier
                                .align(Alignment.TopEnd)
                                // 宽度必须包住整张票（116dp）+ 右对齐余量：旧版 96dp 窄容器
                                // 让票纸左右各溢出 10dp，alpha<1 时被离屏层按边界裁切
                                // = "淡入先部分出现、回 1 才整体弹出"
                                .padding(top = TopBarHeight, end = 2.dp)
                                .size(width = 140.dp, height = 300.dp)
                                .zIndex(2f)
                                // 滚动时整体淡出（FAB 行为）：解决"固定在右上压住卡片/与内容混叠"
                                .alpha(pullAlpha)
                    )
                    // P4 首次提示「下拉过片」已删（2026-10-19 用户要求）：
                    // 实验功能的常驻提示语太吵，扳手本身已经够明显，不再教。
                    } // key(PULL_IMPL) 结束

                    // 实验功能：快门刷新遮罩 overlay（相机扳手 PULL_IMPL=1 · refreshing 驱动；
                    // 无点击语义不拦截触摸；alpha=0 时不可见。宪法 16：用户主动低频操作，
                    // 属 L2 级反馈。轻量版（拍板 3）：压暗 60% 无 blur；白闪已按设计者
                    // 2026-10-18 反馈去除（刺眼）。色彩纪律：scrim 为固定深色拟物压暗层
                    // （宪法 18 固有色例外——语义恒深、不随主题翻转故无主题间差异）；
                    // 其上图标文字用固定浅白，保双主题可读（宪法 15 可读性优先）
                    if (PULL_IMPL == 1) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .zIndex(8f)
                                .background(Color(0xFF121214).copy(alpha = 0.6f * maskAlpha.value))
                        ) {
                            if (maskVisible) {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PhotoCamera,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.92f),
                                        modifier = Modifier
                                            .size(42.dp)
                                            .scale(if (maskVisible) pulseScale else 1f)
                                            .alpha(if (maskVisible) pulseAlpha else 0.9f)
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        text = "快门曝光中...",
                                        fontSize = 13.sp,
                                        letterSpacing = 2.sp,
                                        color = Color.White.copy(alpha = 0.92f)
                                    )
                                }
                            }
                        }
                    }

                    // 方案 01 移植版：底部 WheelJumpSheet（grab+状态 chip+停稳武装+长按 1s ring）
                    val s1ForSheet = LocalS1Colors.current
                    // AnimatedVisibility：退场 250ms 内遮罩仍拦截 → 关闭瞬间触摸不穿透瀑布流
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showPagePicker,
                        enter = androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(250)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        showPagePicker = false
                                    }
                                }
                        )
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showPagePicker,
                        enter = androidx.compose.animation.slideInVertically(
                            androidx.compose.animation.core.tween(320)
                        ) { it / 2 } + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.slideOutVertically(
                            androidx.compose.animation.core.tween(250)
                        ) { it / 2 } + androidx.compose.animation.fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { ctx ->
                                com.example.s1client.ui.WheelJumpSheetView(ctx).apply {
                                    onJump = { target ->
                                        showPagePicker = false
                                        if (target != state.currentPage) onJumpPage(target)
                                    }
                                    onRequestClose = { showPagePicker = false }
                                }
                            },
                            update = { v ->
                                v.ensureSetup(
                                    "跳转到页", 1,
                                    if (state.totalPages > 0) state.totalPages else 1,
                                    state.currentPage.coerceAtLeast(1),
                                    s1ForSheet.accent.toArgb(),
                                    s1ForSheet.accent.copy(alpha = 0.16f).toArgb()
                                )
                                v.setS1Colors(s1ForSheet)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
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
                                        onClick = { showPagePicker = true },
                                        onLongClick = {
                                            runCatching { com.example.s1client.util.Haptic.selection(context) }
                                            showPagePicker = true
                                        }
                                    )
                                    .padding(horizontal = 13.dp, vertical = 8.dp),
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
                                .padding(bottom = 13.dp),
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
                                .padding(bottom = 13.dp),
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

                // 跳页由 WheelJumpSheetView（方案 01 底部面板）承担
            }
        }
    }
}


// R8（审计报告 v2 2026-10-18 第 9 轮）：accentColorOf() 已删除 ——
// 全库 grep 无任何调用点，且唯一作用是把滚轮 accent 硬编码为 Linear 主题的蓝（0x295E6AD2）。
// 滚轮 accent 现统一由 setS1Colors() 从当前主题色板派生（见上方 ensureSetup 调用处），
// 该函数失去存在意义，删除以免后人误以为它是主题色来源。

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
                        .background(s1c.pillTint)
                )
            }
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 14.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(21.dp),
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
                                // 未选中的分类名同样不能用 textSub：顶栏是深色时它几乎看不见。
                                // 用 onHeader 降一点透明度，既保证可读，又和选中态拉开层次。
                                // ① 字色走令牌 onAccent（原写死白色）：深色幻彩板主色上是深字
                                //    （白字压亮青/亮品红仅 2.1~3.1:1）；其余主题 onAccent 仍为白，视觉不变
                                color = if (isSelected) s1c.onAccent else s1c.onHeader.copy(alpha = 0.78f),
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
                        .padding(start = 2.dp, end = 13.dp)
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
                    .padding(8.dp)
                    .background(brush, RoundedCornerShape(10.dp))
            )
            Column(modifier = Modifier.padding(start = 13.dp, end = 13.dp, top = 8.dp, bottom = 13.dp)) {
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

