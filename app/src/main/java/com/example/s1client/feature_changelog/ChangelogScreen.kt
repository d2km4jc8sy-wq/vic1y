package com.example.s1client.feature_changelog

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s1client.R
import com.example.s1client.ui.theme.LocalS1Colors
import com.example.s1client.ui.theme.S1Theme
import com.example.s1client.util.applyS1Theme
import com.example.s1client.util.setupS1EdgeToEdge

/**
 * 更新日志（feature_changelog 宿主容器，阶段二④配套 2026-10-17）。
 * 导航入口在设置页「关于」组（宿主持有，符合 FEATURE_TEMPLATE）。
 */
class ChangelogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applyS1Theme()
        super.onCreate(savedInstanceState)
        setupS1EdgeToEdge()
        setContent {
            S1Theme {
                ChangelogScreen(onFinish = { finish() })
            }
        }
    }

    /** 设置页跳转入口（宿主调用，功能内不自行 startActivity） */
    companion object {
        fun intent(from: android.content.Context): Intent = Intent(from, ChangelogActivity::class.java)
    }
}

@Composable
fun ChangelogScreen(onFinish: () -> Unit) {
    val c = LocalS1Colors.current
    S1Theme {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // —— 顶栏：返回 + 标题（对齐全局顶栏高度节奏） ——
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(\n                    painter = painterResource(R.drawable.ic_back),\n                    contentDescription = "返回",\n                    tint = c.textMain,\n                    modifier = Modifier\n                        .padding(8.dp)\n                        .width(22.dp)\n                        .height(22.dp)\n                        .clickable(onClick = onFinish)\n                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "更新日志",
                    color = c.textMain,
                    fontSize = 17.sp,
                    fontWeight = FontWeight(600)
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ChangelogData.entries, key = { it.version }) { entry ->
                    Column(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                entry.version,
                                color = c.accent,
                                fontSize = 20.sp,
                                fontWeight = FontWeight(600)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(entry.date, color = c.textSub, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        entry.lines.forEach { line ->
                            Row(Modifier.padding(vertical = 3.dp)) {
                                Text(
                                    "·",
                                    color = c.textSub,
                                    fontSize = 13.sp,
                                    modifier = Modifier.width(12.dp)
                                )
                                Text(
                                    line,
                                    color = c.textMain,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        // 收尾分隔线（chipBg 弱化，与全局分组线同语汇）
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(c.chipBg)
                        )
                    }
                }
            }
        }
    }
}
