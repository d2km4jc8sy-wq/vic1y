package com.example.s1client.feature_network

import android.content.Context

/**
 * 网络参数（s1er 对标：网络参数页，阶段四 P8 2026-10-17）。
 *
 * 两条自救路径：
 * - **强制替换基础网址**：整站切到备用域名（如历史上的 bbs.saraba1st.com/2b/）——
 *   域名不同 = Cookie 不互通，切换后需重新登录（s1er 同语义）。
 * - **强制使用 IP 列表**：域名被 DNS 污染时直连真实 IP——实现走 OkHttp Dns 覆盖，
 *   SNI/证书校验仍针对原域名（比 Host 头伪造更稳，s1er 用的是 Host 头方案）。
 *
 * 默认值 = 当前正确数据；改动即时持久化、即时生效（拦截器/Dns 每请求实时读取快照）。
 */
data class NetworkParams(
    val forceBaseUrl: Boolean = false,
    val baseUrl: String = DEFAULT_BASE_URL,
    val forceIp: Boolean = false,
    val ipList: String = DEFAULT_IP_LIST
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://stage1st.com/2b/"
        const val DEFAULT_IP_LIST = "47.97.161.219"
    }
}

object NetworkParamsStore {

    private const val PREFS = "network_params"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun current(context: Context): NetworkParams = NetworkParams(
        forceBaseUrl = prefs(context).getBoolean("force_base_url", false),
        baseUrl = prefs(context).getString("base_url", null) ?: NetworkParams.DEFAULT_BASE_URL,
        forceIp = prefs(context).getBoolean("force_ip", false),
        ipList = prefs(context).getString("ip_list", null) ?: NetworkParams.DEFAULT_IP_LIST
    )

    fun save(context: Context, p: NetworkParams) {
        prefs(context).edit()
            .putBoolean("force_base_url", p.forceBaseUrl)
            .putString("base_url", p.baseUrl)
            .putBoolean("force_ip", p.forceIp)
            .putString("ip_list", p.ipList)
            .apply()
    }

    /** 主域 host（Dns 覆盖与 URL 重写的匹配目标） */
    const val MAIN_HOST = "stage1st.com"
}
