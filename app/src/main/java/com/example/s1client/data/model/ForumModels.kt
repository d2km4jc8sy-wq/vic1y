package com.example.s1client.data.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

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

// ===== 发帖 =====

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

data class MessageInfo(
    @SerializedName("messageval") val messageVal: String?,
    @SerializedName("messagestr") val messageStr: String?
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

// ===== Profile =====

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


// ===== 私信 =====

data class PmListItem(
    val plid: String,
    val touid: String,
    val username: String,
    val lastMessage: String,
    val lastTime: String,
    val msgCount: String,
    val isNew: Boolean
)

data class PmMessage(
    val pmid: String,
    val author: String,
    val message: String,
    val dateline: String,
    val isSelf: Boolean
)
