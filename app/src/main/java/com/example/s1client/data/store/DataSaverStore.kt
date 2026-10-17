package com.example.s1client.data.store

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 图片加载策略（2026-10-17 点1/点2：省流开关升级三档）。
 *
 * ALWAYS 始终加载（默认值，与历史"省流关"行为一致）；
 * WIFI_ONLY 仅 Wi-Fi（流量计费网络不自动加载）；
 * MANUAL 手动（不自动加载 = 原"省流开"行为，封面显示文字卡，进详情看图）。
 */
enum class ImageLoadPolicy {
    ALWAYS,
    WIFI_ONLY,
    MANUAL;

    fun label(): String = when (this) {
        ALWAYS -> "始终"
        WIFI_ONLY -> "仅 Wi-Fi"
        MANUAL -> "手动"
    }

    /** 设置页/快捷面板的循环切换顺序 */
    fun next(): ImageLoadPolicy = when (this) {
        ALWAYS -> WIFI_ONLY
        WIFI_ONLY -> MANUAL
        MANUAL -> ALWAYS
    }

    companion object {
        /** 某档位在当前网络下是否自动加载（纯函数，可单测） */
        fun shouldAutoLoad(p: ImageLoadPolicy, isMetered: Boolean): Boolean = when (p) {
            ALWAYS -> true
            WIFI_ONLY -> !isMetered
            MANUAL -> false
        }
    }
}

/**
 * 省流/图片加载策略全局状态（首页瀑布流、「我的」快捷面板、设置页共用）。
 *
 * 2026-10-17 升级：布尔开关 → 三档策略 + 头像独立策略。
 * 旧 data_saver=true（不自动加载）迁移为 MANUAL，false → ALWAYS；旧 KEY 保留不删（回滚安全）。
 * 默认值不变：ALWAYS。
 */
object DataSaverStore {

    private const val PREF = "s1_home_tabs"
    private const val KEY_LEGACY = "data_saver"
    private const val KEY_POLICY = "image_load_policy"
    private const val KEY_AVATAR_POLICY = "avatar_load_policy"

    private val _policy = MutableStateFlow(ImageLoadPolicy.ALWAYS)
    val policy: StateFlow<ImageLoadPolicy> = _policy.asStateFlow()

    private val _avatarPolicy = MutableStateFlow(ImageLoadPolicy.ALWAYS)
    val avatarPolicy: StateFlow<ImageLoadPolicy> = _avatarPolicy.asStateFlow()

    /** 旧布尔语义兼容（= 手动档）：存量消费点（QuickSheets/Settings）迁移完成前保留 */
    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun init(context: Context) {
        val p = prefs(context)
        _policy.value = if (p.contains(KEY_POLICY)) {
            ImageLoadPolicy.entries.getOrElse(p.getInt(KEY_POLICY, 0)) { ImageLoadPolicy.ALWAYS }
        } else {
            val migrated = if (p.getBoolean(KEY_LEGACY, false)) ImageLoadPolicy.MANUAL else ImageLoadPolicy.ALWAYS
            p.edit().putInt(KEY_POLICY, migrated.ordinal).apply()
            migrated
        }
        _avatarPolicy.value = ImageLoadPolicy.entries
            .getOrElse(p.getInt(KEY_AVATAR_POLICY, 0)) { ImageLoadPolicy.ALWAYS }
        _enabled.value = _policy.value == ImageLoadPolicy.MANUAL
    }

    fun setPolicy(context: Context, value: ImageLoadPolicy) {
        if (_policy.value == value) return
        _policy.value = value
        _enabled.value = value == ImageLoadPolicy.MANUAL
        prefs(context).edit().putInt(KEY_POLICY, value.ordinal).apply()
    }

    fun setAvatarPolicy(context: Context, value: ImageLoadPolicy) {
        if (_avatarPolicy.value == value) return
        _avatarPolicy.value = value
        prefs(context).edit().putInt(KEY_AVATAR_POLICY, value.ordinal).apply()
    }

    /** 旧布尔接口：true=手动档（不自动加载），false=始终 */
    fun set(context: Context, value: Boolean) =
        setPolicy(context, if (value) ImageLoadPolicy.MANUAL else ImageLoadPolicy.ALWAYS)

    fun policy(): ImageLoadPolicy = _policy.value
    fun avatarPolicy(): ImageLoadPolicy = _avatarPolicy.value
    fun isOn(): Boolean = _policy.value == ImageLoadPolicy.MANUAL

    /** View 体系（详情页等非 Compose 场景）即时判断：当前网络下 [p] 是否自动加载 */
    fun autoLoadNow(p: ImageLoadPolicy): Boolean =
        ImageLoadPolicy.shouldAutoLoad(p, com.example.s1client.util.NetworkMonitor.metered.value)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
}
