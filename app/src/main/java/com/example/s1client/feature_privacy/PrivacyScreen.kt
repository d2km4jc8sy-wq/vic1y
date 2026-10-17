package com.example.s1client.feature_privacy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
 * 隐私政策查看页（阶段四 P13 配套，2026-10-17）。
 * 入口：设置页「关于」组。政策全文内置（与仓库 PRIVACY.md 同步维护）。
 */
class PrivacyActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applyS1Theme()
        super.onCreate(savedInstanceState)
        setupS1EdgeToEdge()
        setContent {
            S1Theme {
                PrivacyScreen(onFinish = { finish() })
            }
        }
    }

    companion object {
        fun intent(from: Context): Intent = Intent(from, PrivacyActivity::class.java)
    }
}

/** 政策全文（与仓库 PRIVACY.md 内容同步维护；结构化便于排版） */
private data class PrivacySection(val heading: String, val body: List<String>)

private val PRIVACY_SECTIONS = listOf(
    PrivacySection(
        "一、我们不收集任何个人数据",
        listOf(
            "无统计埋点、无广告、无推送 SDK、无第三方分析服务。",
            "应用不会把你的任何数据上传到我们或第三方的服务器。",
            "应用没有自己的账号系统——你登录的是 Stage1st 论坛官方账号。"
        )
    ),
    PrivacySection(
        "二、数据存储（全部仅存本机）",
        listOf(
            "论坛 Cookie（加密）：维持 Stage1st 登录态，Android Keystore 加密后存本机。",
            "浏览历史 / 阅读进度：离线查看你看过的帖子。",
            "草稿箱：防止发帖内容丢失。",
            "楼层收藏快照：保存你收藏的楼层原文。",
            "黑名单 / 屏蔽设置：隐藏你不想看的内容。",
            "外观与功能设置：主题 / 字体 / 字号等偏好。",
            "网络参数（可选）：你手动配置的基础网址 / IP 直连。",
            "以上数据只保存在你的手机本地，应用卸载即全部删除。"
        )
    ),
    PrivacySection(
        "三、网络访问",
        listOf(
            "Stage1st 论坛官方服务器（默认 https://stage1st.com/2b/，可在「设置 → 数据管理 → 网络参数」中自行更换）——加载论坛内容、登录、发帖、上传图片。",
            "GitHub API（仅当你手动点击「检查更新」时）——获取最新版本信息。",
            "除此之外应用不会连接任何服务器。"
        )
    ),
    PrivacySection(
        "四、所需权限",
        listOf(
            "网络访问：与论坛服务器通信（必需）。",
            "震动：操作触觉反馈（可关闭）。"
        )
    ),
    PrivacySection(
        "五、账号安全",
        listOf(
            "登录凭据（Cookie）经 Android Keystore 加密后存储在本机，应用卸载或清除数据即销毁。",
            "应用不会记录或传输你的论坛密码。",
            "退出登录会立即清除本机登录凭据。"
        )
    ),
    PrivacySection(
        "六、开源",
        listOf(
            "应用为自用开发项目，全部源码可审计——本政策所述内容与代码行为一致。"
        )
    )
)

@Composable
fun PrivacyScreen(onFinish: () -> Unit) {
    val c = LocalS1Colors.current
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = "返回",
                tint = c.textMain,
                modifier = Modifier
                    .padding(8.dp)
                    .width(22.dp)
                    .height(22.dp)
                    .clickable(onClick = onFinish)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "隐私政策",
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
            item {
                Column {
                    Text(
                        "生效日期：2026-10-17 ｜ 适用于 S1 Client（Android 版）",
                        color = c.textSub,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "S1 Client 是 Stage1st 论坛的第三方客户端。我们尽可能少地触碰你的数据——本政策如实说明应用处理哪些信息、存在哪里、去了哪里。",
                        color = c.textMain,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
            items(PRIVACY_SECTIONS, key = { it.heading }) { section ->
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        section.heading,
                        color = c.accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight(600)
                    )
                    Spacer(Modifier.height(4.dp))
                    section.body.forEach { line ->
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
                }
            }
        }
    }
}
