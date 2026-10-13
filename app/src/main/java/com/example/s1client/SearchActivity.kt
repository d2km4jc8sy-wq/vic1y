package com.example.s1client

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.s1client.util.applyS1Theme
import com.example.s1client.util.setupS1EdgeToEdge
import com.example.s1client.ui.showUserCardDialog
import com.example.s1client.util.S1Notify
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.example.s1client.ui.theme.S1Theme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s1client.data.model.ForumDataCache
import com.example.s1client.util.FavoriteManager
import com.example.s1client.util.SearchManager
import kotlinx.coroutines.launch

/** 站内搜索：关键词搜帖，结果复用首页卡片 */
class SearchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        applyS1Theme()
        super.onCreate(savedInstanceState)
        setupS1EdgeToEdge()
        setContent {
            S1Theme {
                SearchScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun SearchScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var keyword by remember { mutableStateOf("") }
    // 细节 06：搜索框聚焦状态（驱动渐变描边）
    val searchInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val searchFocused by searchInteraction.collectIsFocusedAsState()
    val pinkNow = AccentPink
    val s1c = com.example.s1client.ui.theme.LocalS1Colors.current
    var loading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<Post>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var searched by remember { mutableStateOf(false) }
    var longPressedPost by remember { mutableStateOf<Post?>(null) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    fun runSearch() {
        val kw = keyword.trim()
        if (kw.isEmpty()) { message = "请输入关键词"; return }
        loading = true
        message = null
        scope.launch {
            val outcome = SearchManager.search(kw)
            results = outcome.results.map { it.toPost("搜索", emptyMap()) }
            message = if (outcome.results.isEmpty()) outcome.message ?: "没有找到相关帖子" else null
            searched = true
            loading = false
        }
    }

    longPressedPost?.let { post ->
        PostActionDialog(
            post = post,
            onDismiss = { longPressedPost = null },
            onFavorite = {
                longPressedPost = null
                if (!ForumDataCache.isLoggedIn()) {
                    toast(context, "请先登录")
                } else {
                    val tid = post.tid
                    scope.launch {
                        val r = FavoriteManager.toggleFavorite(tid)
                        val msg = when {
                            r.failed -> "操作失败：" + r.message
                            r.isFavorited -> "已收藏"
                            else -> "已取消收藏"
                        }
                        toast(context, msg)
                    }
                }
            },
            onShare = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, post.title ?: "")
                    putExtra(Intent.EXTRA_TEXT, "https://stage1st.com/2b/thread-" + post.tid + "-1-1.html")
                }
                context.startActivity(Intent.createChooser(send, "分享帖子"))
                longPressedPost = null
            }
            // 搜索结果暂不提供「塞抹布」：黑名单存在首页的 ViewModel 里
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgColor, BgColorBottom)))
            .statusBarsPadding()
    ) {
        // 顶栏：返回 + 输入框 + 搜索（玻璃底 + 底边亮线）
        // drawBehind 闭包不是组合上下文，颜色要在组合期先取出来
        val topBarLine = GlassFloatLine
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(GlassBar)
                .drawBehind {
                    drawLine(
                        color = topBarLine,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowBack, contentDescription = "返回",
                    tint = TextMain, modifier = Modifier.size(20.dp)
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    // 控件 06：聚焦外圈柔光（三层递减描边，画在裁剪外才可见）
                    .drawBehind {
                        if (searchFocused) {
                            listOf(0.20f, 0.10f, 0.05f).forEachIndexed { i, a ->
                                drawRoundRect(
                                    color = pinkNow.copy(alpha = a),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = (3f + i * 2.5f) * density
                                    )
                                )
                            }
                        }
                    }
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0x14FFFFFF))
                    // 细节 06：聚焦时胶囊边框变主题渐变 + 加粗（用 onSearch 外的 focus 状态驱动）
                    .let { m ->
                        if (searchFocused) m.border(
                            1.5.dp,
                            Brush.linearGradient(listOf(AccentPink, s1c.flowCoverColors.getOrElse(0) { AccentPink })),
                            RoundedCornerShape(99.dp)
                        ) else m.border(1.dp, GlassFloatLine, RoundedCornerShape(99.dp))
                    }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextSub, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (keyword.isEmpty()) {
                        Text("搜索帖子", color = TextSub, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        singleLine = true,
                        textStyle = TextStyle(color = TextMain, fontSize = 14.sp),
                        cursorBrush = SolidColor(AccentPink),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                        interactionSource = searchInteraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
                if (keyword.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close, contentDescription = "清空", tint = TextSub,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { keyword = "" }
                    )
                }
            }
            Text(
                text = "搜索",
                color = AccentPink,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .clickable { runSearch() }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }

        // 结果区
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                loading -> CircularProgressIndicator(color = AccentPink)
                message != null -> Text(message ?: "", color = TextSub, fontSize = 14.sp)
                !searched -> Text("输入关键词，回车或点「搜索」", color = TextSub, fontSize = 13.sp)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results.size) { index ->
                        val post = results[index]
                        ThreadRow(
                            post = post,
                            isRead = false,
                            onClick = {
                                if (post.tid.isNotBlank()) {
                                    context.startActivity(
                                        Intent(context, ThreadDetailActivity::class.java).apply {
                                            putExtra("tid", post.tid)
                                            putExtra("subject", post.title ?: "")
                                        }
                                    )
                                }
                            },
                            onAvatarClick = {
                                if (!post.authorId.isNullOrBlank()) {
                                    showUserCardDialog(
                                        context as ComponentActivity,
                                        post.authorId,
                                        post.authorName ?: ""
                                    )
                                }
                            },
                            onLongPress = { longPressedPost = post }
                        )
                    }
                }
            }
        }
    }
}

private fun toast(context: android.content.Context, s: String, type: S1Notify.Type = S1Notify.Type.INFO) =
    S1Notify.show(context, s, type)
