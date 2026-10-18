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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s1client.ui.theme.LocalS1Colors
import com.example.s1client.ui.theme.S1Theme
import com.example.s1client.data.store.ReadHistoryStore
import com.example.s1client.util.S1Notify
import com.example.s1client.util.applyS1Theme
import com.example.s1client.util.setupS1EdgeToEdge

/**
 * 浏览历史：看过的帖子（点开卡片时记录 tid + 标题）。
 *
 * 以前「我的」里的「历史」是个点了只弹「即将上线」的死入口，
 * 现在做成真页面 —— 能回看、能点进去、能删。
 */
class ReadHistoryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applyS1Theme()
        super.onCreate(savedInstanceState)
        setupS1EdgeToEdge()

        setContent {
            S1Theme {
                ReadHistoryScreen(onFinish = { finish() })
            }
        }
    }
}

@Composable
private fun ReadHistoryScreen(onFinish: () -> Unit) {
    val c = LocalS1Colors.current
    val context = LocalContext.current
    var entries by remember { mutableStateOf(ReadHistoryStore.list(context)) }
    var confirmClear by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶栏：返回 + 标题 + 清空
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(c.headerGradient))
                    .padding(top = 48.dp, bottom = 10.dp, start = 4.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onFinish) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null,
                        tint = if (c.isDark) c.textMain else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    "历史", fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    color = if (c.isDark) c.textMain else Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (entries.isNotEmpty()) {
                    TextButton(onClick = { confirmClear = true }) {
                        Text("清空", color = if (c.isDark) c.textMain else Color.White, fontSize = 14.sp)
                    }
                }
            }

            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "还没有浏览记录\n看过的帖子会出现在这里",
                        fontSize = 14.sp, color = c.textSub, lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.tid }) { e ->
                        HistoryRow(
                            title = e.title,
                            timeText = com.example.s1client.util.TimeUtil.formatRelative((e.at / 1000).toString()),
                            onClick = {
                                context.startActivity(
                                    Intent(context, ThreadDetailActivity::class.java).apply {
                                        putExtra("tid", e.tid)
                                        putExtra("subject", e.title)
                                    }
                                )
                            },
                            onDelete = {
                                ReadHistoryStore.remove(context, e.tid)
                                entries = ReadHistoryStore.list(context)
                                S1Notify.show(context, "已删除这条记录")
                            }
                        )
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空浏览历史") },
            text = { Text("确定清空全部 ${entries.size} 条浏览记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    ReadHistoryStore.clear(context)
                    entries = emptyList()
                    confirmClear = false
                    S1Notify.show(context, "已清空浏览历史")
                }) { Text("清空", color = c.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消", color = c.textSub) } }
        )
    }
}

@Composable
private fun HistoryRow(
    title: String,
    timeText: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val c = LocalS1Colors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.cardBg, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, color = c.textMain, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(timeText, color = c.textSub, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "删除", color = c.textSub, fontSize = 13.sp,
            modifier = Modifier
                .clickable { onDelete() }
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}
