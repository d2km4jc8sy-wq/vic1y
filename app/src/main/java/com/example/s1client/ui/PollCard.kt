package com.example.s1client.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s1client.data.model.PollOption
import com.example.s1client.data.model.ThreadPoll
import com.example.s1client.ui.theme.LocalS1Colors
import kotlinx.coroutines.launch

/**
 * 投票卡 —— 数据模型与投票协议照搬开源 s1er（models/poll.dart + poll_card.dart），
 * 视觉按本项目宪法重做：一个容器一个主角、间距走斐波那契、单线条。
 *
 * 状态机（与 s1er 一致）：
 *   canVote      → 可投票（未过期、未投过、论坛允许）
 *   showResults  → 显示票数与百分比（visiblepoll=1，或已不能投）
 *   hasUserVoted → 本地缓存里记着我投过哪几项（API 不返回这个字段）
 *
 * 交互：单选直接提交；多选先勾选、点「投票」提交，超过 maxChoices 时 toast 提示。
 */
@Composable
fun PollCard(
    poll: ThreadPoll,
    tid: String,
    onSubmit: suspend (List<String>) -> String?,
    modifier: Modifier = Modifier
) {
    val c = LocalS1Colors.current
    val scope = rememberCoroutineScope()

    // 本地缓存合并：API 不返回"我投了哪项"，进页面时用记住的选择回填
    var voted by remember(poll) { mutableStateOf(poll.userVotedOptionIds.toSet()) }
    var selected by remember(poll) { mutableStateOf(setOf<String>()) }
    var submitting by remember(poll) { mutableStateOf(false) }
    var justVoted by remember(poll) { mutableStateOf(false) }
    var errorMsg by remember(poll) { mutableStateOf<String?>(null) }

    val showResults = poll.showResults || voted.isNotEmpty() || justVoted
    val canVote = poll.canVote && voted.isEmpty() && !justVoted

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.glassCard)
            .padding(13.dp)
    ) {
        // —— 标题行：投票 + 剩余时间/人数 —— 
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "投票", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.accent
            )
            Spacer(Modifier.width(8.dp))
            Text(
                poll.voteModeLabel, fontSize = 11.sp, color = c.textSub
            )
            Spacer(Modifier.weight(1f))
            Text(
                poll.remainTimeLabel, fontSize = 11.sp, color = c.textSub
            )
        }

        Spacer(Modifier.height(8.dp))

        poll.options.forEach { opt ->
            val isMine = opt.id in voted
            val isPicked = opt.id in selected
            val barColor = parsePollColor(opt.colorHex, c.accent)

            if (showResults) {
                // —— 结果态：文字 + 进度条 + 百分比（我投的高亮描边）——
                val anim by animateFloatAsState(
                    targetValue = (opt.percent / 100.0).toFloat().coerceIn(0f, 1f),
                    label = "pollBar"
                )
                Column(Modifier.padding(vertical = 5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isMine) {
                            Box(
                                Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(c.accent)
                            )
                            Spacer(Modifier.width(5.dp))
                        }
                        Text(
                            opt.text, fontSize = 13.sp,
                            color = if (isMine) c.textMain else c.textMain.copy(alpha = 0.85f),
                            fontWeight = if (isMine) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.weight(1f), maxLines = 2
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${opt.percent.toInt()}%", fontSize = 12.sp,
                            fontWeight = FontWeight.Bold, color = barColor
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(c.chipBg)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(anim)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isMine) c.accent else barColor.copy(alpha = 0.55f))
                        )
                    }
                }
            } else {
                // —— 投票态：单选行 / 多选勾选 ——
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (isPicked) c.accent.copy(alpha = 0.10f) else Color.Transparent)
                        .clickable(enabled = !submitting) {
                            selected = if (poll.multiple) {
                                if (isPicked) selected - opt.id
                                else if (selected.size >= poll.maxChoices) {
                                    errorMsg = "最多选择 ${poll.maxChoices} 项"
                                    selected
                                } else selected + opt.id
                            } else setOf(opt.id)
                        }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(17.dp)
                            .clip(if (poll.multiple) RoundedCornerShape(5.dp) else CircleShape)
                            .border(
                                1.5.dp,
                                if (isPicked) c.accent else c.textSub.copy(alpha = 0.5f),
                                if (poll.multiple) RoundedCornerShape(5.dp) else CircleShape
                            )
                            .background(if (isPicked) c.accent else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPicked) {
                            Text(
                                "✓", fontSize = 9.sp, color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        opt.text, fontSize = 13.sp, color = c.textMain,
                        modifier = Modifier.weight(1f), maxLines = 2
                    )
                }
            }
        }

        // —— 底部：提交按钮 / 已投提示 / 人数 ——
        if (canVote) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${poll.votersCount} 人已投票", fontSize = 11.sp, color = c.textSub,
                    modifier = Modifier.weight(1f)
                )
                errorMsg?.let {
                    Text(it, fontSize = 11.sp, color = Color(0xFFB4483F))
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (submitting || selected.isEmpty()) c.chipBg else c.accent)
                        .clickable(enabled = !submitting && selected.isNotEmpty()) {
                            submitting = true
                            scope.launch {
                                val err = onSubmit(selected.toList())
                                submitting = false
                                if (err == null) {
                                    voted = selected
                                    justVoted = true
                                } else {
                                    errorMsg = err
                                }
                            }
                        }
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                ) {
                    Text(
                        if (submitting) "提交中…" else "投票",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (submitting || selected.isEmpty()) c.textSub else Color.White
                    )
                }
            }
        } else if (showResults) {
            Spacer(Modifier.height(8.dp))
            Text(
                "${poll.votersCount} 人已投票" +
                    if (voted.isNotEmpty() || justVoted) " · 已参与" else "",
                fontSize = 11.sp, color = c.textSub
            )
        }
    }
}

/** Discuz 给每个选项的颜色（形如 "6750A4"），解析失败回退主题色 */
internal fun parsePollColor(hex: String, fallback: Color): Color {
    val clean = hex.trim().removePrefix("#")
    if (clean.length != 6 || !clean.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
        return fallback
    }
    return try {
        Color(0xFF000000L or clean.toLong(16))
    } catch (_: Exception) {
        fallback
    }
}
