package com.example.s1client.data.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/** 用户资料域：空间资料 / 积分 / 好友 / 用户帖子项 */

data class ProfileResponse(
    @SerializedName("Variables") val variables: ProfileVariables?
)

data class ProfileVariables(
    @SerializedName("space") val space: JsonElement?,
    @SerializedName("extcredits") val extCredits: JsonElement?
)

data class ProfileData(
    val uid: String = "",
    val username: String = "",
    val groupName: String = "",
    val friends: String = "0",
    val threads: String = "0",
    val posts: String = "0",
    val oltime: String = "",
    val regDate: String = "",
    val lastVisit: String = "",
    val lastActivity: String = "",
    val lastPost: String = "",
    /** 自定义头衔（Discuz 的 customstatus，对应网页版「自定义头衔」那一栏） */
    val customStatus: String = "",
    /**
     * 真正的个人签名 —— Discuz 存在 `sightml`，对应网页版「个人签名」栏。
     * 注意：不是 customStatus（那是头衔），此前一直取错导致签名不显示。
     */
    val signature: String = "",
    val credits: String = "",
    val creditItems: List<CreditItem> = emptyList()
)

data class CreditItem(
    val title: String,
    val value: String,
    val unit: String
)

data class FriendItem(
    val uid: String,
    val username: String,
    val note: String = ""
)

data class UserPostItem(
    val tid: String,
    val subject: String,
    val message: String,
    val dateline: String,
    val forum: String = "",
    /** 回复数 / 查看数：从空间页「回复/查看」列盲抓，抓不到就是空串（列表项自动隐藏） */
    val replies: String = "",
    val views: String = "",
    val hasImage: Boolean = false
)
