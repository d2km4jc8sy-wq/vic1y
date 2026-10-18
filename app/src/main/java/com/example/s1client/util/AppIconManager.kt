package com.example.s1client.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * 应用图标切换（2026-10-17 点3）。
 *
 * 机制：LAUNCHER intent-filter 挂在 4 个 activity-alias 上（默认/蓝/绿/紫），
 * 切换 = 禁用其余 alias、启用目标 alias（DONT_KILL_APP，桌面图标延迟刷新属正常）。
 *
 * 变体图标当前为占位（默认图标 + 色边 layer-list）；设计者自制的 PNG
 * 放入 res 后把对应 alias 的 android:icon 指过去即可，切换逻辑零改动。
 */
object AppIconManager {

    data class Variant(val id: String, val aliasSimpleName: String, val label: String)

    val VARIANTS = listOf(
        Variant("default", "MainAliasDefault", "默认"),
        Variant("legacy", "MainAliasLegacy", "经典圆环"),
        Variant("lemon", "MainAliasLemon", "柠檬黄"),
        Variant("cream", "MainAliasCream", "奶油底"),
        Variant("lilac", "MainAliasLilac", "丁香紫"),
        Variant("pink", "MainAliasPink", "粉色")
    )

    private const val KEY = "app_icon_variant"

    fun current(context: Context): String =
        context.getSharedPreferences("s1_client_prefs", Context.MODE_PRIVATE)
            .getString(KEY, "default") ?: "default"

    fun labelOf(id: String): String = VARIANTS.firstOrNull { it.id == id }?.label ?: "默认"

    /** 切换桌面图标：禁全部 alias、启目标（幂等）；DONT_KILL_APP 避免进程被杀 */
    fun apply(context: Context, id: String) {
        if (VARIANTS.none { it.id == id }) return
        val pm = context.packageManager
        val pkg = context.packageName
        for (v in VARIANTS) {
            val state =
                if (v.id == id) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            pm.setComponentEnabledSetting(
                ComponentName(pkg, "$pkg.${v.aliasSimpleName}"),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
        context.getSharedPreferences("s1_client_prefs", Context.MODE_PRIVATE)
            .edit().putString(KEY, id).apply()
    }

    /** 设置页循环切换：返回下一个档位 label（同步持久化） */
    fun cycle(context: Context): String {
        val idx = VARIANTS.indexOfFirst { it.id == current(context) }.let { if (it < 0) 0 else it }
        val next = VARIANTS[(idx + 1) % VARIANTS.size]
        apply(context, next.id)
        return next.label
    }

    /**
     * 启动自愈（2026-10-17 修复"点图标进应用详情"）：
     * 覆盖安装不会重置 PackageManager 的组件开关记录——若曾切到后来被删除的
     * 变体（如旧占位蓝/绿/紫），PMS 残留"默认禁用+已删变体启用"，升级后
     * 桌面入口指向不存在的组件，launcher 兜底跳应用详情。
     * 本方法在 S1App 启动时调用：SP 记录失效则回退默认；并重申整套 alias
     * 启用状态（幂等，治 PMS 残留错乱）。
     */
    fun ensureValid(context: Context) {
        val cur = current(context)
        apply(context, if (VARIANTS.any { it.id == cur }) cur else "default")
    }
}
