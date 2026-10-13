package com.example.s1client.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 卡片按压缩放：按下时轻微缩小，松手弹性回弹。
 *
 * 用法：interactionSource 要和卡片点击用的是同一个，才能感知按下状态：
 * ```
 * val source = remember { MutableInteractionSource() }
 * Card(modifier = Modifier.pressScale(source).combinedClickable(
 *     interactionSource = source, ...
 * ))
 * ```
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.98f
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        // 无回弹的平滑动画：回弹(MediumBouncy)会过冲原尺寸，看起来像卡顿/抖一下
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
