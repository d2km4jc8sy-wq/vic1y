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
import com.example.s1client.data.store.DraftStore
import com.example.s1client.util.S1Notify
import com.example.s1client.util.TimeUtil
import com.example.s1client.util.applyS1Theme
import com.example.s1client.util.setupS1EdgeToEdge

/**
 * 草稿箱：列出本地暂存的发帖 / 回帖草稿。
 *
 * 以前「我的」里的「草稿」是个点了只弹「即将上线」的死入口，现在做成真页面：
 * 点发帖草稿直接回发帖页（草稿会自动回填），点回帖草稿跳到对应帖子继续写。
 */
class DraftListActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applyS1Theme()
        super.onCreate(savedInstanceState)
        setupS1EdgeToEdge()

        setContent {
            S1Theme {
                DraftListScreen(onFinish = { finish() })
            }
        }
    }
}

@Composable
private fun DraftListScreen(onFinish: () -> Unit) {
    val c = LocalS1Colors.current
    val context = LocalContext.current
    var drafts by remember { mutableStateOf(DraftStore.listDrafts(context)) }
    var deleting by remember { mutableStateOf<DraftStore.Entry?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                        tint = c.onHeader,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    "草稿", fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    color = c.onHeader,
                    modifier = Modifier.weight(1f)
                )
            }

            if (drafts.isEmpty()) {
                // 空态：给一个真出口，而不是干巴巴一句话
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "还没有草稿\n发帖时没写完的内容会自动存到这里",
                            fontSize = 14.sp, color = c.textSub, lineHeight = 20.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "去发一个", color = c.accent, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .background(c.chipBg, RoundedCornerShape(10.dp))
                                .clickable {
                                    context.startActivity(
                                        Intent(context, PostEditorActivity::class.java)
                                    )
                                }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(drafts, key = { it.key }) { d ->
                        DraftRow(
                            summary = d.summary,
                            meta = (if (d.isPost) "发帖草稿" else "回帖草稿") +
                                    (if (d.updatedAt > 0) " · " +
                                        TimeUtil.formatRelative((d.updatedAt / 1000).toString())
                                    else ""),
                            onClick = {
                                if (d.isPost) {
                                    context.startActivity(
                                        Intent(context, PostEditorActivity::class.java).apply {
                                            putExtra("fid", d.targetId)
                                        }
                                    )
                                } else {
                                    context.startActivity(
                                        Intent(context, ThreadDetailActivity::class.java).apply {
                                            putExtra("tid", d.targetId)
                                        }
                                    )
                                }
                            },
                            onDelete = { deleting = d }
                        )
                    }
                }
            }
        }
    }

    deleting?.let { d ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除草稿") },
            text = { Text("确定删除这条草稿吗？删掉就找不回来了。") },
            confirmButton = {
                TextButton(onClick = {
                    DraftStore.clear(context, d.key)
                    drafts = DraftStore.listDrafts(context)
                    deleting = null
                    S1Notify.show(context, "已删除草稿")
                }) { Text("删除", color = c.danger) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消", color = c.textSub) } }
        )
    }
}

@Composable
private fun DraftRow(summary: String, meta: String, onClick: () -> Unit, onDelete: () -> Unit) {
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
                summary, color = c.textMain, fontSize = 15.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(meta, color = c.textSub, fontSize = 12.sp)
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
