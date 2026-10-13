package com.example.s1client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.s1client.util.applyS1Theme
import com.example.s1client.util.setupS1EdgeToEdge
import com.example.s1client.ui.theme.S1Theme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.util.HtmlParser

/**
 * 收藏列表。
 * 不带 uid（或 uid 就是自己）= 我的收藏；带了别人的 uid = TA的收藏。
 * HTML 接口 + 统一列表项（[ThreadRow]）。
 */
class FavoriteListActivity : ComponentActivity() {

    /** 回到前台时 +1，触发列表重新加载（可能刚在详情页取消了收藏） */
    private var resumeTick by mutableStateOf(0)
    private var firstResume = true

    override fun onResume() {
        super.onResume()
        // 首次进入由 LaunchedEffect 自己加载，这里跳过，避免开屏请求两次
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
                UserPostListScreen(
                    title = if (viewingSelf) "我的收藏" else "TA的收藏",
                    emptyText = if (loggedIn) "暂无收藏" else "请先登录",
                    authorId = if (viewingSelf) ForumDataCache.memberUid else uid,
                    authorName = (if (viewingSelf) ForumDataCache.memberUsername else null)
                        ?.takeIf { it.isNotBlank() },
                    onBack = { finish() },
                    load = {
                        if (!loggedIn) {
                            emptyList()
                        } else {
                            val url = if (viewingSelf) {
                                "https://stage1st.com/2b/home.php?mod=space&do=favorite&view=me"
                            } else {
                                "https://stage1st.com/2b/home.php?mod=space&uid=$uid&do=favorite&view=me"
                            }
                            HtmlParser.parseFavoriteList(RetrofitClient.fetchHtml(url))
                        }
                    },
                    reloadTick = resumeTick
                )
            }
        }
    }
}
