package com.example.s1client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Intent
import android.os.Bundle
import com.example.s1client.util.PostLocator
import kotlinx.coroutines.launch
import com.example.s1client.util.S1Notify
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
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
import com.example.s1client.util.NoticeItem
import com.example.s1client.util.NoticeJsonParser
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
import com.example.s1client.ui.design.ChipBg
import com.example.s1client.ui.design.TextMain
import com.example.s1client.ui.showUserCardDialog
import com.example.s1client.ui.design.TextSub
import com.example.s1client.util.PmParser

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

/** 一级分段取值（提醒 / 私信） */
private const val SEG_NOTICE = 0
private const val SEG_PM = 1

/** 「全部」档：概览，三档各取第 1 页（要看全部历史请进具体档位翻页） */
private const val FEED_ALL = "all"

/**
 * 二级筛选档位 —— 对应 Mobile API 的 `view` 参数。
 * 实测 mypost / interactive / system 三个值接口都接受；
 * 「评分」不做独立档：JSON 没有 type 字段，只能靠正文文本猜，跨页会漏。
 */
private val NOTICE_FEEDS = listOf(
    "全部" to FEED_ALL,
    "帖子" to NoticeJsonParser.FEED_MYPOST,
    "@我" to NoticeJsonParser.FEED_INTERACTIVE,
    "系统" to NoticeJsonParser.FEED_SYSTEM
)

/** HTML 兜底路径下各档位给条目打的默认 type */
private fun defaultTypeOf(view: String): String = when (view) {
    NoticeJsonParser.FEED_INTERACTIVE -> "at"
    NoticeJsonParser.FEED_SYSTEM -> "system"
    else -> "reply"
}

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
    // 一级分段：提醒 / 私信（iOS 形制，≤4 段）
    var segment by remember { mutableIntStateOf(SEG_NOTICE) }
    // 二级筛选（只在「提醒」档出现）：全部 / 帖子 / @我 / 系统
    var feedIdx by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(true) }
    var appending by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf<List<ReminderRowItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val loggedIn = remember { ForumDataCache.isLoggedIn() }

    // 客户端已看过的私信 uid（点开会话即记入），让红点可消失、角标随之归零
    var localSeenPm by remember { mutableStateOf<Set<String>>(ReminderUnread.loadSeenPmUids(context)) }

    val feedKey = NOTICE_FEEDS[feedIdx].second

    /**
     * 单档提醒的一页：**JSON 优先、HTML 兜底**（口径同 s1er getNoticeList）。
     * JSON 带 count/perpage，总页数准确、能翻页取到全部提醒；
     * HTML 只是兜底，拿不到总页数，只能按「本页是否满」估。
     */
    suspend fun loadNoticePage(view: String, page: Int): Pair<List<NoticeItem>, Int> {
        try {
            val resp = withContext(Dispatchers.IO) {
                RetrofitClient.api.getNoticeList(view = view, page = page)
            }
            if (NoticeJsonParser.isLoginRequired(resp)) throw IllegalStateException("请先登录后查看提醒")
            val p = NoticeJsonParser.parse(resp, view, page)
            return p.items to p.totalPages
        } catch (e: Exception) {
            if (e is IllegalStateException) throw e
            val url = "https://stage1st.com/2b/home.php?mod=space&do=notice&view=$view"
            val html = withContext(Dispatchers.IO) { RetrofitClient.fetchHtml(url) }
            if (DUMP_HTML) runCatching {
                File(context.getExternalFilesDir(null), "notice_$view.html").writeText(html)
            }
            if (NoticeParser.isLoginPage(html)) throw IllegalStateException("请先登录后查看提醒")
            val parsed = NoticeParser.parse(html, defaultTypeOf(view))
            return parsed to (if (parsed.size >= 20) page + 1 else page)
        }
    }

    /** 「全部」＝三档各取第 1 页合并成概览；要看全部历史请进具体档位翻页 */
    suspend fun loadAllFeeds(): List<NoticeItem> {
        val out = mutableListOf<NoticeItem>()
        for (v in listOf(
            NoticeJsonParser.FEED_MYPOST,
            NoticeJsonParser.FEED_INTERACTIVE,
            NoticeJsonParser.FEED_SYSTEM
        )) {
            out += loadNoticePage(v, 1).first
        }
        return out
    }

    suspend fun fetchPmList(): List<PmListItem> {
        val html = withContext(Dispatchers.IO) {
            RetrofitClient.fetchHtml("https://stage1st.com/2b/home.php?mod=space&do=pm")
        }
        return PmParser.parsePmList(html)
    }

    LaunchedEffect(segment, feedKey, page, reloadKey) {
        if (!loggedIn) {
            loading = false
            appending = false
            items = emptyList()
            error = null
            return@LaunchedEffect
        }
        val first = page == 1
        if (first) loading = true else appending = true
        error = null
        try {
            val rows: List<ReminderRowItem> = when {
                segment == SEG_PM -> {
                    totalPages = 1
                    fetchPmList().map { ReminderRowItem.Pm(it, localSeenPm.contains(it.touid)) }
                }
                feedKey == FEED_ALL -> {
                    // 概览：三档各取第 1 页
                    totalPages = 1
                    loadAllFeeds().map { ReminderRowItem.Notice(it) }
                }
                else -> {
                    val (list, tp) = loadNoticePage(feedKey, page)
                    totalPages = tp
                    list.map { ReminderRowItem.Notice(it) }
                }
            }
            items = if (first) rows else items + rows

            // 已加载到的通知 id 记为已读；私信档批量建立已读基线
            val noticeIds = rows.mapNotNull {
                (it as? ReminderRowItem.Notice)?.data?.id?.takeIf { id -> id.isNotBlank() }
            }
            ReminderUnread.markSeen(context, noticeIds)
            if (segment == SEG_PM) {
                val pmUids = rows.mapNotNull {
                    (it as? ReminderRowItem.Pm)?.data?.touid?.takeIf { u -> u.isNotBlank() }
                }
                pmUids.forEach { ReminderUnread.markPmSeen(context, it) }
                localSeenPm = ReminderUnread.loadSeenPmUids(context)
            }
        } catch (e: Exception) {
            error = e.message ?: "加载失败"
            if (first) items = emptyList()
        }
        loading = false
        appending = false
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

        // ===== 一级分段（iOS 形制）：提醒 / 私信 =====
        IosSegmented(
            options = listOf("提醒", "私信"),
            selected = segment,
            onSelect = { segment = it; page = 1 },
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
        )

        // ===== 二级筛选 chips（只在「提醒」档出现）：全部 / 帖子 / @我 / 系统 =====
        if (segment == SEG_NOTICE) {
            FeedChips(
                options = NOTICE_FEEDS.map { it.first },
                selected = feedIdx,
                onSelect = { feedIdx = it; page = 1 }
            )
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
                    contentPadding = PaddingValues(vertical = 0.dp)
                ) {
                    items(items, key = { it.key }) { row ->
                        val last = row.key == items.last().key
                        when (row) {
                            is ReminderRowItem.Notice -> NoticeRow(row.data, showHairline = !last)
                            is ReminderRowItem.Pm -> PmRow(
                                item = row.data,
                                isSeen = row.isSeen || row.data.touid in localSeenPm,
                                showHairline = !last
                            ) {
                                localSeenPm = localSeenPm + row.data.touid
                                ReminderUnread.markPmSeen(context, row.data.touid)
                            }
                        }
                    }
                    // 触底自动加载下一页：只有「提醒档的具体分类」才有多页
                    if (segment == SEG_NOTICE && feedKey != FEED_ALL && page < totalPages) {
                        item {
                            LaunchedEffect(page, feedKey) {
                                if (!appending && !loading) page++
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 13.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = AccentPink,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * iOS 形制的分段控件：灰底容器 + 高亮滑块。
 * 高 34dp、内缩 2dp、圆角 8dp —— 走斐波那契/整数化比例（宪法第 15 条）。
 * 配色基于 TextMain 的透明度，深浅主题都自适应。
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

/** 横向滚动的筛选 chips：高 34dp、圆角 17dp、间距 8dp（斐波那契） */
@Composable
private fun FeedChips(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 13.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(if (sel) AccentPink else TextMain.copy(alpha = 0.08f))
                    .clickable { onSelect(i) }
                    .padding(horizontal = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (sel) Color.White else TextSub
                )
            }
        }
    }
}

/** 行底发丝线：让列表成为"连续信息流"，行不再浮在空白里（对标 ThreadRow 的分割线） */
@Composable
private fun RowHairline() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 60.dp)
            .height(1.dp)
            .background(ChipBg)
    )
}

@Composable
private fun NoticeRow(item: NoticeItem, showHairline: Boolean = true) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column {
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
                // 三级兜底：① 有 tid → 帖子详情 ② 没 tid 但能抠出 uid → 用户名片 ③ 都没有才提示
                val tid = item.tid?.takeIf { it.isNotBlank() && it != "0" }
                val uid = item.targetUrl?.let { url ->
                    Regex("""uid=(\d+)""").find(url)?.groupValues?.get(1)
                }
                when {
                    tid != null -> {
                        // 回复类提醒带 pid。**这里不再预解析页号**（改在详情页里做），
                        // 原因照 s1er：定位属于"打开帖子"这件事的一部分 ——
                        //   · 在详情页里先定位、再拉那一页，用户不会看到"先闪第 1 页再跳"；
                        //   · 定位失败可以直接在详情页明确提示，而不是在这里静默丢掉页号、
                        //     让用户看到一个停在第 1 页、什么都不说的界面。
                        val pid = item.pid?.takeIf { it.isNotBlank() }
                        android.util.Log.d(
                            "S1Locate",
                            "click tid=$tid pid=$pid type=${item.type} " +
                                "targetUrl=${item.targetUrl} content=${item.content?.take(60)}"
                        )
                        context.startActivity(
                            Intent(context, ThreadDetailActivity::class.java)
                                .putExtra("tid", tid)
                                .apply {
                                    pid?.let { putExtra(ThreadDetailActivity.EXTRA_PID, it) }
                                }
                        )
                    }
                    uid != null -> runCatching {
                        showUserCardDialog(context as? androidx.activity.ComponentActivity ?: return@runCatching,
                            uid, ForumDataCache.memberUsername)
                    }
                    else -> S1Notify.show(context, "该提醒没有可跳转的内容")
                }
            }
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 未读竖条：服务端 new=1 才亮（HTML 兜底路径拿不到，恒为透明）
        Box(
            modifier = Modifier
                .padding(end = 9.dp)
                .width(3.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (item.isNew) AccentPink else Color.Transparent)
        )
        AsyncImage(
            model = NoticeParser.resolveUrl(item.avatar),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(34.dp)
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
    if (showHairline) RowHairline()
    }
}

@Composable
private fun PmRow(
    item: PmListItem,
    isSeen: Boolean,
    showHairline: Boolean = true,
    onSeen: () -> Unit
) {
    val context = LocalContext.current
    Column {
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
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
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
    if (showHairline) RowHairline()
    }
}
