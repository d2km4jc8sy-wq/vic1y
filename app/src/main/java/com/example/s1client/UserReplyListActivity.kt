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
 * 某用户的回帖列表。回复正文会作为标题下的正文行显示。
 * HTML 接口 + 统一列表项（[ThreadRow]）。
 */
class UserReplyListActivity : ComponentActivity() {

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
                    title = if (username.isBlank()) "回帖" else "$username 的回帖",
                    emptyText = "暂无回帖",
                    authorId = uid,
                    authorName = username.ifBlank { null },
                    onBack = { finish() },
                    load = {
                        val url = "https://stage1st.com/2b/home.php?mod=space&uid=$uid" +
                                "&do=thread&view=me&type=reply&order=dateline&from=space"
                        UserPostParser.parseReplyList(RetrofitClient.fetchHtml(url))
                    }
                )
            }
        }
    }
}
