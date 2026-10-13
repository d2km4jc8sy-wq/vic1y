package com.example.s1client.data.model

import com.google.gson.annotations.SerializedName

/** 版块帖子列表域：列表响应 / 版块摘要 / 帖子分类 */

data class ThreadListResponse(
    @SerializedName("Variables") val variables: ThreadListVariables?
)

data class ThreadListVariables(
    @SerializedName("forum_threadlist") val threadList: List<Thread>?,
    @SerializedName("threadtypes") val threadTypes: ThreadTypes?,
    @SerializedName("formhash") val formhash: String?,
    /** 版块信息，里面的 threads 是版块主题总数（用来算真实总页数） */
    @SerializedName("forum") val forum: ForumBrief?,
    /** 每页主题数（Discuz 的 tpp，动漫论坛实测为 50） */
    @SerializedName("tpp") val tpp: String?
)

data class ForumBrief(
    /** 该版块主题总数 */
    @SerializedName("threads") val threads: String?,
    @SerializedName("name") val name: String?
)

data class ThreadTypes(
    @SerializedName("types") val types: Map<String, String>?,
    /** "1" 表示该版面发帖必须选分类 */
    @SerializedName("required") val required: String? = null
)
