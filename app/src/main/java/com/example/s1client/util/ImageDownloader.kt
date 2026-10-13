package com.example.s1client.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.s1client.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object ImageDownloader {

    /**
     * 下载图片并保存到相册。
     * Android 10+ 用 MediaStore（不需要权限）。
     * Android 9- 用外部存储目录（需要 WRITE_EXTERNAL_STORAGE 权限）。
     */
    suspend fun downloadToGallery(
        context: Context,
        url: String,
        onProgress: (Int) -> Unit = {}
    ): String? = withContext(Dispatchers.IO) {
        try {
            val filename = buildFilename(url)

            // 1. 下载
            val req = Request.Builder()
                .url(url)
                .header("Referer", "https://stage1st.com/2b/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            // Response 必须关闭，否则连接不归还连接池（反复保存图片会耗尽连接/内存）
            RetrofitClient.httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null

                val bytes = resp.body?.byteStream()?.readBytes() ?: return@withContext null
                onProgress(100)

                // 2. 保存
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveViaMediaStore(context, filename, bytes)
                } else {
                    saveViaFile(context, filename, bytes)
                }

                filename
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun buildFilename(url: String): String {
        val ext = when {
            url.contains(".png") -> "png"
            url.contains(".gif") -> "gif"
            url.contains(".webp") -> "webp"
            else -> "jpg"
        }
        return "s1_${System.currentTimeMillis()}.$ext"
    }

    private fun saveViaMediaStore(context: Context, filename: String, bytes: ByteArray): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/S1Client")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(bytes)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }
        return true
    }

    private fun saveViaFile(context: Context, filename: String, bytes: ByteArray): Boolean {
        return try {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "S1Client"
            )
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, filename)
            FileOutputStream(file).use { it.write(bytes) }
            // 通知系统刷新相册
            android.media.MediaScannerConnection.scanFile(
                context, arrayOf(file.absolutePath), null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
