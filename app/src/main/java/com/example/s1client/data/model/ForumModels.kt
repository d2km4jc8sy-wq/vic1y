package com.example.s1client.data.model

import com.google.gson.annotations.SerializedName

/** 论坛首页索引域：版块列表 / 分类 / 当前用户摘要 */

data class ForumIndexResponse(
    @SerializedName("Variables") val variables: ForumIndexVariables?
)

data class ForumIndexVariables(
    @SerializedName("forumlist") val forumList: List<SubForum>?,
    @SerializedName("catlist") val catList: List<ForumCategory>?,
    @SerializedName("formhash") val formhash: String?,
    @SerializedName("member_uid") val memberUid: String?,
    @SerializedName("member_username") val memberUsername: String?,
    @SerializedName("member_credits") val memberCredits: String?,
    @SerializedName("groupid") val groupId: String?,
    @SerializedName("group") val group: GroupInfo?
)

data class GroupInfo(
    @SerializedName("groupid") val groupId: String?,
    @SerializedName("grouptitle") val groupTitle: String?
)

data class ForumCategory(
    @SerializedName("fid") val fid: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("forums") val forumIds: List<String>?
)

data class SubForum(
    @SerializedName("fid") val fid: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("threads") val threadCount: String?,
    @SerializedName("posts") val postCount: String?,
    @SerializedName("todayposts") val todayPosts: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("sublist") val subList: List<SubForum>?
)
