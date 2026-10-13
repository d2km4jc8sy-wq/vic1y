package com.example.s1client.data.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/** 帖子域：发帖响应 / 帖子 / 帖子详情与楼层 */

data class NewThreadResponse(
    @SerializedName("Variables") val variables: NewThreadVariables?,
    @SerializedName("Message") val message: MessageInfo?
)

data class NewThreadVariables(
    @SerializedName("formhash") val formhash: String?,
    @SerializedName("threadtypes") val threadTypes: ThreadTypes?,
    /** 发表成功后服务端返回的新帖 tid */
    @SerializedName("tid") val tid: String?,
    @SerializedName("pid") val pid: String?
)

data class Thread(
    @SerializedName("tid") val tid: String?,
    @SerializedName("subject") val subject: String?,
    @SerializedName("author") val author: String?,
    @SerializedName("authorid") val authorId: String?,
    @SerializedName("replies") val replies: String?,
    @SerializedName("views") val views: String?,
    /** 附件数量，>0 表示带图（列表行项的 [图] 徽标用） */
    @SerializedName("attachment") val attachment: String? = null,
    /**
     * 帖子内图片附件的预览信息。服务端可能返回空数组占位，
     * 故用 JsonElement 承载，取值时自行判断是否为对象。
     */
    @SerializedName("attachmentImagePreviewList") val attachmentImagePreviewList: List<JsonElement>?,
    @SerializedName(value = "dateline", alternate = ["dbdateline", "lastpost", "lastpost_dateline"])
    val dateline: String?,
    @SerializedName("displayorder") val displayOrder: String?,
    @SerializedName(value = "typeid", alternate = ["typeId", "threadtypeid"])
    val typeId: String?,
    @SerializedName(value = "typename", alternate = ["typeName"])
    val typeName: String?
)

data class ThreadDetailResponse(
    @SerializedName("Variables") val variables: ThreadDetailVariables?
)

data class ThreadDetailVariables(
    @SerializedName("thread") val thread: ThreadDetail?,
    @SerializedName("postlist") val postList: List<Post>?,
    @SerializedName("formhash") val formhash: String?
)

data class ThreadDetail(
    @SerializedName("tid") val tid: String?,
    @SerializedName("fid") val fid: String?,
    @SerializedName("subject") val subject: String?,
    @SerializedName("author") val author: String?,
    /** 回复数，配合 ppp 算总页数 */
    @SerializedName("replies") val replies: String? = null,
    /** 每页楼层数（服务端返回，一般是 40） */
    @SerializedName("ppp") val ppp: String? = null
)

data class Post(
    @SerializedName("pid") val pid: String?,
    @SerializedName("author") val author: String?,
    @SerializedName("authorid") val authorId: String?,
    @SerializedName(value = "dateline", alternate = ["dbdateline"])
    val dateline: String?,
    @SerializedName("message") val message: String?,
    @SerializedName(value = "position", alternate = ["floor", "postposition"])
    val position: String?,
    @SerializedName("first") val first: String?
)
