package com.example.s1client.feature_network

import com.example.s1client.data.api.RetrofitClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 网络参数解析的契约（阶段四 P8，2026-10-17）。
 * 锚点：无效组合 → null（调用方回落主域）；合法组合 → host/scheme/port/IP 精确解析。
 */
class NetworkParamsTest {

    @Test
    fun `全关时返回空覆盖_即全默认直连主域`() {
        val o = RetrofitClient.parseNetworkOverride(
            forceBaseUrl = false, baseUrlRaw = "https://bbs.saraba1st.com/2b/",
            forceIp = false, ipRaw = "47.97.161.219"
        )
        assertTrue(o != null)
        assertFalse(o!!.forceBaseUrl)
        assertFalse(o.forceIp)
        assertEquals(emptyList<String>(), o.ips)
    }

    @Test
    fun `基础网址解析出scheme_host_port`() {
        val o = RetrofitClient.parseNetworkOverride(
            forceBaseUrl = true, baseUrlRaw = "https://bbs.saraba1st.com/2b/",
            forceIp = false, ipRaw = ""
        )!!
        assertEquals("bbs.saraba1st.com", o.baseUrlHost)
        assertEquals("https", o.baseUrlScheme)
        assertEquals(443, o.baseUrlPort)
        assertTrue(o.forceBaseUrl)
    }

    @Test
    fun `带端口的网址解析端口`() {
        val o = RetrofitClient.parseNetworkOverride(
            forceBaseUrl = true, baseUrlRaw = "http://10.0.0.8:8080/2b/",
            forceIp = false, ipRaw = ""
        )!!
        assertEquals("10.0.0.8", o.baseUrlHost)
        assertEquals("http", o.baseUrlScheme)
        assertEquals(8080, o.baseUrlPort)
    }

    @Test
    fun `强制开启但网址无效返回null_回落主域`() {
        assertNull(RetrofitClient.parseNetworkOverride(
            forceBaseUrl = true, baseUrlRaw = "不是网址",
            forceIp = false, ipRaw = ""
        ))
        assertNull(RetrofitClient.parseNetworkOverride(
            forceBaseUrl = true, baseUrlRaw = "",
            forceIp = false, ipRaw = ""
        ))
    }

    @Test
    fun `IP列表按逗号切分并去空白`() {
        val o = RetrofitClient.parseNetworkOverride(
            forceBaseUrl = false, baseUrlRaw = "",
            forceIp = true, ipRaw = "47.97.161.219, 114.114.114.114 ,, 8.8.8.8"
        )!!
        assertEquals(listOf("47.97.161.219", "114.114.114.114", "8.8.8.8"), o.ips)
        assertTrue(o.forceIp)
    }

    @Test
    fun `强制IP但列表为空返回null_回落系统DNS`() {
        assertNull(RetrofitClient.parseNetworkOverride(
            forceBaseUrl = false, baseUrlRaw = "",
            forceIp = true, ipRaw = " , , "
        ))
    }

    @Test
    fun `默认值等于当前正确数据`() {
        val d = com.example.s1client.feature_network.NetworkParams()
        assertEquals("https://stage1st.com/2b/", d.baseUrl)
        assertEquals("47.97.161.219", d.ipList)
        assertFalse(d.forceBaseUrl)
        assertFalse(d.forceIp)
    }
}
