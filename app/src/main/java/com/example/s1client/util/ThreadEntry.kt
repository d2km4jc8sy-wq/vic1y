package com.example.s1client.util

/**
 * 帖子详情页的「起始页判定」纯函数（阶段二抽取，2026-10-16）。
 *
 * 对应 ThreadDetailActivity.loadFirstPage 里的两段口径——
 * 过去它们内联在 Activity 里，是「进帖落错页 / 进度恢复漂移」两类事故的高发区；
 * 抽成纯函数后口径唯一、可离线单测（ThreadEntryTest，12 个边界用例）。
 *
 * 判定顺序（照 s1er 的 resolveThreadInitialPage）：
 *   ① intent 显式 page → 直接用
 *   ② 带 pid（提醒跳转）→ PostLocator 定位（网络操作，留在调用方，本文件不管）
 *   ③ 本地阅读进度 → 由「绝对楼层 + 当时每页帖数」推页（[guessFromFloor]）
 *   ④ 都没有 → 第 1 页
 * 拿到真实 ppp 后再纠正一次（[correctionTarget]）。
 */
object ThreadEntry {

    /** 服务端未报 ppp 时的兜底每页楼层数（与 ThreadDetailActivity.PAGE_SIZE 同一口径） */
    const val DEFAULT_PER_PAGE = 40

    /**
     * 进度恢复的初始页猜测。
     * [resumeFloor] 绝对楼层（1-based，跨页累计）；[recordedPerPage] 上次阅读时服务端自报的 ppp。
     * resumeFloor ≤ 1 或没有有效 ppp 时落第 1 页。
     */
    fun guessFromFloor(resumeFloor: Int, recordedPerPage: Int): Int =
        if (resumeFloor > 1) {
            val ppp = if (recordedPerPage > 0) recordedPerPage else DEFAULT_PER_PAGE
            ((resumeFloor - 1) / ppp) + 1
        } else {
            1
        }

    /**
     * 拿到真实 ppp 后的页码纠正。
     * 返回非 null = 应该改拉这一页（调用方重取并替换内容）；null = 不纠正。
     * 与原内联口径逐字一致：
     *   仅在「有进度、没有待定位 pid、真实 ppp 有效」时计算；
     *   want 与当前页相同、或超出总页数（totalPages ≤ 0 按 1 兜底）时不纠正。
     */
    fun correctionTarget(
        startPage: Int,
        resumeFloor: Int,
        hasPendingPid: Boolean,
        livePerPage: Int,
        totalPages: Int
    ): Int? {
        if (resumeFloor <= 1 || hasPendingPid || livePerPage <= 0) return null
        val want = ((resumeFloor - 1) / livePerPage) + 1
        return if (want != startPage && want <= totalPages.coerceAtLeast(1)) want else null
    }
}
