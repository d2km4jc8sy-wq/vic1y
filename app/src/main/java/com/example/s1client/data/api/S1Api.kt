package com.example.s1client.data.api

import com.example.s1client.data.model.*
import okhttp3.ResponseBody
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface S1Api {

    @GET("api/mobile/index.php")
    suspend fun getForumIndex(
        @Query("mobile") mobile: String = "no",
        @Query("version") version: Int = 4,
        @Query("module") module: String = "forumindex"
    ): ForumIndexResponse

    @GET("api/mobile/index.php")
    suspend fun getThreadList(
        @Query("mobile") mobile: String = "no",
        @Query("version") version: Int = 4,
        @Query("module") module: String = "forumdisplay",
        @Query("fid") fid: String,
        @Query("page") page: Int = 1,
        @Query("filter") filter: String? = null,
        @Query("typeid") typeId: String? = null
    ): ThreadListResponse

    @GET("api/mobile/index.php")
    suspend fun getThreadDetail(
        @Query("mobile") mobile: String = "no",
        @Query("version") version: Int = 4,
        @Query("module") module: String = "viewthread",
        @Query("tid") tid: String,
        @Query("page") page: Int = 1,
        @Query("authorid") authorid: String? = null
    ): ThreadDetailResponse

    // ============ 发帖 ============

    /**
     * 取发表页信息：该版面的分类列表（threadtypes）和 formhash。
     * 注意：这个模块**只提供表单信息**，不能用来提交（带 postsubmit 打过去它只回表单 JSON）。
     * 真正的发表走 web 端 forum.php?mod=post&action=newthread，提交标志是 topicsubmit=yes。
     */
    @GET("api/mobile/index.php")
    suspend fun getNewThreadForm(
        @Query("mobile") mobile: String = "no",
        @Query("version") version: Int = 4,
        @Query("module") module: String = "newthread",
        @Query("fid") fid: String
    ): NewThreadResponse

    @FormUrlEncoded
    @POST("api/mobile/index.php")
    suspend fun login(
        @Query("mobile") mobile: String = "no",
        @Query("version") version: Int = 4,
        @Query("module") module: String = "login",
        @Query("loginsubmit") loginsubmit: String = "yes",
        @Query("loginfield") loginfield: String = "auto",
        @Query("submodule") submodule: String = "checkpost",
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("formhash") formhash: String,
        @Field("questionid") questionid: String = "0",
        @Field("answer") answer: String = "",
        @Field("cookietime") cookietime: String = "2592000"   // 对齐 S1er：30 天
    ): LoginResponse

    @GET("api/mobile/index.php")
    suspend fun getProfile(
        @Query("mobile") mobile: String = "no",
        @Query("version") version: Int = 4,
        @Query("module") module: String = "profile",
        @Query("uid") uid: String
    ): ProfileResponse

    /**
     * 回帖。支持引用。
     * @param noticeauthor  引用对象，格式 "=r_作者名_pid"（注意前面有 =）
     * @param noticetrimstr 引用 BBCode 内容
     * @param noticeauthormsg 引用给接收者看的文本片段
     */
    @FormUrlEncoded
    @POST("api/mobile/index.php")
    suspend fun sendReply(
        @Query("mobile") mobile: String = "no",
        @Query("version") version: Int = 4,
        @Query("module") module: String = "sendreply",
        @Query("replysubmit") replysubmit: String = "yes",
        @Field("formhash") formhash: String,
        @Field("tid") tid: String,
        @Field("message") message: String,
        @Field("noticeauthor") noticeauthor: String = "",
        @Field("noticetrimstr") noticetrimstr: String = "",
        @Field("noticeauthormsg") noticeauthormsg: String = "",
        /** 附件挂载等额外字段，如 attachnew[aid][description] */
        @FieldMap attachFields: Map<String, String> = emptyMap()
    ): BaseResponse

    // ============ 收藏 ============

    @GET("home.php")
    suspend fun addFavorite(
        @Query("mod") mod: String = "spacecp",
        @Query("ac") ac: String = "favorite",
        @Query("type") type: String = "thread",
        @Query("id") id: String,
        @Query("formhash") formhash: String,
        @Query("infloat") infloat: String = "yes",
        @Query("handlekey") handlekey: String = "k_favorite",
        @Query("inajax") inajax: String = "1",
        @Query("ajaxtarget") ajaxtarget: String = "fwin_content_k_favorite"
    ): ResponseBody

    @GET("home.php")
    suspend fun removeFavoritePrepare(
        @Query("mod") mod: String = "spacecp",
        @Query("ac") ac: String = "favorite",
        @Query("op") op: String = "delete",
        @Query("type") type: String = "thread",
        @Query("id") id: String,
        @Query("formhash") formhash: String,
        @Query("infloat") infloat: String = "yes",
        @Query("handlekey") handlekey: String = "k_favorite",
        @Query("inajax") inajax: String = "1",
        @Query("ajaxtarget") ajaxtarget: String = "fwin_content_k_favorite"
    ): ResponseBody

    // ============ 每日签到 ============
    // Discuz「每日签到」插件，GET 且带 formhash；**会产生服务端写入**，
    // 所以只在用户明确点击时调用一次，失败不自动重试。
    // 基准实现见开源 s1er：lib/config/api_config.dart dailyAttendanceUrl

    @GET("study_daily_attendance-daily_attendance.html")
    suspend fun dailyAttendance(
        @Query("inajax") inajax: String = "1",
        @Query("formhash") formhash: String
    ): ResponseBody

    // ===== 小黑屋（公开处罚列表）=====
    // 基准：s1er lib/config/api_config.dart darkRoomUrl
    // 游标分页：首屏不带 cid，之后带上页返回的游标。
    @GET("forum.php")
    suspend fun getDarkRoom(
        @Query("mod") mod: String = "misc",
        @Query("action") action: String = "showdarkroom",
        @Query("ajaxdata") ajaxdata: String = "json",
        @Query("cid") cid: String? = null
    ): ResponseBody

    // ===== 删除私信会话（Discuz spacecp） =====
    @FormUrlEncoded
    @POST("home.php")
    suspend fun deletePmConversation(
        @Query("mod") mod: String = "spacecp",
        @Query("ac") ac: String = "pm",
        @Query("op") op: String = "delete",
        @Query("plid") plid: String,
        @Query("touid") touid: String,
        @Query("inajax") inajax: String = "1",
        @Field("pmsubmit") pmsubmit: String = "true",
        @Field("formhash") formhash: String
    ): ResponseBody

    // ===== 加好友（Discuz spacecp，发送好友申请） =====
    @GET("home.php")
    suspend fun addFriend(
        @Query("mod") mod: String = "spacecp",
        @Query("ac") ac: String = "friend",
        @Query("op") op: String = "add",
        @Query("uid") uid: String,
        @Query("formhash") formhash: String,
        @Query("addsubmit") addsubmit: String = "1",
        @Query("inajax") inajax: String = "1"
    ): ResponseBody

    // ===== 发送私信 =====
    @FormUrlEncoded
    @POST("home.php")
    suspend fun sendPm(
        @Query("mod") mod: String = "spacecp",
        @Query("ac") ac: String = "pm",
        @Query("op") op: String = "send",
        @Query("touid") touid: String,
        @Query("inajax") inajax: String = "1",
        @Field("pmsubmit") pmsubmit: String = "true",
        @Field("formhash") formhash: String,
        @Field("handlekey") handlekey: String = "showMsgBox",
        @Field("message") message: String
    ): ResponseBody

}
