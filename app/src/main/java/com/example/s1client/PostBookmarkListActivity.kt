package com.example.s1client

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.s1client.ui.showUserCardDialog
import com.example.s1client.ui.theme.LocalS1Colors
import com.example.s1client.ui.theme.S1Theme
import com.example.s1client.util.Haptic
import com.example.s1client.data.store.PostBookmark
import com.example.s1client.data.store.PostBookmarkManager
import com.example.s1client.util.S1Notify
import com.example.s1client.util.applyS1Theme
import com.example.s1client.util.setupS1EdgeToEdge
import kotlinx.coroutines.launch

/**
 * 本地楼层收藏列表。
 *
 * 收藏的楼层以快照形式存在本地，即使原帖被删除或编辑，这里仍能回看当时的内容。
 */
class PostBookmarkListActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applyS1Theme()
        super.onCreate(savedInstanceState)
        setupS1EdgeToEdge()

        setContent {
            S1Theme {
                PostBookmarkListScreen(onFinish = { finish() })
            }
        }
    }
}

@Composable
internal fun PostBookmarkListScreen(onFinish: () -> Unit, embedded: Boolean = false) {
    val c = LocalS1Colors.current
    val context = LocalContext.current
    var bookmarks by remember { mutableStateOf(PostBookmarkManager.getAll()) }
    var viewing by remember { mutableStateOf<PostBookmark?>(null) }
    var deleting by remember { mutableStateOf<PostBookmark?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (embedded) Modifier
                else Modifier.background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
            )
    ) {
        Column {
            if (!embedded) {
                TopBar(title = "楼层收藏", onBack = onFinish)
            }

            if (bookmarks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "还没有收藏的楼层\n在帖子详情里点心形星标即可收藏",
                        fontSize = 14.sp,
                        color = c.textSub,
                        lineHeight = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(bookmarks, key = { it.id }) { bm ->
                        BookmarkCard(
                            bookmark = bm,
                            onClick = { viewing = bm },
                            onDelete = { deleting = bm }
                        )
                    }
                }
            }
        }
    }

    viewing?.let { bm ->
        BookmarkDetailDialog(
            bookmark = bm,
            onDismiss = { viewing = null },
            onDelete = {
                deleting = bm
                viewing = null
            }
        )
    }

    deleting?.let { bm ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除收藏") },
            text = { Text("确定将「${bm.author ?: "未知作者"}」的这条楼层收藏删除吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            PostBookmarkManager.removeById(context, bm.id)
                            bookmarks = PostBookmarkManager.getAll()
                            deleting = null
                            S1Notify.show(context, "已删除")
                        }
                    }
                ) { Text("删除", color = c.danger) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("取消", color = c.textSub) }
            }
        )
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    val c = LocalS1Colors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(c.headerGradient))
            .padding(top = 48.dp, bottom = 10.dp, start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = if (c.isDark) c.textMain else Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = if (c.isDark) c.textMain else Color.White,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BookmarkCard(bookmark: PostBookmark, onClick: () -> Unit, onDelete: () -> Unit) {
    val c = LocalS1Colors.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.glassCard)
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = bookmark.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(c.accent.copy(alpha = 0.2f))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookmark.author ?: "未知作者",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.textMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        bookmark.position?.let { "${it}楼" },
                        bookmark.timeText.takeIf { it.isNotBlank() }
                    ).joinToString(" · "),
                    fontSize = 11.sp,
                    color = c.textSub,
                    maxLines = 1
                )
            }
            IconButton(onClick = {
                Haptic.light(context)
                onDelete()
            }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = c.danger,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 帖子标题
        if (bookmark.subject.isNotBlank()) {
            Text(
                text = bookmark.subject,
                fontSize = 12.sp,
                color = c.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
        }

        // 内容摘要（最多 3 行）
        Text(
            text = bookmark.plainText.take(180),
            fontSize = 14.sp,
            color = c.textMain,
            lineHeight = 20.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BookmarkDetailDialog(
    bookmark: PostBookmark,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val c = LocalS1Colors.current
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 42.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.glassCard)
                    .padding(18.dp)
            ) {
                // 作者头
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = bookmark.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(c.accent.copy(alpha = 0.2f))
                            .clickable(enabled = !bookmark.authorId.isNullOrBlank()) {
                                showUserCardDialog(
                                    context as ComponentActivity,
                                    // R7（审计报告 v2 2026-10-18 第 9 轮）：去掉 !! ——
                                    // authorId 为 null 时传空串（用户卡片按未取到 uid 处理），不再崩溃
                                    bookmark.authorId ?: "",
                                    bookmark.author ?: "未知"
                                )
                            }
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bookmark.author ?: "未知作者",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = c.textMain
                        )
                        Text(
                            text = listOfNotNull(
                                bookmark.position?.let { "${it}楼" },
                                bookmark.timeText.takeIf { it.isNotBlank() }
                            ).joinToString(" · "),
                            fontSize = 11.sp,
                            color = c.textSub
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("关闭", color = c.textSub) }
                }

                Spacer(Modifier.height(10.dp))

                // 帖子标题
                if (bookmark.subject.isNotBlank()) {
                    Text(
                        text = bookmark.subject,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = c.accent,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // 原文
                Text(
                    text = bookmark.plainText.ifBlank { "（无文字内容）" },
                    fontSize = 15.sp,
                    color = c.textMain,
                    lineHeight = 22.sp,
                    modifier = Modifier.heightIn(min = 60.dp, max = 320.dp)
                )

                Spacer(Modifier.height(14.dp))

                // 底部操作
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDelete) {
                        Text("删除收藏", color = c.danger)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        val intent = Intent(context, ThreadDetailActivity::class.java).apply {
                            putExtra("tid", bookmark.tid)
                            putExtra("subject", bookmark.subject)
                        }
                        context.startActivity(intent)
                    }) {
                        Text("查看原帖", color = c.accent)
                    }
                }
            }
        }
    }
}
