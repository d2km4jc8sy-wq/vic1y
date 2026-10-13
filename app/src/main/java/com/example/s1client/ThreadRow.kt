package com.example.s1client

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.s1client.ui.pressScale
import com.example.s1client.util.avatarUrl

/** 热帖火图标：琥珀色 */
private val FireAmber = Color(0xFFFFB454)

/** 回帖数 ≥ 这个值算热帖 */
private const val HOT_REPLY_THRESHOLD = 300

/**
 * 统一的帖子列表项 —— 所有列表页共用（搜索 / 我的主题 / 我的回帖 / 收藏）。
 *
 * 元素与位置沿用 1.0 的 item_thread：
 *   分类竖条标签 · 标题 · [图]  ／  （正文）  ／  分隔线  ／  头像 · 作者 · 日期 …… [火] 回复图标 · 回帖数
 *
 * - 分类与标题顶部对齐（同一 26sp 行高）
 * - 不显示查看数
 * - 回帖数用矢量回复图标 + 数字（无底色）；热帖在左侧多一个琥珀色火图标，不标红
 * - 缺数据的字段直接不显示，不留空白占位
 *
 * 首页是双列瀑布流，用 XhsPostCard，不走这里。
 */
@Composable
fun ThreadRow(
    post: Post,
    isRead: Boolean = false,
    /** 标题下的正文/摘要。只有「我的主题 / 我的回帖」这类有正文的页面才传，搜索页不传 */
    subtitle: String? = null,
    onClick: () -> Unit,
    onAvatarClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null
) {
    val title = post.title ?: "无标题"
    val tag = post.tag
    val author = post.authorName
    val timeText = post.timeText ?: ""
    val replies = post.replies ?: 0
    val isHot = replies >= HOT_REPLY_THRESHOLD
    val accent = if (isRead) TextSub else AccentPink

    // 按压缩放：interactionSource 与点击共用
    val pressSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(pressSource)
            .combinedClickable(
                interactionSource = pressSource,
                indication = androidx.compose.material.ripple.rememberRipple(),
                onClick = onClick,
                onLongClick = { onLongPress?.invoke() }
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, GlassLine)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {

            // ===== 第一行：分类竖条标签 + 标题 + [图] =====
            Row(verticalAlignment = Alignment.Top) {
                if (!tag.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            // 13dp 的竖条在 26sp 的行高里居中
                            .padding(top = 6.dp)
                            .size(width = 3.dp, height = 13.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accent)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = tag,
                        color = accent,
                        fontSize = 14.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 96.dp)
                    )
                    Spacer(Modifier.width(9.dp))
                }

                Text(
                    text = title,
                    color = if (isRead) TextSub else TextMain,
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (post.hasImage) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "[图]",
                        color = AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentCyan.copy(alpha = 0.16f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }

            // ===== 正文（我的主题 / 我的回帖） =====
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = TextSub,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            // ===== 分隔线 =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(1.dp)
                    .background(ChipBg)
            )

            // ===== 第二行：头像 · 作者 · 日期 …… 火 · 回复 · 回帖数 =====
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                val uid = post.authorId
                val avatarUrlStr = if (!uid.isNullOrBlank()) avatarUrl(uid, "small") else null
                val seed = author?.ifBlank { "?" } ?: "?"
                val avatarBrush = remember(seed) { Brush.linearGradient(getAvatarGradient(seed)) }

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(avatarBrush)
                        .then(
                            if (onAvatarClick != null) Modifier.clickable { onAvatarClick() }
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = seed.firstOrNull()?.toString()?.uppercase() ?: "?",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (avatarUrlStr != null) {
                        AsyncImage(
                            model = avatarUrlStr,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                if (!author.isNullOrBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = author,
                        color = TextSub,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(10.dp))
                Text(
                    text = timeText,
                    color = TextSub,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // 热帖：左侧一个琥珀色火图标（数字不变红）
                if (isHot) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = "热帖",
                        tint = FireAmber,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                }

                // 回帖数：矢量图标 + 数字，无底色
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "回复",
                    tint = TextSub,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = replies.toString(),
                    color = TextSub,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
