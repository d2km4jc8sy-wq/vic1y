package com.example.s1client.util

/**
 * HTML 实体反转义（R11 · 审计报告 v2 2026-10-18 第 9 轮）。
 *
 * 背景：服务端 JSON 返回的标题/摘要带转义实体，实测 feed 卡片把「&」显示成「&amp;」
 * （小米 10 截图 mi_home：「【集中楼】百台&amp;泛泛合泥地摔跤专楼」）。
 * 此前全项目靠 5+ 处手工 `.replace("&amp;","&")` 逐处打补丁，形成三种不一致实现：
 *   ① 只解 `&amp;`（FavoriteManager / PmDetailActivity / NoticeJsonParser.allHrefs）
 *   ② 解 5 种但 `&amp;` 前置（FavoriteJsonParser.stripTags / NoticeJsonParser.stripHtml —— 有双重解码隐患）
 *   ③ 解 6 种且 `&amp;` 后置，顺序正确（PostContentParser.parse / RatingParser.decode —— 正确范本）
 *
 * 现统一收口到本函数。模型边界（[com.example.s1client.PostViewModel] 的 toPost /
 * UserPostItem.toPost）调用一次，全链受益，新增展示位置自动正确。
 *
 * **顺序是关键**：`&amp;` 必须放在最后解 —— 否则 `&amp;lt;` 会先被解成 `&lt;` 再被解成 `<`，
 * 即双重解码，把用户故意输入的字面量吃成真标签。口径对齐 PostContentParser.parse。
 *
 * 返回可空：调用方多为 `subject`（可空字段），保留 null 语义由调用处处理默认值。
 */
fun unescapeHtmlEntities(s: String?): String? {
    if (s.isNullOrEmpty()) return s
    return s
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&") // 必须最后：防 &amp;lt; → &lt; → < 双重解码
}
