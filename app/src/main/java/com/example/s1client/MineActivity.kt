package com.example.s1client

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.data.session.SessionRepository
import com.example.s1client.ui.AttendanceContent
import com.example.s1client.ui.ConfettiBurst
import com.example.s1client.ui.S1SystemBarSync
import com.example.s1client.ReadHistoryActivity
import com.example.s1client.data.store.ReadHistoryStore
import com.example.s1client.ui.ThemeSwatch
import com.example.s1client.ui.showUserCardDialog
import com.example.s1client.ui.theme.LocalS1Colors
import com.example.s1client.ui.theme.S1Theme
import com.example.s1client.data.store.AttendanceManager
import com.example.s1client.data.store.BlockedAuthorManager
import com.example.s1client.data.store.HiddenPostManager
import com.example.s1client.util.ReminderUnread
import com.example.s1client.util.S1Notify
import com.example.s1client.util.JoinDays
import com.example.s1client.util.ThemeManager
import com.example.s1client.util.applyS1Theme
import com.example.s1client.util.setupS1EdgeToEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MineActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applyS1Theme()
        super.onCreate(savedInstanceState)
        // 细节 02：页面转场触觉 —— 进「我的」SELECTION 一震
        com.example.s1client.util.Haptic.selection(this)
        setupS1EdgeToEdge()
        setContent {
            S1Theme {
                MineScreen(onFinish = { finish() })
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 「我的」页 · A 秩序玻璃版 · R5 层级化
//
// 结构（自上而下，一级只放高频）：
//   顶栏 → 身份卡(右侧按钮→二级弹窗) → 顶格(提醒/好友，带数字、**无图标**) →
//   浏览历史+签到 合并卡 → 内容组(帖子收藏/帖子/回复) →
//   屏蔽与隐藏(小黑屋/黑名单/抹布贴) → 加入日期
//
// R7 变更（用户拍板）：
//   ① 顶格定稿为「提醒 / 好友」两格、无图标、各带数字（宪法第 15 条：一容器一主角）；
//   ② 「提醒」的未读＝通知+私信合计，点击进 NoticeCenterActivity（本身已含私信轨）；
//   ③ 黑名单移出顶格，并入「屏蔽与隐藏」组（与论坛封禁、隐藏帖子同类）；
//   ④ 沿用 R6 的：删版块收藏（用户未做该功能）、删互动消息组（避免重复入口）。
//
// 二级 A：头像右侧弹窗 QuickSettingsSheet（主题/文字大小/字体/交互震动/省流）
// 二级 B：设置页 SettingsActivity（齿轮进入；外观组回归成全集，屏蔽组取消）
//
// 落地细节：25 stagger 入场 · 23 签到彩带 · 24 里程碑徽章(在 AttendanceContent 内) ·
//           28 私信未读呼吸点 · 齿轮 → SettingsActivity
//
// 图标铁律：全 App icon 只用单线条风格，颜色统一 strongIcon。
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun MineScreen(onFinish: () -> Unit) {
    val c = LocalS1Colors.current
    S1SystemBarSync()
    val context = LocalContext.current
    var showHiddenPostsDialog by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showQuickSheet by remember { mutableStateOf(false) }
    var confettiKey by remember { mutableIntStateOf(0) }

    var loggedIn by remember { mutableStateOf(ForumDataCache.isLoggedIn()) }
    // R9 修复（审计报告 v2 2026-10-18 第 9 轮）：登录态改 resume 复检，与 MainActivity
    // 的 homeLoggedIn（:366-368）保持同一机制。原来是组合期一次性快照 ——
    // 游客态进「我的」页 → 点头像（:274 登录入口）→ 登录成功 finish 返回后，
    // 本页仍停留在未登录态（无用户名/无签名/无三功能格/无签到），必须手动切走再切回才恢复。
    // MineActivity 用的是普通 startActivity，收不到 LoginActivity 的 setResult，
    // 所以只能在 resume 时复检 ForumDataCache（登录成功的唯一权威写点是 SessionRepository）。
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                loggedIn = ForumDataCache.isLoggedIn()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // ===== 每日签到状态 =====
    var signedToday by remember { mutableStateOf(AttendanceManager.isSignedToday(context)) }
    var attendanceStreak by remember { mutableStateOf(AttendanceManager.streak(context)) }
    var attendanceTotal by remember { mutableStateOf(AttendanceManager.total(context)) }
    var signingIn by remember { mutableStateOf(false) }

    // ===== 私信未读数（细节 28 呼吸点）=====
    var unreadCount by remember { mutableIntStateOf(0) }

    // ===== 三功能格（R6：替换原「楼层收藏/草稿/历史」数据条）=====
    // 私信 = 未读数（本地）· 好友 = profile 的 friends 字段（服务端）· 黑名单 = 本地已屏蔽数
    var friendCount by remember { mutableIntStateOf(0) }
    var blockedCount by remember { mutableIntStateOf(0) }

    // ===== 黑名单管理弹窗（R6：自设置页上移到一级页）=====
    var showBlockedAuthors by remember { mutableStateOf(false) }

    LaunchedEffect(loggedIn, refresh) {
        if (loggedIn) {
            unreadCount = runCatching { ReminderUnread.computeUnread(context) }.getOrDefault(0)
        } else {
            unreadCount = 0
        }
        blockedCount = runCatching { BlockedAuthorManager.allBlocked().size }.getOrDefault(0)
    }

    val username = remember {
        if (ForumDataCache.isLoggedIn()) ForumDataCache.memberUsername.ifBlank { "S1 用户" }
        else "未登录"
    }
    val uid = remember { ForumDataCache.memberUid }

    // 自动拉一次自己的 profile：缓存 regDate + customStatus，并取好友数（三功能格用）
    // R6 说明：原来只在 regDate/signature 为空时才拉；现在每次进页都拉一次，
    // 否则「好友」格的数字会一直是首次缓存值（加/删好友后不更新）。profile 请求很轻。
    LaunchedEffect(uid) {
        if (ForumDataCache.isLoggedIn()) {
            try {
                val resp = withContext(Dispatchers.IO) {
                    RetrofitClient.api.getProfile(uid = uid)
                }
                val variables = resp.variables
                val parsed = withContext(Dispatchers.IO) {
                    com.example.s1client.util.JsonParser.parseProfile(
                        variables?.space, variables?.extCredits, uid
                    )
                }
                friendCount = parsed.friends.trim().toIntOrNull() ?: 0
                // 头衔与签名分开存：customstatus 是头衔，sightml 才是签名（写点收敛进 SessionRepository）
                fun usable(raw: String): String {
                    val t = raw.trim()
                    return if (t.isEmpty() || t == "0" || t == "null" || t == "-") "" else t
                }
                SessionRepository.refreshProfile(
                    regDate = parsed.regDate,
                    groupTitle = usable(parsed.customStatus),
                    signature = usable(parsed.signature)
                )
                refresh++
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            // 卡片间距统一交给父布局：所有功能卡之间恒定 12dp，不再贴死也不再忽大忽小
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TopBar(
                onBack = onFinish,
                onGearClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }
            )

            // 细节 29（轻量版）：登录成功回到本页时，用户卡做一次"归位"弹簧入场
            var prevLoggedIn by remember { mutableStateOf(loggedIn) }
            val heroScale = remember { Animatable(1f) }
            LaunchedEffect(loggedIn) {
                if (loggedIn && !prevLoggedIn) {
                    heroScale.snapTo(1.12f)
                    heroScale.animateTo(
                        1f,
                        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
                    )
                }
                prevLoggedIn = loggedIn
            }

            StaggerIn(0) {
                Box(Modifier.graphicsLayer { scaleX = heroScale.value; scaleY = heroScale.value }) {
                    // 控件 10：幻彩主题下身份卡接管旋转流光边框；其他主题保持静态玻璃
                    FlowGlassCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp),
                        rotating = false,
                        soft = true
                    ) {
                        UserCard(
                            username = username, uid = uid,
                            groupTitle = if (loggedIn) ForumDataCache.groupTitle else "",
                            signature = if (loggedIn) ForumDataCache.memberSignature else "",
                            regDate = if (loggedIn) ForumDataCache.memberRegDate else "",
                            loggedIn = loggedIn,
                            onClick = {
                                if (loggedIn) {
                                    showUserCardDialog(context as ComponentActivity, uid, username)
                                } else {
                                    context.startActivity(Intent(context, LoginActivity::class.java))
                                }
                            },
                            trailing = {
                                // R5：头像右侧 = 二级弹窗入口（原 palette 主题直开，
                                // 升级为「快捷」面板：主题/字号/字体/震动/省流）
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(c.chipBg)
                                        .clickable { showQuickSheet = true },
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Outlined.Tune,
                                        contentDescription = "快捷",
                                        tint = c.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            if (loggedIn) {
                // —— 三功能格（R6：替换原数据条）——
                // 原「楼层收藏/草稿/历史」是纯数字、低频，占位不划算；
                // 换成高频的人际入口：私信（未读角标）/ 好友（好友数）/ 黑名单（已屏蔽数）。
                StaggerIn(1) {
                    // 控件 10：静态柔彩配角框（一页只留身份卡一个旋转）
                    FlowGlassCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp),
                        rotating = false,
                        soft = true
                    ) {
                        SocialTripod(
                            unreadCount = unreadCount,
                            friendCount = friendCount,
                            uid = uid,
                            username = username
                        )
                    }
                }

                // —— 主题 + 签到 合并玻璃卡（控件 10：静态柔彩；签到成功彩边快转一圈庆祝）——
                StaggerIn(2) {
                    FlowGlassCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp),
                        rotating = false,
                        soft = true,
                        pulseKey = confettiKey
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    context.startActivity(
                                        Intent(context, ReadHistoryActivity::class.java)
                                    )
                                }
                                .padding(horizontal = 13.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(c.chipBg),
                                contentAlignment = Alignment.Center
                            ) {
                                // P4：主题卡退役，换成浏览历史快捷入口（主题入口收敛到设置页）
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = "浏览历史",
                                    tint = c.accent,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("浏览历史", color = c.textMain, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    "最近看过的 ${ReadHistoryStore.list(context).size} 个帖子",
                                    color = c.textSub, fontSize = 11.5.sp
                                )
                            }
                            Chevron()
                        }

                        Box(
                            Modifier
                                .padding(horizontal = 13.dp)
                                .height(1.dp)
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.07f))
                        )

                        AttendanceContent(
                            loggedIn = loggedIn,
                            signedToday = signedToday,
                            streak = attendanceStreak,
                            total = attendanceTotal,
                            signingIn = signingIn,
                            onSign = {
                                signingIn = true
                                scope.launch {
                                    val r = AttendanceManager.sign(context)
                                    signingIn = false
                                    when (r.outcome) {
                                        AttendanceManager.Outcome.SIGNED_NOW -> {
                                            signedToday = true
                                            refresh++
                                            confettiKey++   // 细节 23：全屏彩带 + SUCCESS 组合震
                                            S1Notify.show(context, "签到成功", S1Notify.Type.SUCCESS, silent = true)
                                        }
                                        AttendanceManager.Outcome.ALREADY_SIGNED -> {
                                            signedToday = true
                                            S1Notify.show(context, "今日已签到", S1Notify.Type.INFO)
                                        }
                                        else -> S1Notify.show(
                                            context,
                                            r.message.ifBlank { "签到失败" },
                                            S1Notify.Type.ERROR
                                        )
                                    }
                                    attendanceStreak = AttendanceManager.streak(context)
                                    attendanceTotal = AttendanceManager.total(context)
                                }
                            }
                        )
                        }
                    }
                }

                // —— 内容组（R6：删掉「版块收藏」——用户未做该功能，瀑布流也不需要）——
                StaggerIn(3) {
                    // 清单组也纳入静态柔彩框（用户拍板：幻彩里所有功能模块统一）
                    FlowGlassCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp),
                        rotating = false,
                        soft = true
                    ) {
                    MenuGroup(
                        listOf(
                            MenuItem("我的收藏", Icons.Outlined.StarOutline) {
                                context.startActivity(Intent(context, FavoriteListActivity::class.java))
                            },
                            MenuItem("帖子", Icons.Outlined.Description) {
                                context.startActivity(Intent(context, UserThreadListActivity::class.java).apply {
                                    putExtra("uid", uid); putExtra("username", username)
                                })
                            },
                            MenuItem("回复", Icons.Outlined.Forum) {
                                context.startActivity(Intent(context, UserReplyListActivity::class.java).apply {
                                    putExtra("uid", uid); putExtra("username", username)
                                })
                            }
                        )
                    )
                    }
                }

                // —— 屏蔽与隐藏（R7：原「论坛查阅」改名 + 黑名单自顶格并入）——
                // 用户梳理的语义：小黑屋＝论坛封禁的用户（只读查阅）、
                // 黑名单＝我自己屏蔽的人（可增删）、抹布贴＝我隐藏的帖子（可恢复）。
                // 原组名「论坛查阅」有歧义（容易被理解成查帖子/收藏/发帖记录）。
                StaggerIn(4) {
                    FlowGlassCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp),
                        rotating = false,
                        soft = true
                    ) {
                    MenuGroup(
                        listOf(
                            // v18 新增的小黑屋入口（论坛公开处罚列表）—— 重建清单时补回
                            MenuItem("小黑屋", Icons.Outlined.Info) {
                                if (loggedIn) {
                                    context.startActivity(Intent(context, DarkRoomActivity::class.java))
                                } else {
                                    S1Notify.show(context, "请先登录后查看小黑屋", S1Notify.Type.INFO)
                                }
                            },
                            MenuItem(
                                "黑名单", Icons.Outlined.Block,
                                value = if (blockedCount > 0) "$blockedCount" else null
                            ) { showBlockedAuthors = true },
                            MenuItem("抹布贴", Icons.Outlined.VisibilityOff) { showHiddenPostsDialog = true }
                        )
                    )
                    }
                }
            }

        }

        // —— 细节 23：签到成功全屏彩带 ——
        if (confettiKey > 0) {
            Box(Modifier.matchParentSize().zIndex(5f)) {
                ConfettiBurst(trigger = confettiKey, onDone = { confettiKey = 0 })
            }
        }
    }

    // R5 层级化：二级 A 弹窗（快捷）优先渲染；主题面板由弹窗内跳转（避免嵌套 ModalBottomSheet）
    if (showQuickSheet) {
        com.example.s1client.ui.QuickSettingsSheet(
            onDismiss = { showQuickSheet = false },
            onOpenThemePicker = {
                showQuickSheet = false
                showThemeSheet = true
            },
            onOpenSettings = {
                showQuickSheet = false
                context.startActivity(Intent(context, SettingsActivity::class.java))
            }
        )
    }
    if (showThemeSheet) {
        com.example.s1client.ui.ThemePickerSheet(onDismiss = { showThemeSheet = false })
    }
    if (showHiddenPostsDialog) {
        HiddenPostsDialog(onDismiss = { showHiddenPostsDialog = false })
    }
    // R6：黑名单管理自设置页上移到一级页三功能格（复用 HomeDialogs 的现成弹窗）
    if (showBlockedAuthors) {
        BlockedAuthorsDialog(
            blocked = BlockedAuthorManager.allBlocked(),
            onDismiss = { showBlockedAuthors = false },
            onUnblock = { blockedUid ->
                BlockedAuthorManager.unblock(blockedUid)
                blockedCount = BlockedAuthorManager.allBlocked().size
                S1Notify.show(context, "已取消屏蔽", S1Notify.Type.SUCCESS)
            },
            onClearAll = {
                BlockedAuthorManager.allBlocked().keys.toList()
                    .forEach { BlockedAuthorManager.unblock(it) }
                blockedCount = 0
                S1Notify.show(context, "已清空黑名单", S1Notify.Type.SUCCESS)
            }
        )
    }
}

// ————————— 控件 10 包装：幻彩主题用旋转流光边框卡，其他主题用静态玻璃卡 —————————

@Composable
fun FlowGlassCard(
    modifier: Modifier = Modifier,
    rotating: Boolean = true,
    soft: Boolean = false,
    pulseKey: Int = 0,
    content: @Composable () -> Unit
) {
    if (ThemeManager.currentTheme == ThemeManager.THEME_IRIDESCENT) {
        // 幻彩「彩色亮丝」卡框（2026-10-15 用户定稿，替代原旋转流光环）：
        // 与瀑布流卡框同一套语法、同一粗细 —— 1.5dp 主题色板渐变描边
        // （s1RingColors 四色，左上→右下，0.88）+ 贴边 4dp@0.10 同色柔光；
        // 五张卡一致对待，无主次分离（用户：「不走主次分离，和其他边框统一」）。
        // 描边画在边界内侧（Canvas 覆盖层不参与测量），内容层与常规主题尺寸完全一致。
        // rotating / soft 参数保留以兼容调用点，已不参与绘制；
        // pulseKey 保留 = 签到成功时彩丝提亮加粗一格（0.6s）。
        val c = LocalS1Colors.current
        val band = remember(c) { com.example.s1client.ui.theme.s1RingColors(c) }
        val pulse = remember { Animatable(0f) }
        LaunchedEffect(pulseKey) {
            if (pulseKey > 0) {
                pulse.snapTo(1f)
                pulse.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
            }
        }
        val corner = 16.dp
        Box(modifier = modifier) {
            // 内容层：clip + 玻璃底，与常规主题同一布局尺寸
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(corner))
                    .background(c.glassCard)
            ) { content() }
            // 边框层：铺满外层 Box（matchParentSize 不参与测量），画在最上层
            Canvas(Modifier.matchParentSize()) {
                val rr = corner.toPx()
                // 柔光：贴边低透明度宽描边，让彩丝有「亮」的晕染
                val glowPx = 4.dp.toPx()
                val glowInset = glowPx / 2f
                drawRoundRect(
                    brush = Brush.linearGradient(
                        band.map { it.copy(alpha = 0.10f + pulse.value * 0.10f) },
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    ),
                    topLeft = Offset(glowInset, glowInset),
                    size = Size(size.width - glowPx, size.height - glowPx),
                    cornerRadius = CornerRadius((rr - glowInset).coerceAtLeast(0f)),
                    style = Stroke(width = glowPx)
                )
                // 彩丝本体：1.5dp@0.88（签到脉冲时短暂加粗提亮）
                val strokePx = (1.5.dp * (1f + pulse.value)).toPx()
                val inset = strokePx / 2f
                drawRoundRect(
                    brush = Brush.linearGradient(
                        band.map { it.copy(alpha = 0.88f + pulse.value * 0.12f) },
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    ),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - strokePx, size.height - strokePx),
                    cornerRadius = CornerRadius((rr - inset).coerceAtLeast(0f)),
                    style = Stroke(width = strokePx)
                )
            }
        }
    } else {
        Box(
            modifier
                .clip(RoundedCornerShape(16.dp))
                .background(LocalS1Colors.current.glassCard)
        ) { content() }
    }
}

// ————————— 细节 25：stagger 入场（每级延迟 60ms 上浮淡入）—————————

@Composable
private fun StaggerIn(index: Int, content: @Composable () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val ty = remember { Animatable(16f) }
    LaunchedEffect(Unit) {
        delay(index * 60L)
        launch { alpha.animateTo(1f, tween(320)) }
        launch { ty.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)) }
    }
    Box(
        Modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = ty.value
        }
    ) { content() }
}

// ————————— 顶格：提醒 / 好友（各带数字，无图标）———————

/**
 * 顶格（R7 定稿）：提醒 / 好友，取代原「楼层收藏 / 草稿 / 历史」数据条。
 *
 * 用户拍板：
 *   ① 那三个是纯数字的低频入口，占位不划算，换成真正会点的人际入口；
 *   ② 每格都带数字 —— 提醒＝未读数（通知+私信合计）、好友＝好友数；
 *   ③ 顶格用「方案 A」：**无图标**，数字是唯一主角（宪法第 15 条：一容器一主角）；
 *   ④ 黑名单移出顶格，并入下面的「屏蔽与隐藏」组（关联度更高）。
 */
@Composable
private fun SocialTripod(
    unreadCount: Int,
    friendCount: Int,
    uid: String,
    username: String
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 提醒：走提醒中心（本身已包含私信轨），未读＝通知+私信合计
        TripodCell(
            modifier = Modifier.weight(1f),
            count = unreadCount,
            label = "提醒",
            showDot = unreadCount > 0
        ) { context.startActivity(Intent(context, NoticeCenterActivity::class.java)) }

        TripodCell(
            modifier = Modifier.weight(1f),
            count = friendCount,
            label = "好友"
        ) {
            context.startActivity(Intent(context, FriendListActivity::class.java).apply {
                putExtra("uid", uid); putExtra("username", username)
            })
        }
    }
}

/**
 * 顶格里的单格：**数字 → 标签**，无图标。
 *
 * 比例（宪法第 15 条）：数字 21sp : 标签 13sp ≈ 1.615 ≈ φ；
 * 间距 5dp、上下内边距 13dp —— 斐波那契。
 *
 * [modifier] 由调用方传 `Modifier.weight(1f)`：weight 只在 RowScope 里解析，
 * 提取成独立函数后拿不到外层 Row 的作用域，不能在函数体内写死。
 */
@Composable
private fun TripodCell(
    modifier: Modifier = Modifier,
    count: Int,
    label: String,
    showDot: Boolean = false,
    onClick: () -> Unit
) {
    val c = LocalS1Colors.current
    // 横向版式：标签在左、数字在右。原来的"上数字下标签"约占 60dp，
    // 改成单行后约 43dp，为「我的」页一屏全显腾出空间（数字仍是主角，21sp 不变）。
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(c.glassCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = c.textSub)
        Spacer(Modifier.width(5.dp))
        Box(contentAlignment = Alignment.CenterEnd) {
            Text(
                "$count",
                fontSize = 21.sp, lineHeight = 21.sp,
                fontWeight = FontWeight.Bold, color = c.textMain
            )
            // 细节 28：有未读时数字右上角亮呼吸点
            if (showDot) {
                BreathingDot(Modifier.align(Alignment.TopEnd).offset(x = 7.dp, y = (-3).dp))
            }
        }
    }
}


// ————————— 菜单组：单色单线图标 + 分隔线 —————————

private data class MenuItem(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    val iconRes: Int? = null,
    val badge: Int = 0,
    /** 行尾数值（如收藏数），与 badge 呼吸点互不排斥 */
    val value: String? = null,
    val onClick: () -> Unit
)

@Composable
private fun MenuGroup(items: List<MenuItem>) {
    val c = LocalS1Colors.current
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEachIndexed { i, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = item.onClick)
                    .padding(horizontal = 13.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(c.chipBg),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.icon != null) {
                            Icon(
                                imageVector = item.icon, contentDescription = null,
                                tint = c.strongIcon, modifier = Modifier.size(15.dp)
                            )
                        } else if (item.iconRes != null) {
                            Icon(
                                painter = painterResource(item.iconRes), contentDescription = null,
                                tint = c.strongIcon, modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    // 细节 28：私信未读呼吸点
                    if (item.badge > 0) {
                        BreathingDot(Modifier.align(Alignment.TopEnd))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(item.name, fontSize = 13.5.sp, color = c.textMain, modifier = Modifier.weight(1f))
                if (!item.value.isNullOrBlank()) {
                    Text(item.value, fontSize = 12.sp, color = c.textSub)
                    Spacer(Modifier.width(6.dp))
                }
                Chevron()
            }
            if (i < items.lastIndex) {
                Box(
                    Modifier
                        .padding(horizontal = 13.dp)
                        .height(1.dp)
                        .fillMaxWidth()
                        .background(c.chipBg)
                )
            }
        }
    }
}

/** 呼吸圆点：scale 0.85→1.25 循环（细节 28） */
@Composable
private fun BreathingDot(modifier: Modifier = Modifier) {
    val c = LocalS1Colors.current
    val transition = rememberInfiniteTransition(label = "breath")
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse"
    )
    Box(
        modifier
            .size(9.dp)
            .graphicsLayer { scaleX = pulse; scaleY = pulse }
            .background(c.heart, CircleShape)
    )
}

@Composable
private fun Chevron() {
    val c = LocalS1Colors.current
    Icon(
        painter = painterResource(R.drawable.ic_chevron_right),
        contentDescription = null,
        tint = c.textSub.copy(alpha = 0.6f),
        modifier = Modifier.size(13.dp)
    )
}


// ————————— 顶栏（渐变 + 白字/图标） —————————

@Composable
private fun TopBar(onBack: () -> Unit, onGearClick: () -> Unit) {
    val c = LocalS1Colors.current
    // 顶栏背景 headerGradient 在浅色主题下本身是浅色系，文字必须用深色；
    // 原来写死"浅色主题用白字"会导致标题与返回箭头在浅底上彻底看不见。
    // 这里统一跟随主题文字色，背景深浅两种情况都不会失配。
    // 与首页顶栏**完全同构**：同一个 TopBarHeight 常量、内容垂直居中、左右对称。
    // 原来这里写死 padding(top = 48.dp) 且 start/end 不对称（3dp/8dp），
    // 而首页是 height(TopBarHeight) —— 两套算法导致侧滑切换时顶栏错位。
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(c.headerGradient))
            .statusBarsPadding()
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TopBarHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null,
                tint = c.textMain,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            "我的", fontSize = 17.sp, fontWeight = FontWeight.Bold,
            color = c.textMain,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onGearClick) {
            Icon(
                painterResource(R.drawable.ic_settings), contentDescription = null,
                tint = c.textMain,
                modifier = Modifier.size(22.dp)
            )
        }
    }
    }
}

// ————————— 用户卡（大头像 + 签名） —————————

@Composable
private fun UserCard(
    username: String, uid: String, groupTitle: String,
    signature: String, regDate: String,
    loggedIn: Boolean,
    onClick: () -> Unit,
    trailing: (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)? = null
) {
    val c = LocalS1Colors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 13.dp, vertical = 9.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(c.glassCard)
            .padding(11.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(c.accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (loggedIn && uid.isNotEmpty()) {
                    val uidInt = uid.toIntOrNull() ?: 0
                    if (uidInt > 0) {
                        val padded = String.format("%09d", uidInt)
                        val url = "https://avatar.stage1st.com/" +
                                "${padded.substring(0, 3)}/${padded.substring(3, 5)}/" +
                                "${padded.substring(5, 7)}/${padded.substring(7, 9)}_avatar_middle.jpg"
                        AsyncImage(
                            model = url, contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(13.dp))
                        )
                    } else {
                        Text(
                            username.firstOrNull()?.toString()?.uppercase() ?: "?",
                            color = c.accent, fontSize = 22.sp, fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        username.firstOrNull()?.toString()?.uppercase() ?: "?",
                        color = c.accent, fontSize = 22.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f).padding(end = 30.dp)) {

                // ——— 第一组：名字与 UID 同行（名字是主角、编号是附属）———
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(username, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.textMain,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false))
                    if (loggedIn && uid.isNotBlank()) {
                        Text(
                            uid, fontSize = 11.sp, color = c.textSub,
                            modifier = Modifier.padding(start = 7.dp), maxLines = 1
                        )
                    } else if (!loggedIn) {
                        Text(
                            "点击登录", fontSize = 11.sp, color = c.textSub,
                            modifier = Modifier.padding(start = 7.dp)
                        )
                    }
                }

                val sigText = signature.trim()
                val titleText = groupTitle.trim()

                // ——— 第二组：头衔（标签·弱）与签名（正文·重）同行 ———
                if (titleText.isNotEmpty() || sigText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(top = 9.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(c.chipBg)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 9.dp)
                    ) {
                        if (titleText.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(c.accent.copy(alpha = 0.13f))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    titleText, fontSize = 10.5.sp, color = c.accent,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (sigText.isNotEmpty()) {
                            Text(
                                sigText, fontSize = 13.sp, color = c.textMain.copy(alpha = 0.88f),
                                lineHeight = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = if (titleText.isNotEmpty()) 8.dp else 0.dp)
                            )
                        }
                    }
                }

                // —— 欢迎语：移到身份卡内、压在头衔与签名之下 ——
                // 发光只做"呼吸式明暗"。**不用 Modifier.shadow** —— 它会在文字后面
                // 画出一块可见的光晕矩形，浅色底上尤其脏（用户实测反馈"背景怪异"）。
                val days = JoinDays.daysSince(regDate) ?: 0
                if (loggedIn && days > 0) {
                    val glow = rememberInfiniteTransition(label = "joinDaysGlow")
                    val glowA by glow.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
                        label = "joinDaysAlpha"
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            "欢迎加入 stage1",
                            fontSize = 11.sp, color = c.textSub.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "$days",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = c.accent.copy(alpha = glowA)
                        )
                        Text(
                            " 天",
                            fontSize = 11.sp, color = c.textSub.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            trailing?.invoke(this)
        }
    }
}

// ————————— 抹布贴：查看被「隐藏该帖」隐藏的帖子 —————————

@Composable
private fun HiddenPostsDialog(onDismiss: () -> Unit) {
    val c = LocalS1Colors.current
    var hidden by remember { mutableStateOf(HiddenPostManager.allHidden()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("抹布贴（${hidden.size}）", color = c.textMain) },
        text = {
            if (hidden.isEmpty()) {
                Text("还没有被隐藏的帖子", color = c.textSub, fontSize = 14.sp)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(hidden.size) { index ->
                        val item = hidden[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = item.title,
                                    color = c.textMain,
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.author.isNotBlank() || item.time.isNotBlank()) {
                                    Text(
                                        text = listOfNotNull(
                                            item.author.takeIf { it.isNotBlank() },
                                            item.time.takeIf { it.isNotBlank() }
                                        ).joinToString(" · "),
                                        color = c.textSub,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text(
                                text = "恢复",
                                color = c.accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .clickable {
                                        HiddenPostManager.unhide(item.tid)
                                        hidden = HiddenPostManager.allHidden()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (hidden.isNotEmpty()) {
                TextButton(onClick = {
                    HiddenPostManager.unhideAll()
                    hidden = emptyList()
                }) {
                    Text("全部恢复", color = Color(0xFFFF3B30))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭", color = c.textSub) }
        }
    )
}

