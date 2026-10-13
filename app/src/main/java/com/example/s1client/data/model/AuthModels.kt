package com.example.s1client.data.model

import com.google.gson.annotations.SerializedName

/** 账户与通用响应域：登录 / 基础操作响应 / 服务端消息体 */

data class LoginResponse(
    @SerializedName("Variables") val variables: LoginVariables?,
    /** 登录失败时服务端会在这里给出原因（如"安全提问答案错误"） */
    @SerializedName("Message") val message: MessageInfo?
)

data class LoginVariables(
    @SerializedName("member_uid") val memberUid: String?,
    @SerializedName("member_username") val memberUsername: String?,
    @SerializedName("formhash") val formhash: String?,
    @SerializedName("member_credits") val memberCredits: String?,
    @SerializedName("groupid") val groupId: String?,
    @SerializedName("group") val group: GroupInfo?
)

data class BaseResponse(
    @SerializedName("Variables") val variables: BaseVariables?,
    /** 服务端提示放这里：messageval 是代码（如 post_sm_isnull），messagestr 是给人看的文案 */
    @SerializedName("Message") val message: MessageInfo?
)

data class BaseVariables(
    @SerializedName("message") val message: String?,
    @SerializedName("formhash") val formhash: String?,
    @SerializedName("tid") val tid: String?,
    /** 回帖成功时是新楼层 pid，失败时为 "0" */
    @SerializedName("pid") val pid: String?
)

/** Discuz 通用消息体：messageval 是代码，messagestr 是给人看的文案 */
data class MessageInfo(
    @SerializedName("messageval") val messageVal: String?,
    @SerializedName("messagestr") val messageStr: String?
)
