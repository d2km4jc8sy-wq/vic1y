package com.example.s1client.feature_changelog

/**
 * 更新日志条目（feature_changelog，阶段二④配套 2026-10-17）。
 *
 * 数据为**内置静态**：新版本发布时在此列表头部追加一条即可，
 * 不走网络（版本信息随包分发，天然与安装包一致）。
 */
data class ChangelogEntry(
    val version: String,
    val date: String,
    val lines: List<String>
)

object ChangelogData {

    /** 内置更新日志：最新的在最上（ChangelogTest 锚定顺序与内容非空） */
    val entries: List<ChangelogEntry> = listOf(
        ChangelogEntry(
            version = "1.1.0",
            date = "2026-10-17",
            lines = listOf(
                "新增：更新日志页、per-app 语言声明、targetSdk 35",
                "修复：评分负分不显示（内联明细全量收录）",
                "性能：幻彩流光降载（小米10 左右滑掉帧 A/B -61%）、图片区复用、屏蔽精准刷新",
                "交互：票夹默认隐藏，下滑超过视口约 8% 才现身，触摸区收紧",
                "工程：详情页状态收敛 ViewModel、ThreadRepository、GitHub Actions + 截图冒烟",
            )
        ),
        ChangelogEntry(
            version = "1.0.0",
            date = "2026-10-16",
            lines = listOf(
                "首个 Beta：瀑布流首页、多主题（含幻彩流光）、帖子详情全套交互",
                "签到 / 收藏 / 黑名单 / 小黑屋 / 私信 / 提醒中心 / 投票",
                "Cookie 加密存储、崩溃兜底、阅读进度恢复、楼层收藏快照",
            )
        ),
    )
}
