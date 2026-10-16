package com.example.s1client.data.remote

import android.util.Log
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.model.Post
import com.example.s1client.util.RateInfo
import com.example.s1client.util.RatingParser
import com.example.s1client.util.ThreadEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 帖子内容仓库（阶段二 P0-2 收尾，2026-10-17）：详情页网络管道的唯一出口。
 *
 * 只做「请求 + 解析」，**不写任何全局状态、不碰 UI**——状态提交由调用方
 * （ThreadDetailActivity.fetchOnePage 薄壳）在主线程经 VM 委托完成。
 * Discuz 的接口契约（mobile=2 被重定向到桌面模板、ppp 口径、每页截断）只在这里出现；
 * RatingParser 的契约另有 fixture 快照测试看护（RatingParserContractTest）。
 */
object ThreadRepository {

    /** 默认每页楼层数（与 ThreadEntry.DEFAULT_PER_PAGE 同一口径；服务端未自报 ppp 时兜底） */
    private const val DEFAULT_PER_PAGE = ThreadEntry.DEFAULT_PER_PAGE

    /** 一页楼层的请求结果；-1 / "" / null = 响应未自报，调用方保留旧值 */
    data class PageData(
        val posts: List<Post>,
        /** 响应自报的总页数；-1 = 未自报 replies（调用方保留旧值） */
        val totalPages: Int,
        /** 响应自报的每页楼层数；-1 = 未自报 ppp（调用方保留旧值） */
        val perPage: Int,
        /** 帖子所属版块 fid；"" = 未自报 */
        val threadFid: String,
        /** 页面级令牌；"" = 未自报 */
        val formhash: String,
        /** 仅 page==1 携带：投票帖 special 字段原始值 */
        val pollSpecial: String?,
        /** 仅 page==1 携带：special_poll 原始 JSON */
        val pollJson: com.google.gson.JsonObject?,
        /** 仅 page==1 携带：主楼首张内容图（瀑布流封面缓存用） */
        val coverUrl: String?
    )

    /**
     * 拉一页楼层（40 楼口径，超出截断，不足全收）。
     * 截断上限用**本次响应自报的 ppp**——服务端每页数不是 40 时（如 20/50），
     * 按 40 截断会凭空吞掉楼层（历史事故：「部分贴子缺页/楼层错位」）。
     */
    suspend fun fetchPage(
        tid: String,
        page: Int,
        authorId: String?,
        filterToAuthor: Boolean
    ): PageData = withContext(Dispatchers.IO) {
        try {
            val r = RetrofitClient.api.getThreadDetail(
                tid = tid, page = page,
                authorid = if (filterToAuthor) authorId else null
            )
            val formhash = r.variables?.formhash.orEmpty()
            val threadFid = r.variables?.thread?.fid?.takeIf { it.isNotBlank() }.orEmpty()
            var totalPages = -1
            var perPage = -1
            r.variables?.thread?.let { t ->
                val replies = t.replies?.toIntOrNull() ?: -1
                val ppp = t.ppp?.toIntOrNull()?.takeIf { it > 0 } ?: DEFAULT_PER_PAGE
                perPage = ppp
                if (replies >= 0) {
                    // 总页数 = (回复数 + 楼主层) / 每页楼层数（口径在 util/Paging，有单测）
                    totalPages = com.example.s1client.util.Paging.totalPages(replies, ppp)
                }
            }
            val posts = r.variables?.postList ?: emptyList()
            var pollSpecial: String? = null
            var pollJson: com.google.gson.JsonObject? = null
            var coverUrl: String? = null
            if (page == 1) {
                // 投票数据顺手带走：整个帖子的属性，第 1 页的响应里就有，
                // 不必再为它单独发一次请求
                pollSpecial = r.variables?.thread?.special
                pollJson = r.variables?.specialPoll
                // 瀑布流封面缓存：服务端只对约 1/4 帖子返回预览图，
                // 进详情页看到图就顺手记下来，下次瀑布流这张卡就有封面（零额外请求）
                coverUrl = posts.firstOrNull { it.first == "1" }?.message?.let { msg ->
                    com.example.s1client.util.CoverResolver.pickFirstContentImage(msg)
                }
            }
            val cap = if (perPage > 0) perPage else DEFAULT_PER_PAGE
            PageData(
                posts = if (posts.size > cap) posts.take(cap) else posts,
                totalPages = totalPages,
                perPage = perPage,
                threadFid = threadFid,
                formhash = formhash,
                pollSpecial = pollSpecial,
                pollJson = pollJson,
                coverUrl = coverUrl
            )
        } catch (e: Exception) {
            // 不把异常吞成无痕的空列表：留下类型和页码（曾出现过 HTTP 200 却
            // 因子线程碰 View 失败、被 catch 吞成空 → 「进任何帖子都加载失败」）。
            if (e.javaClass.simpleName == "CalledFromWrongThreadException") {
                Log.e("S1Thread", "fetchPage(page=$page) 在子线程碰了 View —— 编程错误，看栈", e)
            } else {
                Log.w(
                    "S1Thread",
                    "fetchPage(page=$page) failed: ${e.javaClass.simpleName}: ${e.message}"
                )
            }
            PageData(
                posts = emptyList(), totalPages = -1, perPage = -1,
                threadFid = "", formhash = "",
                pollSpecial = null, pollJson = null, coverUrl = null
            )
        }
    }

    /** 某一页的评分聚合（网页版 viewthread HTML 的 ratelog；移动版 API 没有）。失败返回空 Map */
    suspend fun fetchRatings(tid: String, page: Int): Map<String, RateInfo> =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://stage1st.com/2b/forum.php?mod=viewthread" +
                        "&tid=$tid&page=$page&mobile=2"
                val html = RetrofitClient.httpClient.newCall(
                    okhttp3.Request.Builder().url(url).build()
                ).execute().use { it.body?.string().orEmpty() }
                RatingParser.parse(html)
            } catch (e: Exception) {
                Log.e("S1Rate", "fetch ratelog failed p$page", e)
                emptyMap()
            }
        }

    /** 某一层的完整评分历史（不受内联 ratelog 条数截断限制）。失败返回 null */
    suspend fun fetchFullRatings(tid: String, pid: String): RateInfo? =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://stage1st.com/2b/forum.php?mod=misc&action=viewratings" +
                        "&tid=$tid&pid=$pid&mobile=2"
                val html = RetrofitClient.httpClient.newCall(
                    okhttp3.Request.Builder().url(url).build()
                ).execute().use { it.body?.string().orEmpty() }
                Log.d("S1Rate", "viewratings pid=$pid len=${html.length}")
                RatingParser.parseViewRatings(html, pid)
            } catch (e: Exception) {
                Log.e("S1Rate", "fetch full ratings failed pid=$pid", e)
                null
            }
        }
}
