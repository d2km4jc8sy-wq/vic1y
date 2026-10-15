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

// ================= 底部导航 =================
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
            .padding(horizontal = 13.dp, vertical = 2.dp)
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
                        .padding(horizontal = 3.dp),
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
internal val AvatarGradients: List<List<Color>> = listOf(
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

