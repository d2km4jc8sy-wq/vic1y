package com.example.s1client

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.data.model.UserPostItem
import com.example.s1client.data.store.ForumFavoriteStore
import com.example.s1client.ui.theme.S1Theme
import com.example.s1client.util.applyS1Theme
import com.example.s1client.util.setupS1EdgeToEdge

/**
 * 版块收藏列表（R5 新增，内容组第 4 行）。
 * 对标开源 s1er：收藏是帖子/版块双轨，本页是 forum 轨。
 * 行走点击 → [ForumThreadListActivity]（该版块的帖子列表）。
 */
class ForumFavoriteListActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applyS1Theme()
        super.onCreate(savedInstanceState)
        setupS1EdgeToEdge()

        val loggedIn = ForumDataCache.isLoggedIn()

        setContent {
            S1Theme {
                UserPostListScreen(
                    title = "版块收藏",
                    emptyText = if (loggedIn) "暂无收藏版块" else "请先登录",
                    authorId = null,
                    authorName = null,
                    onBack = { finish() },
                    load = {
                        if (!loggedIn) {
                            emptyList()
                        } else {
                            ForumFavoriteStore.fetchAll(ForumDataCache.memberUid)
                                .map { fav ->
                                    UserPostItem(
                                        tid = fav.fid,
                                        subject = fav.title,
                                        message = "",
                                        dateline = fav.dateline
                                    )
                                }
                        }
                    },
                    onItemClick = { item ->
                        startActivity(
                            Intent(this, ForumThreadListActivity::class.java).apply {
                                putExtra("fid", item.tid)
                                putExtra("forumName", item.subject)
                            }
                        )
                    }
                )
            }
        }
    }
}
