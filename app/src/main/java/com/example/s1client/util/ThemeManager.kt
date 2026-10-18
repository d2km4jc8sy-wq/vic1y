package com.example.s1client.util

import android.content.Context
import com.example.s1client.R
import kotlin.random.Random

object ThemeManager {

    private const val PREF = "s1_theme_prefs"
    private const val KEY = "theme_index"
    /** 老版本索引 → 新索引的迁移只跑一次，避免把新版「粉色佳人」(=2) 误迁成 OLED */
    private const val KEY_MIGRATED = "theme_migrated_v1"
    private const val KEY_FOLLOW = "follow_system_dark"

    // 主题索引（全部）
    const val THEME_LIGHT = 0       // 焙茶午后
    const val THEME_OLED = 1        // 曜石玄夜
    const val THEME_PINK = 2        // 樱雾粉霜
    const val THEME_FC = 3          // 朱砂红白
    const val THEME_RP5 = 4         // 苔绿灰阶
    const val THEME_IRIDESCENT = 5  // 幻彩渐变
    const val THEME_LINEAR = 6      // Linear 墨蓝
    /** 纯白（2026-10-18 新增）：参考 iOS 默认浅色。索引追加在末尾，避免重排已存的值 */
    const val THEME_PURE = 7

    private const val THEME_MAX = THEME_PURE

    /** 无图卡片风格数量（4 套：深色莫兰迪 / 微网格 / 大引号 / 暗色玻璃） */
    const val COVER_STYLE_COUNT = 4

    /**
     * 设置页里暴露给用户的主题（2026-10-18 定稿四套）：
     *   纯白（iOS 默认）→ Linear 墨蓝（iOS 深色）→ 焙茶午后（S1 底色）→ 幻彩渐变。
     * 曜石玄夜 / 樱雾粉霜 / 朱砂红白 / 苔绿灰阶 已从选择列表移除，
     * 老用户若停留在这些主题上，由 migrateHiddenTheme 就近迁移。
     */
    val VISIBLE_THEMES: List<Int> = listOf(
        THEME_PURE,        // 纯白：参考 iOS 默认浅色
        THEME_LINEAR,      // Linear 墨蓝：参考 iOS 深色
        THEME_LIGHT,       // 焙茶午后：S1 网页主色，是特色
        THEME_IRIDESCENT   // 幻彩渐变：固定流光封面
    )

    private var appContext: Context? = null

    /** 深色跟随系统开关（默认关；存储在主题 prefs，随 ThemeManager 生命周期） */
    private var followSystemDark = false

    /**
     * 当前主题索引。用 Compose 的 mutableStateOf 承载：
     * 换主题时整棵 Compose 树立即重组 —— 即时变色，不重建 Activity、不丢滚动位置。
     * 非 Compose 代码照旧读 `ThemeManager.currentTheme`，行为不变。
     */
    private val currentThemeState = androidx.compose.runtime.mutableStateOf(THEME_LIGHT)

    var currentTheme: Int
        get() = currentThemeState.value
        private set(value) {
            currentThemeState.value = value
        }

    /**
     * 本次进程内锁定的卡片风格（0..4）。
     * 冷启动时随机一次；切换主题时也重新随机，普通主题全场卡片统一用它。
     */
    var sessionCoverStyle: Int = Random.nextInt(COVER_STYLE_COUNT)
        private set

    /**
     * 幻彩渐变本次进程锁定的色板（0=浅色紫粉 / 1=深色紫粉）。
     * 色板 2026-10-12 从 5 套精简为 2 套（颜色统一到紫粉家族），
     * 但**抽取逻辑保留**：每次进主题/切主题仍从这 2 套里随机抽一套。
     */
    var iridescentSeed: Int = Random.nextInt(2)
        private set

    private val appliedByActivity = mutableMapOf<String, Int>()

    private fun rollSessionRandoms() {
        sessionCoverStyle = Random.nextInt(COVER_STYLE_COUNT)
        iridescentSeed = Random.nextInt(2)
    }

    /**
     * 隐藏/已删除主题 → 可见主题的迁移映射（按"原本的明暗意图"就近落位）：
     *  樱雾粉霜(2, 浅色)、苔绿灰阶(4, 浅色) → 焙茶午后；
     *  朱砂红白(3, 深色)、曜石玄夜(1, 深色·已删除) → Linear 墨蓝。
     * 老版本用户如果正停留在这些主题上，升级后自动落到最接近的主力主题之一。
     */
    private fun migrateHiddenTheme(index: Int): Int = when (index) {
        THEME_PINK, THEME_RP5 -> THEME_LIGHT
        THEME_FC, THEME_OLED -> THEME_LINEAR
        else -> index
    }

    fun init(context: Context) {
        if (appContext == null) {
            // R7（审计报告 v2 2026-10-18 第 9 轮）：去掉 appContext!! ——
            // 直接用入参的 applicationContext（同一实例），不再依赖可空属性 + 强制解包
            val ctx = context.applicationContext
            appContext = ctx
            val prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            val theme = if (prefs.getBoolean(KEY_MIGRATED, false)) {
                // 新版索引 0-5 原样保留，上限扩到 THEME_MAX
                prefs.getInt(KEY, THEME_LIGHT).coerceIn(THEME_LIGHT, THEME_MAX)
            } else {
                // 老版本索引迁移（仅首次升级时执行一次）：
                // 0(午后红茶) -> 0，1/2/3 -> 1(OLED)，其他 -> 0
                val saved = prefs.getInt(KEY, THEME_LIGHT)
                val migrated = when (saved) {
                    THEME_LIGHT -> THEME_LIGHT
                    1, 2, 3 -> THEME_OLED
                    else -> THEME_LIGHT
                }
                prefs.edit()
                    .putInt(KEY, migrated)
                    .putBoolean(KEY_MIGRATED, true)
                    .apply()
                migrated
            }
            // 主题收敛：如果存档落在已隐藏的主题上，迁移到最接近的可见主题并落盘
            val visible = migrateHiddenTheme(theme)
            if (visible != theme) {
                prefs.edit().putInt(KEY, visible).apply()
            }
            currentTheme = visible
            // 冷启动随机：不同主题不同的随机结果
            rollSessionRandoms()
            // 深色跟随（2026-10-17）：启动时若开关开启，按当前系统深浅重映射
            followSystemDark = prefs.getBoolean(KEY_FOLLOW, false)
            if (followSystemDark) {
                val systemDark = (context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
                applyTheme(mapFollowSystem(systemDark), fromUser = false)
            }
        }
    }

    /**
     * 当前主题对应的无图卡片风格：
     * -1 = OLED 纯黑专用封面（主题已删除，此分支保留兼容）；-2 = Linear 专用封面；
     * 5 = 幻彩渐变专用流光封面（固定，不再抽其它风格）；其余 = 本次进程随机锁定的 0..3
     */
    fun coverStyleForCurrent(): Int = when (currentTheme) {
        THEME_OLED -> -1
        THEME_LINEAR -> -2   // Linear 专用：纯色底 + 底部微光
        THEME_IRIDESCENT -> 5   // 幻彩渐变：固定用流光封面
        else -> sessionCoverStyle
    }

    fun getThemeRes(): Int = when (currentTheme) {
        THEME_LIGHT -> R.style.Theme_S1Client_Light
        THEME_PURE -> R.style.Theme_S1Client_Pure
        THEME_OLED -> R.style.Theme_S1Client_OLED
        THEME_PINK -> R.style.Theme_S1Client_Pink
        THEME_FC -> R.style.Theme_S1Client_FC
        THEME_RP5 -> R.style.Theme_S1Client_RP5
        // 幻彩有两套色板（浅紫粉 / 深紫），每次启动随机抽一套：
        // XML 页面（帖子详情等）必须跟着当次抽到的那套，否则会「首页深色、详情浅色」
        THEME_IRIDESCENT -> if (iridescentSeed == 1) R.style.Theme_S1Client_IridescentDark
                            else R.style.Theme_S1Client_Iridescent
        THEME_LINEAR -> R.style.Theme_S1Client_OLED
        else -> R.style.Theme_S1Client_Light
    }

    fun setTheme(index: Int) {
        applyTheme(index, fromUser = true)
    }

    /**
     * 主题应用内核心（2026-10-17 深色跟随系统拆出）：
     * fromUser=true 表示用户在主题面板手动选择 —— 自动退出跟随模式（用户显式接管）。
     */
    private fun applyTheme(index: Int, fromUser: Boolean) {
        // 兜底：任何调用方传进来隐藏主题索引，都按明暗意图落到可见主题，绝不串位
        val next = migrateHiddenTheme(index).coerceIn(THEME_LIGHT, THEME_MAX)
        if (next != currentTheme) {
            currentTheme = next
            appContext?.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                ?.edit()?.putInt(KEY, currentTheme)?.apply()
            // 切主题立刻重随：卡片风格/幻彩色板换一套
            rollSessionRandoms()
            // 即时生效的关键：Compose 此刻已经重组完成，把已登记过的 Activity 全部记为
            // 新主题，这样它们下次 onResume 不会再走一遍 recreate（会白闪一下、还丢位置）。
            appliedByActivity.keys.toList().forEach { appliedByActivity[it] = next }
        }
        // 手动选主题 = 用户显式接管：自动退出跟随模式
        if (fromUser && followSystemDark) {
            followSystemDark = false
            appContext?.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                ?.edit()?.putBoolean(KEY_FOLLOW, false)?.apply()
        }
    }

    /** 系统深浅变化（S1App.onConfigurationChanged 转发）：跟随模式下重映射主题 */
    fun onSystemNightModeChanged(systemDark: Boolean) {
        if (!followSystemDark) return
        applyTheme(mapFollowSystem(systemDark), fromUser = false)
    }

    /** 跟随映射（纯函数，可单测）：系统深色→Linear 墨蓝，浅色→焙茶午后 */
    fun mapFollowSystem(systemDark: Boolean): Int =
        if (systemDark) THEME_LINEAR else THEME_LIGHT

    fun isFollowSystemDark(): Boolean = followSystemDark

    /** 设置页开关入口：开启时立即按当前系统深浅应用映射 */
    fun setFollowSystemDark(context: Context, enabled: Boolean) {
        followSystemDark = enabled
        appContext?.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            ?.edit()?.putBoolean(KEY_FOLLOW, enabled)?.apply()
        if (enabled) {
            val dark = (context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
            applyTheme(mapFollowSystem(dark), fromUser = false)
        }
    }

    fun markApplied(activityClass: String) {
        appliedByActivity[activityClass] = currentTheme
    }

    fun hasThemeChanged(activityClass: String): Boolean {
        val applied = appliedByActivity[activityClass] ?: return false
        return applied != currentTheme
    }

    /** 全部主题名（按索引对齐，隐藏主题也在内，方便内部查名） */
    fun getAllThemeNames(): List<String> = listOf(
        "焙茶午后",      // 0
        "曜石玄夜",      // 1（隐藏）
        "樱雾粉霜",      // 2（隐藏）
        "朱砂红白",      // 3（隐藏）
        "苔绿灰阶",      // 4（隐藏）
        "幻彩渐变",      // 5
        "Linear 墨蓝",   // 6
        "纯白"           // 7
    )

    /**
     * 设置页选择列表：只暴露 4 套主力主题，按 VISIBLE_THEMES 里的出现顺序排列。
     * 每项是 Pair(主题索引, 主题名)，点击时用索引去 setTheme。
     */
    fun getVisibleThemes(): List<Pair<Int, String>> {
        val all = getAllThemeNames()
        return VISIBLE_THEMES.map { idx -> idx to all.getOrElse(idx) { "" } }
    }

    /** 兼容旧调用：返回可见主题名列表（顺序同 VISIBLE_THEMES） */
    fun getThemeNames(): List<String> = getVisibleThemes().map { it.second }
}
