package com.example.s1client.data.session

import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.data.model.SubForum

/**
 * 会话状态的唯一写入口（阶段二 SessionRepository，2026-10-16）。
 *
 * ForumDataCache 仍是**存储与读取面**（内部 StateFlow-backed，读取不受影响）；
 * 本对象只收编「谁在什么时机写什么」：登录采纳、formhash 回填、资料刷新、版块树刷新。
 * 此前 formhash 有 4 个直写点、资料字段散在各 Activity——正是「拆东补西」的结构根源，
 * 收敛后全项目对会话字段的赋值只发生在本文件内（grep `ForumDataCache.formhash =` 仅此处命中）。
 *
 * 各方法的守卫条件与原调用点逐字一致，行为不变；唯一收紧见 [refreshFormhash]。
 */
object SessionRepository {

    /**
     * 登录成功：整包采纳登录响应的会话字段并落盘。
     * 参数的兜底逻辑（用户名取响应值否则输入名、formhash 取响应值否则预取值）
     * 保留在调用方（LoginActivity.doLogin），本方法只负责写入与持久化。
     */
    fun adoptLogin(
        uid: String,
        username: String,
        formhash: String,
        credits: String,
        groupId: String,
        groupTitle: String
    ) {
        ForumDataCache.memberUid = uid
        ForumDataCache.memberUsername = username
        ForumDataCache.formhash = formhash
        ForumDataCache.memberCredits = credits
        ForumDataCache.groupId = groupId
        ForumDataCache.groupTitle = groupTitle
        ForumDataCache.saveUser()
    }

    /**
     * formhash 回填：forumindex / 详情响应里顺带拿到的页面级令牌。
     * 守卫：空与空白不覆盖——相比原 ThreadDetailShareNav 的无条件写入
     * （空串会清掉有效缓存），这里收紧为「只在有值时写」，其余调用点守卫逐字一致。
     */
    fun refreshFormhash(fromForumIndex: String?) {
        if (!fromForumIndex.isNullOrBlank()) {
            ForumDataCache.formhash = fromForumIndex
        }
    }

    /**
     * 「我的」页资料刷新：注册日期 / 头衔（customstatus）/ 签名（sightml）。
     * 守卫与原 MineActivity 逐字一致（regDate 用 isNotBlank、头衔签名用 isNotEmpty），
     * 三个字段互不干扰，写入后统一落盘。
     */
    fun refreshProfile(regDate: String, groupTitle: String, signature: String) {
        if (regDate.isNotBlank()) ForumDataCache.memberRegDate = regDate
        if (groupTitle.isNotEmpty()) ForumDataCache.groupTitle = groupTitle
        if (signature.isNotEmpty()) ForumDataCache.memberSignature = signature
        ForumDataCache.saveUser()
    }

    /** 版块树刷新：forumindex 响应喂进全局缓存（详情页品牌行 / 发帖选版共用） */
    fun refreshForumTree(raw: List<SubForum>) {
        if (raw.isNotEmpty()) {
            ForumDataCache.forumList = raw
        }
    }
}
