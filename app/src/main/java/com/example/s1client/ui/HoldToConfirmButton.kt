package com.example.s1client.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s1client.ui.theme.LocalS1Colors
import com.example.s1client.util.Haptic
import kotlinx.coroutines.launch

/**
 * 长按确认按钮（细节 27）：危险操作的物理防误触。
 *
 * 按住 1 秒，红色进度条从左到右充满才触发 onConfirm；
 * 中途松手进度回弹（配 SELECTION 震），充满瞬间 HEAVY 震。
 * 用于「退出登录」等破坏性操作，替代二次弹窗拦截。
 */
@Composable
fun HoldToConfirmButton(
    label: String,
    modifier: Modifier = Modifier,
    holdMillis: Int = 1000,
    fillColor: Color = LocalS1Colors.current.danger,
    onConfirm: () -> Unit
) {
    val c = LocalS1Colors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var confirmedOnce = remember { androidx.compose.runtime.mutableStateOf(false) }
    // 按住期间换文案：让"正在确认"这件事看得见（真机反馈：原静默长按无感知）
    val holdLabel = when {
        confirmedOnce.value -> "已退出登录"
        progress.value > 0.02f -> "松开取消"
        else -> label
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(fillColor.copy(alpha = 0.10f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        confirmedOnce.value = false
                        val fill = scope.launch {
                            progress.animateTo(1f, tween(holdMillis))
                            if (progress.value >= 1f && !confirmedOnce.value) {
                                confirmedOnce.value = true
                                Haptic.heavy(context)
                                onConfirm()
                            }
                        }
                        tryAwaitRelease()
                        if (!confirmedOnce.value && progress.value < 1f) {
                            fill.cancel()
                            if (progress.value > 0.05f) Haptic.selection(context)
                            scope.launch {
                                progress.animateTo(0f, tween(220))
                            }
                        }
                    }
                )
            }
    ) {
        // 进度填充层
        Box(
            Modifier
                .fillMaxWidth(progress.value.coerceIn(0f, 1f))
                .matchParentSize()
                .background(fillColor.copy(alpha = 0.55f))
        )
        Text(
            holdLabel,
            color = if (progress.value > 0.85f) Color.White else fillColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center).padding(vertical = 13.dp)
        )
    }
}
