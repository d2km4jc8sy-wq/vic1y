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

// ================= 瀑布流帖子卡 =================

// ————————— 幻彩流光封面（coverStyle = 5）—————————
// 三色兜底色板（取不到 s1.coverColors 时才用）；标题固定深紫黑（封面永远是亮底）
internal val FlowColorTop = Color(0xFFB569F6)
internal val FlowColorBottom = Color(0xFFFF7AB5)
internal val FlowColorMid = Color(0xFFC03DEA)
internal val FlowCoverText = Color(0xFF2E1B38)
/** 光斑呼吸循环总时长（秒）：24s 一眼可辨，再快就晃眼 */
internal const val FLOW_CYCLE_SECONDS = 24f

/**
 * 瀑布流骨架屏：双列占位卡 + 一道缓慢扫过的高光。
 * 加载时先有「形」，不再是屏幕中间一个孤零零的转圈。
 */

@Composable
fun XhsPostCard(
    post: Post,
    coverStyle: Int,
    dataSaver: Boolean,
    /** 头像独立加载开关（2026-10-17 点2：三档策略在调用方算成布尔；false=字母渐变头像兜底） */
    avatarAutoLoad: Boolean = true,
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
    // 相位由系统时间直接计算（全 App 一面墙一套相位，与卡片何时进入组合无关）。
    // 阶段二性能修复（2026-10-17，小米10 左右滑后掉帧）：原 rememberInfiniteTransition
    // 让每张幻彩卡以屏幕刷新率重绘 —— 一屏 ~10 卡 × 60fps 的无效重绘叠加翻页图片解码，
    // 是低端芯片滚动掉帧的主源。光斑是 24s 周期的慢速运动，15fps 采样视觉无损：
    // 改为帧控心跳（每 ~4 帧发一次相位），重绘负载降为 1/4；卡片移出组合
    // （Pager 切版后旧版块销毁）时 LaunchedEffect 自动取消。
    val flowHeartbeat: State<Float> = if (coverStyle == 5) {
        val phase = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            val minIntervalNs = 1_000_000_000L / 15
            var lastEmit = 0L
            while (true) {
                androidx.compose.runtime.withFrameNanos { now ->
                    if (now - lastEmit >= minIntervalNs) {
                        lastEmit = now
                        phase.floatValue =
                            ((now / (FLOW_CYCLE_SECONDS * 1_000_000_000.0)) % 1.0).toFloat()
                    }
                }
            }
        }
        phase
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
    // 投影（2026-10-18 三改）：单层 6dp → 4dp。用户实测双层与单层无感，
    // 而 6dp 的扩散会渗进两列之间约 10dp 的缝里，把暖色底压灰一截；4dp 收窄后缝隙干净。
    // 深色主题不画：黑影落在近黑底上等于没画，深色靠「卡面比底亮一档 + 1px 描边」分层。
    // 回退方式：把下面一行换回
    //   Modifier.shadow(2.dp, cardShape, clip = false).shadow(10.dp, cardShape, clip = false)
    // 两级 = 贴边实 + 扩散虚；单层 = 一圈均匀的糊影，更轻，但卡片边缘没那么「压得住」。
    val shadowMods = if (s1.isDark) Modifier
                     else Modifier.shadow(elevation = 4.dp, shape = cardShape, clip = false)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(pressSource)
            .then(shadowMods)
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
                coverStyle == -2 -> s1.cardBg
                else -> GlassCard
            }
        ),
        // 边框改为自绘的液态玻璃（见下方 Box 的 drawWithContent），此处不再用 BorderStroke
        border = null,
        // 投影不再交给 Material 高程：浅色主题要的是「贴边 + 环境」两级，
        // 单层 elevation 只能给出一圈均匀的糊影（见上方 shadowMods）。
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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

            // 标题区：有图卡在封面下方（纯卡片底）；无图卡走纯色封面：
            //  - 幻彩渐变（coverStyle=5）：流动光斑封面（该主题的标识，保留）
            //  - 其余（含 Linear -2）：一块纯色底 + 居中标题，无任何纹饰
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
                // 封面减法（2026-10-18 用户定案）：无图封面只保留「一块纯色底 + 标题」。
                // 已清零：径向光斑、顶部/底部色条、微网格、大引号、玻璃高光、底部磨砂纱、全部渐变。
                // 幻彩（coverStyle = 5）是唯一例外 —— 流光本身就是那套主题的标识，绘制保持原样。

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
                    else -> Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 120.dp)
                        .background(s1.coverTint)
                        .drawBehind {
                            // 暗色下相邻两级明度的感知差远小于浅色，两块会「连成一片」，
                            // 眼睛一直找不到边界就会累。这里用一条 1px 底色线把它切成上下两截；
                            // 浅色不需要（#E5E5EA 与 #FFFFFF 本身已经分开）。
                            if (s1.isDark && s1.metaTint == null) {
                                drawLine(
                                    color = s1.bgTop,
                                    start = Offset(0f, size.height - 0.5f),
                                    end = Offset(size.width, size.height - 0.5f),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }
                }

                // 幻彩封面（style 5）= strange-cobra 彩边 + 内层双色：
                // 深色变体白字 / 浅色变体深字（FlowCoverText）
                val baseTitleColor = if (coverStyle == 5) {
                    if (s1.isDark) Color.White else FlowCoverText
                } else s1.textMain
                // 已读：保持原色相，只降不透明度（不动封面、不动卡片底色）。
                // 可读性底线：幻彩封面下限 60%（≥3:1，实测 42% 时仅 2.4:1 糊掉）；
                // 其他主题维持既有 42% 不动。
                val readAlphaFactor = if (coverStyle == 5) 0.6f else 0.42f
                val titleColor = if (read) baseTitleColor.copy(alpha = baseTitleColor.alpha * readAlphaFactor)
                                 else baseTitleColor

                Box(
                    modifier = coverModifier
                        // 幻彩保留 12dp 内缩（流光块是卡内的一枚色卡）；
                        // 其余主题封面铺满卡片上半部，直到卡边 —— 这是「上下两色」成立的前提，
                        // 留着白边就变成「白卡里贴了一块浅色纸」，两色对抗立刻失效。
                        .then(if (coverStyle == 5) Modifier.padding(12.dp) else Modifier)
                ) {
                    Text(
                        text = title,
                        color = titleColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center,
                        style = androidx.compose.material3.LocalTextStyle.current,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .padding(
                                start = if (coverStyle == 5) 8.dp else 18.dp,
                                end = if (coverStyle == 5) 8.dp else 18.dp,
                                top = 26.dp,
                                bottom = 26.dp
                            )
                    )
                }
            }

            // 元信息区（作者 / 回帖数 / 分类 / 时间）整块包一层：
            // 朱砂主题下 metaTint = 红，「上黑下红」由此成立——
            // 顶上一条 1px 底色线，就是原来红带下方那条边，分区清楚又不加边框。
            val hasMeta = s1.metaTint != null
            val metaFg = if (hasMeta) Color.White.copy(alpha = 0.94f) else TextSub
            val metaFgDim = if (hasMeta) Color.White.copy(alpha = 0.72f) else TextSub.copy(alpha = 0.7f)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(s1.metaTint ?: Color.Transparent)
                    .drawBehind {
                        if (hasMeta) {
                            drawLine(
                                color = s1.bgTop,
                                start = Offset(0f, 0.5f),
                                end = Offset(size.width, 0.5f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
            ) {
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
                    if (avatarUrlStr != null && avatarAutoLoad) {
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
                    color = metaFg,
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
                    color = if (isHot) s1.heart else metaFg,
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
                        color = metaFg,
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
                        color = metaFgDim,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
            }
        }
      }  // ← 关闭液态玻璃描边 Box
    }  // ← 关闭 Card
}

// ================= 长按操作菜单 =================
