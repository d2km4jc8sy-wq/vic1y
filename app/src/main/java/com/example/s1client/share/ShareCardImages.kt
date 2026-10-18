package com.example.s1client.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 分享卡媒体预加载（2026-10-18 二问「头像/图不显示」修复）。
 *
 * 分享卡是离屏 View 渲染（ShareCardCapture），没有 Coil 在 Compose UI 里的异步占位
 * 机制，所以必须在渲染前把头像与正文图一次性拉进内存（软位图，allowHardware(false)），
 * buildCardView 渲染时直接贴。
 *
 * - 头像：uid → avatarUrl(uid, "small")，与 App 内同源 URL
 * - 正文图：PostContentParser 解出的楼层图片 URL
 * - 降级：拉不到（超时/断网/防盗链）就缺席——头像退回首字母圆，图片不显示，
 *   不阻塞出图（设计者已确认此降级方案）
 * - 超时由调用方控制（ShareCardActivity withTimeoutOrNull 8s）：结果写入 [out]，
 *   中途超时也保留已拉到的部分
 * - 内存：单图按最长边 1080 解码，避免 S1 原图（200~450KB，长边数千 px）吃满内存
 * - 不走 DataSaver 省流策略：分享卡是显式用户动作，生成即同意拉图
 */
object ShareCardImages {

    suspend fun preload(
        ctx: Context,
        posts: List<ShareCardPost>,
        out: ConcurrentHashMap<String, Bitmap>
    ) = withContext(Dispatchers.IO) {
        val loader = coil.Coil.imageLoader(ctx)
        val urls = buildList {
            posts.forEach { p ->
                p.authorId?.let { uid ->
                    com.example.s1client.util.avatarUrl(uid, "small")?.let { add(it) }
                }
                addAll(p.imageUrls)
            }
        }.distinct()
        urls.forEach { url ->
            if (out.containsKey(url)) return@forEach
            runCatching {
                val req = coil.request.ImageRequest.Builder(ctx)
                    .data(url)
                    .allowHardware(false)
                    .size(1080)
                    .build()
                val bmp = (loader.execute(req).drawable as? BitmapDrawable)?.bitmap
                if (bmp != null && bmp.config != Bitmap.Config.HARDWARE) out[url] = bmp
            }
        }
    }
}
