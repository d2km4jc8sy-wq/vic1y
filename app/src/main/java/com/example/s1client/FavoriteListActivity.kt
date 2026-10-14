package com.example.s1client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.ui.design.BgColor
import com.example.s1client.ui.design.BgColorBottom
import com.example.s1client.ui.design.GlassBar
import com.example.s1client.ui.design.TextMain
import com.example.s1client.ui.design.TextSub
import com.example.s1client.ui.theme.LocalS1Colors
import com.example.s1client.ui.theme.S1Theme
import com.example.s1client.util.UserPostParser
import com.example.s1client.util.applyS1Theme
import com.example.s1client.util.setupS1EdgeToEdge

/**
 * 「我的收藏」——两个入口合到一个页面。
 *
 * 用户拍板（人类使用逻辑：主题大于回复，所以主题在左）：
 *   · **主题**（左）：服务端收藏，`home.php?do=favorite&view=me`；
 *   · **回复**（右）：本地楼层收藏（[PostBookmarkListScreen]），存的是快照，
 *     原帖被删改也能回看 —— 这一半是服务端给不了的。
 *
 * 顺带修掉一个老 bug：本页曾把自己的 uid/用户名当"作者"传给列表，
 * 结果整张表都显示成自己的头像和名字；而且 Discuz 收藏页**本身不返回
 * 每条收藏的作者**（解析出来是空串），所以这里直接关掉作者行，
 * 改由标题前的版块名与末尾的收藏时间承载信息（见 `showAuthor = false`）。
 *
 * 看别人的收藏（TA的收藏）仍走单列表 —— 那时显示收藏者身份本来就是对的。
 */
class FavoriteListActivity : ComponentActivity() {

    /** 回到前台时 +1，触发列表重新加载（可能刚在详情页取消了收藏） */
    private var resumeTick by mutableIntStateOf(0)
    private var firstResume = true

    override fun onResume() {
        super.onResume()
        // 首次进入由各列表自己加载，这里跳过，避免开屏请求两次
        if (firstResume) firstResume = false else resumeTick++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyS1Theme()
        super.onCreate(savedInstanceState)
        setupS1EdgeToEdge()

        val uid = intent.getStringExtra("uid") ?: ""
        val viewingSelf = uid.isBlank() || uid == ForumDataCache.memberUid
        val loggedIn = ForumDataCache.isLoggedIn()

        setContent {
            S1Theme {
                FavoritesScreen(
                    viewingSelf = viewingSelf,
                    targetUid = uid,
                    loggedIn = loggedIn,
                    reloadTick = resumeTick,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
private fun FavoritesScreen(
    viewingSelf: Boolean,
    targetUid: String,
    loggedIn: Boolean,
    reloadTick: Int,
    onBack: () -> Unit
) {
    var segment by remember { mutableIntStateOf(0) }
    val emptyText = if (loggedIn) "暂无收藏" else "请先登录"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgColor, BgColorBottom)))
            .statusBarsPadding()
    ) {
        TopBar(title = if (viewingSelf) "我的收藏" else "TA的收藏", onBack = onBack)

        // 只有看自己的收藏才有「主题 / 回复」两个入口
        if (viewingSelf) {
            IosSegmented(
                options = listOf("主题", "回复"),
                selected = segment,
                onSelect = { segment = it },
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                // TA 的收藏：单列表，显示收藏者身份本来就是对的
                !viewingSelf -> UserPostListScreen(
                    title = "TA的收藏",
                    emptyText = emptyText,
                    authorId = targetUid,
                    authorName = null,
                    onBack = onBack,
                    load = {
                        if (!loggedIn) {
                            emptyList()
                        } else {
                            UserPostParser.parseFavoriteList(
                                RetrofitClient.fetchHtml(
                                    "https://stage1st.com/2b/home.php?mod=space&uid=$targetUid" +
                                        "&do=favorite&view=me"
                                )
                            )
                        }
                    },
                    reloadTick = reloadTick,
                    embedded = true
                )

                // 我的收藏 · 主题（服务端）
                segment == 0 -> UserPostListScreen(
                    title = "我的收藏",
                    emptyText = emptyText,
                    // 页面不返回每条收藏的作者 —— 不传身份，避免整表显示成自己
                    authorId = null,
                    authorName = null,
                    onBack = onBack,
                    load = {
                        if (!loggedIn) {
                            emptyList()
                        } else {
                            UserPostParser.parseFavoriteList(
                                RetrofitClient.fetchHtml(
                                    "https://stage1st.com/2b/home.php?mod=space" +
                                        "&do=favorite&view=me"
                                )
                            )
                        }
                    },
                    reloadTick = reloadTick,
                    showAuthor = false,
                    embedded = true
                )

                // 我的收藏 · 回复（本地楼层收藏快照）
                else -> PostBookmarkListScreen(onFinish = onBack, embedded = true)
            }
        }
    }
}

/**
 * iOS 形制分段控件：高 34dp、圆角 8dp、内缩 2dp —— 走斐波那契（宪法第 15 条）。
 * 与提醒中心那套同形不同源，两页各自独立演进。
 */
@Composable
private fun IosSegmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(TextMain.copy(alpha = 0.08f))
            .padding(2.dp)
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (sel) TextMain.copy(alpha = 0.85f) else Color.Transparent)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (sel) BgColor else TextMain
                )
            }
        }
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(GlassBar)
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
