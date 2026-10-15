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
import com.example.s1client.data.store.CoverCacheStore
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.rotate
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

// ================= 瀑布流帖子卡（无图文字封面体系） =================
/**
 * 无图帖子的文字封面：
 * style -1  OLED 纯黑（曜石玄夜专用，黑底+琥珀金点缀）
 * style 0..4 五种风格，每种自动按主题明暗渲染深/浅两版：
 *           0 角落光晕 / 1 微网格+顶彩条 / 2 大引号 / 3 玻璃描边+顶高光 / 4 组合
 * style 5   幻彩渐变专用：低饱和浅色三段柔和渐变（Apple 风），色板来自当前主题
 */
internal val CoverGlowDark = listOf(
    Color(0xFFB48CFF), Color(0xFFFFA08C), Color(0xFF8CC8FF), Color(0xFF8CFFB4)
)
internal val CoverBarDark = listOf(
    Color(0xFF7C6AD6), Color(0xFFD67A6A), Color(0xFF6A9BD6), Color(0xFF6AD6A0)
)
internal val CoverBaseDark = listOf(
    Color(0xFF1E1E24), Color(0xFF1C1B20), Color(0xFF1C1C1E), Color(0xFF1B1B22), Color(0xFF1E1E24)
)

/**
 * Linear 主题（coverStyle = -2）专用：
 * 卡片底几乎与页面底同色，靠 1px 描边分层；顶部一条 2dp 语义色条做点缀。
 * 色条按 tid 取 5 色之一（低饱和、与主色同族），保留"每张卡略有不同"的节奏。
 */
internal val LinearCardBase = Color(0xFF0D0E10)
internal val LinearCardLine = Color(0xFF191B1F)
internal val LinearStripColors = listOf(
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
internal val FlowColorTop = Color(0xFFB569F6)
/** 兜底：封面下半主色（粉） */
internal val FlowColorBottom = Color(0xFFFF7AB5)
/** 兜底：渐变中段过渡色 */
internal val FlowColorMid = Color(0xFFC03DEA)

/** 封面标题用色：封面永远是亮底，所以固定深紫黑（不随主题明暗变） */
internal val FlowCoverText = Color(0xFF2E1B38)

/**
 * 循环总时长（秒）。渐变色标沿 x 轴横移一个来回所用的时间。
 * 首版 60s 实测太慢、肉眼察觉不到动，改为 24s（慢但可辨，再快就晃眼）。
 */
internal const val FLOW_CYCLE_SECONDS = 24f

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
fun XhsPostCard(
    post: Post,
    coverStyle: Int,
    dataSaver: Boolean,
    /** 已读过的帖子：标题降一档不透明度，刷过的一眼跳过 */
    read: Boolean = false,
    onAvatarClick: () -> Unit,
    onMarkRead: () -> Unit,
    onLongPress: () -> Unit,
    /** 滚轮呼出等模态场景：锁定卡片点击/长按 */
    interactionsLocked: Boolean = false
) {
    val title = post.title ?: "无标题"
    val tag = post.tag ?: "杂谈"
    val author = post.authorName ?: "匿名"
    val replies = post.replies ?: 0
    val timeText = post.timeText ?: ""
    val isHot = replies >= 300
    // 省流模式：不加载封面图，一律用风格文字封面
    // 瀑布流封面图：接口不返回 attachmentImagePreviewList 内容，改为懒加载解析
    // 帖子详情 HTML 的首楼第一张内容图（本项目差异化功能，s1er 没有）。
    // 只在 coverUrl 为空时触发；有值直接复用，不再发请求。
    // 封面优先级：接口给的 > 本地缓存（用户进过详情页才会写入）> 无图
    // 服务端只对约 1/4 帖子返回预览图，所以本地缓存是「看得越多封面越全」的关键
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var cachedCover by remember(post.tid) {
        mutableStateOf(CoverCacheStore.get(ctx).get(post.tid))
    }
    val effectiveCover = post.coverUrl ?: cachedCover
    val showImage = !dataSaver && !effectiveCover.isNullOrBlank()
    val context = androidx.compose.ui.platform.LocalContext.current
    val s1 = LocalS1Colors.current
    // 按压缩放：interactionSource 与点击共用，按下才有反馈
    val pressSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    // 卡片圆角：Linear 收敛到 9dp，其余 14dp
    val corner = if (coverStyle == -2) 9f else 14f
    val cardShape = RoundedCornerShape(corner.dp)
    // 幻彩流光相位（方案②·光斑呼吸，2026-10-15 用户定稿）：
    // 相位由系统时间直接计算（全 App 一面墙一套相位，与卡片何时进入组合无关，
    // 根治「每卡独立时钟各自闪烁」）；这里的 infiniteTransition 只当「心跳」，
    // 负责让幻彩卡每帧重绘（在绘制阶段读取，不触发重组），非幻彩主题不建动画。
    val flowHeartbeat: State<Float> = if (coverStyle == 5) {
        rememberInfiniteTransition(label = "flowTick")
            .animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween((FLOW_CYCLE_SECONDS * 1000).toInt(), easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "flowTickValue"
            )
    } else {
        remember { mutableStateOf(0f) }
    }
    // 液态玻璃边框（方案 B）：整圈渐变描边 + 内侧顶部高光
    val gTop = s1.glassEdgeTop
    val gMid = s1.glassEdgeMid
    val gBot = s1.glassEdgeBottom
    val gInset = s1.glassInsetLight
    // 幻彩主题下**无条件**用主题色环做卡框（原来只在 coverStyle==5 的分支里画，
    // 一旦封面风格不是 5，幻彩卡片就退化成"半透明白玻璃边" —— 在浅色粉底上等于没有边，
    // 这正是"瀑布流幻彩卡片边框太细 / 主题不突出"的真因）。
    val isIridescentTheme = ThemeManager.currentTheme == ThemeManager.THEME_IRIDESCENT
    val ringBand = remember(s1) { com.example.s1client.ui.theme.s1RingColors(s1) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(pressSource)
            .combinedClickable(
                interactionSource = pressSource,
                indication = androidx.compose.material.ripple.rememberRipple(),
                enabled = !interactionsLocked,
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
                // 幻彩卡面与「我的」页同源（不再写死冷灰 #F5F4F8 / 近黑 #0B0B10）：
                // 浅色 #FFFFFF / 深色 #221830，消除「同主题三张白、冷暖相冲」
                coverStyle == 5 -> s1.cardBg
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
              if (isIridescentTheme) {
                  // 幻彩「彩色亮丝」卡框（2026-10-15 用户定稿，替代原 4dp 饱和粗环）：
                  // 1.5dp 主题色板渐变描边（s1RingColors 四色，左上→右下）+ 贴边 4dp@0.10
                  // 同色柔光（「亮」的来源，想更安静可整段删掉柔光那截）。
                  // 描边整体内缩半个线宽——Card/Surface 会把内容 clip 到圆角形状，
                  // 直接画在边界上时靠外的一半会被裁掉（4dp 只剩 2dp 的老坑）。
                  // 与「我的」页卡框同一套语法、同一粗细（1.5dp），幻彩主题内边框全部统一。
                  Modifier.drawWithContent {
                      drawContent()
                      val band = ringBand
                      // 柔光：贴边的低透明度宽描边，让彩丝有「亮」的晕染
                      val glowPx = 4.dp.toPx()
                      val glowInset = glowPx / 2f
                      drawRoundRect(
                          brush = Brush.linearGradient(
                              band.map { it.copy(alpha = 0.10f) },
                              start = Offset.Zero,
                              end = Offset(size.width, size.height)
                          ),
                          topLeft = Offset(glowInset, glowInset),
                          size = Size(size.width - glowPx, size.height - glowPx),
                          cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                              (corner.dp.toPx() - glowInset).coerceAtLeast(0f)
                          ),
                          style = androidx.compose.ui.graphics.drawscope.Stroke(width = glowPx)
                      )
                      // 彩丝本体：1.5dp@0.88
                      val strokePx = 1.5.dp.toPx()
                      val inset = strokePx / 2f
                      drawRoundRect(
                          brush = Brush.linearGradient(
                              band.map { it.copy(alpha = 0.88f) },
                              start = Offset.Zero,
                              end = Offset(size.width, size.height)
                          ),
                          topLeft = Offset(inset, inset),
                          size = Size(size.width - strokePx, size.height - strokePx),
                          cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                              (corner.dp.toPx() - inset).coerceAtLeast(0f)
                          ),
                          style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokePx)
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
            val cover = effectiveCover
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
                    color = if (read) TextMain.copy(alpha = 0.42f) else TextMain,
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
                        // 幻彩封面（方案②·光斑呼吸，2026-10-15 用户定稿；技术源头：
                        // uiverse fast-badger-70 多层 radial 极光 / blue-mole-92 磨砂纱）：
                        //   ① 封面色板打底（s1.coverColors，浅粉调 / 深紫调）
                        //   ② 三个径向光斑大圆慢漂 —— 幅度减半（±5%）、全墙同频
                        //      （相位 = 系统时间取模，与卡片组合时机无关）、已读定格
                        //   ③ 一层薄磨砂霜（近似 backdrop 磨砂，压住光斑浓度）
                        //   ④ 底部 42% 起白/深纱，标题无论几行都落在纱上
                        //   ⑤ 无任何扫光/亮带（方案①②③对比后用户选 ②：流动感全部
                        //      来自色彩呼吸，不来自位移扫动；cobraPhase 亮带已删）
                        // 旧实现只有一条 1dp「strange-cobra」五色内框（绿/黄/青 off-palette
                        // 残留，与紫粉色板无关），已删。
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .drawBehind {
                                flowHeartbeat.value // 心跳：读取以建立重绘依赖（相位本体来自系统时间）
                                val w = size.width; val h = size.height
                                val cover = s1.coverColors.ifEmpty {
                                    listOf(FlowColorBottom, FlowColorMid, FlowColorTop)
                                }
                                // ① 底色
                                drawRect(
                                    color = if (s1.isDark) Color(0xFF1E1428) else Color(0xFFF2D9EA)
                                )
                                // ② 光斑慢漂（半幅 ±5%，全局同频；已读卡定格在固定相位退为背景）
                                fun blob(color: Color, bx: Float, by: Float, radius: Float, dx: Float, dy: Float, shift: Float) {
                                    val ph = if (read) 0.25f
                                    else ((System.nanoTime() / (FLOW_CYCLE_SECONDS * 1_000_000_000.0)) % 1.0).toFloat()
                                    val ang = ((ph + shift) % 1f) * 2f * kotlin.math.PI.toFloat()
                                    val cx = bx + dx * kotlin.math.sin(ang)
                                    val cy = by + dy * kotlin.math.cos(ang)
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(color.copy(alpha = 0.92f), color.copy(alpha = 0f)),
                                            center = Offset(cx, cy),
                                            radius = radius
                                        ),
                                        radius = radius,
                                        center = Offset(cx, cy)
                                    )
                                }
                                blob(cover[0], w * 0.18f, h * 0.20f, w * 0.60f, w * 0.05f, h * 0.06f, 0f)
                                blob(cover[1 % cover.size], w * 0.84f, h * 0.16f, w * 0.55f, w * 0.045f, h * 0.07f, 0.34f)
                                blob(cover[cover.size - 1], w * 0.45f, h * 0.88f, w * 0.68f, w * 0.06f, h * 0.04f, 0.67f)
                                // ③ 磨砂霜
                                drawRect(
                                    color = (if (s1.isDark) Color(0xFF140A1E) else Color.White)
                                        .copy(alpha = if (s1.isDark) 0.16f else 0.22f)
                                )
                                // ④ 底部纱（42% 起）
                                val veil = if (s1.isDark) Color(0xFF12091C) else Color.White
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            veil.copy(alpha = 0.55f),
                                            veil.copy(alpha = 0.94f)
                                        ),
                                        startY = h * 0.38f,
                                        endY = h
                                    )
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
                // 已读：保持原色相，只降不透明度（不动封面、不动卡片底色）。
                // 可读性底线：幻彩封面下限 60%（≥3:1，实测 42% 时仅 2.4:1 糊掉）；
                // 其他主题维持既有 42% 不动。
                val readAlphaFactor = if (coverStyle == 5) 0.6f else 0.42f
                val titleColor = if (read) baseTitleColor.copy(alpha = baseTitleColor.alpha * readAlphaFactor)
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
                        // 幻彩流光封面标题居中；其余深色封面仍左下角，浅色封面仍居中。
                        // 标题可读性由底部磨砂纱保证，不再叠白字柔光阴影（浅色下会洗字）。
                        textAlign = if (coverStyle == 5 || !isDarkCover) TextAlign.Center else TextAlign.Start,
                        style = androidx.compose.material3.LocalTextStyle.current,
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

// ================= 长按操作菜单 =================
