package com.example.s1client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.model.DarkRoomEntry
import com.example.s1client.ui.theme.LocalS1Colors
import com.example.s1client.ui.theme.S1Theme
import com.example.s1client.util.DarkRoomParser
import com.example.s1client.util.applyS1Theme
import com.example.s1client.util.setupS1EdgeToEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

/**
 * 小黑屋 —— 论坛公开处罚列表。
 *
 * 接口与解析口径完全抄自开源 s1er：
 * - URL   `forum.php?mod=misc&action=showdarkroom&ajaxdata=json[&cid=游标]`
 * - 解析  `util/DarkRoomParser.kt`（对应 s1er 的 `parseDarkRoomJson`）
 *
 * 视觉沿用项目既有语言：玻璃卡、纯文字、无多余装饰。
 */
class DarkRoomActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applyS1Theme()
        super.onCreate(savedInstanceState)
        setupS1EdgeToEdge()

        setContent {
            S1Theme {
                DarkRoomScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun DarkRoomScreen(onBack: () -> Unit) {
    val c = LocalS1Colors.current
    val listState = rememberLazyListState()
    // Composable 里不能直接 startActivity（没有 Activity 引用），走 LocalContext
    val ctx = LocalContext.current

    var items by remember { mutableStateOf<List<DarkRoomEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }
    var cursor by remember { mutableStateOf("") }
    var hasMore by remember { mutableStateOf(true) }

    // 首屏 + 翻页统一入口。cursor 为空表示首屏。
    suspend fun loadPage(cid: String): DarkRoomParser.Page = withContext(Dispatchers.IO) {
        try {
            val body = RetrofitClient.api.getDarkRoom(cid = cid.takeIf { it.isNotBlank() }).string()
            DarkRoomParser.parse(body, requestCursor = cid)
        } catch (e: Exception) {
            DarkRoomParser.Page(parseError = e.message ?: "加载失败")
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        val p = loadPage("")
        loading = false
        if (p.loginRequired) {
            errorText = "请先登录后查看小黑屋"
        } else if (p.items.isEmpty() && p.parseError.isNotEmpty()) {
            errorText = p.parseError
        } else {
            items = p.items
            cursor = p.nextCursor
            hasMore = p.hasMore
        }
    }

    // 滚到底自动加载下一页
    LaunchedEffect(listState, items.size, hasMore, loadingMore) {
        if (!hasMore || loadingMore || loading || items.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collectLatest { last ->
                val total = listState.layoutInfo.totalItemsCount
                // last 可为空（列表还没布局），空就直接跳过这次
                if (last != null && total > 0 && last >= total - 3) {
                    loadingMore = true
                    val p = loadPage(cursor)
                    loadingMore = false
                    if (p.items.isNotEmpty()) {
                        items = items + p.items
                        cursor = p.nextCursor
                        hasMore = p.hasMore
                    } else {
                        hasMore = false
                    }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // 页面底色跟其他列表页一致：用主题的上下渐变（没有 pageBg 这个字段）
            .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
    ) {
        // ===== 顶栏 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = c.textMain
                )
            }
            Text(
                text = "小黑屋",
                color = c.textMain,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(8.dp))
            if (items.isNotEmpty()) {
                Text(
                    text = "${items.size} 条",
                    color = c.textSub,
                    fontSize = 12.sp
                )
            }
        }

        // ===== 内容 =====
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = c.accent, modifier = Modifier.size(28.dp)) }

                errorText.isNotEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(errorText, color = c.textSub, fontSize = 14.sp)
                }

                items.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无处罚记录", color = c.textSub, fontSize = 14.sp)
                }

                else -> LazyColumn(state = listState) {
                    items(items.size) { index ->
                        DarkRoomRow(items[index]) { uid ->
                            // 点用户名进他的帖子列表
                            ctx.startActivity(
                                android.content.Intent(ctx, UserThreadListActivity::class.java)
                                    .apply {
                                        putExtra("uid", uid)
                                        putExtra("username", items[index].username)
                                    }
                            )
                        }
                    }
                    if (loadingMore) {
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = c.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun DarkRoomRow(entry: DarkRoomEntry, onUserClick: (String) -> Unit) {
    val c = LocalS1Colors.current
    Column(
        modifier = Modifier
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.glassCard)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // 第一行：被处罚者（可点进主页） + 处罚动作
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = entry.username.ifBlank { "用户 ${entry.uid}" },
                color = c.textMain,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickable { onUserClick(entry.uid) }
            )
            Spacer(Modifier.width(10.dp))
            // 处罚动作药丸
            Text(
                text = entry.action.ifBlank { "处罚" },
                color = c.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(c.chipBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        if (entry.reason.isNotBlank()) {
            Spacer(Modifier.height(7.dp))
            Text(
                text = entry.reason,
                color = c.textSub,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(8.dp))
        // 第三行：处罚人 · 时间 · 到期
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = buildString {
                    if (entry.operatorName.isNotBlank()) {
                        append("由 ${entry.operatorName} 执行")
                    }
                    if (entry.dateline.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(entry.dateline)
                    }
                },
                color = c.textSub.copy(alpha = 0.8f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (entry.groupExpiry.isNotBlank()) {
                Text(
                    text = if (entry.neverExpires) "永久" else "至 ${entry.groupExpiry}",
                    color = c.textSub.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}
