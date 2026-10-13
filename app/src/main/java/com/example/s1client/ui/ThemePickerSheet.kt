package com.example.s1client.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s1client.ui.theme.LocalS1Colors
import com.example.s1client.ui.theme.s1PaletteFor
import com.example.s1client.util.ThemeManager

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * 主题色板小图：外圈是该主题的页面底色渐变，中间一颗强调色圆点。
 * 给「我的」页的主题行用。
 */
@Composable
fun ThemeSwatch(themeIndex: Int, size: Dp = 36.dp) {
    val p = s1PaletteFor(themeIndex)
    val shape = RoundedCornerShape(size * 0.3f)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(p.bgTop, p.bgBottom)))
            // 描边：深色主题（墨蓝/玄夜）底色接近弹窗底，没有这圈线会糊在一起
            .border(1.dp, p.glassLine, shape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.38f)
                .clip(CircleShape)
                .background(p.accent)
        )
    }
}

/** 主题预览卡片尺寸（轮播里居中那张的大小） */
private val CardSize = 156.dp

/**
 * 单个主题的「微缩预览」卡片：不是只写名字，而是把该主题的
 * 页面底色 / 卡片底色 / 主色 / 文字色 都按真实比例铺一遍，
 * 滑过去就能看清这套主题长什么样。
 */
@Composable
private fun ThemePreviewCard(themeIndex: Int, name: String, active: Boolean) {
    val p = s1PaletteFor(themeIndex)
    // 幻彩在列表里固定展示浅色那套 —— 否则点一下卡片会在深浅之间跳，容易被误解成 bug。
    // 实际"深浅随机"由主题名「紫粉流光 · 深浅随机」传达。
    val pPreview = if (themeIndex == ThemeManager.THEME_IRIDESCENT) {
        s1PaletteFor(themeIndex, seed = 0)
    } else p
    val shape = RoundedCornerShape(28.dp)
    // 每套主题一句特征说明；幻彩渐变提一句"深浅随机"
    val sub = when {
        themeIndex == ThemeManager.THEME_IRIDESCENT -> "紫粉流光 · 深浅随机"
        pPreview.isDark -> "深色 · 护眼"
        else -> "浅色 · 通透"
    }

    Column(
        modifier = Modifier
            .size(CardSize)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(pPreview.bgTop, pPreview.bgBottom)))
            .border(1.dp, if (active) pPreview.accent.copy(alpha = 0.75f) else pPreview.glassLine, shape)
            .padding(15.dp)
    ) {
        // —— 迷你卡片：模拟瀑布流里一张帖子的样子 ——
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(pPreview.cardBg)
        ) {
            // 顶部语义色条（Linear 墨蓝的标志性点缀）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(pPreview.flowCoverColors.getOrElse(0) { pPreview.accent })
            )
            Column(modifier = Modifier.padding(start = 9.dp, end = 9.dp, top = 13.dp)) {
                // 标题占位
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(pPreview.textMain.copy(alpha = 0.60f))
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.52f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(pPreview.textSub.copy(alpha = 0.45f))
                )
                Spacer(Modifier.height(9.dp))
                // 分类胶囊（用主色低透明铺底）
                Box(
                    modifier = Modifier
                        .size(width = 30.dp, height = 11.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(pPreview.accent.copy(alpha = 0.18f))
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            text = name,
            color = pPreview.textMain,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = sub,
            color = pPreview.textSub,
            fontSize = 10.5.sp,
            maxLines = 1
        )
    }
}

/**
 * 主题选择器（底部弹窗 · 左右滑动轮播）。
 *
 * 交互：左右滑动 → 居中那张自动生效（只有 4 套主题且切换是即时的，
 * 边滑边看效果，整页 + 本弹窗都会跟着变色）；点两侧的卡片则把它滑到中间。
 * 生效即时是因为 ThemeManager.currentTheme 是可观察状态，Compose 直接重组；
 * 这里额外把系统栏按新调色板刷一遍（不再重建 Activity，XML 主题属性还是旧的）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerSheet(onDismiss: () -> Unit) {
    val c = LocalS1Colors.current
    val hapticCtx = androidx.compose.ui.platform.LocalContext.current
    val themes = ThemeManager.getVisibleThemes()
    val startIndex = themes.indexOfFirst { it.first == ThemeManager.currentTheme }
        .coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { themes.size })
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // 滑到哪张就用哪张：状态变化即切换
    LaunchedEffect(pagerState, themes) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val idx = themes.getOrNull(page)?.first ?: return@collect
                if (ThemeManager.currentTheme != idx) {
                    ThemeManager.setTheme(idx)
                    // 细节 06 延伸：主题真正切换的瞬间给一次 SELECTION（切换手感）
                    runCatching { com.example.s1client.util.Haptic.selection(hapticCtx) }
                    // 系统栏的同步不在这里做：本组件是 ModalBottomSheet，
                    // 这里的 LocalContext 不是 Activity，强转必失败。
                    // 由宿主页面的 S1SystemBarSync 监听主题变化统一处理。
                }
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.glassDialog,
        contentColor = c.textMain,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = c.glassDialogLine) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 26.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "主题",
                    color = c.textMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "左右滑动切换 · 立即生效",
                    color = c.textSub,
                    fontSize = 11.5.sp
                )
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                // 首尾留白 = (可用宽 - 卡片宽)/2，保证第一张/最后一张也能滑到正中间
                val side = ((maxWidth - CardSize) / 2).coerceAtLeast(10.dp)
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = side),
                    pageSpacing = 14.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val item = themes.getOrNull(page) ?: return@HorizontalPager
                    // 与中线距离 0..1：中间的放大不透明，两侧缩小淡化（对齐参考稿）
                    val raw = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    val offset = (if (raw < 0f) -raw else raw).coerceIn(0f, 1f)
                    val t = 1f - offset
                    val scale = 0.85f + 0.15f * t
                    val alpha = 0.60f + 0.40f * t

                    // 细节 26：按压 3D 微倾 —— 按触点在左/右半边决定倾斜方向，
                    // 像捏着一张实体卡片看；松手弹簧回正
                    var tiltTarget by remember { mutableFloatStateOf(0f) }
                    val tilt by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = tiltTarget,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = 0.6f,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                        ),
                        label = "tilt"
                    )
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                                rotationY = tilt
                                cameraDistance = 8000f
                            }
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    tiltTarget = if (down.position.x < size.width / 2f) -4f else 4f
                                    waitForUpOrCancellation()
                                    tiltTarget = 0f
                                }
                            }
                            .clickable(enabled = page != pagerState.currentPage) {
                                // 点两侧卡片：滑到中间（到位后自动生效）
                                scope.launch { pagerState.animateScrollToPage(page) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        ThemePreviewCard(
                            themeIndex = item.first,
                            name = item.second,
                            active = page == pagerState.currentPage
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 小圆点指示器
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(themes.size) { i ->
                    val on = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (on) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (on) c.textMain else c.textSub.copy(alpha = 0.32f)
                            )
                    )
                }
            }

        }
    }
}

/**
 * 「我的」页的主题入口行：调色板图标 + 主题名 + 右侧当前配色预览。
 */
@Composable
fun ThemeEntryRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalS1Colors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.glassCard)
            .clickable { onClick() }
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
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                tint = c.accent,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("主题", color = c.textMain, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                text = ThemeManager.getAllThemeNames()
                    .getOrElse(ThemeManager.currentTheme) { "" },
                color = c.textSub,
                fontSize = 11.5.sp
            )
        }
        ThemeSwatch(themeIndex = ThemeManager.currentTheme, size = 30.dp)
        Spacer(Modifier.width(6.dp))
        Text("›", color = c.textSub, fontSize = 20.sp)
    }
}
