package com.example.s1client.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s1client.data.model.UserPostItem
import com.example.s1client.ui.design.AccentPink
import com.example.s1client.ui.design.ChipBg
import com.example.s1client.ui.design.GlassCard
import com.example.s1client.ui.design.GlassLine
import com.example.s1client.ui.design.TextMain
import com.example.s1client.ui.design.TextSub
import java.util.Locale

/**
 * 收藏列表的行 —— **指标块**版式。
 *
 * 为什么不用 [com.example.s1client.ThreadRow]：那个是帖子列表的版式，
 * 收藏页照用会长得一模一样、没有记忆点。这里改成「版块名 + 标题 + 三个指标格」，
 * 数字 21sp : 标签 13sp ≈ φ、格间距与内边距走斐波那契（宪法第 15 条），
 * 与「我的」页顶格同一套比例，视觉上成一家。
 *
 * 三个格按需出现：**数值为 0 或取不到就整格隐藏**，不留空位。
 * 收藏接口不返回作者，所以这里没有作者行（版块名与指标承担信息）。
 *
 * @param onForumClick 点版块名跳转；为 null 时标签不可点
 * @param onLongPress 长按 = 取消收藏
 */
@Composable
fun FavoriteRow(
    item: UserPostItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onForumClick: (() -> Unit)? = null,
    onLongPress: () -> Unit
) {
    val views = item.views.trim().toIntOrNull() ?: 0
    val replies = item.replies.trim().toIntOrNull() ?: 0

    val metrics = buildList {
        if (views > 0) add(views.readableCount() to "浏览")
        if (replies > 0) add(replies.readableCount() to "回复")
        if (item.dateline.isNotBlank()) add(item.dateline to "收藏")
    }

    val pressSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = pressSource,
                indication = androidx.compose.material.ripple.rememberRipple(),
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        border = BorderStroke(1.dp, GlassLine)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {

            // —— 标题行：竖杠 + 版块名 + 标题（标签不独占一行，否则右侧空着不好看）——
            Row(verticalAlignment = Alignment.Top) {
                if (item.forum.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .width(3.dp)
                            .height(13.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(AccentPink)
                    )
                    Spacer(Modifier.width(5.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentPink.copy(alpha = 0.13f))
                            .then(
                                if (onForumClick != null) Modifier.clickable { onForumClick() }
                                else Modifier
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            item.forum,
                            color = AccentPink,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }

                Text(
                    item.subject,
                    color = TextMain,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 21.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            if (metrics.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .height(1.dp)
                        .background(ChipBg)
                )
                Row(modifier = Modifier.padding(top = 2.dp)) {
                    metrics.forEach { (n, label) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                n,
                                color = TextMain,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 21.sp,
                                maxLines = 1
                            )
                            Text(
                                label,
                                color = TextSub,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 大数字压缩显示：1234 → 1.2k、12345 → 12.3k、999 → 999 */
internal fun Int.readableCount(): String =
    if (this >= 1000) String.format(Locale.CHINA, "%.1fk", this / 1000.0) else toString()
