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
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInWindow
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

// ================= 首页对话框与操作面板 =================
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

/* ================= 长按 Peek · 全息门票（2026-10-15 用户定稿）=================
 * 票面恒深色：两种主题一张票；打孔锯齿边 = TicketShape 真挖孔。
 * 调色收敛在 TicketPalette —— 后续幻彩主题调色只动这里（可从 s1 色板派生新 palette）。
 */

/** 上下半裁剪（真撕票拆层用）*/

/** 门票调色板：幻彩主题调色的唯一入口 */
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
internal fun HammerMenuItem(
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
internal fun ReorderableTabColumn(
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
internal data class UndoBarData(
    val message: String,
    val actionLabel: String,
    val onUndo: () -> Unit
)
