package com.example.s1client

import android.content.Intent
import android.os.Bundle
import com.example.s1client.util.S1Notify
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.data.model.PmListItem
import com.example.s1client.ui.theme.S1Theme
import com.example.s1client.util.HtmlParser
import com.example.s1client.util.NoticeItem
import com.example.s1client.util.NoticeParser
import com.example.s1client.util.ReminderUnread
import com.example.s1client.util.applyS1Theme
import com.example.s1client.util.avatarUrl
import com.example.s1client.util.setupS1EdgeToEdge
import java.io.File
import com.example.s1client.ui.design.AccentPink
import com.example.s1client.ui.design.BgColor
import com.example.s1client.ui.design.BgColorBottom
import com.example.s1client.ui.design.GlassBar
import com.example.s1client.ui.design.HeartRed
import com.example.s1client.ui.design.TextMain
import com.example.s1client.ui.design.TextSub

/**
 * 站内「提醒」中心（替代原底栏「私信」入口）。
 * 单一标签行：全部 / 回复我 / @我 / 私信 / 系统。
 * 打开后把已加载到的通知 id / 私信 uid 标记为已读，底栏「提醒」图标的未读徽标随之清零。
 */
class NoticeCenterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        applyS1Theme()
        super.onCreate(savedInstanceState)
        setupS1EdgeToEdge()
        setContent {
            S1Theme {
                ReminderCenterScreen(onBack = { finish() })
            }
        }
    }
}

/** 调试开关：把原始 HTML 与解析结果落盘到应用私有目录（验证解析用，日常关闭） */
private const val DUMP_HTML = false

private sealed class ReminderRowItem {
    abstract val key: String

    data class Notice(val data: NoticeItem) : ReminderRowItem() {
        override val key get() = "n_${data.id}"
    }

    data class Pm(val data: PmListItem, val isSeen: Boolean) : ReminderRowItem() {
        override val key get() = "p_${data.touid}"
    }
}

@Composable
fun ReminderCenterScreen(onBack: () -> Unit) {
    val subTabs = listOf(
        "全部" to "all",
        "回复我" to "mypost",
        "@我" to "interactive",
        "私信" to "pm",
        "系统" to "system"
    )
    var tab by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<ReminderRowItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val loggedIn = remember { ForumDataCache.isLoggedIn() }

    // 客户端已看过的私信 uid（点开会话即记入），让红点可消失、角标随之归零
    var localSeenPm by remember { mutableStateOf<Set<String>>(ReminderUnread.loadSeenPmUids(context)) }

    suspend fun fetchNotices(view: String, defaultType: String): List<NoticeItem> {
        val url = "https://stage1st.com/2b/home.php?mod=space&do=notice" +
            if (view.isNotEmpty()) "&view=$view" else ""
        val html = RetrofitClient.fetchHtml(url)
        if (DUMP_HTML) runCatching {
            val f = File(context.getExternalFilesDir(null), "notice_${view.ifEmpty { "all" }}.html")
            f.writeText(html)
        }
        if (NoticeParser.isLoginPage(html)) throw IllegalStateException("请先登录后查看提醒")
        val parsed = NoticeParser.parse(html, defaultType)
        if (DUMP_HTML) runCatching {
            val f = File(context.getExternalFilesDir(null), "notice_${view.ifEmpty { "all" }}_parsed.txt")
            f.writeText(
                "count=${parsed.size}\n" + parsed.take(5).joinToString("\n") {
                    "type=${it.type} tid=${it.tid} author=${it.author} time=${it.time} content=${it.content.take(40)}"
                }
            )
        }
        return parsed
    }

    suspend fun fetchPmList(): List<PmListItem> {
        val html = RetrofitClient.fetchHtml("https://stage1st.com/2b/home.php?mod=space&do=pm")
        return HtmlParser.parsePmList(html)
    }

    LaunchedEffect(tab, reloadKey) {
        if (!loggedIn) {
            loading = false
            items = emptyList()
            error = null
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            val list: List<ReminderRowItem> = when (subTabs[tab].second) {
                "all" -> {
                    val notices = listOf(
                        fetchNotices("mypost", "reply"),
                        fetchNotices("interactive", "at"),
                        fetchNotices("system", "system")
                    ).flatten().map { ReminderRowItem.Notice(it) }
                    val pms = fetchPmList().map { ReminderRowItem.Pm(it, localSeenPm.contains(it.touid)) }
                    notices + pms
                }
                "mypost" -> fetchNotices("mypost", "reply").map { ReminderRowItem.Notice(it) }
                "interactive" -> fetchNotices("interactive", "at").map { ReminderRowItem.Notice(it) }
                "system" -> fetchNotices("system", "system").map { ReminderRowItem.Notice(it) }
                "pm" -> fetchPmList().map { ReminderRowItem.Pm(it, localSeenPm.contains(it.touid)) }
                else -> emptyList()
            }
            items = list

            // 已加载到的通知 id 记为已读；私信只有在「全部/私信」页才批量建立基线
            val noticeIds = list.mapNotNull {
                (it as? ReminderRowItem.Notice)?.data?.id?.takeIf { id -> id.isNotBlank() }
            }
            ReminderUnread.markSeen(context, noticeIds)
            if (subTabs[tab].second == "all" || subTabs[tab].second == "pm") {
                val pmUids = list.mapNotNull {
                    (it as? ReminderRowItem.Pm)?.data?.touid?.takeIf { u -> u.isNotBlank() }
                }
                pmUids.forEach { ReminderUnread.markPmSeen(context, it) }
                localSeenPm = ReminderUnread.loadSeenPmUids(context)
            }
        } catch (e: Exception) {
            error = e.message ?: "加载失败"
            items = emptyList()
        }
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgColor, BgColorBottom)))
            .statusBarsPadding()
    ) {
        // ===== 顶栏 =====
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
                Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = TextMain, modifier = Modifier.size(20.dp))
            }
            Text(
                text = "提醒",
                color = TextMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 56.dp)
            )
        }

        // ===== 标签行：全部 / 回复我 / @我 / 私信 / 系统 =====
        TabRow(
            selectedTabIndex = tab,
            containerColor = GlassBar,
            contentColor = AccentPink,
            divider = {}
        ) {
            subTabs.forEachIndexed { i, (label, _) ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    text = {
                        Text(
                            label,
                            color = if (tab == i) AccentPink else TextSub,
                            fontSize = 13.sp,
                            fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                loading -> CircularProgressIndicator(color = AccentPink)
                error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error ?: "", color = TextSub, fontSize = 14.sp)
                    if (loggedIn) {
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
                }
                items.isEmpty() -> Text(if (loggedIn) "暂无提醒" else "请先登录", color = TextSub, fontSize = 14.sp)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(items, key = { it.key }) { row ->
                        when (row) {
                            is ReminderRowItem.Notice -> NoticeRow(row.data)
                            is ReminderRowItem.Pm -> PmRow(
                                item = row.data,
                                isSeen = row.isSeen || row.data.touid in localSeenPm
                            ) {
                                localSeenPm = localSeenPm + row.data.touid
                                ReminderUnread.markPmSeen(context, row.data.touid)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeRow(item: NoticeItem) {
    val context = LocalContext.current
    val typeColor = when (item.type) {
        "reply" -> Color(0xFFFF6B6B)
        "at" -> Color(0xFF4F80F2)
        else -> TextSub
    }
    val typeLabel = when (item.type) {
        "reply" -> "回复"
        "at" -> "@"
        else -> "系统"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (item.tid != null) {
                    context.startActivity(
                        Intent(context, ThreadDetailActivity::class.java)
                            .putExtra("tid", item.tid)
                    )
                } else {
                    S1Notify.show(context, "该提醒暂不支持跳转")
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = NoticeParser.resolveUrl(item.avatar),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(TextSub.copy(alpha = 0.15f))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!item.author.isNullOrBlank()) {
                    Text(
                        item.author,
                        color = TextMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    typeLabel,
                    color = typeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(typeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                item.content,
                color = TextMain,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (!item.time.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(item.time, color = TextSub, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PmRow(item: PmListItem, isSeen: Boolean, onSeen: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onSeen()
                context.startActivity(
                    Intent(context, PmDetailActivity::class.java)
                        .putExtra("touid", item.touid)
                        .putExtra("username", item.username)
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFE94A72), Color(0xFFF48FB1))))
        ) {
            val letter = item.username.firstOrNull()?.toString()?.uppercase() ?: "?"
            Text(
                letter, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
            val av = avatarUrl(item.touid, "middle")
            if (av != null) {
                AsyncImage(
                    model = av, contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
            }
            if (item.isNew && !isSeen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(11.dp)
                        .background(HeartRed, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.username, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                if (item.lastTime.isNotBlank()) {
                    Text(item.lastTime, color = TextSub, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                item.lastMessage.ifBlank { "（无内容）" },
                color = if (item.isNew && !isSeen) TextMain else TextSub,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
