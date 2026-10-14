package com.example.s1client.data.model

import com.google.gson.annotations.SerializedName

/**
 * 提醒列表（Mobile API v3 `module=mynotelist`）的 JSON 响应模型。
 *
 * 基准实现：开源 s1er `lib/services/api_service.dart` 的 `parseNoticeListJson`。
 *
 * 为什么不再抓 HTML：原来的 `home.php?mod=space&do=notice` HTML 里没有稳定的
 * 分页锚点，解析器只能拿到**第一页**（用户反馈过"提醒只显示网页版第一页"）。
 * 这条 JSON 接口在 `Variables` 里给了 `count` / `perpage`，总页数可据此算出，
 * 于是能翻页取到全部提醒。HTML 抓取仍保留作兜底路径。
 *
 * 实测（未登录亦返回结构）：`view=mypost / interactive / system` 三个值接口都接受。
 */
data class NoticeListResponse(
    @SerializedName("Version") val version: String?,
    @SerializedName("Variables") val variables: NoticeListVariables?,
    @SerializedName("Message") val message: NoticeMessage?
)

data class NoticeListVariables(
    /** 本页提醒项 */
    @SerializedName("list") val list: List<NoticeJsonItem>?,
    /** 提醒总数；未登录或异常时为 null */
    @SerializedName("count") val count: String?,
    /** 每页条数（通常 20） */
    @SerializedName("perpage") val perpage: String?,
    /** 服务端回看时的当前页码 */
    @SerializedName("page") val page: String?,
    /** 各类未读计数，可直接驱动角标 */
    @SerializedName("notice") val notice: NoticeUnreadCounts?
)

data class NoticeJsonItem(
    @SerializedName("id") val id: String?,
    @SerializedName("authorid") val authorid: String?,
    @SerializedName("author") val author: String?,
    /** 通知正文（含 <a> 链接的 HTML 片段），需去标签并从中提取 tid */
    @SerializedName("note") val note: String?,
    /** Unix 时间戳（秒） */
    @SerializedName("dateline") val dateline: String?,
    /** "1" = 服务端判定未读 */
    @SerializedName("new") val isNew: String?
)

data class NoticeUnreadCounts(
    @SerializedName("newpush") val newpush: String?,
    @SerializedName("newpm") val newpm: String?,
    @SerializedName("newprompt") val newprompt: String?,
    @SerializedName("newmypost") val newmypost: String?
)

data class NoticeMessage(
    @SerializedName("messageval") val messageval: String?,
    @SerializedName("messagestr") val messagestr: String?
)
