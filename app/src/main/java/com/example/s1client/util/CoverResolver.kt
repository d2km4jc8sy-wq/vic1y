package com.example.s1client.util

import android.util.Log
import com.example.s1client.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.Request

/**
 * 瀑布流封面图解析 —— **本项目的差异化功能**。
 *
 * ## 为什么需要它
 * Discuz 的 `forumdisplay` 接口**不返回** `attachmentImagePreviewList` 的内容
 * （实测 fid=6 前 8 条：`attachment=2` 有附件，但 `attachmentImagePreviewList` 全是空数组），
 * 所以首页瀑布流拿不到封面 URL。开源 s1er 干脆不做列表封面图。
 * 我们决定做 —— 数据源只能走帖子详情 HTML 的首楼第一张内容图。
 *
 * ## 实现要点
 * - **限流**：一屏 6~10 张卡同时解析会打爆服务端，用 [Semaphore] 控制并发（默认 3）；
 * - **缓存**：tid → URL 内存缓存，翻页回来不重复请求；
 * - **抠图过滤**：排除头像 / 表情 / 公共图标 / 勋章，只认附件图或站外图，
 *   否则首页会冒出满屏的表情和图标；
 * - **失败静默**：任何异常都返回 null，卡片退回纯文字样式，不弹错不卡列表。
 */
object CoverResolver {

    private const val TAG = "S1Cover"

    /** 同时最多几个解析任务（服务端友好 + 不给用户网络添堵） */
    private const val MAX_CONCURRENT = 3

    private val gate = Semaphore(MAX_CONCURRENT)

    /** tid -> 封面 URL；null 表示"已确认这张帖没有可用图"，也要缓存，避免反复请求 */
    private val cache = HashMap<String, String?>()

    /** 正在解析中的 tid，避免同屏重复入队 */
    private val inFlight = HashSet<String>()

    /** 不需要再解析的 tid 集合（已出结果的） */
    private val done = HashSet<String>()

    /**
     * 取某个帖子的封面图 URL。
     * @return 图片绝对 URL；无图 / 解析失败返回 null
     */
    suspend fun resolve(tid: String): String? {
        if (tid.isBlank()) return null
        synchronized(cache) {
            if (cache.containsKey(tid)) return cache[tid]
            if (!inFlight.add(tid)) return null   // 已在解析，先返回 null
        }
        return try {
            gate.withPermit { fetchFirstImage(tid) }
                .also { url -> synchronized(cache) { cache[tid] = url } }
        } catch (e: Exception) {
            Log.w(TAG, "resolve tid=$tid failed: ${e.javaClass.simpleName}: ${e.message}")
            null
        } finally {
            synchronized(cache) { inFlight.remove(tid) }
        }
    }

    /** 批量解析（供"预取下一屏"用） */
    suspend fun resolveAll(tids: List<String>): Map<String, String?> = coroutineScope {
        tids.filter { it.isNotBlank() }
            .map { tid -> async(Dispatchers.IO) { tid to resolve(tid) } }
            .awaitAll()
            .toMap()
    }

    /** 已经拿到结果的 tid（含"确认无图"），供 UI 判断要不要显示占位 */
    fun isResolved(tid: String): Boolean = synchronized(cache) { cache.containsKey(tid) }

    fun cached(tid: String): String? = synchronized(cache) { cache[tid] }

    fun clear() = synchronized(cache) { cache.clear(); done.clear() }

    /**
     * 请求帖子详情 HTML，抠出第一张可用的内容图。
     * 用 `mobile=no` 拿 PC 版 —— 移动版会把图片裁成缩略图，PC 版保留原图 URL。
     */
    private suspend fun fetchFirstImage(tid: String): String? = withIo {
        val req = Request.Builder()
            .url("https://stage1st.com/2b/forum.php?mod=viewthread&tid=$tid&page=1&mobile=no")
            .header("Referer", "https://stage1st.com/2b/forum.php")
            .build()
        RetrofitClient.httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withIo null
            val html = resp.body?.string().orEmpty()
            pickFirstContentImage(html)
        }
    }

    /**
     * 从 HTML 里挑第一张"内容图"。
     * 排除项（这些在首页当封面会非常丑）：
     *   - `static/image/smiley/` 表情
     *   - `static/image/common/` 公共图标（箭头、勋章、装饰）
     *   - `static/image/magic/` 魔法道具
     *   - 头像（class="avatar" / avatars 域名）
     * 保留项：`data/attachment/` 附件图、`uc_server/avatar` 之外的站外图。
     */
    internal fun pickFirstContentImage(html: String): String? {
        if (html.isBlank()) return null
        val imgRe = Regex("""<img[^>]+?src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        for (m in imgRe.findAll(html)) {
            val src = m.groupValues[1].trim()
            if (src.isEmpty()) continue
            // 取这个 img 标签的完整文本，用来排除头像
            val whole = m.value.lowercase()
            val isAvatar = whole.contains("avatar") || whole.contains("uc_server")
            if (isAvatar) continue
            if (src.contains("static/image/smiley")) continue
            if (src.contains("static/image/common")) continue
            if (src.contains("static/image/magic")) continue
            if (src.startsWith("data:image")) continue          // 内联 base64 占位
            if (src.startsWith("//")) continue
            // 附件图或真正的站外图
            val ok = src.startsWith("data/attachment") ||
                src.startsWith("http://") || src.startsWith("https://")
            if (!ok) continue
            return if (src.startsWith("http")) src
            else "https://stage1st.com/2b/$src"
        }
        return null
    }

    private suspend inline fun <T> withIo(crossinline block: () -> T): T =
        kotlinx.coroutines.withContext(Dispatchers.IO) { block() }
}
