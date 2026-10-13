package com.example.s1client.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtil {

    /** 首页列表：相对时间，如"3分钟前"、"2小时前"、"10-08" */
    fun formatRelative(dateline: String?): String {
        val ts = dateline?.trim()?.toLongOrNull() ?: return ""
        if (ts <= 0) return ""
        val now = System.currentTimeMillis() / 1000
        val diff = now - ts
        return when {
            diff < 0 -> ""              // 未来时间，异常值
            diff < 60 -> "刚刚"
            diff < 3600 -> "${diff / 60}分钟前"
            diff < 86400 -> "${diff / 3600}小时前"
            diff < 86400 * 7 -> "${diff / 86400}天前"
            diff < 86400 * 365 -> {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Date(ts * 1000))
            }
            else -> {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Date(ts * 1000))
            }
        }
    }

    /** 首页列表（旧）：精确到年月日 */
    fun formatDate(dateline: String?): String {
        val ts = dateline?.trim()?.toLongOrNull() ?: return ""
        if (ts <= 0) return ""
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date(ts * 1000))
    }

    /** 详情页：精确到分 */
    fun formatDateTime(dateline: String?): String {
        val ts = dateline?.trim()?.toLongOrNull() ?: return ""
        if (ts <= 0) return ""
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(ts * 1000))
    }

    /**
     * 帖子详情页楼层时间：一周内显示相对时间（几分钟前 / N 小时前 / N 天前），
     * 更早显示原日期。服务端对久远的楼层会直接返回 "2016-11-11 22:37" 这种串，
     * 那就原样显示。
     */
    fun formatSmart(dateline: String?): String {
        val s = dateline?.trim().orEmpty()
        if (s.isEmpty()) return ""
        if (s.toLongOrNull() == null) return s
        return formatRelative(s)
    }
}
