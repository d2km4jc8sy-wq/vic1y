package com.example.s1client.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.s1client.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Discuz 附件上传（本地图片）。
 *
 * 抓包确认的发帖页隐藏表单：
 *   action="misc.php?mod=swfupload&operation=upload&simple=1&type=image"
 *   隐藏域 uid / hash，文件域 Filedata
 * 上传成功返回 "DISCUZUPLOAD|status|aid|isimage|sizelimit"（status=0 为成功），
 * aid 就是附件 id：正文里写 [attachimg]aid[/attachimg] 内嵌显示。
 *
 * 注意：提交帖子时还必须带上 attachnew[aid][readperm|price|description] 字段，
 * 服务端 updateattach() 才会把这个「未使用附件」挂到帖子上，否则图不会渲染。
 *
 * uid / hash 只渲染在「有发帖权限」的页面里，所以这里按顺序尝试多个页面，
 * 取第一个拿得到的。uid / hash 是会话级的，取一次缓存即可。
 */
object ImageUploader {

    private const val TAG = "S1Upload"
    private const val BASE = "https://stage1st.com/2b/"
    private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    @Volatile private var cachedUid: String = ""
    @Volatile private var cachedHash: String = ""
    @Volatile private var cachedReferer: String = BASE

    /** 发新主题页（该版面没发帖权限时不含凭证） */
    fun newthreadPage(fid: String): String = "forum.php?mod=post&action=newthread&fid=$fid"

    /** 回复页：这个帖子能回就一定有凭证 —— 版务/测试这类受限版面发图靠它兜底 */
    fun replyPage(tid: String, fid: String = ""): String =
        "forum.php?mod=post&action=reply&tid=$tid" + if (fid.isBlank()) "" else "&fid=$fid"

    /** 编辑页（自己的楼层） */
    fun editPage(tid: String, pid: String, fid: String = ""): String =
        "forum.php?mod=post&action=edit&tid=$tid&pid=$pid" + if (fid.isBlank()) "" else "&fid=$fid"

    /** imagelist 返回的 <img> 里 aid / size / key 三件套 */
    private val IMAGE_KEY_RE = Regex(
        """mod=image&(?:amp;)?aid=(\d+)&(?:amp;)?size=(\d+x\d+)&(?:amp;)?key=([0-9a-fA-F]+)"""
    )

    /**
     * 取当前账号「未使用附件」的预览图地址：aid -> 可直接显示的 url。
     *
     * 附件图必须带 key（实测不带 key 会 302），而 key 只在
     * forum.php?mod=ajax&action=imagelist 的返回里，所以预览本地图片前先来取一次。
     */
    suspend fun attachmentImageUrls(): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(BASE + "forum.php?mod=ajax&action=imagelist&posttime=0&inajax=yes")
                .header("User-Agent", UA)
                .header("Referer", BASE)
                .get()
                .build()
            val html = RetrofitClient.httpClient.newCall(req).execute()
                .use { it.body?.string().orEmpty() }
            Log.d(TAG, "imagelist len=${html.length}")
            IMAGE_KEY_RE.findAll(html).associate { m ->
                val aid = m.groupValues[1]
                aid to BASE + "forum.php?mod=image&aid=" + aid +
                        "&size=" + m.groupValues[2] + "&key=" + m.groupValues[3] +
                        "&nocache=yes&type=fixnone"
            }
        } catch (e: Exception) {
            Log.w(TAG, "attachmentImageUrls failed", e)
            emptyMap()
        }
    }

    /** 上传一张本地图片，成功返回附件 aid */
    suspend fun upload(context: Context, uri: Uri, tokenPages: List<String>): String =
        withContext(Dispatchers.IO) {
            val (uid, hash, referer) = ensureToken(tokenPages)
            val mime = context.contentResolver.getType(uri) ?: "image/*"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("读取图片失败")
            val fileName = "s1_" + System.currentTimeMillis() + guessExt(mime)

            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("uid", uid)
                .addFormDataPart("hash", hash)
                .addFormDataPart("Filedata", fileName, bytes.toRequestBody(mime.toMediaTypeOrNull()))
                .build()

            val url = BASE + "misc.php?mod=swfupload&operation=upload&simple=1&type=image"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .header("Referer", referer)
                .post(body)
                .build()

            Log.d(TAG, "uploading file=$fileName size=${bytes.size} mime=$mime referer=$referer")
            val resp = RetrofitClient.httpClient.newCall(req).execute()
            val code = resp.code
            val text = resp.use { it.body?.string().orEmpty() }
            Log.d(TAG, "upload HTTP=$code response=[${text.trim()}]")
            parseAid(text)
        }

    private fun parseAid(text: String): String {
        // 服务端格式：DISCUZUPLOAD|status|aid|isimage|sizelimit
        // status=0 表示成功；aid 在第 3 段（不是第 2 段！）
        val parts = text.trim().split("|")
        if (parts.size >= 3 && parts[0].contains("DISCUZUPLOAD")) {
            val status = parts[1].trim().toIntOrNull() ?: -1
            val aid = parts[2].trim()
            if (status == 0 && aid.isNotEmpty() && aid != "0") return aid
            throw IllegalStateException("上传失败（状态 $status）：" + text.trim().take(120))
        }
        throw IllegalStateException("上传失败：" + text.trim().take(120))
    }

    /**
     * 依次尝试这些「带发帖框」的页面（相对 /2b/ 的路径），取第一组 uid + hash。
     *
     * 为什么不能只用发帖页：uid/hash 是跟「有没有发帖权限」一起渲染的，
     * 「版务管理区」「测试」这类只让回帖的版面，action=newthread 会返回
     * 「抱歉，您没有权限发表」而不含凭证，于是上传必然失败。
     * 所以编辑走编辑页、回帖走回复页，再兜底发帖页。
     */
    private suspend fun ensureToken(pages: List<String>): Triple<String, String, String> {
        if (cachedUid.isNotBlank() && cachedHash.isNotBlank()) {
            return Triple(cachedUid, cachedHash, cachedReferer)
        }
        for (path in pages) {
            if (path.isBlank()) continue
            val url = if (path.startsWith("http")) path else BASE + path
            val html = try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", UA)
                    .header("Referer", BASE)
                    .get()
                    .build()
                RetrofitClient.httpClient.newCall(req).execute()
                    .use { it.body?.string().orEmpty() }
            } catch (e: Exception) {
                Log.w(TAG, "ensureToken 请求失败 $url", e)
                continue
            }
            val uid = inputValueOf(html, "uid")
            val hash = inputValueOf(html, "hash")
            Log.d(TAG, "ensureToken url=$url len=${html.length} uid=$uid hashLen=${hash.length}")
            if (uid.isNotBlank() && hash.isNotBlank()) {
                cachedUid = uid
                cachedHash = hash
                cachedReferer = url
                return Triple(uid, hash, url)
            }
        }
        throw IllegalStateException("获取上传凭证失败，请确认已登录")
    }

    /** 取 <input name="field" value="..."> 的 value，兼容属性顺序与单/双引号 */
    private fun inputValueOf(html: String, field: String): String {
        val tagRe = Regex(
            """<input\b[^>]*\bname\s*=\s*["']?\b""" + field + """\b["']?[^>]*>""",
            RegexOption.IGNORE_CASE
        )
        val valRe = Regex("""\bvalue\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
        return tagRe.findAll(html)
            .mapNotNull { valRe.find(it.value)?.groupValues?.get(1) }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
    }

    private fun guessExt(mime: String): String = when {
        mime.contains("png") -> ".png"
        mime.contains("gif") -> ".gif"
        mime.contains("webp") -> ".webp"
        mime.contains("bmp") -> ".bmp"
        else -> ".jpg"
    }
}
