package com.example.s1client.share

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.s1client.ui.theme.LocalS1Colors
import com.example.s1client.ui.theme.S1Theme
import kotlinx.coroutines.withTimeoutOrNull

class ShareCardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra("title") ?: "Stage1st 帖子"
        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        val raw = intent.getParcelableArrayListExtra<ShareCardPost>("posts")
        val posts: ArrayList<ShareCardPost> = raw ?: arrayListOf()
        setContent {
            S1Theme {
                ShareCardScreen(title, posts)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = LocalS1Colors.current.textSub,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun <T> SegmentedRow(
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    val accent = LocalS1Colors.current.accent
    val onAccent = LocalS1Colors.current.onAccent
    val textMain = LocalS1Colors.current.textMain
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (label, value) ->
            val sel = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(value) }
                    .background(
                        if (sel) accent else Color.Transparent,
                        RoundedCornerShape(13.dp)
                    )
                    .border(1.dp, accent, RoundedCornerShape(13.dp))
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (sel) onAccent else textMain,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ToggleChip(label: String, on: Boolean, onToggle: () -> Unit) {
    val accent = LocalS1Colors.current.accent
    val onAccent = LocalS1Colors.current.onAccent
    val textMain = LocalS1Colors.current.textMain
    Box(
        modifier = Modifier
            .clickable { onToggle() }
            .background(if (on) accent else Color.Transparent, RoundedCornerShape(13.dp))
            .border(1.dp, accent, RoundedCornerShape(13.dp))
            .padding(horizontal = 13.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (on) onAccent else textMain, fontSize = 13.sp)
    }
}

@Composable
fun ShareCardScreen(title: String, posts: List<ShareCardPost>) {
    var style by remember { mutableStateOf(CardStyle.EDGE) }
    var showAvatar by remember { mutableStateOf(true) }
    var showTime by remember { mutableStateOf(true) }
    var showFloor by remember { mutableStateOf(true) }
    var showImages by remember { mutableStateOf(true) }
    var bgMode by remember { mutableStateOf(BackgroundMode.THEME) }
    var fmt by remember { mutableStateOf(ImageFormat.PNG) }
    var resW by remember { mutableStateOf(1080) }

    val context = LocalContext.current
    val config = ShareCardConfig(style, showAvatar, showTime, showFloor, showImages, bgMode, fmt, resW)
    val colors = computeColors(config)

    // —— 媒体预加载（真头像 + 正文图）：进页拉一次，8s 超时降级（缺图不阻塞出图）——
    var bitmaps by remember { mutableStateOf<Map<String, android.graphics.Bitmap>>(emptyMap()) }
    var loadingMedia by remember { mutableStateOf(true) }
    LaunchedEffect(posts) {
        loadingMedia = true
        val out = java.util.concurrent.ConcurrentHashMap<String, android.graphics.Bitmap>()
        try {
            withTimeoutOrNull(8_000L) {
                ShareCardImages.preload(context.applicationContext, posts, out)
            }
        } catch (_: Exception) { /* 超时/失败都用已拿到的部分 */ }
        bitmaps = out.toMap()
        loadingMedia = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalS1Colors.current.bgBottom)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SectionTitle("分享卡预览")
        Spacer(Modifier.height(8.dp))
        if (loadingMedia) {
            Text(
                "正在拉取头像与图片…（超时自动降级）",
                fontSize = 12.sp,
                color = LocalS1Colors.current.textSub
            )
            Spacer(Modifier.height(8.dp))
        }
        // 配置变化即重建预览（保证所见即所得）
        key(config, colors, bitmaps) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx -> buildCardView(ctx, config, posts, colors, title, bitmaps) }
            )
        }
        Spacer(Modifier.height(21.dp))

        SectionTitle("卡片样式（默认：卡片描边）")
        Spacer(Modifier.height(8.dp))
        SegmentedRow(
            listOf(
                "经典文字" to CardStyle.CLASSIC,
                "极简竖杠" to CardStyle.BARS,
                "卡片描边" to CardStyle.EDGE
            ),
            style
        ) { style = it }
        Spacer(Modifier.height(13.dp))

        SectionTitle("包含内容")
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToggleChip("头像", showAvatar) { showAvatar = !showAvatar }
            ToggleChip("发布时间", showTime) { showTime = !showTime }
            ToggleChip("楼层号", showFloor) { showFloor = !showFloor }
            ToggleChip("正文图片", showImages) { showImages = !showImages }
        }
        Spacer(Modifier.height(13.dp))

        SectionTitle("背景")
        Spacer(Modifier.height(8.dp))
        SegmentedRow(
            listOf(
                "跟随主题" to BackgroundMode.THEME,
                "纯白" to BackgroundMode.WHITE,
                "纯黑" to BackgroundMode.BLACK
            ),
            bgMode
        ) { bgMode = it }
        Spacer(Modifier.height(13.dp))

        SectionTitle("格式 / 分辨率")
        Spacer(Modifier.height(8.dp))
        SegmentedRow(
            listOf("PNG 无损" to ImageFormat.PNG, "JPG 高压缩" to ImageFormat.JPG),
            fmt
        ) { fmt = it }
        Spacer(Modifier.height(8.dp))
        SegmentedRow(
            listOf("标准 1080" to 1080, "高清 1440" to 1440, "超清 2160" to 2160),
            resW
        ) { resW = it }
        Spacer(Modifier.height(21.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = LocalS1Colors.current.accent,
                contentColor = LocalS1Colors.current.onAccent
            ),
            onClick = {
                ShareCardCapture.generateAndShare(
                    context as Activity, config, posts, title, bitmaps
                )
            }
        ) {
            Text("生成并分享", fontSize = 15.sp)
        }
    }
}
