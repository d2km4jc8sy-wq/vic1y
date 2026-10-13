package com.example.s1client.data.model

import com.google.gson.annotations.SerializedName

/**
 * 小黑屋（公开处罚列表）条目。
 *
 * 字段口径抄自开源 s1er `lib/models/dark_room_entry.dart`，不自行增删。
 */
data class DarkRoomEntry(
    /** 处罚记录 id，翻页游标 */
    @SerializedName("cid") val cid: String = "",
    /** 被处罚者 uid */
    @SerializedName("uid") val uid: String = "",
    @SerializedName("username") val username: String = "",
    /** 处罚人 */
    @SerializedName("operatorid") val operatorId: String = "",
    @SerializedName("operator") val operatorName: String = "",
    /** 处罚动作，如「禁止发言」 */
    @SerializedName("action") val action: String = "",
    /** 处罚理由（论坛公开，可能包含粗暴用词，展示时不做额外处理） */
    @SerializedName("reason") val reason: String = "",
    @SerializedName("dateline") val datelineRaw: String = "",
    /** 处罚到期时间，「永不过期」或具体日期 */
    @SerializedName("groupexpiry") val groupExpiryRaw: String = ""
) {
    val dateline: String get() = datelineRaw.trim()
    val groupExpiry: String get() = groupExpiryRaw.trim()
    val neverExpires: Boolean get() = groupExpiry.contains("永不过期") || groupExpiry.isBlank()
}
