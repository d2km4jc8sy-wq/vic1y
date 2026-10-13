package com.example.s1client.util

import android.content.Context
import java.io.File

/**
 * 图片缓存清理 —— 原先内嵌在设置页里，设置页层级取消后挪到这里，
 * 供「我的」页的齿轮弹窗直接调用。
 */

/** Coil 磁盘缓存目录（S1App 里配置的 image_cache） */
fun imageCacheDir(context: Context): File = context.cacheDir.resolve("image_cache")

/** 目录总大小（含子目录），目录不存在返回 0 */
fun dirSize(dir: File): Long {
    if (!dir.exists()) return 0L
    return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}

/** 人类可读的体积文案：0 B / 12.3 KB / 4.5 MB */
fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    return when {
        kb < 1024 -> "%.1f KB".format(kb)
        else -> "%.1f MB".format(kb / 1024.0)
    }
}
