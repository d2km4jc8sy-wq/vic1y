package com.example.s1client.data.store

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 省流模式开关（首页瀑布流 & 「我的」齿轮面板共用同一份状态）。
 *
 * 主页的开关原先只存在 PostViewModel 的内存 StateFlow 里，从「我的」改就会和首页不同步，
 * 所以抽成全局单例：两处读同一个 Flow，改一次两边同时生效。
 */
object DataSaverStore {

    private const val PREF = "s1_home_tabs"
    private const val KEY = "data_saver"

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun init(context: Context) {
        _enabled.value = prefs(context).getBoolean(KEY, false)
    }

    fun set(context: Context, value: Boolean) {
        if (_enabled.value == value) return
        _enabled.value = value
        prefs(context).edit().putBoolean(KEY, value).apply()
    }

    fun isOn(): Boolean = _enabled.value

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
}
