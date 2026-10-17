package com.example.s1client.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.s1client.util.ThemeManager
import com.example.s1client.data.store.SettingStore

/**
 * Compose 侧主题调色板。
 * 业务代码只从 LocalS1Colors 取色，禁止再写死颜色常量。
 */
data class S1Colors(
    val bgTop: Color,
    val bgBottom: Color,
    /** 顶栏渐变（从上到下 2 色） */
    val headerGradient: List<Color>,
    val textMain: Color,
    val textSub: Color,
    val accent: Color,
    val onAccent: Color,
    val accentCyan: Color,
    val heart: Color,
    val cardBg: Color,
    /** 9 宫格/小卡片玻璃底（半透明） */
    val glassCard: Color,
    /** 玻璃卡片装饰线（半透明） */
    val glassLine: Color,
    val glassBar: Color,
    val glassFloat: Color,
    val glassFloatLine: Color,
    val glassDialog: Color,
    val glassDialogLine: Color,
    val chipBg: Color,
    val strongIcon: Color,
    val snackbarBg: Color,
    val onSnackbar: Color,
    val flowCoverColors: List<Color>,
    /** 幻彩渐变封面的浅色幻彩色板（3 色，低饱和）；其他主题为空 */
    val coverColors: List<Color> = emptyList(),
    /**
     * 卡片液态玻璃边框（方案 B）：顶部 → 中部 → 底部的描边渐变色。
     * 深色主题对比强（明显），浅色主题对比弱（含蓄）。null = 该主题不用玻璃边框。
     */
    val glassEdgeTop: Color? = null,
    val glassEdgeMid: Color? = null,
    val glassEdgeBottom: Color? = null,
    /** 卡片内侧顶部高光（inset 高光），null = 不画 */
    val glassInsetLight: Color? = null,
    /** 是否深色主题（供 Material 配色桥接用） */
    val isDark: Boolean
)

// ————————— 原有三套（不动，只扩展 headerGradient）—————————

/** 午后红茶 */
private val TeaPalette = S1Colors(
    bgTop = Color(0xFFFBF3E0), bgBottom = Color(0xFFF2E7C8),
    headerGradient = listOf(Color(0xFFA0522D), Color(0xFFC9A86C)),
    textMain = Color(0xFF3A3A3A), textSub = Color(0xFF8B7355),
    accent = Color(0xFFA0522D), onAccent = Color(0xFFFFF8DC),
    accentCyan = Color(0xFF1FB8FF), heart = Color(0xFFFF2C55),
    cardBg = Color(0xFFFFFCF0),
    glassCard = Color(0xF2FFFCF0), glassLine = Color(0x59FFFFFF),
    glassBar = Color(0xE6C9B896), glassFloat = Color(0xF2FFFCF0),
    glassFloatLine = Color(0x33A0522D),
    glassDialog = Color(0xF2FFFCF0), glassDialogLine = Color(0x33A0522D),
    chipBg = Color.Black.copy(alpha = 0.06f),
    strongIcon = Color(0xFF5C2E0E),
    snackbarBg = Color(0xFF3A2410), onSnackbar = Color(0xFFFFF8DC),
    flowCoverColors = listOf(Color(0xFFFF6FA5), Color(0xFF1FB8FF), Color(0xFF8B5CF6), Color(0xFFFF6FA5)),
    // 液态玻璃（浅色 · 含蓄）：描边仅比卡面亮/暗一点点，靠内侧细高光透出质感
    glassEdgeTop = Color(0x33FFFFFF), glassEdgeMid = Color(0x1AFFFFFF),
    glassEdgeBottom = Color(0x14000000),
    glassInsetLight = Color(0x66FFFFFF),
    isDark = false
)

/** OLED 纯黑 */
val OledPalette = S1Colors(
    bgTop = Color(0xFF000000), bgBottom = Color(0xFF0A0A0A),
    headerGradient = listOf(Color(0xFF1A1A1A), Color(0xFF000000)),
    textMain = Color(0xFFCCCCCC), textSub = Color(0xFF888888),
    accent = Color(0xFFD08040), onAccent = Color(0xFF000000),
    accentCyan = Color(0xFF1FB8FF), heart = Color(0xFFFF2C55),
    cardBg = Color(0xFF0A0A0A),
    glassCard = Color(0xF20A0A0A), glassLine = Color(0x3DFFFFFF),
    glassBar = Color(0xE6000000), glassFloat = Color(0xE60A0A0A),
    glassFloatLine = Color(0x3DFFFFFF),
    glassDialog = Color(0xF20A0A0A), glassDialogLine = Color(0x2EFFFFFF),
    chipBg = Color.White.copy(alpha = 0.08f),
    strongIcon = Color(0xFFE8C88A),
    snackbarBg = Color(0xFF1F1F1F), onSnackbar = Color(0xFFE8C88A),
    flowCoverColors = listOf(Color(0xFFD08040), Color(0xFF1FB8FF), Color(0xFFE8C88A), Color(0xFFD08040)),
    // 液态玻璃（真 OLED · 明显）：纯黑底上玻璃描边要够亮才看得见
    glassEdgeTop = Color(0x59FFFFFF), glassEdgeMid = Color(0x2EFFFFFF),
    glassEdgeBottom = Color(0x1FFFFFFF),
    glassInsetLight = Color(0x8AFFFFFF),
    isDark = true
)

/** 粉色佳人 */
private val PinkPalette = S1Colors(
    bgTop = Color(0xFFFDF3F6), bgBottom = Color(0xFFFCE4E9),
    headerGradient = listOf(Color(0xFFE94A72), Color(0xFFFCE4E9)),
    textMain = Color(0xFF2B2B2B), textSub = Color(0xFF8C6A73),
    accent = Color(0xFFE94A72), onAccent = Color(0xFFFFFFFF),
    accentCyan = Color(0xFF1FB8FF), heart = Color(0xFFFF2C55),
    cardBg = Color(0xFFFFFFFF),
    glassCard = Color(0xF2FFFFFF), glassLine = Color(0x33E94A72),
    glassBar = Color(0xF2FDF3F6), glassFloat = Color(0xF2FFFFFF),
    glassFloatLine = Color(0x40E94A72),
    glassDialog = Color(0xF2FFFFFF), glassDialogLine = Color(0x40E94A72),
    chipBg = Color.Black.copy(alpha = 0.06f),
    strongIcon = Color(0xFFC2185B),
    snackbarBg = Color(0xFF4A2530), onSnackbar = Color(0xFFFFFFFF),
    flowCoverColors = listOf(Color(0xFFE94A72), Color(0xFF1FB8FF), Color(0xFFC2185B), Color(0xFFE94A72)),
    isDark = false
)

// ————————— 三套新主题（视觉差异最大化）—————————

/**
 * FC 红白（任天堂 Famicom）：**深色复古游戏风**
 * 深黑底 + 主机深红强调 + 金色像素装饰线
 * 顶栏深红→黑渐变；玻璃卡片深红装饰边
 */
private val FcPalette = S1Colors(
    bgTop = Color(0xFF111111), bgBottom = Color(0xFF000000),
    headerGradient = listOf(Color(0xFFC41E3A), Color(0xFF111111)),
    textMain = Color(0xFFF0E8D8), textSub = Color(0xFF887A60),
    accent = Color(0xFFC41E3A), onAccent = Color(0xFFFFFFFF),
    accentCyan = Color(0xFFD4AF37), heart = Color(0xFFFF2C55),
    cardBg = Color(0xFF1A1A1A),
    glassCard = Color(0xE6222222), glassLine = Color(0x55C41E3A),
    glassBar = Color(0xE60A0A0A), glassFloat = Color(0xE6222222),
    glassFloatLine = Color(0x55C41E3A),
    glassDialog = Color(0xE6222222), glassDialogLine = Color(0x40D4AF37),
    chipBg = Color.White.copy(alpha = 0.08f),
    strongIcon = Color(0xFFD4AF37),
    snackbarBg = Color(0xFFC41E3A), onSnackbar = Color(0xFFFFFFFF),
    flowCoverColors = listOf(Color(0xFFC41E3A), Color(0xFFD4AF37), Color(0xFF111111), Color(0xFFC41E3A)),
    isDark = true
)

/**
 * RP5 灰彩（复古游戏掌机）：**暖灰底 + LCD 绿屏风**
 * 掌机外壳暖灰 + 内屏琥珀绿/荧光绿 + 深色字（模拟 GameBoy / RG552）
 */
private val Rp5Palette = S1Colors(
    bgTop = Color(0xFFD4D0B8), bgBottom = Color(0xFFB8B498),
    headerGradient = listOf(Color(0xFF8FA868), Color(0xFFB8B498)),
    textMain = Color(0xFF2F3A1E), textSub = Color(0xFF5B6A3E),
    accent = Color(0xFF4A7C23), onAccent = Color(0xFFFFFFFF),
    accentCyan = Color(0xFF8FA868), heart = Color(0xFFFF2C55),
    cardBg = Color(0xFFECE8D4),
    glassCard = Color(0xF2ECE8D4), glassLine = Color(0x404A7C23),
    glassBar = Color(0xE6B8B498), glassFloat = Color(0xF2ECE8D4),
    glassFloatLine = Color(0x404A7C23),
    glassDialog = Color(0xF2ECE8D4), glassDialogLine = Color(0x308FA868),
    chipBg = Color.Black.copy(alpha = 0.07f),
    strongIcon = Color(0xFF2F3A1E),
    snackbarBg = Color(0xFF2F3A1E), onSnackbar = Color(0xFFD4D0B8),
    flowCoverColors = listOf(Color(0xFF4A7C23), Color(0xFF8FA868), Color(0xFFB8B498), Color(0xFF4A7C23)),
    isDark = false
)

/**
 * 幻彩渐变：**固定 2 套**（浅色 / 深色），共用同一组「紫 → 品红 → 粉」色板。
 *
 * 2026-10-12 用户定稿：原来 5 套互不相干的色板（樱粉/薄荷/薰衣草/蜜桃/云水）精简成 2 套，
 * 颜色统一到参考图那一组 —— 紫 #B569F6（上）+ 粉 #FF7AB5（下），过渡品红 #C03DEA，
 * 暖橙核 #FF775F。**抽取逻辑保留**：每次进主题仍从这 2 套里随机抽一套。
 */
private class IridescentPreset(
    val bgTop: Color, val bgBottom: Color,
    val accent: Color, val strongIcon: Color,
    val headerA: Color, val headerB: Color,
    val line: Color, val bar: Color,
    val cover: List<Color>,
    // —— 深浅变体所需的其余令牌（浅色用默认值）——
    val isDark: Boolean = false,
    val cardBg: Color = Color(0xFFFFFFFF),
    val textMain: Color = Color(0xFF3D3845),
    val textSub: Color = Color(0xFF8A8296),
    val glassCard: Color = Color(0xF2FFFFFF),
    val glassFloat: Color = Color(0xF2FFFFFF),
    val glassDialog: Color = Color(0xF2FFFFFF),
    val chipBg: Color = Color.Black.copy(alpha = 0.05f),
    val snackbarBg: Color = Color(0xFF3D3845),
    val onSnackbar: Color = Color(0xFFFFFFFF),
    val glassEdgeTop: Color = Color(0x40FFFFFF),
    val glassEdgeMid: Color = Color(0x1FFFFFFF),
    val glassEdgeBottom: Color = Color(0x14000000),
    val glassInsetLight: Color = Color(0x73FFFFFF)
)

private val IridescentPresets = listOf(
    // 0 · 浅色板 —— **iOS 27 Liquid Glass 基调 + 紫粉调**
    //    用户反馈原版「过于偏白、刺眼」，原 #FBF7FD/#FDF4F9 已接近纯白。
    //    本轮整体压暗并加重粉紫：
    //      · 页面底色 #F2EAF6 → #FBE4F0（淡紫到粉的柔和渐变，比原版暗约 8%，
    //        消除纯白的刺眼感，同时保留 Liquid Glass 的通透）；
    //      · 文字用深紫灰 #3D2438 / #8A6B85（不用纯黑，避免在粉底上过硬）；
    //      · 主色 #D6339C（紫粉），与封面流光板同色系。
    IridescentPreset(
        Color(0xFFF2EAF6), Color(0xFFFBE4F0),
        Color(0xFFD6339C), Color(0xFF8B3FD9),
        Color(0xFFF7EBFC), Color(0xFFFDE6F3),
        Color(0x33B569F6), Color(0xE6F7F0F9),
        listOf(Color(0xFFFF6FA6), Color(0xFFB66BF7), Color(0xFFFF8FC7)),
        isDark = false,
        cardBg = Color(0xFFFFFFFF),
        textMain = Color(0xFF3D2438), textSub = Color(0xFF8A6B85),
        glassCard = Color(0xF7FFFFFF), glassFloat = Color(0xF2FFFFFF),
        glassDialog = Color(0xFAFFFFFF),
        chipBg = Color(0x1AB569F6),
        snackbarBg = Color(0xFFD6339C), onSnackbar = Color(0xFFFFFFFF),
        glassEdgeTop = Color(0x8AFFFFFF), glassEdgeMid = Color(0x4DFFFFFF),
        glassEdgeBottom = Color(0x1AFFFFFF), glassInsetLight = Color(0x6EFFFFFF)
    ),
    // 1 · 深色板（封面取自用户参考图 2：紫底 + 品红光 + 底部淡紫）
    //    页面深紫、卡片深紫灰；封面紫 → 品红 → 淡紫
    IridescentPreset(
        Color(0xFF16101F), Color(0xFF1E1428),
        Color(0xFFFF6FB5), Color(0xFFC99BF0),
        Color(0xFF241735), Color(0xFF2E1836),
        Color(0x33FF6FB5), Color(0xE6141020),
        listOf(Color(0xFFB66BF7), Color(0xFFC636E7), Color(0xFFBE80F8)),
        isDark = true,
        cardBg = Color(0xFF221830),
        textMain = Color(0xFFF4ECFA), textSub = Color(0xFFA99BBE),
        glassCard = Color(0xF2221830), glassFloat = Color(0xF22A1D3C),
        glassDialog = Color(0xF22A1D3C),
        chipBg = Color(0x14FFFFFF),
        snackbarBg = Color(0xFF2A1D3C), onSnackbar = Color(0xFFF4ECFA),
        glassEdgeTop = Color(0x59FFFFFF), glassEdgeMid = Color(0x2EFFFFFF),
        glassEdgeBottom = Color(0x1AFFFFFF), glassInsetLight = Color(0x8AFFFFFF)
    )
)

/**
 * 幻彩描边取色 —— **「我的」页卡框与瀑布流卡框共用**，保证同一套幻彩语言。
 *
 * ⚠️ 血泪教训（两次都错在这里）：**只用饱和色**，不要用 `headerGradient`。
 * `headerGradient` 在多数主题里是"顶栏那一整块**背景**"的渐变，浅色端接近白：
 *   · 幻彩浅色板 = #F7EBFC / #FDE6F3（近白）
 *   · 樱雾粉霜   = #E94A72 / #FCE4E9（一半近白）
 * 拿它当描边色，线性渐变从左上角起就是一片淡色，"彩边"自然看不出来。
 * 同理也不能用 accentCyan（幻彩浅色板 #7FB8D8 淡蓝，叠粉底上直接融进背景）。
 *
 * 只用这三类：主色 + 封面流光色 + strongIcon —— 它们在所有主题里都是高饱和的。
 */
fun s1RingColors(c: S1Colors): List<Color> {
    val out = ArrayList<Color>(5)
    out.add(c.accent)
    c.flowCoverColors.forEach { if (it != c.accent) out.add(it) }
    if (out.size < 3) out.add(c.strongIcon)
    return out.distinct()
}

private fun buildIridescentPalette(seed: Int): S1Colors {
    val p = IridescentPresets[seed.coerceIn(0, IridescentPresets.lastIndex)]
    return S1Colors(
        bgTop = p.bgTop, bgBottom = p.bgBottom,
        headerGradient = listOf(p.headerA, p.headerB),
        textMain = p.textMain, textSub = p.textSub,
        accent = p.accent, onAccent = Color(0xFFFFFFFF),
        accentCyan = Color(0xFF7FB8D8), heart = Color(0xFFFF2C55),
        cardBg = p.cardBg,
        glassCard = p.glassCard, glassLine = p.line,
        glassBar = p.bar, glassFloat = p.glassFloat,
        glassFloatLine = p.line,
        glassDialog = p.glassDialog, glassDialogLine = p.line,
        chipBg = p.chipBg,
        strongIcon = p.strongIcon,
        snackbarBg = p.snackbarBg, onSnackbar = p.onSnackbar,
        flowCoverColors = p.cover + p.cover.first(),
        coverColors = p.cover,
        // 液态玻璃：浅色含蓄 / 深色明显
        glassEdgeTop = p.glassEdgeTop, glassEdgeMid = p.glassEdgeMid,
        glassEdgeBottom = p.glassEdgeBottom,
        glassInsetLight = p.glassInsetLight,
        isDark = p.isDark
    )
}

// ————————— Linear 暗色（第 1 批改造新增）—————————

/**
 * Linear 风格暗色：**低饱和 + 单强调色 + 靠边框分层**
 * 底 #08090A / 卡片 #0D0E10 / 悬浮 #111215 / 边框 #191B1F / 主色 #5E6AD2
 * 不用阴影，靠 1px 描边与按压高亮区分层级；圆角统一收敛到 9dp（见 MainActivity）
 */
val LinearPalette = S1Colors(
    bgTop = Color(0xFF08090A), bgBottom = Color(0xFF08090A),
    headerGradient = listOf(Color(0xFF101114), Color(0xFF08090A)),
    textMain = Color(0xFFEDEDEF), textSub = Color(0xFF8A8F98),
    accent = Color(0xFF5E6AD2), onAccent = Color(0xFFFFFFFF),
    accentCyan = Color(0xFF828FFF), heart = Color(0xFFFF2C55),
    cardBg = Color(0xFF0D0E10),
    // 玻璃层：Linear 用「几乎不透明的近黑 + 极细描边」，避免传统玻璃的高光感
    glassCard = Color(0xF20D0E10), glassLine = Color(0xFF191B1F),
    glassBar = Color(0xF208090A), glassFloat = Color(0xFF111215),
    glassFloatLine = Color(0xFF23262D),
    glassDialog = Color(0xFF111215), glassDialogLine = Color(0xFF23262D),
    chipBg = Color(0xFF141518),
    strongIcon = Color(0xFFEDEDEF),
    snackbarBg = Color(0xFF16181D), onSnackbar = Color(0xFFEDEDEF),
    // 卡片顶部语义色条：低饱和，与主色同族，避免花
    flowCoverColors = listOf(
        Color(0xFF5E6AD2), Color(0xFF3E8E6E), Color(0xFFC79A3A),
        Color(0xFF9A5BB8), Color(0xFF5E8FBF)
    ),
    // 液态玻璃（Linear · 明显）：整圈渐变描边 + 内侧高光，是这套主题的核心质感
    glassEdgeTop = Color(0x66FFFFFF), glassEdgeMid = Color(0x33FFFFFF),
    glassEdgeBottom = Color(0x26FFFFFF),
    glassInsetLight = Color(0x99FFFFFF),
    isDark = true
)

val LocalS1Colors = staticCompositionLocalOf { OledPalette }

/**
 * 主题索引 → 调色板。唯一一份映射，供 S1Theme 与「非 Compose 场景」共用
 * （例如换主题后同步系统栏颜色，见 ThemeEdge.syncSystemBarsWithPalette）。
 */
fun s1PaletteFor(themeIndex: Int, seed: Int = ThemeManager.iridescentSeed): S1Colors = when (themeIndex) {
    ThemeManager.THEME_LIGHT -> TeaPalette
    ThemeManager.THEME_OLED -> OledPalette
    ThemeManager.THEME_PINK -> PinkPalette
    ThemeManager.THEME_FC -> FcPalette
    ThemeManager.THEME_RP5 -> Rp5Palette
    ThemeManager.THEME_LINEAR -> LinearPalette
    ThemeManager.THEME_IRIDESCENT -> buildIridescentPalette(ThemeManager.iridescentSeed)
    else -> TeaPalette
}

@Composable
fun S1Theme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // 读的是 ThemeManager 的可观察状态：换主题 → 这里重组 → 整个 App 即时变色
    val colors = s1PaletteFor(ThemeManager.currentTheme)

    // —— 设置中心的全局文字设置（对标 s1er：textScaler 全局覆盖 + 自定义字体）——
    // 读 SettingStore.changes：设置页改字号/导入字体 → bump → 这里重组 → 全 App 生效
    val settingsVersion = com.example.s1client.data.store.SettingStore.changes.intValue
    val context = androidx.compose.ui.platform.LocalContext.current
    val fontScale = remember(settingsVersion) {
        com.example.s1client.data.store.SettingStore.fontScale(context)
    }
    val customFontFile = remember(settingsVersion) {
        com.example.s1client.data.store.SettingStore.customFontName(context)
            ?.let { com.example.s1client.data.store.SettingStore.fontFile(context) }
            ?.takeIf { it.exists() && android.os.Build.VERSION.SDK_INT >= 26 }
    }
    // 用户字体 × 用户缩放，叠加在系统 density 之上
    val density = androidx.compose.ui.platform.LocalDensity.current
    val scaledDensity = androidx.compose.ui.unit.Density(
        density = density.density,
        fontScale = density.fontScale * fontScale
    )

    val materialColorScheme = if (colors.isDark) {
        darkColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent,
            background = colors.bgTop, onBackground = colors.textMain,
            surface = colors.cardBg, onSurface = colors.textMain,
            surfaceVariant = colors.cardBg, onSurfaceVariant = colors.textSub,
            outline = colors.textSub, error = colors.heart, onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent,
            background = colors.bgTop, onBackground = colors.textMain,
            surface = colors.cardBg, onSurface = colors.textMain,
            surfaceVariant = colors.cardBg, onSurfaceVariant = colors.textSub,
            outline = colors.textSub, error = colors.heart, onError = Color.White
        )
    }

    // 自定义字体注入 Material 排版（默认样式全部继承），未导入时用系统默认。
    // 用反射探测 File/Path 两种构造：不同 Compose 版本 platform.Font 的公开面不同，
    // 反射保证任何版本都能编译，运行时找不到就优雅回退系统字体。
    val typography = if (customFontFile != null) {
        // 阶段四 P10（2026-10-17）：原实现反射探测 androidx.compose.ui.text.platform.Font——
        // 该内部类在 Compose 1.6（BOM 2024.02）不存在，catch 静默回退系统字体，
        // 导致「字体导入永远看不到效果」。改为直接框架调用 Typeface.createFromFile，
        // FontFamily(Typeface) 公开重载稳定存在。
        val family: androidx.compose.ui.text.font.FontFamily = try {
            val frameworkTypeface = android.graphics.Typeface.createFromFile(customFontFile)
            androidx.compose.ui.text.font.FontFamily(frameworkTypeface)
        } catch (_: Throwable) {
            androidx.compose.ui.text.font.FontFamily.Default
        }
        applyFontFamily(materialTypography(), family)
    } else {
        materialTypography()
    }

    CompositionLocalProvider(LocalS1Colors provides colors) {
        MaterialTheme(colorScheme = materialColorScheme, typography = typography) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides scaledDensity,
                content = content
            )
        }
    }
}

/** Material3 默认排版（缓存一份，避免每次重组重建） */
@Composable
private fun materialTypography() = MaterialTheme.typography

/** 把 fontFamily 应用到全部 15 档 Material 样式 */
private fun applyFontFamily(
    base: androidx.compose.material3.Typography,
    family: androidx.compose.ui.text.font.FontFamily
): androidx.compose.material3.Typography =
    androidx.compose.material3.Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family)
    )
