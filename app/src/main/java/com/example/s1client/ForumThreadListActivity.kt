package com.example.s1client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.model.UserPostItem
import com.example.s1client.ui.theme.S1Theme
import com.example.s1client.util.applyS1Theme
import com.example.s1client.util.setupS1EdgeToEdge

/**
 * 版块帖子列表（R5 新增）：版块收藏的下一级。
 * 走已有 JSON 接口 getThreadList(module=forumdisplay, fid)，渲染复用 [UserPostListScreen]，
 * 行走回到默认行为（进帖子详情）。
 */
class ForumThreadListActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applyS1Theme()
        super.onCreate(savedInstanceState)
        setupS1EdgeToEdge()

        val fid = intent.getStringExtra("fid") ?: ""
        val presetName = intent.getStringExtra("forumName") ?: "版块"

        setContent {
            S1Theme {
                UserPostListScreen(
                    title = presetName,
                    emptyText = "暂无帖子",
                    authorId = null,
                    authorName = null,
                    onBack = { finish() },
                    load = {
                        if (fid.isBlank()) {
                            emptyList()
                        } else {
                            val resp = RetrofitClient.api.getThreadList(fid = fid, page = 1)
                            val forumName = resp.variables?.forum?.name ?: presetName
                            resp.variables?.threadList.orEmpty().map { t ->
                                UserPostItem(
                                    tid = t.tid.orEmpty(),
                                    subject = t.subject.orEmpty(),
                                    message = "",
                                    dateline = t.dateline.orEmpty(),
                                    forum = forumName,
                                    replies = t.replies.orEmpty(),
                                    views = t.views.orEmpty(),
                                    hasImage = (t.attachment?.toIntOrNull() ?: 0) > 0
                                )
                            }.filter { it.tid.isNotBlank() }
                        }
                    }
                )
            }
        }
    }
}
