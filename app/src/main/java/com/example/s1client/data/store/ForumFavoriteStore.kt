package com.example.s1client.data.store

import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.model.ForumFavoriteItem
import com.example.s1client.util.UserPostParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 版块收藏（R5 新增，对标开源 s1er favoriteMembership 的 forum 轨）。
 *
 * 业务逻辑照搬 s1er：收藏列表走 home.php?do=favorite HTML，版块轨加 &type=forum&mobile=2；
 * 翻页上限 10 页（s1er 同款封顶），解析见 [UserPostParser.parseFavoriteForumList]。
 */
object ForumFavoriteStore {

    private const val MAX_PAGES = 10

    /** 拉全量版块收藏（自动翻页）；未登录/网络失败返回空列表 */
    suspend fun fetchAll(uid: String): List<ForumFavoriteItem> = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext emptyList()
        val out = mutableListOf<ForumFavoriteItem>()
        var page = 1
        while (page <= MAX_PAGES) {
            val url = "https://stage1st.com/2b/home.php?mod=space&uid=$uid&do=favorite" +
                "&view=me&type=forum&page=$page&mobile=2"
            val html = runCatching { RetrofitClient.fetchHtml(url) }.getOrNull()
            if (html.isNullOrBlank()) break
            val items = UserPostParser.parseFavoriteForumList(html)
            if (items.isEmpty()) break
            out.addAll(items)
            // 总页数（title="共 N 页"）优先；取不到就靠「不足一页」或空页收尾
            val totalPages = Regex("""title="共\s*(\d+)\s*页""").find(html)?.groupValues?.get(1)?.toIntOrNull()
            if (totalPages != null && page >= totalPages) break
            if (items.size < 20) break
            page++
        }
        out.distinctBy { it.fid }
    }
}
