package com.example.s1client.share

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.View.MeasureSpec.UNSPECIFIED
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.makeMeasureSpec
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * 分享卡渲染与分享。
 *
 * 渲染策略：先在「设计基准宽度 360dp」上把卡片画出来（dp→px 按设备密度，比例正确），
 * 再按目标宽（1080/1440/2160）整体缩放 —— 既保证卡片各间距相对宽度比例正确，
 * 又拿到高分辨率输出。纯 Android View 离屏 measure/layout/draw，可靠、无 Compose 时序隐患。
 */
object ShareCardCapture {

    private const val BASE_DP = 360

    /** 离屏渲染成位图（含按比例放大到目标宽） */
    fun captureBitmap(
        ctx: Context,
        config: ShareCardConfig,
        posts: List<ShareCardPost>,
        colors: CardColors,
        title: String
    ): Bitmap {
        val density = ctx.resources.displayMetrics.density
        val baseW = (BASE_DP * density).toInt().coerceAtLeast(1)
        val view = buildCardView(ctx, config, posts, colors, title)
        view.layoutParams = ViewGroup.LayoutParams(baseW, ViewGroup.LayoutParams.WRAP_CONTENT)
        view.measure(makeMeasureSpec(baseW, EXACTLY), makeMeasureSpec(0, UNSPECIFIED))
        val h = view.measuredHeight.coerceAtLeast(1)
        view.layout(0, 0, baseW, h)

        val bmp = Bitmap.createBitmap(baseW, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        // JPG 无透明通道：先把整张铺成卡底色，圆角外的区域也就是卡底色（不露黑）
        if (config.format == ImageFormat.JPG) canvas.drawColor(colors.cardBg)
        view.draw(canvas)

        val target = config.widthPx
        return if (target != bmp.width) {
            val th = (bmp.height * target.toFloat() / bmp.width).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bmp, target, th, true)
        } else {
            bmp
        }
    }

    /** 渲染 → 存缓存 → 调起系统分享面板 */
    fun generateAndShare(
        activity: Activity,
        config: ShareCardConfig,
        posts: List<ShareCardPost>,
        title: String
    ) {
        try {
            val colors = computeColors(config)
            val bmp = captureBitmap(activity, config, posts, colors, title)
            val ext = if (config.format == ImageFormat.PNG) "png" else "jpg"
            val file = File(activity.cacheDir, "share_card_${System.currentTimeMillis()}.$ext")
            FileOutputStream(file).use { os ->
                val fmt = if (config.format == ImageFormat.PNG) Bitmap.CompressFormat.PNG
                else Bitmap.CompressFormat.JPEG
                val quality = if (config.format == ImageFormat.PNG) 100 else 92
                bmp.compress(fmt, quality, os)
            }
            bmp.recycle()

            val authority = "${activity.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(activity, authority, file)
            val mime = if (config.format == ImageFormat.PNG) "image/png" else "image/jpeg"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(Intent.createChooser(intent, "分享分享卡"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(activity, "生成分享卡失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
