package com.example.s1client

import com.example.s1client.ui.mixWhite
import com.example.s1client.ui.TicketPalette
import com.example.s1client.ui.ticketPaletteFor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.zIndex
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.s1client.PostViewModel
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.data.model.ProfileData
import com.example.s1client.data.store.BlacklistManager
import com.example.s1client.data.store.BlockedAuthorManager
import com.example.s1client.data.store.FavoriteManager
import com.example.s1client.ui.RopeRefreshView
import com.example.s1client.ui.design.AccentPink
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
import com.example.s1client.ui.design.S1Radius
import com.example.s1client.ui.design.SnackbarBg
import com.example.s1client.ui.design.StrongIcon
import com.example.s1client.ui.design.TextMain
import com.example.s1client.ui.design.TextSub
import com.example.s1client.ui.pressScale
import com.example.s1client.ui.theme.LocalS1Colors
import com.example.s1client.ui.theme.S1Theme
import com.example.s1client.util.Haptic
import com.example.s1client.util.JsonParser
import com.example.s1client.util.S1Notify
import com.example.s1client.util.ThemeManager
import com.example.s1client.util.applyS1Theme
import com.example.s1client.util.avatarUrl
import com.example.s1client.util.setupS1EdgeToEdge
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 帖子预览门票（PostPeekDialog）+ 票据组件（2026-10-15 从 HomeDialogs.kt 拆出）。
 * 长按瀑布流卡弹出的预览门票：盖章收藏 / 真撕票隐藏 / 就近票根撤销 / 统一抽出关闭。
 */

internal class HalfTicketShape(private val top: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val split = size.height * .5f
        val path = Path()
        if (top) {
            path.moveTo(0f, 0f); path.lineTo(size.width, 0f)
            path.lineTo(size.width, split); path.lineTo(0f, split); path.close()
        } else {
            path.moveTo(0f, split); path.lineTo(size.width, split)
            path.lineTo(size.width, size.height); path.lineTo(0f, size.height); path.close()
        }
        return Outline.Generic(path)
    }
}
internal val TopHalfTicketShape = HalfTicketShape(top = true)
internal val BottomHalfTicketShape = HalfTicketShape(top = false)

// 票面调色板已抽为共用模块 ui/TicketPalette.kt（宪法20条：票据色唯一色源），此处直接引用。

/** 上下打孔锯齿边：票体圆角矩形减去上下两排圆（真挖孔，露出 scrim） */
private class TicketShape(
    private val perfR: Dp,
    private val teeth: Int = 14,
    private val corner: Dp = 6.dp
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { perfR.toPx() }
        val c = with(density) { corner.toPx() }
        val base = Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(
                0f, 0f, size.width, size.height,
                androidx.compose.ui.geometry.CornerRadius(c, c)))
        }
        val holes = Path()
        val step = size.width / teeth
        for (i in 0..teeth) {
            holes.addOval(androidx.compose.ui.geometry.Rect(i * step - r, -r, i * step + r, r))
            holes.addOval(androidx.compose.ui.geometry.Rect(i * step - r, size.height - r, i * step + r, size.height + r))
        }
        val merged = Path().apply { op(base, holes, PathOperation.Difference) }
        return Outline.Generic(merged)
    }
}

/** 撕裂虚线 + 两侧半圆缺口（贴片圆出界半被 Shape 裁掉，只留内半圆） */
@Composable
private fun TearLine(p: TicketPalette) {
    Box(Modifier.fillMaxWidth().height(14.dp)) {
        Canvas(Modifier.fillMaxWidth().height(1.5.dp).align(Alignment.CenterStart)) {
            var x = 0f
            while (x < size.width) {
                val end = minOf(x + 6f, size.width)
                drawLine(
                    color = p.accent.copy(alpha = 0.45f),
                    start = Offset(x, size.height / 2f),
                    end = Offset(end, size.height / 2f),
                    strokeWidth = size.height
                )
                x += 11f
            }
        }
        Box(Modifier.align(Alignment.CenterStart).size(14.dp).offset((-7).dp).background(Color(0x59000000), CircleShape))
        Box(Modifier.align(Alignment.CenterEnd).size(14.dp).offset(7.dp).background(Color(0x59000000), CircleShape))
    }
}

/** 门票风格操作按钮（操作带 LIGHT 震动；icon 可选） */
@Composable
private fun TicketAction(
    text: String,
    modifier: Modifier,
    p: TicketPalette,
    hide: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val ctx = LocalContext.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(p.btnBg)
            .border(1.dp, p.accentSoft, RoundedCornerShape(9.dp))
            .clickable(enabled = enabled) { Haptic.light(ctx); onClick() }
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                androidx.compose.material3.Icon(
                    icon, contentDescription = null,
                    tint = if (hide) p.hideTint else Color.White,
                    modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
            }
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = if (hide) p.hideTint else Color.White)
        }
    }
}

@Composable
fun PostPeekDialog(
    post: Post,
    viewModel: PostViewModel,
    onDismiss: () -> Unit,
    onFavorite: () -> Unit,
    onUndoFavorite: () -> Unit,
    onShare: () -> Unit,
    onHidePost: () -> Unit,
    onOpen: () -> Unit
) {
    val ctx = LocalContext.current
    var preview by remember(post.tid) { mutableStateOf<String?>(null) }
    var loading by remember(post.tid) { mutableStateOf(true) }
    LaunchedEffect(post.tid) {
        loading = true
        preview = viewModel.fetchThreadPreview(post.tid)
        loading = false
        // 静默同步服务器收藏状态 → 本地库（打开 Peek 数秒后点收藏即可走撤销票根）
        launch {
            runCatching {
                if (FavoriteManager.isFavorited(post.tid) == true) {
                    FavoriteManager.markLocalFaved(post.tid)
                }
            }
        }
    }

    // 出场：TICK 震动 + 缩放弹簧 + 淡入
    val scale = remember { Animatable(0.88f) }
    val alphaA = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        Haptic.light(ctx)
        launch { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) }
        launch { alphaA.animateTo(1f, tween(220)) }
    }

    val p = ticketPaletteFor(LocalS1Colors.current)
    val author = post.authorName ?: "匿名"
    val seed = author.ifBlank { "?" }
    val hot = (post.replies ?: 0) >= 1000
    // 票据相关动画：收藏=盖章；隐藏=真撕票（沿撕裂线拆两层反向飞出），播完才回调
    var stamping by remember { mutableStateOf(false) }
    var hiding by remember { mutableStateOf(false) }
    // 所有关闭路径统一「整票向下抽出+旋转+淡出」（撕票仅隐藏专属）
    var closing by remember { mutableStateOf(false) }
    var afterClose by remember { mutableStateOf<(() -> Unit)?>(null) }
    // 按钮禁用条件（含收尾动画期）
    val interBusy = stamping || hiding || closing
    // 就近票根（Dialog 内 Popup）：null=无；"hide"=隐藏待确认；"fav"=已在收藏中
    var undoMode by remember { mutableStateOf<String?>(null) }
    var undoDone by remember { mutableStateOf(false) }
    val closeAnim = remember { Animatable(0f) }
    LaunchedEffect(closing) {
        if (closing) {
            closeAnim.animateTo(1f, tween(300, easing = FastOutLinearInEasing))
            afterClose?.invoke()
        }
    }
    fun closeWithAnim(after: () -> Unit) {
        if (!interBusy) { afterClose = after; closing = true }
    }
    val stampScope = androidx.compose.runtime.rememberCoroutineScope()
    val stampAnim = remember { Animatable(0f) }
    val hideAnim = remember { Animatable(0f) }
    // 「隐藏」「收藏」按钮窗口坐标（撕票后票根撤销钮就近视现用）
    var hideBtnCenter by remember { mutableStateOf(androidx.compose.ui.unit.IntOffset.Zero) }
    var favBtnCenter by remember { mutableStateOf(androidx.compose.ui.unit.IntOffset.Zero) }
    LaunchedEffect(hiding) {
        if (hiding) {
            hideAnim.animateTo(1f, tween(430, easing = FastOutLinearInEasing))
            undoMode = "hide"   // 撕完出票根：撤销 / 7s 超时确认
        }
    }
    // 票根超时：hide=确认隐藏；fav=收回提示
    LaunchedEffect(undoMode) {
        when (undoMode) {
            "hide" -> { delay(5000); if (undoMode == "hide") { undoMode = null; onHidePost() } }
            "fav" -> { delay(3000); if (undoMode == "fav") undoMode = null }
            else -> {}
        }
    }
    // 盖章流程走 stampScope（startStamp），不再用 key 敏感的 LaunchedEffect
    fun startStamp() {
        if (stamping) return
        stamping = true
        Haptic.success(ctx)
        stampScope.launch {
            stampAnim.snapTo(0f)
            launch { stampAnim.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)) }
            delay(640)
            com.example.s1client.data.store.FavoriteManager.markLocalFaved(post.tid)
            println("FAVDBG stamped tid=${post.tid}")
            // 盖章完用抽出动画收尾（创意不浪费）
            closing = true
            afterClose = { onFavorite() }
            stamping = false
        }
    }

    Dialog(onDismissRequest = { closeWithAnim { onDismiss() } }) {
        Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale.value; scaleY = scale.value
                    alpha = alphaA.value * (1f - closeAnim.value)
                    translationY = closeAnim.value * 480f
                    rotationZ = closeAnim.value * 12f
                    cameraDistance = 12f * density
                }
        ) {
            val ticketSurface: @Composable () -> Unit = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, TicketShape(7.dp), clip = false)
                    .clip(TicketShape(7.dp))
                    .background(p.paper)
                    .drawBehind {
                        // 全息光晕：粉/紫/金/樱 四团 + 微弱锥形扫光（静态已成立）
                        fun glow(c: Color, cx: Float, cy: Float, r: Float, a: Float) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(c.copy(alpha = a), Color.Transparent),
                                    center = Offset(cx, cy), radius = r),
                                radius = r, center = Offset(cx, cy))
                        }
                        glow(p.glowA, size.width * .12f, size.height * .06f, size.width * .9f, .40f)
                        glow(p.glowB, size.width * .88f, size.height * .18f, size.width * .8f, .36f)
                        glow(p.glowC, size.width * .20f, size.height * .88f, size.width * .9f, .20f)
                        glow(p.glowD, size.width * .85f, size.height * .92f, size.width * .7f, .24f)
                        drawRoundRect(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    p.glowB.copy(alpha = .14f), p.glowA.copy(alpha = .18f),
                                    p.glowC.copy(alpha = .12f), p.glowB.copy(alpha = .14f),
                                    p.glowD.copy(alpha = .18f), p.glowB.copy(alpha = .14f)),
                                center = Offset(size.width * .55f, size.height * .45f)),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f),
                            alpha = .35f)
                    }
            ) {
                Column {
                    // 分类名：票头最上角（右上）
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.weight(1f))
                        post.tag?.takeIf { it.isNotBlank() }?.let {
                            Text("· $it", fontSize = 9.5.sp, color = p.onTicketSub.copy(alpha = .75f))
                        }
                    }
                    // STAGE1 大字票头（恢复初版）
                    Text(
                        "STAGE1",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        color = Color(0xFFE6D9F2).copy(alpha = .85f),
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                    TearLine(p)
                    // 标题
                    Text(
                        text = post.title ?: "无标题",
                        color = p.onTicket,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 19.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                    // 票头数据条（2026-10-16）：回复数 / 浏览 / 最后回复。
                    // 放在标题与正文之间 —— 票头就该有票头信息，它直接决定"要不要点进去"。
                    // 语言取票面次级色 + 竖杠分隔（项目竖杠语汇），不加边框不加底色保持克制。
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val meta = buildList {
                            post.replies?.takeIf { it > 0 }?.let { add("$it 回复") }
                            post.views?.takeIf { it > 0 }?.let { add("$it 浏览") }
                            post.lastReplyAt()?.let { add(it) }
                        }
                        meta.forEachIndexed { i, item ->
                            if (i > 0) {
                                // 竖杠分隔（项目通用语汇）
                                Text(
                                    " | ",
                                    fontSize = 9.sp,
                                    color = p.onTicketSub.copy(alpha = .45f)
                                )
                            }
                            Text(
                                item,
                                fontSize = 9.5.sp,
                                color = p.onTicketSub.copy(alpha = .8f)
                            )
                        }
                    }
                    // 正文预览三态（内滚）
                    when {
                        loading -> {
                            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = AccentPink)
                                Spacer(Modifier.width(8.dp))
                                Text("正在取票…", color = p.onTicketSub, fontSize = 13.sp)
                            }
                        }
                        preview.isNullOrBlank() -> {
                            Text(
                                "正文预览没取到，可以直接进帖看看。",
                                color = p.onTicketSub, fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        else -> {
                            Box(Modifier.padding(horizontal = 16.dp).heightIn(max = 148.dp)) {
                                val scroll = rememberScrollState()
                                Text(
                                    text = preview ?: "",
                                    color = Color(0xFFD9CDEA),
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.verticalScroll(scroll)
                                )
                            }
                        }
                    }
                    // 条形码（数据文本已按验收删去，仅留票面装饰）
                    Row(Modifier.fillMaxWidth().padding(end = 16.dp, top = 8.dp)) {
                        Spacer(Modifier.weight(1f))
                        Canvas(Modifier.size(width = 40.dp, height = 22.dp)) {
                            val s = post.tid.hashCode().absoluteValue
                            var x = 0f
                            var i = 0
                            while (x < size.width) {
                                val bar = 1f + ((s shr (i % 20)) and 3)
                                val wBar = bar.coerceAtMost(size.width - x)
                                drawRect(
                                    color = p.accent.copy(alpha = .85f),
                                    topLeft = Offset(x, 0f),
                                    size = Size(wBar, size.height))
                                x += wBar + 2f
                                i++
                            }
                        }
                    }
                    // 三操作（动画期禁点；记「隐藏/收藏」按钮坐标供就近票根定位）
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                            .onGloballyPositioned { c ->
                                val b = c.boundsInWindow()
                                hideBtnCenter = androidx.compose.ui.unit.IntOffset(
                                    (b.left + b.width * 5f / 6f).toInt(), ((b.top + b.bottom) / 2f).toInt())
                                favBtnCenter = androidx.compose.ui.unit.IntOffset(
                                    (b.left + b.width / 6f).toInt(), ((b.top + b.bottom) / 2f).toInt())
                            }
                    ) {
                        TicketAction("收藏", Modifier.weight(1f), p, icon = Icons.Filled.StarBorder, enabled = !interBusy) {
                            if (com.example.s1client.data.store.FavoriteManager.isLocalFaved(post.tid)) {
                                // 已在收藏中：就近票根提供「撤销」（手动选择后才真取消）
                                Haptic.light(ctx)
                                undoDone = false
                                undoMode = "fav"
                            } else if (!stamping) {
                                startStamp()
                            }
                        }
                        Spacer(Modifier.width(7.dp))
                        TicketAction("分享", Modifier.weight(1f), p, icon = Icons.Filled.Share, enabled = !interBusy) {
                            closeWithAnim { onShare() }
                        }
                        Spacer(Modifier.width(7.dp))
                        TicketAction("隐藏", Modifier.weight(1f), p, hide = true,
                            icon = Icons.Outlined.VisibilityOff, enabled = !interBusy) {
                            if (!hiding) { hiding = true; Haptic.medium(ctx) }
                        }
                    }
                    // 签名行（票根签名位：持票人 + 细字进帖）
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Brush.linearGradient(getAvatarGradient(seed)))
                                .border(1.5.dp, p.avatarRing.copy(alpha = .55f), RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(seed.take(1), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                            AsyncImage(
                                model = post.authorId?.takeIf { it.isNotBlank() }?.let { avatarUrl(it, "small") },
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(author, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = p.onTicket)
                            Text(post.timeText, fontSize = 8.5.sp, color = p.onTicketSub.copy(alpha = .72f))
                        }
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .border(1.dp, p.openBorder, RoundedCornerShape(99.dp))
                                .clickable { Haptic.selection(ctx); closeWithAnim { onOpen() } }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("查看完整帖 ›", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = p.accent)
                        }
                    }
                }
                // 热帖验票章（回复 ≥1000 才盖；top 下移避开分类行）
                if (hot) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 10.dp, top = 30.dp)
                            .graphicsLayer { rotationZ = -12f }
                            .size(50.dp)
                            .drawBehind {
                                drawCircle(
                                    color = p.glowD.copy(alpha = .45f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 2.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 5f))))
                                drawCircle(color = Color(0x33241A33))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("热帖", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                                color = mixWhite(p.glowD, .5f), letterSpacing = 2.sp)
                            Text("HOT ${post.replies}", fontSize = 7.sp,
                                color = mixWhite(p.glowD, .5f).copy(alpha = .8f))
                        }
                    }
                }
                // 收藏盖章动画：金虚线圆章盖在票面中上（scale 1.9→1 + 旋正）
                if (stamping) {
                    val s = stampAnim.value
                    val sScale = 1.9f - 0.9f * s
                    val sRot = -20f + 12f * s
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 86.dp)
                            .graphicsLayer {
                                scaleX = sScale; scaleY = sScale
                                rotationZ = sRot
                                alpha = (s * 2.2f).coerceAtMost(1f)
                            }
                            .size(84.dp)
                            .drawBehind {
                                drawCircle(
                                    color = Color(0xE6FFD76E),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 2.5.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f))))
                                drawCircle(color = Color(0x4D241A33))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("已收藏", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFFD76E), letterSpacing = 2.sp)
                            Text("FAVORITE", fontSize = 7.sp, color = Color(0xFFFFD76E).copy(alpha = .8f), letterSpacing = 1.5.sp)
                        }
                    }
                }
            }
            }
            // 真撕票：平时整票；点隐藏后沿撕裂线拆两层反向飞出 + 金闪
            if (!hiding) {
                ticketSurface()
            } else {
                Box(Modifier.fillMaxWidth()) {
                    // 上半：左旋上飞
                    Box(
                        Modifier
                            .clip(TopHalfTicketShape)
                            .graphicsLayer {
                                translationY = -hideAnim.value * 150f
                                rotationZ = -hideAnim.value * 18f
                                alpha = 1f - hideAnim.value
                            }
                    ) { ticketSurface() }
                    // 下半：右旋下沉（matchParentSize 铺底，不额外占高）
                    Box(
                        Modifier
                            .matchParentSize()
                            .clip(BottomHalfTicketShape)
                            .graphicsLayer {
                                translationY = hideAnim.value * 180f
                                rotationZ = hideAnim.value * 14f
                                alpha = 1f - hideAnim.value
                            }
                    ) { ticketSurface() }
                    // 撕裂处金色闪光（hideAnim 0.5→0.8 脉冲）
                    val fa = if (hideAnim.value > .5f && hideAnim.value < .8f)
                        1f - kotlin.math.abs(hideAnim.value - .65f) / .15f else 0f
                    Box(
                        Modifier
                            .matchParentSize()
                            .graphicsLayer { alpha = fa }
                            .drawBehind {
                                val y = size.height * .5f
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent, Color(0xFFFFE9F6), Color(0xFFFFD76E),
                                            Color(0xFFFFE9F6), Color.Transparent)),
                                    topLeft = Offset(0f, y - 1.5f),
                                    size = Size(size.width, 3f))
                            }
                    )
                }
            }

        }
            if (undoMode != null) {
                val anchor = if (undoMode == "hide") hideBtnCenter else favBtnCenter
                val d = LocalDensity.current
                val wPx = with(d) { 208.dp.toPx() }.roundToInt()
                val hPx = with(d) { 40.dp.toPx() }.roundToInt()
                Box(
                    Modifier
                        .matchParentSize()
                ) {
                    Row(
                        Modifier
                            .offset { androidx.compose.ui.unit.IntOffset(
                                (anchor.x - wPx / 2).coerceAtLeast(0),
                                (anchor.y - hPx / 2).coerceAtLeast(0)) }
                            .width(208.dp)
                            .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
                            .clip(RoundedCornerShape(12.dp))
                            .background(p.paper, RoundedCornerShape(12.dp))
                            .border(
                                1.5.dp,
                                Brush.linearGradient(listOf(p.glowA, p.glowB)),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(1.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (undoDone) {
                            // 撤销完成：票据色调确认章
                            Box(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    if (undoMode == "hide") "✓ 已撤销 · 帖子已恢复" else "✓ 已撤销收藏",
                                    color = Color(0xFFFFD76E), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .background(Color(0x1AFFD76E))
                                                                        .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("已生效", color = Color(0xFFE9C8EE), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    if (undoMode == "hide") "已隐藏该帖" else "已在收藏中",
                                    color = p.onTicket, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    if (undoMode == "hide") "可在 我的-抹布贴 恢复" else "点撤销取消收藏",
                                    color = p.onTicketSub.copy(alpha = .72f), fontSize = 8.5.sp)
                            }
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .background(Brush.linearGradient(listOf(p.glowC, p.glowD)))
                                                                        .clickable {
                                        Haptic.success(ctx)
                                        if (undoMode == "hide") {
                                            // 撤销隐藏：两半飞回原位，整票复原，不执行隐藏
                                            undoMode = null
                                            hiding = false
                                            stampScope.launch { hideAnim.snapTo(0f) }
                                        } else {
                                            // 撤销收藏：真取消
                                            undoDone = true
                                            onUndoFavorite()
                                        }
                                    }
                                    .padding(horizontal = 15.dp, vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("撤销", color = p.paper, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Peek 里的操作按钮：竖杠风格，和顶栏文字一致 */

@Composable
internal fun PeekActionButton(text: String, modifier: Modifier, onClick: () -> Unit) {
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

/**
 * 票头数据条的「最后回复」文案。
 * 列表模型已把时间格式化好（[Post.timeText]），为空则整项不显示，不占位。
 */
private fun Post.lastReplyAt(): String? = timeText.takeIf { it.isNotBlank() }
