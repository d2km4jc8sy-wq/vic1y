package com.example.s1client.util

import com.example.s1client.data.model.FavoriteJsonItem
import com.example.s1client.data.model.FavoriteListResponse
import com.example.s1client.data.model.UserPostItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil

/**
 * 收藏列表 JSON（`module=myfavthread`）的解析与分页。
 *
 * 基准实现：开源 s1er `api_service.parseFavoriteThreadListJson`，**总页数算法照搬**
 * （`ceil(count / perpage)`，取不到 count 时退化为「本页满则假定还有下一页」），
 * 不自行发明规则。
 *
 * 输出复用 [UserPostItem]，因为收藏页与帖子列表共用同一套数据模型，
 * 换版面的事交给 UI 层，这里只负责把字段如实搬出来。
 */
object FavoriteJsonParser {

    data class FavoritePage(
        val items: List<UserPostItem>,
        val currentPage: Int,
        val totalPages: Int,
        /** 哪些条目是版块收藏（本页不会混入帖子轨以外的脏数据） */
        val forumOnly: Boolean = false
    )

    fun isLoginRequired(resp: FavoriteListResponse): Boolean {
        val v = resp.message?.messageval.orEmpty()
        return v.contains("login_before_enter_home") || v.contains("to_login")
    }

    fun parse(resp: FavoriteListResponse, page: Int): FavoritePage {
        val raw = resp.variables?.list
        val items = if (raw == null) {
            emptyList()
        } else {
            // 只保留帖子轨（type=thread）；版块轨 R5 已随功能下线一并移除
            raw.filter { !it.isForumItem() }.mapNotNull { it.toUserPostItem() }
        }

        val currentPage = page
        val perPage = resp.variables?.perpage?.trim()?.toIntOrNull() ?: DEFAULT_PER_PAGE
        val count = resp.variables?.count?.trim()?.toIntOrNull()
        val totalPages = if (count != null && count > 0 && perPage > 0) {
            ceil(count.toDouble() / perPage).toInt()
        } else {
            if (items.size >= perPage) currentPage + 1 else currentPage
        }

        return FavoritePage(
            items = items,
            currentPage = currentPage,
            totalPages = if (totalPages < 1) 1 else totalPages
        )
    }

    private fun FavoriteJsonItem.isForumItem(): Boolean {
        val t = idtype?.trim()?.lowercase()
        return t == "forum" || t == "fid"
    }

    private fun FavoriteJsonItem.toUserPostItem(): UserPostItem? {
        val tidVal = tid?.trim()?.takeIf { it.isNotEmpty() } ?: id?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null
        val ttl = (title ?: subject).orEmpty().let { stripTags(it) }.trim()
        if (ttl.isEmpty()) return null

        return UserPostItem(
            tid = tidVal,
            subject = ttl,
            message = "",
            // 收藏时间：dateline / dbdateline 都是 Unix 秒
            dateline = formatCollectTime(dateline ?: dbdateline),

            replies = replies?.trim().orEmpty(),
            hasImage = false
        )
    }

    /** 收藏时间：指标块里只占一格，用「月-日」即可；解析不出来就留空让该格隐藏 */
    private fun formatCollectTime(sec: String?): String {
        val s = sec?.trim()?.toLongOrNull() ?: return ""
        if (s <= 0) return ""
        return try {
            val c = Calendar.getInstance()
            c.timeInMillis = s * 1000
            SimpleDateFormat("MM-dd", Locale.CHINA).format(c.time)
        } catch (_: Exception) {
            ""
        }
    }

    private fun stripTags(html: String): String =
        // R11 收敛（2026-10-18 第 9 轮审计）：原来此处把 &amp; 放在 &lt; 前面解，
        // `&amp;lt;` 会被双重解码成 `<`（顺序 bug）。公共函数已把 &amp; 置于末位，换之。
        unescapeHtmlEntities(html.replace(Regex("<[^>]*>"), ""))?.trim() ?: ""

    private const val DEFAULT_PER_PAGE = 20
}
