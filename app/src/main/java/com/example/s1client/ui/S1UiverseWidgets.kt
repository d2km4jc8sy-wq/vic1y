// ============================================================================
// S1UiverseWidgets.kt — 10 个 Uiverse 风格控件的 Compose 复刻
//
// 使用方式：
//   1. 把本文件放进 app/src/main/java/com/example/s1client/ui/（包名按需改）
//   2. 所有颜色从 LocalS1Colors 取，7 套主题自动适配，无需改动
//   3. 每个控件自包含、互不依赖，单独拷一个也能编译
//
// 性能说明：
//   - 无限动画用 rememberInfiniteTransition，只在控件处于组合中时运行，
//     滑出屏幕 / 页面销毁即自动停止，不产生后台开销
//   - 全部是绘制层效果（Brush / drawBehind），零位图、零额外内存
//   - 建议同一屏同时可见的无限动画控件 ≤ 2 个
// ============================================================================

package com.example.s1client.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s1client.ui.theme.LocalS1Colors
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════
// 01 · 流光渐变按钮 — 签到 / 登录
//     渐变横向流动 + 高光扫过 + 按压缩放。uiverse: "glow gradient button"
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun FlowingGradientButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val c = LocalS1Colors.current
    val transition = rememberInfiniteTransition(label = "flowBtn")
    val shift by transition.animateFloat(           // 渐变流动 0→1
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "shift"
    )
    val sweep by transition.animateFloat(           // 高光扫过
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2400, delayMillis = 400, easing = FastOutSlowInEasing),
            RepeatMode.Restart
        ),
        label = "sweep"
    )

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "press"
    )

    val cover = c.flowCoverColors   // 主题自带 4 色流光板
    val colors = listOf(cover[0], cover[1], cover[2], cover[0])

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(99.dp))
            .drawBehind {
                // 背景：随 shift 移动的线性渐变
                val w = size.width
                val start = Offset(shift * 2f * w - w, 0f)
                drawRect(Brush.linearGradient(colors, start = start, end = start + Offset(w, w)))
                // 高光：随 sweep 扫过的亮带
                val s2 = Offset(sweep * 3f * w - w, 0f)
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent),
                        start = s2, end = s2 + Offset(w, w)
                    )
                )
            }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 42.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 02 · 日 / 夜 太阳月亮开关 — 深浅色快捷切换
//     滑块弹性位移 + 轨道换色 + 凹面阴影消失(月亮→太阳)。uiverse: "toggle day night"
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun SunMoonSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalS1Colors.current
    val knobX by animateDpAsState(
        targetValue = if (checked) 36.dp else 3.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "knob"
    )
    val track by animateColorAsState(
        // iOS 模式：球恒白，轨道区分状态——开 = accent 实色，关 = 中性灰
        targetValue = if (checked) c.accent else c.chipBg,
        label = "track"
    )
    val knobBg by animateColorAsState(
        targetValue = Color.White,
        label = "knobBg"
    )

    Box(
        modifier = modifier
            .width(74.dp).height(38.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(track)
            .border(1.dp, c.glassLine, RoundedCornerShape(99.dp))
            .clickable { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobX, y = 3.dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(knobBg)
                .drawBehind {
                    // 未选中(月亮)时画凹面阴影，选中(太阳)时消失
                    if (!checked) {
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.35f),
                            radius = size.minDimension / 2f - 3.dp.toPx(),
                            center = Offset(size.width / 2f + 2.dp.toPx(), size.height / 2f - 1.5f.dp.toPx())
                        )
                    }
                }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 03 · 骨架屏加载卡 — 瀑布流首屏 / 列表加载
//     微光扫过占位卡，替代白屏。uiverse: "skeleton shimmer"
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    coverHeight: Dp = 80.dp
) {
    val c = LocalS1Colors.current
    val transition = rememberInfiniteTransition(label = "skel")
    val shimmer by transition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "shimmer"
    )
    var widthPx by remember { mutableStateOf(1f) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(c.glassCard)
            .padding(10.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
    ) {
        SkeletonLine(coverHeight, 10.dp, shimmer, widthPx, c)
        Spacer(Modifier.height(8.dp))
        SkeletonLine(9.dp, 8.dp, shimmer, widthPx, c)
        Spacer(Modifier.height(6.dp))
        SkeletonLine(9.dp, 8.dp, shimmer, widthPx, c, widthFraction = 0.55f)
    }
}

@Composable
private fun SkeletonLine(
    height: Dp, corner: Dp, shimmer: Float, widthPx: Float,
    c: com.example.s1client.ui.theme.S1Colors,
    widthFraction: Float = 1f
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(corner))
            .drawBehind {
                val band = widthPx
                val x = shimmer * band
                drawRect(SolidColor(Color.White.copy(alpha = 0.06f)))
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.14f), Color.Transparent),
                        start = Offset(x, 0f), end = Offset(x + band, 0f)
                    )
                )
            }
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// 04 · 三色轨道刷新指示器 — 下拉刷新尾态 / 分页加载
//     三颗主题色珠子沿圆轨道追逐。uiverse: "orbit spinner"
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun OrbitDotsLoader(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 56.dp,
    dotSize: Dp = 10.dp
) {
    val c = LocalS1Colors.current
    val angle by rememberInfiniteTransition(label = "orbit").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "angle"
    )
    val dots = listOf(c.accent, c.flowCoverColors[1], c.flowCoverColors[0])

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        dots.forEachIndexed { i, color ->
            // 每颗珠子相位差 40°，形成追逐感
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = angle + i * 40f }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 0.dp)
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 05 · 爱心爆裂点赞 — 帖子详情点赞 / 收藏心形
//     心形弹簧放大 + 6 粒主题色粒子迸发。uiverse: "heart burst"
// ═══════════════════════════════════════════════════════════════════════════

private val BurstVectors = listOf(
    Offset(-26f, -20f), Offset(24f, -24f), Offset(-30f, 8f),
    Offset(28f, 14f), Offset(0f, -32f), Offset(-14f, 24f)
)

@Composable
fun BurstLikeButton(
    liked: Boolean,
    count: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalS1Colors.current
    val scope = rememberCoroutineScope()
    val burst = remember { Animatable(1f) }   // 1 = 静止，0 = 爆发起点
    val heartScale by animateFloatAsState(
        targetValue = if (liked) 1.25f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium),
        label = "heart"
    )

    Row(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null
        ) {
            onToggle()
            if (!liked) {
                scope.launch {
                    burst.snapTo(0f)
                    burst.animateTo(1f, tween(600, easing = FastOutLinearInEasing))
                }
            }
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            // 粒子层
            if (burst.value < 1f) {
                val p = burst.value
                BurstVectors.forEachIndexed { i, v ->
                    val colors = listOf(c.accent, c.flowCoverColors[1], c.flowCoverColors[0])
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = v.x * p
                                translationY = v.y * p
                                alpha = (1f - p)
                            }
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(colors[i % colors.size])
                    )
                }
            }
            Text(
                text = if (liked) "❤" else "♡",
                fontSize = 26.sp,
                color = if (liked) c.heart else c.textSub,
                modifier = Modifier.graphicsLayer { scaleX = heartScale; scaleY = heartScale }
            )
        }
        Spacer(Modifier.width(10.dp))
        Text("$count", fontSize = 14.sp, color = c.textMain, fontWeight = FontWeight.SemiBold)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 06 · 聚焦发光输入框 — 搜索 / 登录
//     聚焦时边框变主题双色渐变 + 外圈柔光。uiverse: "gradient focus input"
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun FocusGlowField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier
) {
    val c = LocalS1Colors.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val glow by animateFloatAsState(if (focused) 1f else 0f, tween(300), label = "glow")

    val shape = RoundedCornerShape(12.dp)
    BasicTextField(
        value = value, onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = c.textMain, fontSize = 13.sp),
        cursorBrush = SolidColor(c.accent),
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.chipBg)
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                if (glow > 0.01f) {
                    val brush = Brush.linearGradient(listOf(c.flowCoverColors[1], c.accent))
                    // 外圈柔光：用低透明度色刷模拟扩散（Stroke 无 alpha 参数）
                    val glowBrush = Brush.linearGradient(
                        listOf(
                            c.flowCoverColors[1].copy(alpha = 0.18f * glow),
                            c.accent.copy(alpha = 0.18f * glow)
                        )
                    )
                    drawRoundRect(
                        brush = glowBrush, cornerRadius = CornerRadius(12.dp.toPx()),
                        style = Stroke(stroke * 3f)
                    )
                    // 渐变主边框
                    drawRoundRect(
                        brush = brush, cornerRadius = CornerRadius(12.dp.toPx()),
                        style = Stroke(stroke)
                    )
                } else {
                    drawRoundRect(
                        color = c.glassLine, cornerRadius = CornerRadius(12.dp.toPx()),
                        style = Stroke(stroke)
                    )
                }
            }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        decorationBox = { inner ->
            if (value.isEmpty() && !focused) {
                Text(hint, color = c.textSub, fontSize = 13.sp)
            } else inner()
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// 07 · 流光进度条 — 签到连续天数 / 上传 / 同步
//     填充生长动画 + 表面高光持续流过。uiverse: "shimmer progress"
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun FlowProgressBar(
    progress: Float,            // 0f..1f
    leftLabel: String,
    rightLabel: String,
    modifier: Modifier = Modifier
) {
    val c = LocalS1Colors.current
    val fill by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1600, easing = FastOutSlowInEasing),
        label = "fill"
    )
    val sweep by rememberInfiniteTransition(label = "prog").animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "sweep"
    )
    var widthPx by remember { mutableStateOf(1f) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill)
                    .height(10.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Brush.linearGradient(listOf(c.flowCoverColors[1], c.accent)))
                    .drawBehind {
                        // 表面高光流过
                        val x = sweep * widthPx
                        drawRect(
                            Brush.linearGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.5f), Color.Transparent),
                                start = Offset(x, 0f), end = Offset(x + widthPx * 0.4f, 0f)
                            )
                        )
                    }
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(leftLabel, fontSize = 11.sp, color = c.textSub)
            Text(rightLabel, fontSize = 11.sp, color = c.textSub)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 08 · 卡片式单选 — 主题选择补位 / 排序方式
//     选中上浮 + 主色描边 + 发光。uiverse: "radio card"
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun CardRadioGroup(
    items: List<Pair<String, List<Color>>>,   // (名称, 预览渐变色)
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalS1Colors.current
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEachIndexed { i, (name, colors) ->
            val selected = i == selectedIndex
            val lift by animateFloatAsState(
                targetValue = if (selected) -3f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "lift"
            )
            Column(
                modifier = Modifier
                    .graphicsLayer { translationY = lift }
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.glassCard)
                    .border(
                        2.dp,
                        if (selected) c.accent else Color.White.copy(alpha = 0.10f),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelect(i) }
                    .padding(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(64.dp).height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(colors))
                )
                Text(
                    name, fontSize = 10.sp,
                    color = if (selected) c.textMain else c.textSub,
                    modifier = Modifier.padding(top = 6.dp, bottom = 3.dp).fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 09 · 弹性胶囊 Tab — 首页分区导航 / 列表分类
//     渐变胶囊弹性滑到目标项，文字颜色跟随。uiverse: "sliding pill tabs"
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun PillTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalS1Colors.current
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(4.dp)
    ) {
        val tabW = maxWidth / tabs.size
        val pillX by animateDpAsState(
            targetValue = tabW * selectedIndex,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
            label = "pill"
        )
        Box(
            modifier = Modifier
                .offset(x = pillX)
                .width(tabW)
                .height(32.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Brush.linearGradient(listOf(c.flowCoverColors[1], c.accent)))
        )
        Row(Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { i, label ->
                Text(
                    label,
                    fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                    color = if (i == selectedIndex) Color.White else c.textSub,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(99.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(i) }
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 10 · 旋转流光边框卡 — 加精帖封面 / 成就卡 / 「我的」身份卡
//     conic 色相环绕旋转的 1.5dp 边框，内部任意内容。
//     uiverse: "rotating gradient border card"
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun ConicFlowBorderCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 3.5.dp,
    /** false = 静态柔彩（配角卡）；true = 全饱和旋转（主角卡，一页只留一个） */
    rotating: Boolean = true,
    /** 描边透明度：配角卡 0.5f 退到底色里 */
    borderAlpha: Float = 1f,
    /** >0 且静态时：pulseKey 变化触发一次 0.6s 快转脉冲（签到庆祝用） */
    pulseKey: Int = 0,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    val c = LocalS1Colors.current
    // 角色分离：主角无限旋转；配角静止，只在 pulseKey 变化时快转一圈庆祝
    val infAngle = if (rotating) {
        rememberInfiniteTransition(label = "conic").animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
            label = "angle"
        ).value
    } else null
    val staticAngle = remember { androidx.compose.animation.core.Animatable(45f) }
    androidx.compose.runtime.LaunchedEffect(pulseKey) {
        if (!rotating && pulseKey > 0) {
            staticAngle.snapTo(45f)
            staticAngle.animateTo(
                405f,
                androidx.compose.animation.core.tween(
                    600, easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            )
            staticAngle.snapTo(45f)
        }
    }
    val angle = infAngle ?: staticAngle.value
    val colors = listOf(
        c.flowCoverColors[1].copy(alpha = borderAlpha),
        c.accent.copy(alpha = borderAlpha),
        c.flowCoverColors[0].copy(alpha = borderAlpha),
        c.accentCyan.copy(alpha = borderAlpha),
        c.flowCoverColors[1].copy(alpha = borderAlpha)
    )
    val shape = RoundedCornerShape(cornerRadius)

    Box(modifier = modifier) {
        // 边框层：matchParentSize 跟随内容层尺寸（内容层是唯一 wrap-content 子级，
        // 由它撑起外层 Box —— 之前两层都 matchParentSize 会导致 Box 坍缩成 0×0）
        Canvas(Modifier.matchParentSize().clip(shape)) {
            val w = size.width; val h = size.height
            val path = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        0f, 0f, w, h,
                        CornerRadius(cornerRadius.toPx())
                    )
                )
            }
            clipPath(path) {
                rotate(angle, pivot = Offset(w / 2f, h / 2f)) {
                    // 画一个足够大的旋转色轮，保证任何角度都盖满
                    val big = maxOf(w, h) * 2.2f
                    drawRect(
                        brush = Brush.sweepGradient(colors),
                        topLeft = Offset(w / 2f - big / 2f, h / 2f - big / 2f),
                        size = Size(big, big)
                    )
                }
            }
        }
        // 内容层：wrap-content（撑起外层），留出边框厚度，内部玻璃卡底
        Box(
            modifier = Modifier
                .padding(borderWidth)
                .clip(RoundedCornerShape(cornerRadius - borderWidth))
                .background(c.glassCard),
            content = content
        )
    }
}
