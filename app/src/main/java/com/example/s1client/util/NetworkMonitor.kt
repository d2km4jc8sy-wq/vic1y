package com.example.s1client.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 网络计费状态（2026-10-17 点1/点2「仅 Wi-Fi」档的判断源）。
 *
 * metered=true 表示按流量计费（蜂窝），false 表示 Wi-Fi / 免流量网络。
 * registerDefaultNetworkCallback 实时刷新；init 由 S1App 调用一次。
 */
object NetworkMonitor {

    private val _metered = MutableStateFlow(false)
    val metered: StateFlow<Boolean> = _metered.asStateFlow()

    fun init(context: Context) {
        val cm = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        _metered.value = cm.isActiveNetworkMetered
        try {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    _metered.value = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                }
            })
        } catch (_: Exception) {
            // 平台/权限差异：保留初始读取值
        }
    }
}
