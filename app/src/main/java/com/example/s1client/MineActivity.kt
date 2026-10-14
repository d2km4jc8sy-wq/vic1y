package com.example.s1client

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
//   顶栏 → 身份卡(右侧按钮→二级弹窗) → 三功能格(私信/好友/黑名单，均带数字) →
//   浏览历史+签到 合并卡 → 内容组(帖子收藏/帖子/回复) →
//   论坛查阅(小黑屋/抹布贴) → 加入日期
//
// R6 变更（用户拍板）：
//   ① 原数据条（楼层收藏/草稿/历史）是低频纯数字入口，换成三功能格，各带数量；
//   ② 删「版块收藏」——用户未做该功能、瀑布流也不需要，整条数据链路一并移除；
//   ③ 删「互动消息」组——私信/好友已上移到三功能格，此处不再重复；
//   ④ 黑名单管理自设置页上移到三功能格。
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
                if (parsed.regDate.isNotBlank()) ForumDataCache.memberRegDate = parsed.regDate
                if (parsed.customStatus.isNotBlank()) {
                    val raw = parsed.customStatus.trim()
                    if (raw != "0" && raw != "null" && raw != "-")
                        ForumDataCache.memberSignature = raw
                }
                ForumDataCache.saveUser()
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                        rotating = false,
                        soft = true
                    ) {
                        SocialTripod(
                            unreadCount = unreadCount,
                            friendCount = friendCount,
                            blockedCount = blockedCount,
                            uid = uid,
                            username = username,
                            onBlocked = { showBlockedAuthors = true }
                        )
                    }
                }

                // —— 主题 + 签到 合并玻璃卡（控件 10：静态柔彩；签到成功彩边快转一圈庆祝）——
                StaggerIn(2) {
                    FlowGlassCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
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
                                .padding(horizontal = 16.dp, vertical = 13.dp),
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
                                .padding(horizontal = 16.dp)
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                        rotating = false,
                        soft = true
                    ) {
                    MenuGroup(
                        listOf(
                            MenuItem("帖子收藏", Icons.Outlined.StarOutline) {
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

                // —— 论坛查阅（R5：自原社交组拆出，月活级低频）——
                // R6：「互动消息」组已取消——私信/好友上移到顶部三功能格，此处不再重复。
                StaggerIn(4) {
                    FlowGlassCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
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
                            MenuItem("抹布贴", Icons.Outlined.Block) { showHiddenPostsDialog = true }
                        )
                    )
                    }
                }
            }

            // 「欢迎加入 stage1 N 天」
            if (loggedIn) {
                val regDate = ForumDataCache.memberRegDate
                if (regDate.isNotBlank()) {
                    val welcome = com.example.s1client.util.JoinDays.welcomeText(regDate)
                    val line = welcome ?: run {
                        val m = Regex("""(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})""").find(regDate)
                        if (m != null)
                            "${m.groupValues[1]}年${m.groupValues[2].toInt()}月${m.groupValues[3].toInt()}日 加入 S1"
                        else regDate
                    }
                    Text(
                        line,
                        fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        color = c.textSub.copy(alpha = 0.85f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 22.dp, bottom = 28.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Spacer(Modifier.height(28.dp))
                }
            } else {
                Spacer(Modifier.height(28.dp))
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
        com.example.s1client.ui.ConicFlowBorderCard(
            modifier = modifier,
            cornerRadius = 16.dp,
            borderWidth = if (soft) 1.5.dp else 2.5.dp,
            rotating = rotating,
            borderAlpha = if (soft) 0.4f else 1f,
            pulseKey = pulseKey
        ) { content() }
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

// ————————— 三功能格：私信 / 好友 / 黑名单（各带数字）———————

/**
 * 三功能格（R6）：私信 / 好友 / 黑名单，取代原「楼层收藏 / 草稿 / 历史」数据条。
 *
 * 用户拍板：① 那三个是纯数字的低频入口，占位不划算，换成真正会点的人际入口；
 * ② 每格都带数字 —— 私信＝未读数、好友＝好友数、黑名单＝已屏蔽数。
 */
@Composable
private fun SocialTripod(
    unreadCount: Int,
    friendCount: Int,
    blockedCount: Int,
    uid: String,
    username: String,
    onBlocked: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TripodCell(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.MailOutline,
            count = unreadCount,
            label = "私信",
            showDot = unreadCount > 0
        ) { context.startActivity(Intent(context, PmListActivity::class.java)) }

        TripodCell(
            modifier = Modifier.weight(1f),
            iconRes = R.drawable.ic_account_circle,
            count = friendCount,
            label = "好友"
        ) {
            context.startActivity(Intent(context, FriendListActivity::class.java).apply {
                putExtra("uid", uid); putExtra("username", username)
            })
        }

        TripodCell(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Block,
            count = blockedCount,
            label = "黑名单",
            onClick = onBlocked
        )
    }
}

/**
 * 三功能格里的单格：图标（可带未读呼吸点）→ 数字 → 标签。
 *
 * [modifier] 由调用方传 `Modifier.weight(1f)`：weight 只能在 RowScope 里解析，
 * 所以不能在函数体内写死（提取成独立函数后就拿不到外层 Row 的作用域了）。
 */
@Composable
private fun TripodCell(
    modifier: Modifier = Modifier,
    count: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconRes: Int? = null,
    showDot: Boolean = false,
    onClick: () -> Unit
) {
    val c = LocalS1Colors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(c.glassCard)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(c.chipBg),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    androidx.compose.material3.Icon(
                        icon, contentDescription = null,
                        tint = c.strongIcon, modifier = Modifier.size(17.dp)
                    )
                } else if (iconRes != null) {
                    androidx.compose.material3.Icon(
                        painter = painterResource(iconRes), contentDescription = null,
                        tint = c.strongIcon, modifier = Modifier.size(17.dp)
                    )
                }
            }
            // 细节 28：私信有未读时亮呼吸点（与菜单行一致）
            if (showDot) {
                BreathingDot(Modifier.align(Alignment.TopEnd))
            }
        }
        Spacer(Modifier.height(5.dp))
        Text("$count", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.textMain)
        Text(label, fontSize = 10.5.sp, color = c.textSub)
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
                    .padding(horizontal = 16.dp, vertical = 11.dp),
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
                        .padding(horizontal = 16.dp)
                        .height(1.dp)
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.06f))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(c.headerGradient))
            .padding(top = 48.dp, bottom = 10.dp, start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null,
                tint = if (c.isDark) c.textMain else Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            "我的", fontSize = 17.sp, fontWeight = FontWeight.Bold,
            color = if (c.isDark) c.textMain else Color.White,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onGearClick) {
            Icon(
                painterResource(R.drawable.ic_settings), contentDescription = null,
                tint = if (c.isDark) c.textMain else Color.White,
                modifier = Modifier.size(22.dp)
            )
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
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(c.glassCard)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
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
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
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
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f).padding(end = 30.dp)) {
                Text(username, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.textMain)
                val sub = buildString {
                    if (loggedIn) {
                        append("UID: ").append(uid)
                        if (groupTitle.isNotBlank()) append("  ·  ").append(groupTitle)
                    } else append("点击登录")
                }
                Text(sub, fontSize = 11.sp, color = c.textSub, modifier = Modifier.padding(top = 3.dp))

                if (signature.isNotBlank()) {
                    val rawSig = signature.trim()
                    val sig = if (rawSig == "0" || rawSig == "null" || rawSig == "-") "" else rawSig
                    if (sig.isNotBlank()) {
                        Text(
                            sig, fontSize = 12.sp, color = c.textMain.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 7.dp),
                            maxLines = 2, lineHeight = 16.sp
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

