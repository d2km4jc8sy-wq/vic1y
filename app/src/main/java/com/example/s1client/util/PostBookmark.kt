package com.example.s1client.util

/**
 * 本地收藏的楼层快照。
 *
 * 存的是点击「收藏」那一刻的完整状态：作者、楼号、标题、原文、时间、头像链接、楼层直达链接。
 * 即使原帖后来被删除/编辑，本地快照仍然保留，点进详情能完整回看。
 */
data class PostBookmark(
    /** 内部唯一 id（UUID） */
    val id: String,
    /** 帖子 tid */
    val tid: String,
    /** 楼层 pid */
    val pid: String?,
    /** 楼号（如 "8"） */
    val position: String?,
    /** 作者名 */
    val author: String?,
    /** 作者 uid */
    val authorId: String?,
    /** 帖子标题 */
    val subject: String,
    /** 原始 message（含 BBCode / HTML 标签） */
    val message: String?,
    /** 解析后的纯文本（去掉表情占位符） */
    val plainText: String,
    /** 服务端原始时间戳字符串 */
    val dateline: String?,
    /** 收藏时展示用的时间文本 */
    val timeText: String,
    /** 头像链接 */
    val avatarUrl: String?,
    /** 楼层直达链接 */
    val floorUrl: String,
    /** 收藏时间 */
    val createdAt: Long
)
