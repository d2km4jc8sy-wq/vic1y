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
    /** 幻彩固定色板开关（2026-10-19 由「深色跟随系统」改造而来）+ 被锁定的那套色板索引 */
    private const val KEY_IRI_LOCK = "iridescent_lock"
    private const val KEY_IRI_SEED = "iridescent_locked_seed"
    /** 幻彩色板数量（2026-10-19：v1 极光家族剔除后仅剩糖果家族深浅 2 套，见 S1Theme.kt IridescentPresets） */
    private const val IRIDESCENT_PRESET_COUNT = 2

    // 主题索引
    // 2026-10-19 清理：曜石玄夜 / 樱雾粉霜 / 朱砂红白 / 苔绿灰阶 四套隐藏主题连同
    // 色板（colors.xml 的 oled_* / pink_* / fc_* / rp5_*）与 OLED style 一起删除，
    // 索引 1-4 作废。同日幻彩 v1 极光家族（原索引 5）按用户决定剔除，索引 5 一并作废，
    // 幻彩渐变由原 v2（索引 11）顶替名号并移到主题列表末位。
    // 老版本存档若落在作废索引上，由 normalize() 直接落到默认主题赤红。
    const val THEME_IRIDESCENT = 11  // 幻彩渐变（原 v2 霓虹糖果家族：夜市/镭射糖果，随机抽板）
    const val THEME_LINEAR = 6      // Linear 墨蓝（2026-10-19 按 OLED 夜间护眼重做）
    /**
     * 焙茶午后（原名「纯白」，2026-10-19 改名）。索引 7 承接的就是原来那套纯白。
     * 旧的焙茶午后（索引 0，米黄 #EFE6D3 + 茶褐 #A0522D）同日删除 ——
     * 它与改版后的纯白（纸感暖白）同为浅色暖调、功能重复，用户选择保留后者并让它继承名字。
     * 老用户存档若停在 0 上，normalize() 自动落到本主题，等于无痛迁移。
     */
    const val THEME_TEA = 7
    /**
     * 赤红（2026-10-19 定稿，默认主题）：页面深砖红 #4A1C26 + 卡片暖灰 #4A4245。
     * 索引 10 承接的正是 2026-10-19 那版「New 赤红」色板；旧版赤红（索引 8）同日删除 ——
     * 它第一眼惊艳，但页面比卡片亮、久看刺眼（用户实测：看约 20 分钟眼睛就特别难受）。
     * 老用户存档若停在 8 上，normalize() 会自动落到 10，等于无痛升级到新版赤红。
     */
    const val THEME_CRIMSON = 10
    /** 机器猫（2026-10-18 新增）：蓝做缝、灰做面、红做强调。哆啦A梦红蓝双色 */
    const val THEME_DORAEMON = 9
    /**
     * 毛线主题（2026-10-20 新增）：毛线配色的原创浅色主题。
     * 注：针织纹理 / 手缝虚线 / 毛线球等绘制效果已因性能原因剔除，
     * 现仅保留配色，不增加任何绘制开销。
     * 粉彩奶油底 + 焦糖棕字 + 柔珊瑚强调，走"织物"的温暖路线；
     * 登录页角色球在 THEME_YARN 下换成原创毛线球（不照搬任天堂角色）。
     * 纹理/缝合描边等材质细节由各页面按 currentTheme == THEME_YARN 自行点缀。
     *
     * 2026-10-20 用户决定：从设置页选择列表摘除（见 VISIBLE_THEMES），
     * **代码与色板保留**，只是不给入口；枚举/名称映射/XML 样式均未删除。
     */
    const val THEME_YARN = 13

    /** 幻彩专属件判定（流光封面、彩边、旋转流光边框等，现仅糖果家族一家） */
    fun isIridescentFamily(theme: Int): Boolean =
        theme == THEME_IRIDESCENT

    /**
     * 当前主题对应 IridescentPresets 里的色板索引（0=夜市 / 1=镭射糖果）。
     * 深浅由 seed 随机决定。v1 极光家族剔除后仅剩糖果家族两板
     * （2026-10-19：原 4 板 2 家族收缩为 2 板 1 家族）。
     * [seed] 默认取本次进程抽到的 [iridescentSeed]；显式传入时按传入值算，
     * 供预览/单测锁定指定板。
     */
    fun iridescentPaletteIndex(seed: Int = iridescentSeed): Int = seed % 2

    /** 无图卡片风格数量（4 套：深色莫兰迪 / 微网格 / 大引号 / 暗色玻璃） */
    const val COVER_STYLE_COUNT = 4

    /**
     * 设置页里暴露给用户的主题（2026-10-20 起五套）：
     * Red赤红 → 焙茶午后 → Linear 墨蓝 → 机器猫 → 幻彩渐变（末位，用户指定）。
     * 幻彩渐变 = 霓虹糖果家族深浅随机（特殊通道，资源独享）。
     *
     * 2026-10-20 用户决定：毛线主题（索引 13）从本列表摘除，不再出现在主题选择器里。
     * 只摘入口、不删实现 —— YarnPalette / Theme.S1Client.Yarn / 名称映射全部原样保留，
     * 想捡回来把 THEME_YARN 加回本列表即可（normalize 照样认它）。
     * 存档停在该索引上的，normalize() 自动落到默认主题赤红。
     */
    val VISIBLE_THEMES: List<Int> = listOf(
        THEME_CRIMSON,       // Red赤红：默认主题，红做氛围、灰做内容
        THEME_TEA,           // 焙茶午后：纸感暖白 + 黛青（暖调浅色）
        THEME_LINEAR,        // Linear 墨蓝：OLED 夜间护眼深色
        THEME_DORAEMON,      // 机器猫：蓝缝 + 灰面 + 红强调
        THEME_IRIDESCENT     // 幻彩渐变：夜市/镭射糖果随机（2026-10-19 移到最后一位）
    )

    private var appContext: Context? = null

    /** 深色跟随系统开关（默认关；存储在主题 prefs，随 ThemeManager 生命周期） */
    private var followSystemDark = false

    /**
     * 当前主题索引。用 Compose 的 mutableStateOf 承载：
     * 换主题时整棵 Compose 树立即重组 —— 即时变色，不重建 Activity、不丢滚动位置。
     * 非 Compose 代码照旧读 `ThemeManager.currentTheme`，行为不变。
     */
    private val currentThemeState = androidx.compose.runtime.mutableStateOf(THEME_CRIMSON)

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
     * 幻彩渐变本次进程锁定的色板（0=霓虹·夜市 / 1=镭射·糖果）。
     * 特殊通道（用户定）：幻彩资源独享、与其他池子不互享、每次进主题随机抽一套。
     * 2026-10-19：v1 极光家族剔除，从 4 套收缩为糖果家族 2 套，抽取逻辑保留。
     */
    var iridescentSeed: Int = Random.nextInt(IRIDESCENT_PRESET_COUNT)
        private set

    /**
     * 幻彩固定色板（2026-10-19）：关 = 每次冷启动随机抽一套（原规则）；
     * 开 = 锁定当前这一套，冷启动/切主题都不再重随。
     * 存的是「打开开关那一刻正在显示的那套」，所以想换锁定对象：先关掉 → 重启抽到想要的 → 再打开。
     */
    private var iridescentLocked = false

    private val appliedByActivity = mutableMapOf<String, Int>()

    private fun rollSessionRandoms() {
        sessionCoverStyle = Random.nextInt(COVER_STYLE_COUNT)
        // 锁定后不再掷骰子：色板是用户显式钉住的那一套
        if (!iridescentLocked) iridescentSeed = Random.nextInt(IRIDESCENT_PRESET_COUNT)
    }

    /**
     * 归档索引 → 有效主题。
     * 2026-10-19：四套隐藏主题（索引 1-4）连同色板一起删除，
     * 老版本存档若落在这些（或任何已不存在的）索引上，一律落到默认主题赤红。
     */
    private fun normalize(index: Int): Int =
        if (index in VISIBLE_THEMES) index else THEME_CRIMSON

    fun init(context: Context) {
        if (appContext == null) {
            // R7（审计报告 v2 2026-10-18 第 9 轮）：去掉 appContext!! ——
            // 直接用入参的 applicationContext（同一实例），不再依赖可空属性 + 强制解包
            val ctx = context.applicationContext
            appContext = ctx
            val prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            // 任何落在已作废索引（隐藏主题 1-4、幻彩 v1 的 5 等）上的存档，一律落到默认主题赤红并回写
            val saved = prefs.getInt(KEY, THEME_CRIMSON)
            val theme = normalize(saved)
            if (theme != saved) {
                prefs.edit().putInt(KEY, theme).putBoolean(KEY_MIGRATED, true).apply()
            }
            currentTheme = theme
            // 幻彩固定色板（2026-10-19）：开 → 读回被钉住的那套；关 → 冷启动重随
            iridescentLocked = prefs.getBoolean(KEY_IRI_LOCK, false)
            if (iridescentLocked) {
                iridescentSeed = prefs.getInt(KEY_IRI_SEED, 1)
                    .coerceIn(0, IRIDESCENT_PRESET_COUNT - 1)
            }
            // 冷启动随机：不同主题不同的随机结果（色板锁定时 rollSessionRandoms 不会动它）
            rollSessionRandoms()
            // 旧「深色跟随系统」已被幻彩固定色板顶替：老存档里的开关直接清掉，
            // 否则老用户会在界面上找不到开关的情况下被旧规则改主题。
            followSystemDark = false
            if (prefs.getBoolean(KEY_FOLLOW, false)) {
                prefs.edit().putBoolean(KEY_FOLLOW, false).apply()
            }
        }
    }

    /**
     * 当前主题对应的无图卡片风格：
     * -2 = Linear 专用封面；5 = 幻彩渐变专用流光封面（固定，不再抽其它风格）；
     * 其余 = 本次进程随机锁定的 0..3
     */
    fun coverStyleForCurrent(): Int = when (currentTheme) {
        THEME_LINEAR, THEME_CRIMSON, THEME_DORAEMON -> -2  // 深色基底专用：纯色封面 + 收敛圆角
        THEME_IRIDESCENT -> 5                              // 幻彩渐变：固定用流光封面
        else -> sessionCoverStyle
    }

    fun getThemeRes(): Int = when (currentTheme) {
        THEME_TEA -> R.style.Theme_S1Client_Tea
        // 幻彩渐变（夜市/糖果两板随机）：XML 页面（帖子详情等）必须跟着当次抽到的那套，
        // 否则会「首页深色、详情浅色」
        THEME_IRIDESCENT -> if (iridescentSeed % 2 == 0) R.style.Theme_S1Client_IridescentNeon
                           else R.style.Theme_S1Client_IridescentCandy
        // 深色主题各有专属 XML 样式（原先 Linear 复用 OLED，页面标题会变成琥珀金）
        THEME_LINEAR -> R.style.Theme_S1Client_Linear
        THEME_CRIMSON -> R.style.Theme_S1Client_Crimson
        THEME_DORAEMON -> R.style.Theme_S1Client_Doraemon
        THEME_YARN -> R.style.Theme_S1Client_Yarn
        else -> R.style.Theme_S1Client_Tea
    }

    fun setTheme(index: Int) {
        applyTheme(index, fromUser = true)
    }

    /**
     * 主题应用内核心（2026-10-17 深色跟随系统拆出）：
     * fromUser=true 表示用户在主题面板手动选择 —— 自动退出跟随模式（用户显式接管）。
     */
    private fun applyTheme(index: Int, fromUser: Boolean) {
        // 兜底：任何调用方传进来已作废的索引（隐藏主题 1-4 等），都落到默认主题赤红
        val next = normalize(index)
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
        if (systemDark) THEME_LINEAR else THEME_TEA

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

    /**
     * 幻彩固定色板（2026-10-19，顶替原「深色跟随系统」开关）。
     *
     * 开：把**当前正在显示的那套**色板钉住 —— 冷启动、切主题都不再重随，
     *     幻彩从此只有这一种样子（XML 样式也跟着它走，详情页不会跳板）。
     * 关：恢复原规则，每次冷启动随机抽一套。
     *
     * 想换锁定对象：先关掉 → 重启抽到想要的那套 → 再打开。
     */
    fun setIridescentLock(enabled: Boolean) {
        iridescentLocked = enabled
        val ed = appContext?.getSharedPreferences(PREF, Context.MODE_PRIVATE)?.edit() ?: return
        if (enabled) {
            ed.putBoolean(KEY_IRI_LOCK, true).putInt(KEY_IRI_SEED, iridescentSeed).apply()
        } else {
            ed.putBoolean(KEY_IRI_LOCK, false).apply()
        }
    }

    fun isIridescentLocked(): Boolean = iridescentLocked

    fun markApplied(activityClass: String) {
        appliedByActivity[activityClass] = currentTheme
    }

    fun hasThemeChanged(activityClass: String): Boolean {
        val applied = appliedByActivity[activityClass] ?: return false
        return applied != currentTheme
    }

    /** 全部主题名（按索引对齐；隐藏主题与幻彩 v1 已于 2026-10-19 删除，作废索引不再有名字） */
    fun getAllThemeNames(): List<String> = listOf(
        "",              // 0（旧焙茶午后，2026-10-19 删除）
        "",              // 1（已删除）
        "",              // 2（已删除）
        "",              // 3（已删除）
        "",              // 4（已删除）
        "",              // 5（幻彩渐变v1 极光家族，2026-10-19 剔除）
        "Linear 墨蓝",   // 6
        "焙茶午后",      // 7
        "",              // 8（旧版赤红，2026-10-19 删除）
        "机器猫",        // 9
        "Red赤红",       // 10（2026-10-19 显示名由「赤红」改为「Red赤红」）
        "幻彩渐变",       // 11（原 v2 霓虹糖果家族顶替名号：夜市/镭射糖果随机）
        "",               // 12（雨过天青：2026-10-19 新增、同日删除，索引留墓碑；存档落此索引时 normalize 迁移到赤红）
        "毛线主题"        // 13（2026-10-20 新增；同日从 VISIBLE_THEMES 摘除，保留不暴露）
    )

    /**
     * 设置页选择列表：暴露 VISIBLE_THEMES 里的 5 套主题，按其出现顺序排列。
     * 每项是 Pair(主题索引, 主题名)，点击时用索引去 setTheme。
     */
    fun getVisibleThemes(): List<Pair<Int, String>> {
        val all = getAllThemeNames()
        return VISIBLE_THEMES.map { idx -> idx to all.getOrElse(idx) { "" } }
    }

    /** 兼容旧调用：返回可见主题名列表（顺序同 VISIBLE_THEMES） */
    fun getThemeNames(): List<String> = getVisibleThemes().map { it.second }
}
