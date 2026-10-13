package com.example.s1client.util

/**
 * 帖子分页的纯计算逻辑。
 *
 * 抽出来的原因：分页是本项目最容易出"最后一页够不着"这类边界 bug 的地方
 * （曾出现只能滑到 n-1 页），而它同时又不依赖任何 Android 上下文，适合直接单测。
 */
object Paging {

    /** 兜底每页楼层数：与论坛 API 的 `ppp` 一致（拿不到时用） */
    const val FALLBACK_PER_PAGE = 40

    /**
     * 由「回复数 + 每页楼层数」算总页数。
     *
     * @param replies 帖子回复数（**不含**楼主那层），非法值（负数/空）返回 [fallbackTotal]
     * @param perPage 每页楼层数，非正数时退回 [FALLBACK_PER_PAGE]
     * @param fallbackTotal replies 拿不到时的兜底页数
     *
     * 口径：总楼层 = replies + 1（含楼主），页数 = 向上取整。与开源 S1er 一致。
     */
    fun totalPages(replies: Int?, perPage: Int?, fallbackTotal: Int = 1): Int {
        val r = replies ?: return fallbackTotal
        if (r < 0) return fallbackTotal
        val ppp = if (perPage != null && perPage > 0) perPage else FALLBACK_PER_PAGE
        val totalPosts = r + 1
        return ((totalPosts + ppp - 1) / ppp).coerceAtLeast(1)
    }

    /**
     * 由楼层号推页码（1-based）与页内下标（0-based）。
     *
     * @param floor 楼层号，从 1 开始
     */
    fun locateFloor(floor: Int, perPage: Int?): Pair<Int, Int> {
        val ppp = if (perPage != null && perPage > 0) perPage else FALLBACK_PER_PAGE
        val f = floor.coerceAtLeast(1)
        return ((f - 1) / ppp + 1) to ((f - 1) % ppp)
    }

    /** 目标页是否在帖子范围内（总页数未知时保守放行） */
    fun pageInRange(page: Int, totalPages: Int): Boolean =
        page >= 1 && (totalPages <= 0 || page <= totalPages)

    /** 顺序往下还能不能继续加载 */
    fun morePagesToLoad(loadedMaxPage: Int, totalPages: Int): Boolean =
        totalPages <= 0 || loadedMaxPage < totalPages
}

/**
 * 「欢迎加入 stage1 N 天」的纯计算。
 *
 * 要求：按**注册时间到真实当前时间**算，而不是本地累加天数（累加会因改机时间、
 * 时区、跨夏令时算错）。跨天时以本地自然日为界。
 */
object JoinDays {

    /**
     * 从各种写法的注册日期算出「已加入多少天」。
     *
     * @param regDate 形如 "2024-05-01" / "2024/5/1" / "2024-05-01 12:30:00"
     * @param now 当前时间（可注入，便于测试）
     * @return 天数；解析不出返回 null（调用方决定降级文案）
     */
    fun daysSince(regDate: String, now: java.util.Calendar = java.util.Calendar.getInstance()): Int? {
        val m = Regex("""(\d{4})\s*[-/.年]\s*(\d{1,2})\s*[-/.月]\s*(\d{1,2})""").find(regDate)
            ?: return null
        val y = m.groupValues[1].toIntOrNull() ?: return null
        val mo = m.groupValues[2].toIntOrNull() ?: return null
        val d = m.groupValues[3].toIntOrNull() ?: return null
        if (mo !in 1..12 || d !in 1..31) return null

        // 抹掉时分秒，只按自然日相减
        val reg = java.util.Calendar.getInstance().apply {
            set(y, mo - 1, d, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val today = java.util.Calendar.getInstance().apply {
            time = now.time
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (today.before(reg)) return 0
        val diff = today.timeInMillis - reg.timeInMillis
        return (diff / (24L * 60 * 60 * 1000)).toInt()
    }

    /** 完整的欢迎文案；解析失败时返回 null，调用方回退到原来的「YYYY年M月D日 加入 S1」 */
    fun welcomeText(regDate: String, now: java.util.Calendar = java.util.Calendar.getInstance()): String? {
        val d = daysSince(regDate, now) ?: return null
        return "欢迎加入 stage1 $d 天"
    }
}
