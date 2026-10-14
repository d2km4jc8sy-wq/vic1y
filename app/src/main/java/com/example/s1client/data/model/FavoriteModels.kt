package com.example.s1client.data.model

import com.google.gson.annotations.SerializedName

/**
 * 收藏列表（Mobile API `module=myfavthread`）的 JSON 响应模型。
 *
 * 基准实现：开源 s1er `api_service._getFavoriteThreadListJson` +
 * `lib/models/favorite_item.dart` 的 `FavoriteItem.fromThreadJson`。
 *
 * 为什么不用 HTML 抓取：那条路（`home.php?do=favorite&view=me`）只能稳定拿到
 * 标题和时间，**版块名 / 浏览数 / 回复数 / 分页全都拿不到** —— 所以收藏页的
 * 「💬 0」是假的（不是没人回，是压根没解析到），版块名也一直空着。
 * 这条 JSON 接口把这些字段全都给了。
 *
 * 注意：接口**不返回作者**（HTML 和 JSON 两条路都没有，s1er 同样没做），
 * 所以收藏列表不显示作者行。
 */
data class FavoriteListResponse(
    @SerializedName("Version") val version: String?,
    @SerializedName("Variables") val variables: FavoriteVariables?,
    @SerializedName("Message") val message: FavoriteMessage?
)

data class FavoriteVariables(
    @SerializedName("list") val list: List<FavoriteJsonItem>?,
    @SerializedName("perpage") val perpage: String?,
    @SerializedName("count") val count: String?
)

data class FavoriteJsonItem(
    @SerializedName("favid") val favid: String?,
    /** idtype = thread / forum，用来区分收藏的是帖子还是版块 */
    @SerializedName("idtype") val idtype: String?,
    @SerializedName("tid") val tid: String?,
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("subject") val subject: String?,
    @SerializedName("replies") val replies: String?,
    @SerializedName("dateline") val dateline: String?,
    @SerializedName("dbdateline") val dbdateline: String?
)

data class FavoriteMessage(
    @SerializedName("messageval") val messageval: String?,
    @SerializedName("messagestr") val messagestr: String?
)
