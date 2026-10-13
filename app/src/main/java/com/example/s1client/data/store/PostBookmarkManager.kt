package com.example.s1client.data.store

import android.content.Context
import com.example.s1client.data.model.Post
import com.example.s1client.ui.PostAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import com.example.s1client.util.PostContentParser

/**
 * 楼层收藏管理：本地 JSON 持久化 + 内存缓存。
 *
 * 每个收藏的楼层保存一份**快照**（作者、时间、标题、原文、纯文本、链接），
 * 即使原帖被删也能在本地查看。
 */
object PostBookmarkManager {

    private const val FILE_NAME = "post_bookmarks.json"

    private val mutex = Mutex()
    private val gson = Gson()

    @Volatile
    private var bookmarks: List<PostBookmark> = emptyList()
    @Volatile
    private var loaded = false

    /** 应用启动时调用，把磁盘数据读到内存 */
    fun init(context: Context) {
        load(context)
    }

    /** 是否已收藏某楼层（按 pid 判断；pid 为空返回 false） */
    fun isBookmarked(pid: String?): Boolean {
        if (pid.isNullOrBlank()) return false
        return bookmarks.any { it.pid == pid }
    }

    /** 收藏/更新某楼层快照；同一 pid 只保留最新一条，新收藏插到最前面 */
    suspend fun add(context: Context, post: Post, tid: String, subject: String, timeText: String) {
        mutex.withLock {
            ensureLoaded(context)
            val existingIndex = bookmarks.indexOfFirst { it.pid == post.pid }
            val newList = bookmarks.toMutableList()
            val bm = createBookmark(post, tid, subject, timeText)
            if (existingIndex >= 0) {
                newList[existingIndex] = bm
            } else {
                newList.add(0, bm)
            }
            bookmarks = newList
            writeFile(context, newList)
        }
    }

    /** 按 pid 取消收藏 */
    suspend fun remove(context: Context, pid: String?) {
        if (pid.isNullOrBlank()) return
        mutex.withLock {
            ensureLoaded(context)
            val newList = bookmarks.filter { it.pid != pid }
            if (newList.size == bookmarks.size) return@withLock
            bookmarks = newList
            writeFile(context, newList)
        }
    }

    /** 按内部 id 取消收藏 */
    suspend fun removeById(context: Context, id: String?) {
        if (id.isNullOrBlank()) return
        mutex.withLock {
            ensureLoaded(context)
            val newList = bookmarks.filter { it.id != id }
            if (newList.size == bookmarks.size) return@withLock
            bookmarks = newList
            writeFile(context, newList)
        }
    }

    /** 获取所有收藏（按收藏时间倒序，最新的在最前） */
    fun getAll(): List<PostBookmark> = bookmarks

    /** 获取某条收藏 */
    fun getById(id: String?): PostBookmark? {
        if (id.isNullOrBlank()) return null
        return bookmarks.firstOrNull { it.id == id }
    }

    private fun load(context: Context) {
        if (loaded) return
        val file = File(context.filesDir, FILE_NAME)
        bookmarks = if (file.exists()) {
            try {
                val json = file.readText()
                val type = object : TypeToken<List<PostBookmark>>() {}.type
                gson.fromJson<List<PostBookmark>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        loaded = true
    }

    private fun ensureLoaded(context: Context) {
        if (!loaded) load(context)
    }

    private suspend fun writeFile(context: Context, list: List<PostBookmark>) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, FILE_NAME)
                file.writeText(gson.toJson(list))
            } catch (_: Exception) {
                // 写失败不抛，避免崩溃；下次进入再试
            }
        }
    }

    private fun createBookmark(
        post: Post,
        tid: String,
        subject: String,
        timeText: String
    ): PostBookmark {
        val tidValue = tid.takeIf { it.isNotBlank() } ?: ""
        val pidValue = post.pid?.takeIf { it.isNotBlank() }
        val plain = try {
            PostContentParser.parse(post.message ?: "")
                .text.toString()
                .replace("\uFFFC", "")
                .trim()
        } catch (_: Exception) {
            ""
        }
        val floorUrl = if (!tidValue.isBlank() && !pidValue.isNullOrBlank()) {
            "https://stage1st.com/2b/forum.php?mod=redirect&goto=findpost&ptid=$tidValue&pid=$pidValue"
        } else if (!tidValue.isBlank()) {
            "https://stage1st.com/2b/thread-$tidValue-1-1.html"
        } else {
            ""
        }
        return PostBookmark(
            id = UUID.randomUUID().toString(),
            tid = tidValue,
            pid = pidValue,
            position = post.position?.takeIf { it.isNotBlank() },
            author = post.author?.takeIf { it.isNotBlank() },
            authorId = post.authorId?.takeIf { it.isNotBlank() },
            subject = subject,
            message = post.message,
            plainText = plain,
            dateline = post.dateline?.takeIf { it.isNotBlank() },
            timeText = timeText,
            avatarUrl = post.authorId?.let { PostAdapter.buildAvatarUrl(it) },
            floorUrl = floorUrl,
            createdAt = System.currentTimeMillis()
        )
    }
}
