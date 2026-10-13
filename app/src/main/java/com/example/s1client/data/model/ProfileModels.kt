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
    val customStatus: String = "",
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

/**
 * 版块收藏项（R5 新增，对标开源 s1er favoriteMembership 的 forum 分支）。
 * Discuz 收藏是帖子/版块双轨，S1Client 原先只解析了帖子轨。
 */
data class ForumFavoriteItem(
    /** 版块 fid（点击进版块帖子列表用） */
    val fid: String,
    /** 版块名 */
    val title: String,
    /** 收藏记录 id（取消收藏用，解析不到时为空） */
    val favid: String = "",
    /** 收藏时间（页面上的日期串，展示用） */
    val dateline: String = ""
)
