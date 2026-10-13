package com.example.s1client.util

import android.content.Context

/** 短 Toast。用法：toast("已保存") */
fun Context.toast(s: String) =
    S1Notify.show(this, s)

/** dp -> px。用法：dpToPx(16) */
fun Context.dpToPx(dp: Int): Int =
    (dp * resources.displayMetrics.density).toInt()

/**
 * 根据 uid 生成 S1 头像 URL。
 * @param size "middle" 或 "small"
 * @return null 表示 uid 无效
 */
fun avatarUrl(uid: String, size: String = "middle"): String? {
    val n = uid.toIntOrNull() ?: return null
    if (n <= 0) return null
    val padded = String.format("%09d", n)
    val path = "${padded.substring(0, 3)}/${padded.substring(3, 5)}/" +
               "${padded.substring(5, 7)}/${padded.substring(7, 9)}"
    return "https://avatar.stage1st.com/${path}_avatar_$size.jpg"
}

/**
 * 根据附件相对路径生成 S1 图片 URL。
 * @param path 形如 "202610/01/095114xwy8m1ioi35w1159.png"
 * @return null 表示路径无效
 */
fun attachmentUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    return "https://stage1st.com/2b/data/attachment/forum/$path"
}
