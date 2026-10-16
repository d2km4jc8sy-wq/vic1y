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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s1client.data.model.PollOption
import com.example.s1client.data.model.ThreadPoll
import com.example.s1client.ui.theme.LocalS1Colors
import kotlinx.coroutines.launch

/**
 * 投票卡 —— 数据模型、投票协议与**渲染语义照搬开源 s1er**
 * （models/poll.dart + widgets/poll_card.dart），视觉按本项目宪法收敛。
 *
 * ⚠️ 上一版最大的错：把"投票态"和"结果态"做成了**二选一**
 * （`if (showResults) 结果行 else 投票行`）。于是 `visiblepoll=1` 的帖
 * （未投票也能看结果）只剩百分比、**没有选项框 → 根本没法投**。
 *
 * s1er 的 `_PollOptionTile` 是**同时**渲染的：
 *   - `canSelect` → 画单选圆框 / 多选方框
 *   - `showResults` → 同一行右侧给百分比、行下给进度条与票数
 * 所以这里改成一份选项行同时承载「可选框 + 结果」，判定条件与 s1er 逐条对齐：
 *   canInteract = poll.canVote && isLoggedIn
 *   showResults = poll.showResults（visiblepoll=1 或已不能投）
 */
@Composable
fun PollCard(
    poll: ThreadPoll,
    tid: String,
    onSubmit: suspend (List<String>) -> String?,
    isLoggedIn: Boolean,
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
    // 与 s1er 一致：能投票 = 帖还没结束 + 已登录
    val canInteract = poll.canVote && isLoggedIn

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.glassCard)
            .padding(13.dp)
    ) {
        // —— 标题行：投票 + 单选/多选 + 剩余时间 ——
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("投票", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.accent)
            Spacer(Modifier.width(8.dp))
            Text(poll.voteModeLabel, fontSize = 11.sp, color = c.textSub)
            Spacer(Modifier.weight(1f))
            Text(poll.remainTimeLabel, fontSize = 11.sp, color = c.textSub)
        }

        Spacer(Modifier.height(8.dp))

        poll.options.forEach { opt ->
            PollOptionRow(
                option = opt,
                selected = opt.id in selected,
                canSelect = canInteract,
                multiple = poll.multiple,
                showResults = showResults,
                enabled = !submitting,
                onToggle = {
                    selected = if (poll.multiple) {
                        when {
                            opt.id in selected -> selected - opt.id
                            selected.size >= poll.maxChoices -> {
                                errorMsg = "最多选择 ${poll.maxChoices} 项"
                                selected
                            }
                            else -> selected + opt.id
                        }
                    } else setOf(opt.id)
                }
            )
        }

        // —— 底部：已投提示 / 参与人数 / 提交 ——
        Spacer(Modifier.height(8.dp))

        if (voted.isNotEmpty() || justVoted) {
            Text(
                "已标注您投过的选项",
                fontSize = 11.sp, color = c.accent,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "共 ${poll.votersCount} 人参与",
                fontSize = 11.sp, color = c.textSub,
                modifier = Modifier.weight(1f)
            )
            errorMsg?.let {
                // 细节轮2：过期提示改用主题语义红（原为硬编码 #B4483F，违反宪法18）
                Text(it, fontSize = 11.sp, color = c.heart)
            }
            if (canInteract) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (submitting || selected.isEmpty()) c.chipBg else c.accent)
                        .clickable(enabled = !submitting && selected.isNotEmpty()) {
                            submitting = true
                            errorMsg = null
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
                        color = if (submitting || selected.isEmpty()) c.textSub else c.onAccent
                    )
                }
            }
        }

        if (poll.canVote && !isLoggedIn) {
            Spacer(Modifier.height(5.dp))
            Text("登录后可参与投票", fontSize = 11.sp, color = c.textSub)
        }
    }
}

/**
 * 单个选项行 —— 结构与 s1er 的 `_PollOptionTile` 对齐：
 *   [选项框]? 选项文字 [我的投票]? [百分比]?
 *   [进度条]
 *   [票数]（右对齐）
 *
 * 选项框与进度条**可以同时出现**（能投票 + 可看结果时就是这种组合）。
 */
@Composable
private fun PollOptionRow(
    option: PollOption,
    selected: Boolean,
    canSelect: Boolean,
    multiple: Boolean,
    showResults: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val c = LocalS1Colors.current
    val isMine = option.isUserVote
    val highlight = isMine || (selected && canSelect)
    val barColor = parsePollColor(option.colorHex, c.accent)
    val boxShape = if (multiple) RoundedCornerShape(5.dp) else CircleShape

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(if (highlight) c.accent.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(enabled = canSelect && enabled) { onToggle() }
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (canSelect) {
                Box(
                    modifier = Modifier
                        .size(17.dp)
                        .clip(boxShape)
                        .border(
                            1.5.dp,
                            if (selected) c.accent else c.textSub.copy(alpha = 0.5f),
                            boxShape
                        )
                        .background(if (selected) c.accent else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Text(
                            "✓", fontSize = 9.sp, color = c.onAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
            } else if (isMine) {
                // 不能投票时，只给"我投过这项"一个小圆点（不抢文字）
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(c.accent)
                )
                Spacer(Modifier.width(5.dp))
            }

            Text(
                option.text, fontSize = 13.sp,
                color = if (highlight) c.textMain else c.textMain.copy(alpha = 0.85f),
                fontWeight = if (highlight) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.weight(1f), maxLines = 2
            )

            if (isMine) {
                Spacer(Modifier.width(5.dp))
                Text(
                    "我的投票", fontSize = 10.sp, color = c.accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(c.accent.copy(alpha = 0.13f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }

            if (showResults) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "${option.percent.toInt()}%", fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, color = barColor
                )
            }
        }

        if (showResults) {
            val anim by animateFloatAsState(
                targetValue = (option.percent / 100.0).toFloat().coerceIn(0f, 1f),
                label = "pollBar"
            )
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
            Spacer(Modifier.height(2.dp))
            Text(
                "${option.votes} 票", fontSize = 10.sp, color = c.textSub,
                modifier = Modifier.align(Alignment.End)
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
