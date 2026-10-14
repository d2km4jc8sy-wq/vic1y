package com.example.s1client

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s1client.data.model.UserPostItem
import com.example.s1client.ui.design.AccentPink
import com.example.s1client.ui.design.BgColor
import com.example.s1client.ui.design.BgColorBottom
import com.example.s1client.ui.design.GlassBar
import com.example.s1client.ui.design.GlassFloatLine
import com.example.s1client.ui.design.TextMain
import com.example.s1client.ui.design.TextSub

/**
 * 「我的主题 / 我的回帖 / 收藏」共用的列表页。
 *
 * 数据仍走各自的 HTML 接口（[load] 里注入），渲染统一交给 [ThreadRow]，
 * 这样以后列表样式只维护一处。
 */
@Composable
fun UserPostListScreen(
    title: String,
    emptyText: String,
    authorId: String?,
    authorName: String?,
    onBack: () -> Unit,
    load: suspend () -> List<UserPostItem>,
    /** 外部触发重载（例如收藏页回到前台时刷新）；值一变就重新加载 */
    reloadTick: Int = 0,
    /**
     * 是否显示每条记录的作者。收藏列表传 false ——
     * Discuz 收藏页不返回作者，页面只能给出标题 / 版块名 / 收藏时间。
     */
    showAuthor: Boolean = true,
    /**
     * 嵌入模式：宿主已经画了背景、状态栏与顶栏，这里就只出列表。
     * 「我的收藏」用它当「主题」分段；独立页面（如「我的主题」）保持默认 false。
     */
    embedded: Boolean = false
) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var posts by remember { mutableStateOf<List<UserPostItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey, reloadTick) {
        loading = true
        error = null
        try {
            posts = load()
        } catch (e: Exception) {
            error = e.message ?: "加载失败"
            posts = emptyList()
        }
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (embedded) Modifier
                else Modifier
                    .background(Brush.verticalGradient(listOf(BgColor, BgColorBottom)))
                    .statusBarsPadding()
            )
    ) {
        if (!embedded) {
            // ===== 顶栏（玻璃 + 底边亮线） =====
            // drawBehind 闭包不是组合上下文，颜色要在组合期先取出来
            val topBarLine = GlassFloatLine
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(GlassBar)
                    .drawBehind {
                        drawLine(
                            color = topBarLine,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = TextMain,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    color = TextMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 56.dp)
                )
            }
        }

        // ===== 内容 =====
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                loading -> CircularProgressIndicator(color = AccentPink)

                error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error ?: "", color = TextSub, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "点击重试",
                        color = AccentPink,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .clickable { reloadKey++ }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                posts.isEmpty() -> Text(emptyText, color = TextSub, fontSize = 14.sp)

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(posts.size) { index ->
                        val item = posts[index]
                        ThreadRow(
                            post = item.toPost(authorId, authorName),
                            isRead = false,
                            subtitle = item.message.takeIf { it.isNotBlank() },
                            showAuthor = showAuthor,
                            onClick = {
                                if (item.tid.isNotBlank()) {
                                    context.startActivity(
                                        Intent(context, ThreadDetailActivity::class.java).apply {
                                            putExtra("tid", item.tid)
                                            putExtra("subject", item.subject)
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/** HTML 列表项 → 统一列表项模型（作者信息由页面注入） */
private fun UserPostItem.toPost(uid: String?, name: String?): Post = Post(
    id = tid.toIntOrNull() ?: 0,
    title = subject.ifBlank { "(无标题)" },
    coverUrl = null,
    tag = forum.takeIf { it.isNotBlank() },
    authorName = name,
    authorId = uid,
    replies = replies.toIntOrNull() ?: 0,
    tid = tid,
    timeText = dateline,
    views = views.toIntOrNull() ?: 0,
    hasImage = hasImage
)
