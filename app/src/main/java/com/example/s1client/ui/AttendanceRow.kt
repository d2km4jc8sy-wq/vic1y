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

    val subtitle = when {
        !loggedIn -> "登录后可签到"
        signingIn -> "签到中…"
        signedToday && streak > 0 -> "已连续签到 $streak 天 · 累计 $total 天"
        signedToday -> "今日已签到 · 累计 $total 天"
        streak > 0 -> "已连续 $streak 天"
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
                    .background(if (signedToday) c.accent.copy(alpha = 0.18f) else c.chipBg),
                contentAlignment = Alignment.Center
            ) {
                if (signedToday) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = c.accent,
                        modifier = Modifier.size(17.dp)
                    )
                } else {
                    Text(
                        text = "签",
                        color = c.accent,
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

        // —— 本周进度 + 里程碑徽章（登录后显示）——
        if (loggedIn) {
            Spacer(Modifier.height(10.dp))
            WeekProgressBar(progress = streak.coerceAtMost(7) / 7f, streak = streak)
            Spacer(Modifier.height(8.dp))
            MilestoneBadges(streak = streak)
        }
    }
}

/** 细流光进度条：填充 + 表面高光持续扫过 */
@Composable
private fun WeekProgressBar(progress: Float, streak: Int) {
    val c = LocalS1Colors.current
    val transition = rememberInfiniteTransition(label = "attSweep")
    val sweep by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "sweep"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(c.accent.copy(alpha = 0.12f))
    ) {
        if (streak > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0.04f, 1f))
                    .height(5.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(c.accent)
                    .drawBehind {
                        // 表面高光带，随 sweep 左右扫过
                        val band = size.width * 0.4f
                        val x = sweep * size.width
                        drawRect(
                            brush = Brush.linearGradient(
                                listOf(
                                    androidx.compose.ui.graphics.Color.Transparent,
                                    androidx.compose.ui.graphics.Color.White.copy(alpha = 0.65f),
                                    androidx.compose.ui.graphics.Color.Transparent
                                ),
                                start = androidx.compose.ui.geometry.Offset(x, 0f),
                                end = androidx.compose.ui.geometry.Offset(x + band, 0f)
                            )
                        )
                    }
            )
        }
    }
}

/** 连签里程碑徽章：7 / 30 / 100 天，达到即点亮（渐变实心），未达为灰底（细节 24） */
@Composable
private fun MilestoneBadges(streak: Int) {
    val c = LocalS1Colors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        listOf(7, 30, 100).forEachIndexed { i, milestone ->
            val lit = streak >= milestone
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (lit) androidx.compose.ui.graphics.SolidColor(c.accent)
                        else androidx.compose.ui.graphics.SolidColor(c.chipBg)
                    )
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    "连签$milestone",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (lit) androidx.compose.ui.graphics.Color.White else c.textSub.copy(alpha = 0.7f)
                )
            }
            if (i < 2) Spacer(Modifier.width(6.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(
            when {
                streak >= 100 -> "签到传说"
                streak >= 30 -> "签到大师"
                streak >= 7 -> "签到新手"
                else -> "继续加油"
            },
            fontSize = 9.5.sp,
            color = c.textSub.copy(alpha = 0.8f)
        )
    }
}
