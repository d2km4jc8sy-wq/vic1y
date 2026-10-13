package com.example.s1client.util

import com.example.s1client.data.store.AttendanceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 签到响应解析测试。
 *
 * Discuz 把消息包在 `succeedhandle_k_attendance(['<b>签到成功</b>',1])` 这类 JS 里，
 * 参数里还可能带嵌套括号与引号，切分逻辑很容易错 —— 这里锁住。
 */
class AttendanceManagerParseTest {

    @Test
    fun `解析签到成功`() {
        val body = """succeedhandle_k_attendance(['<b>签到成功</b>',1])"""
        val r = AttendanceManager.parseResponse(body)
        assertEquals(AttendanceManager.Outcome.SIGNED_NOW, r.outcome)
        assertEquals("签到成功", r.message)
    }

    @Test
    fun `解析今日已签到`() {
        val body = """succeedhandle_k_attendance(['今日已签到',1])"""
        val r = AttendanceManager.parseResponse(body)
        assertEquals(AttendanceManager.Outcome.ALREADY_SIGNED, r.outcome)
    }

    @Test
    fun `解析失败提示`() {
        val body = """errorhandle_k_attendance(['<b>您还没有登录</b>',0])"""
        val r = AttendanceManager.parseResponse(body)
        assertEquals(AttendanceManager.Outcome.FAILED, r.outcome)
    }

    @Test
    fun `errorhandle 里的已签到也判定为已签到`() {
        // 服务端把"今天已经签过了"放在 errorhandle 分支里，仍应判定为已签到而非失败
        val body = """errorhandle_k_attendance(['今日已签到，请明天再来',0])"""
        val r = AttendanceManager.parseResponse(body)
        assertEquals(AttendanceManager.Outcome.ALREADY_SIGNED, r.outcome)
    }

    @Test
    fun `消息里的逗号不被误切`() {
        // "恭喜您，获得随机奖励 5,10 积分" —— 逗号在引号内，不能当参数分隔符
        val body = """succeedhandle_k_attendance(['恭喜您，获得随机奖励 5,10 积分',1])"""
        val r = AttendanceManager.parseResponse(body)
        assertEquals(AttendanceManager.Outcome.SIGNED_NOW, r.outcome)
        assertTrue(r.message.contains("积分"))
        assertEquals("恭喜您，获得随机奖励 5,10 积分", r.message)
    }

    @Test
    fun `消息里的嵌套括号不被误切`() {
        val body = """succeedhandle_k_attendance(['获得奖励（金币 x2）',1])"""
        val r = AttendanceManager.parseResponse(body)
        assertEquals(AttendanceManager.Outcome.SIGNED_NOW, r.outcome)
        assertEquals("获得奖励（金币 x2）", r.message)
    }

    @Test
    fun `去掉 html 标签`() {
        val body = """succeedhandle_k_attendance(['<b><i>签到成功</i></b>',1])"""
        val r = AttendanceManager.parseResponse(body)
        assertEquals("签到成功", r.message)
    }

    @Test
    fun `被 ajax 外壳包住也能解析`() {
        val body =
            """<div class="alert_info">succeedhandle_k_attendance(['签到成功',1])</div>"""
        val r = AttendanceManager.parseResponse(body)
        assertEquals(AttendanceManager.Outcome.SIGNED_NOW, r.outcome)
    }

    @Test
    fun `空响应为 unknown`() {
        assertEquals(AttendanceManager.Outcome.UNKNOWN, AttendanceManager.parseResponse("").outcome)
        assertEquals(
            AttendanceManager.Outcome.UNKNOWN,
            AttendanceManager.parseResponse("   ").outcome
        )
    }

    @Test
    fun `看不懂的响应为 unknown 而非崩溃`() {
        val r = AttendanceManager.parseResponse("<html>随便什么内容</html>")
        assertEquals(AttendanceManager.Outcome.UNKNOWN, r.outcome)
    }

    @Test
    fun `服务端各种已签到措辞都能识别`() {
        // 服务端措辞不固定，任一种都不能误报成失败
        val variants = listOf(
            "已签到",
            "今日已签到",
            "今天已经签到过了",
            "重复签到",
            "今天已签到，请明天再来"
        )
        for (v in variants) {
            val r = AttendanceManager.parseResponse("""succeedhandle_k_attendance(['$v',1])""")
            assertEquals("措辞「$v」应判为已签到", AttendanceManager.Outcome.ALREADY_SIGNED, r.outcome)
        }
    }

    @Test
    fun `真正成功的文案不被误判为已签到`() {
        val r = AttendanceManager.parseResponse("""succeedhandle_k_attendance(['签到成功，获得随机奖励',1])""")
        assertEquals(AttendanceManager.Outcome.SIGNED_NOW, r.outcome)
    }

    @Test
    fun `空消息也能判定状态`() {
        // succeedhandle_k_attendance(['',1]) → 签到了但没文案
        val r = AttendanceManager.parseResponse("""succeedhandle_k_attendance(['',1])""")
        assertEquals(AttendanceManager.Outcome.SIGNED_NOW, r.outcome)
        assertEquals("签到成功", r.message)
    }
}
