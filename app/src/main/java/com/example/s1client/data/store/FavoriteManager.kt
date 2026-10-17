package com.example.s1client.data.store

import com.example.s1client.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.util.UserPostParser
import com.example.s1client.util.unescapeHtmlEntities

/**
 * 收藏 / 取消收藏。
 *
 * Discuz 没有纯 JSON 的收藏接口：先请求 home.php 取回一张带 formhash 的表单 HTML，
 * 再把表单原样 POST 回去才算完成，所以这套逻辑单独抽出来给首页和帖子页共用。
 */
object FavoriteManager {

    /** 本地收藏状态（跨 Peek 实例/重启持久）：Peek 二次点收藏的判定依据 */
    private lateinit var localPrefs: android.content.SharedPreferences

    /** 应用启动时初始化（与 ForumDataCache 等同款） */
    fun initLocal(app: android.app.Application) {
        localPrefs = app.getSharedPreferences("fav_local", android.content.Context.MODE_PRIVATE)
    }

    fun isLocalFaved(tid: String): Boolean {
        val r = tid.isNotBlank() && localPrefs.getStringSet("faved", emptySet())!!.contains(tid)
        return r
    }

    fun markLocalFaved(tid: String) {
        if (tid.isBlank()) return
        val set = localPrefs.getStringSet("faved", emptySet())!!.toMutableSet()
        set.add(tid)
        localPrefs.edit().putStringSet("faved", set).apply()
    }

    fun unmarkLocalFaved(tid: String) {
        if (tid.isBlank()) return
        val set = localPrefs.getStringSet("faved", emptySet())!!.toMutableSet()
        set.remove(tid)
        localPrefs.edit().putStringSet("faved", set).apply()
    }

    /** isFavorited：操作后的状态；failed：请求本身失败，message 为原因 */
    data class Result(
        val isFavorited: Boolean = false,
        val failed: Boolean = false,
        val message: String = ""
    )

    /**
     * 查询某个帖子当前是否已收藏。
     *
     * 为什么需要它：帖子详情页原先从 Intent 的 `favorited` extra 取初始状态，
     * 但全项目**没有任何一处 putExtra("favorited", ...)**，于是初始值恒为 false ——
     * 表现就是"进帖子星星永远是空心的"，哪怕它其实已收藏。
     * 复用收藏列表页同一套 HTML 解析来判断，逻辑只有一份。
     *
     * @return null 表示查询失败（调用方应保持原状态，不要误显示成未收藏）
     */
    suspend fun isFavorited(tid: String): Boolean? {
        if (tid.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://stage1st.com/2b/home.php?mod=space&do=favorite&view=me"
                val html = RetrofitClient.fetchHtml(url)
                if (html.isBlank()) return@withContext null
                val items = com.example.s1client.util.UserPostParser.parseFavoriteList(html)
                // 收藏列表为空是有意义的结论（真的没收藏）；解析不出任何东西才当失败
                if (items.isEmpty() && html.contains("请先登录")) return@withContext null
                items.any { it.tid == tid }
            } catch (e: Exception) {
                android.util.Log.w("FavoriteManager", "isFavorited failed: ${e.message}")
                null
            }
        }
    }

    /** 已收藏则取消，未收藏则收藏。调用方需自行确认已登录。 */
    suspend fun toggleFavorite(tid: String): Result {
        if (tid.isBlank()) return Result(failed = true, message = "缺少 tid")
        val fh = fetchFormhash(tid)
        if (fh.isBlank()) return Result(failed = true, message = "获取 formhash 失败")

        return withContext(Dispatchers.IO) {
            try {
                val addBody = RetrofitClient.api.addFavorite(id = tid, formhash = fh).string()
                when {
                    addBody.contains("抱歉，您已收藏") || addBody.contains("已经收藏") ->
                        removeFavorite(tid, fh)
                    addBody.contains("favoriteform") || addBody.contains("favoritesubmit") ->
                        submitForm(addBody, fh, isDelete = false)
                    addBody.contains("alert_right") || addBody.contains("收藏成功") -> {
                        markLocalFaved(tid)
                        Result(isFavorited = true)
                    }
                    addBody.contains("alert_error") || addBody.contains("抱歉") || addBody.contains("失败") ->
                        Result(failed = true, message = addBody.take(120))
                    else -> { markLocalFaved(tid); Result(isFavorited = true) }
                }
            } catch (e: Exception) {
                Result(failed = true, message = e.message ?: "")
            }
        }
    }

    /** 只收藏不取消（长按 Peek 单向收藏用）：已收藏时友好返回，绝不走 remove。
     *  取消收藏请到收藏列表手动操作（2026-10-15 用户定稿：Peek 不做 toggle）。 */
    suspend fun addFavorite(tid: String): Result {
        if (tid.isBlank()) return Result(failed = true, message = "缺少 tid")
        val fh = fetchFormhash(tid)
        if (fh.isBlank()) return Result(failed = true, message = "获取 formhash 失败")

        return withContext(Dispatchers.IO) {
            try {
                val addBody = RetrofitClient.api.addFavorite(id = tid, formhash = fh).string()
                when {
                    addBody.contains("抱歉，您已收藏") || addBody.contains("已经收藏") -> {
                        markLocalFaved(tid)
                        Result(isFavorited = true, message = "已在收藏中")
                    }
                    addBody.contains("favoriteform") || addBody.contains("favoritesubmit") ->
                        submitForm(addBody, fh, isDelete = false)
                    addBody.contains("alert_right") || addBody.contains("收藏成功") ->
                        Result(isFavorited = true)
                    addBody.contains("alert_error") || addBody.contains("抱歉") || addBody.contains("失败") ->
                        Result(failed = true, message = addBody.take(120))
                    else -> Result(isFavorited = true)
                }
            } catch (e: Exception) {
                Result(failed = true, message = e.message ?: "")
            }
        }
    }

    /** 只取消不收藏（Peek 二次点收藏的「撤销」用，2026-10-15 用户定稿）：未收藏时友好返回 */
    suspend fun cancelFavorite(tid: String): Result {
        if (tid.isBlank()) return Result(failed = true, message = "缺少 tid")
        val fh = fetchFormhash(tid)
        if (fh.isBlank()) return Result(failed = true, message = "获取 formhash 失败")
        val r = removeFavorite(tid, fh)
        if (!r.failed) unmarkLocalFaved(tid)
        return r
    }

    private suspend fun fetchFormhash(tid: String): String {
        val fh = try {
            withContext(Dispatchers.IO) { RetrofitClient.api.getThreadDetail(tid = tid).variables?.formhash }
        } catch (e: Exception) {
            null
        }
        return fh ?: ForumDataCache.formhash
    }

    private suspend fun removeFavorite(tid: String, fh: String): Result {
        val delBody = RetrofitClient.api.removeFavoritePrepare(id = tid, formhash = fh).string()
        if (delBody.contains("抱歉") || delBody.contains("未收藏")) return Result(isFavorited = false)
        return submitForm(delBody, fh, isDelete = true)
    }

    private fun submitForm(html: String, fh: String, isDelete: Boolean): Result {
        return try {
            val actionRaw = Regex("""<form[^>]*?action="([^"]+)"""").find(html)?.groupValues?.get(1)
                ?: return Result(failed = true, message = "未找到表单")
            // R11 收敛（2026-10-18 第 9 轮审计）：form action URL 的实体解码。
            // 原来只解 &amp;（URL 查询参数分隔符必须还原）；现统一走公共函数，
            // 其余实体在 URL 中极少见，单次 replace 不会过度解码。
            val action = unescapeHtmlEntities(actionRaw) ?: actionRaw

            val params = Regex("""<input[^>]*?name="([^"]+)"[^>]*?value="([^"]*)"""")
                .findAll(html).map { it.groupValues[1] to it.groupValues[2] }.toList()

            val formBody = okhttp3.FormBody.Builder()
            for ((k, v) in params) if (k != "formhash") formBody.add(k, v)
            formBody.add("formhash", fh)
            if (isDelete) formBody.add("deletesubmit", "true") else formBody.add("favoritesubmit", "true")

            val req = okhttp3.Request.Builder()
                .url("https://stage1st.com/2b/" + action).post(formBody.build())
                .header("Referer", "https://stage1st.com/2b/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            // use{}：Response 必须关闭，否则连接不归还连接池（反复收藏/取消会耗尽连接）
            val outcome = RetrofitClient.httpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                val ok = body.contains("alert_right") ||
                        (body.contains("成功") && !body.contains("失败")) ||
                        (resp.code in 200..299 && !body.contains("抱歉") && !body.contains("alert_error"))
                if (ok) null else body.take(100)
            }
            if (outcome == null) Result(isFavorited = !isDelete)
            else Result(failed = true, message = outcome)
        } catch (e: Exception) {
            Result(failed = true, message = e.message ?: "")
        }
    }
}
