package com.example.s1client.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s1client.ui.theme.LocalS1Colors

/**
 * 独立玻璃卡形态（保留兼容）。
 * 「我的」页 A 秩序玻璃版会把 [AttendanceContent] 合并进主题+签到合并卡。
 */
@Composable
fun AttendanceRow(
    loggedIn: Boolean,
    signedToday: Boolean,
    streak: Int,
    total: Int,
    signingIn: Boolean = false,
    onSign: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LocalS1Colors.current.glassCard)
    ) {
        AttendanceContent(loggedIn, signedToday, streak, total, signingIn, onSign)
    }
}

/** 签到行内容（无卡片底），供独立卡与合并卡两种形态复用 */
@Composable
fun AttendanceContent(
    loggedIn: Boolean,
    signedToday: Boolean,
    streak: Int,
    total: Int,
    signingIn: Boolean = false,
    onSign: () -> Unit
) {
    val c = LocalS1Colors.current

    // 精简：不再堆"连续 + 累计"，只保留最能驱动回访的连签天数
    val subtitle = when {
        !loggedIn -> "登录后可签到"
        signingIn -> "签到中…"
        streak > 0 -> "已连续签到 $streak 天"
        signedToday -> "今日已签到"
        else -> "每日签到"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                // 已签 / 未登录 / 请求中不给点击反馈；未签到整行可点
                if (signedToday || !loggedIn || signingIn) Modifier
                else Modifier.clickable { onSign() }
            )
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    // 圆底统一取 chipBg：原来「已签到 = accent 18%」和「未签到 = chipBg」混出两种红
                    .background(c.chipBg),
                contentAlignment = Alignment.Center
            ) {
                if (signedToday) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = c.accentCyan,
                        modifier = Modifier.size(17.dp)
                    )
                } else {
                    Text(
                        text = "签",
                        color = c.accentCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("签到", color = c.textMain, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = subtitle,
                    color = c.textSub,
                    fontSize = 11.5.sp
                )
            }
            Text(
                text = if (signedToday) "已完成" else "去签到",
                color = if (signedToday) c.textSub else c.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // —— 本周进度：去掉发光与徽章，只留一条安静的细线（视觉弱化，信息保留在副标题里）
        if (loggedIn) {
            Spacer(Modifier.height(8.dp))
            WeekProgressBar(progress = streak.coerceAtMost(7) / 7f, streak = streak)
        }
    }
}

/**
 * 本周连签进度条：**静态细线，无发光**（视觉弱化）。
 * 原设计是"填充 + 高光持续扫过"的流光条，天天闪会钝化；
 * 发光已挪到「里程碑达成那一刻」，这里只负责安静地表示进度。
 */
@Composable
private fun WeekProgressBar(progress: Float, streak: Int) {
    val c = LocalS1Colors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(c.accent.copy(alpha = 0.10f))
    ) {
        if (streak > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0.04f, 1f))
                    .height(3.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(c.accent.copy(alpha = 0.75f))
            )
        }
    }
}
