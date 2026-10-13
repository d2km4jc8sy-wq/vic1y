package com.example.s1client.util

import org.jsoup.nodes.Element

/** HTML 解析共享件：导航词过滤 + Discuz 链接/日期提取 */

internal val HTML_NAV_WORDS = setOf(
    "查看资料", "去串个门", "打个招呼", "发送消息", "互动", "管理",
    "首页", "论坛", "登录", "注册", "返回", "查看", "删除", "备注",
    "好友分组", "更多", "下一页", "上一页", "收听TA", "取消收听",
    "我的", "设置", "消息", "提醒", "退出", "积分", "用户组",
    "全部收藏", "帖子", "版块", "归档", "收藏"
)

internal fun extractUidFromSpaceUrl(href: String): String? {
    Regex("""space-uid-(\d+)""").find(href)?.groupValues?.get(1)?.let { return it }
    Regex("""[?&]uid=(\d+)""").find(href)?.groupValues?.get(1)?.let { return it }
    return null
}

internal fun extractTid(href: String): String? {
    Regex("""thread-(\d+)""").find(href)?.groupValues?.get(1)?.let { return it }
    Regex("""p?tid[=\-](\d+)""").find(href)?.groupValues?.get(1)?.let { return it }
    Regex("""viewthread[^"]*?tid[=\-](\d+)""").find(href)?.groupValues?.get(1)?.let { return it }
    return null
}

internal fun extractTidFromRow(row: Element): String? {
    row.selectFirst("td.icn a[href*=tid]")?.let {
        extractTid(it.attr("href"))?.let { tid -> return tid }
    }
    return null
}

internal fun extractDateFromText(text: String): String {
    // 匹配 "2026-5-28 22:40" 或 "2026-05-28" 之类
    val m = Regex("""\d{4}-\d{1,2}-\d{1,2}(\s+\d{1,2}:\d{2})?""").find(text)
    return m?.value ?: ""
}
