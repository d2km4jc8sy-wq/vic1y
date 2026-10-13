package com.example.s1client.data.model

/** 私信域：会话列表项 / 消息体 */

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
