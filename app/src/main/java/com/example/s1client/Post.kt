package com.example.s1client

data class Post(
    val id: Int,
    val title: String?,
    val coverUrl: String?,
    val tag: String?,
    val typeId: String? = null,   // 帖子分类 id（threadtype），用于按分类筛选
    val authorName: String?,
    val authorId: String? = null,   // 作者 UID，用于点击头像
    val likes: Int? = null,
    val replies: Int? = null,        // 回复数
    val coverAspect: Float? = null,  // 封面宽高比(w/h)，null 表示无图/未知
    val tid: String = "",
    val timeText: String = "",
    val views: Int? = null,          // 查看数（列表行项显示用）
    val hasImage: Boolean = false    // 是否带图，用于 [图] 徽标
)
