package com.example.s1client.fake

import com.example.s1client.data.api.S1Api
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.data.model.LoginResponse
import com.example.s1client.data.model.LoginVariables
import com.example.s1client.data.model.MessageInfo
import com.example.s1client.data.model.GroupInfo
import io.mockk.coEvery
import io.mockk.mockk

/**
 * FakeAuthRepository（仅测试目录，P3 拍板方案）
 *
 * 用 MockK 伪造登录态：
 * 1. mock S1Api.login 端点，按预设返回"登录成功/失败"响应；
 * 2. 通过 ForumDataCache 公共 API 预置登录态（不碰生产代码）。
 *
 * 依赖注入方式：测试里构造被测对象时把 mockS1Api 传进去（或直接使用 apply() 预置状态）。
 */
class FakeAuthRepository(
    /** true = 预设"登录成功"；false = 预设"登录失败" */
    private val loggedIn: Boolean,
    val mockS1Api: S1Api = mockk(relaxed = true),
    val username: String = "fixture_user",
    val uid: String = "229182"
) {
    /** 预设 S1Api.login 的返回：成功给出 Variables + succeed 文案；失败给出错误文案 */
    fun stubLoginEndpoint() {
        val response = if (loggedIn) {
            LoginResponse(
                variables = LoginVariables(
                    memberUid = uid,
                    memberUsername = username,
                    formhash = "fixture_hash",
                    memberCredits = "1234",
                    groupId = "17",
                    group = GroupInfo(groupId = "17", groupTitle = "正式会员")
                ),
                message = MessageInfo(
                    messageVal = "login_succeed",
                    messageStr = "登录成功"
                )
            )
        } else {
            LoginResponse(
                variables = LoginVariables(
                    memberUid = "0",
                    memberUsername = "",
                    formhash = "fixture_hash",
                    memberCredits = "0",
                    groupId = "7",
                    group = null
                ),
                message = MessageInfo(
                    messageVal = "login_invalid",
                    messageStr = "登录失败,您还可以尝试 4 次"
                )
            )
        }
        coEvery { mockS1Api.login(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns response
    }

    /** 把登录态写入 ForumDataCache（走公共 API，等价于登录成功后生产代码的行为） */
    fun applyLoginState() {
        if (loggedIn) {
            ForumDataCache.memberUid = uid
            ForumDataCache.memberUsername = username
            ForumDataCache.memberCredits = "1234"
            ForumDataCache.groupId = "17"
            ForumDataCache.groupTitle = "正式会员"
            ForumDataCache.formhash = "fixture_hash"
            ForumDataCache.saveUser()
        } else {
            ForumDataCache.clearUser()
        }
    }

    /** 一站式：stub 端点 + 预置状态 */
    fun install() {
        stubLoginEndpoint()
        applyLoginState()
    }
}
