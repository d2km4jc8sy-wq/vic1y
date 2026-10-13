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

    // ============ Discuz showmessage 页 ============
    // 签到插件并不总回 succeedhandle_/errorhandle_：真机上实测到它返回一张
    // showmessage 页，被 XML 外壳 + CDATA 包着。以前缺这一支，解析落到 UNKNOWN，
    // 把整段 XML 截 120 字当错误弹给用户（表现为「点签到弹一坨乱码」）。

    /** 真实响应形状：`<?xml?><root><![CDATA[ <h3 class="flb">…<div id="messagetext">… ]]></root>` */
    private fun xmlWrapped(inner: String): String =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<root><![CDATA[$inner]]></root>"

    @Test
    fun `showmessage 成功页判为签到成功`() {
        val inner = """<h3 class="flb"><em>提示信息</em><span><a href="javascript:;" class="flbc">关闭</a></span></h3>""" +
                """<div id="messagetext" class="alert_right"><p>签到成功，获得 2 金币</p></div>"""
        val r = AttendanceManager.parseResponse(xmlWrapped(inner))
        assertEquals(AttendanceManager.Outcome.SIGNED_NOW, r.outcome)
        // 必须提出干净正文：不能把 HTML 源码当消息丢给用户（真机踩过这个坑）
        assertEquals("签到成功，获得 2 金币", r.message)
        assertTrue("不该残留标签，实际=${r.message}", !r.message.contains("<"))
    }

    @Test
    fun `showmessage 已签到页判为已签到`() {
        val inner = """<div id="messagetext" class="alert_error"><p>您今天已经签到过了</p></div>"""
        val r = AttendanceManager.parseResponse(xmlWrapped(inner))
        assertEquals(AttendanceManager.Outcome.ALREADY_SIGNED, r.outcome)
        assertEquals("您今天已经签到过了", r.message)
    }

    @Test
    fun `showmessage 表单验证串错误判为失败`() {
        val inner = """<div id="messagetext" class="alert_error"><p>您的请求来路不正确或表单验证串不符，无法提交</p></div>"""
        val r = AttendanceManager.parseResponse(xmlWrapped(inner))
        assertEquals(AttendanceManager.Outcome.FAILED, r.outcome)
        assertEquals("您的请求来路不正确或表单验证串不符，无法提交", r.message)
    }

    @Test
    fun `showmessage 正文不该混入标题与按钮噪声`() {
        val inner = """<h3 class="flb"><em>提示信息</em><span><a href="javascript:;" class="flbc">关闭</a></span></h3>""" +
                """<div class="c"><div class="alert_error"><p>已签到,请不要重新签到！</p></div></div>""" +
                """<p class="o pns"><button id="closebtn"><strong>确定</strong></button></p>"""
        val r = AttendanceManager.parseResponse(xmlWrapped(inner))
        assertTrue("不应含「提示信息」，实际=${r.message}", !r.message.contains("提示信息"))
        assertTrue("不应含「关闭」，实际=${r.message}", !r.message.contains("关闭"))
        assertTrue("不应含「确定」，实际=${r.message}", !r.message.contains("确定"))
    }

    /**
     * 真机实测响应（一加13T，2026-10-14）。服务端对"今天已签过"的真实回包：
     * showmessage 页 + alert_error + 内嵌 errorhandle_ 的 script。
     * 这条锁住「已签到」不能被误报成失败、也不能把 XML 源码当消息弹出来。
     */
    @Test
    fun `真机实测的已签到响应`() {
        val body = """<?xml version="1.0" encoding="utf-8"?>
            |<root><![CDATA[<h3 class="flb"><em>提示信息</em><span><a href="javascript:;" class="flbc" onclick="hideWindow('');" title="关闭">关闭</a></span></h3>
            |<div class="c altw">
            |<div class="alert_error">已签到,请不要重新签到！<script type="text/javascript" reload="1">if(typeof errorhandle_=='function') {errorhandle_('已签到,请不要重新签到！', {});}</script></div>
            |</div>
            |<p class="o pns">
            |<button type="button" class="pn pnc" id="closebtn" onclick="hideWindow('');"><strong>确定</strong></button>
            |<script type="text/javascript" reload="1">if($('closebtn')) {$('closebtn').focus();}</script>
            |</p>
            |]]></root>""".trimMargin()
        val r = AttendanceManager.parseResponse(body)
        assertEquals(AttendanceManager.Outcome.ALREADY_SIGNED, r.outcome)
        assertEquals("已签到,请不要重新签到！", r.message)
    }
}
