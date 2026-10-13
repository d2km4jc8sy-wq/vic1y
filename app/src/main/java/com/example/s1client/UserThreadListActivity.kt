package com.example.s1client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.s1client.util.applyS1Theme
import com.example.s1client.util.setupS1EdgeToEdge
import com.example.s1client.ui.theme.S1Theme
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.util.HtmlParser
import com.example.s1client.util.UserPostParser

/**
 * 某用户的主题列表。
 * HTML 接口 + 统一列表项（[ThreadRow]），与搜索/回帖/收藏同一套渲染。
 */
class UserThreadListActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applyS1Theme()
        super.onCreate(savedInstanceState)
        setupS1EdgeToEdge()

        val uid = intent.getStringExtra("uid") ?: ""
        val username = intent.getStringExtra("username") ?: ""
        if (uid.isEmpty()) {
            finish()
            return
        }

        setContent {
            S1Theme {
                UserPostListScreen(
                    title = if (username.isBlank()) "主题" else "$username 的主题",
                    emptyText = "暂无主题",
                    authorId = uid,
                    authorName = username.ifBlank { null },
                    onBack = { finish() },
                    load = {
                        val url = "https://stage1st.com/2b/home.php?mod=space&uid=$uid" +
                                "&do=thread&view=me&from=space"
                        UserPostParser.parseThreadList(RetrofitClient.fetchHtml(url))
                    }
                )
            }
        }
    }
}
