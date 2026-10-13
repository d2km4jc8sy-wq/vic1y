package com.example.s1client.util

/**
 * HTML 解析门面：保留原调用点 API 不变。
 * 实际解析按页面域拆分在同包的 FriendParser / UserPostParser / SearchParser / PmParser。
 * 阶段 2 迁移调用点后此门面可退役。
 */
object HtmlParser {
    fun parseFriendList(html: String): List<com.example.s1client.data.model.FriendItem> =
        FriendParser.parseFriendList(html)

    fun parseThreadList(html: String): List<com.example.s1client.data.model.UserPostItem> =
        UserPostParser.parseThreadList(html)

    fun parseReplyList(html: String): List<com.example.s1client.data.model.UserPostItem> =
        UserPostParser.parseReplyList(html)

    fun parseFavoriteList(html: String): List<com.example.s1client.data.model.UserPostItem> =
        UserPostParser.parseFavoriteList(html)

    fun parseSearchResult(html: String): List<com.example.s1client.data.model.Thread> =
        SearchParser.parseSearchResult(html)

    fun parsePmList(html: String): List<com.example.s1client.data.model.PmListItem> =
        PmParser.parsePmList(html)

    fun parsePmConversation(html: String): List<com.example.s1client.data.model.PmMessage> =
        PmParser.parsePmConversation(html)
}
