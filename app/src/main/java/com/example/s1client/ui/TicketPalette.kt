package com.example.s1client.ui

import androidx.compose.ui.graphics.Color
import com.example.s1client.ui.theme.S1Colors

/**
 * 票面调色板 —— 全 App「票据」视觉语言的**唯一色源**（宪法第 20 条）。
 *
 * 起源：首页长按预览卡（PostPeekDialog）的 TicketPalette，本文件由它抽出成共用模块，
 * 让便签、个人资料卡、撤销票根等所有票据面 UI 引用同一套派生逻辑，
 * 杜绝各处硬编码色值造成同一概念在不同页面长得不一样。
 *
 * 质感公式（**不是**半透明浅色纸）：
 * - 票纸 = 当前主题底色压暗 45%（深票体，在浅色页面上是明确的深色块）
 * - 四色荧光晕染 glowA/B/C/D = 主题 accent / headerGradient / accentCyan / heart 混白
 * - 票面文字恒亮白（票体深，浅字才读得清 —— 可读性高于一切）
 * - 幻彩主题走专属夜光板 TicketNight（紫粉）
 *
 * 重要教训（2026-10-16）：此前把便签做成「88% 半透明浅色纸」是方向错误 ——
 * 半透明只会让便签与页面底色混成一片灰蒙，既没有票据质感，也牺牲了正文对比度。
 */
data class TicketPalette(
    val paper: Color,        // 票纸底（深）
    val glowA: Color, val glowB: Color, val glowC: Color, val glowD: Color, // 全息光四色
    val onTicket: Color,     // 票面主文字
    val onTicketSub: Color,  // 票面次文字/数据/票头
    val accent: Color,       // 撕裂线/条码/进帖链接
    val accentSoft: Color,   // 操作按钮边框
    val btnBg: Color,        // 操作按钮底
    val hideTint: Color,     // 「隐藏」按钮色调
    val avatarRing: Color,   // 头像环
    val openBorder: Color    // 进帖胶囊边框
) {
    /** 转成 Android Int 色（ARGB），供 View 体系（XML/drawable tint）使用 */
    fun paperInt(): Int = paper.toArgbInt()
    fun glowInt(i: Int): Int = when (i) {
        0 -> glowA.toArgbInt(); 1 -> glowB.toArgbInt()
        2 -> glowC.toArgbInt(); else -> glowD.toArgbInt()
    }
    fun onTicketInt(): Int = onTicket.toArgbInt()
    fun onTicketSubInt(): Int = onTicketSub.toArgbInt()
    fun accentInt(): Int = accent.toArgbInt()
}

/** 幻彩主题专属夜光票板（紫粉） */
val TicketNight = TicketPalette(
    paper = Color(0xFF241A33),
    glowA = Color(0xFFFF6FB5), glowB = Color(0xFFB66BF7),
    glowC = Color(0xFFFFC96B), glowD = Color(0xFFFF8FC7),
    onTicket = Color(0xFFFFFFFF),
    onTicketSub = Color(0xFFE9C8EE),
    accent = Color(0xFFE9C8EE),
    accentSoft = Color(0x47FFD6F0),
    btnBg = Color(0x1AFFFFFF),
    hideTint = Color(0xFFFFC7DE),
    avatarRing = Color(0xFFFFD6F0),
    openBorder = Color(0x66E9C8EE)
)

/** 颜色向白混合 f∈0..1（提亮到深色票面上可读） */
fun mixWhite(c: Color, f: Float): Color =
    Color(c.red + (1f - c.red) * f, c.green + (1f - c.green) * f, c.blue + (1f - c.blue) * f, 1f)

/** 颜色向黑压 f∈0..1（票纸底深化） */
fun darken(c: Color, f: Float): Color =
    Color(c.red * (1f - f), c.green * (1f - f), c.blue * (1f - f), 1f)

/**
 * 按主题派生票面调色板：幻彩 = 紫粉夜光；其他主题 = 从该主题色板派生。
 *
 * 判断幻彩的方式与 Peek 卡一致：[S1Colors.coverColors] 非空即幻彩。
 */
fun ticketPaletteFor(s1: S1Colors): TicketPalette {
    if (s1.coverColors.isNotEmpty()) return TicketNight
    val glowA = mixWhite(s1.accent, .35f)
    val glowB = mixWhite(s1.headerGradient.getOrNull(1) ?: s1.accent, .30f)
    val glowC = mixWhite(s1.accentCyan, .40f)
    val glowD = mixWhite(s1.heart, .35f)
    val line = Color(0xB8FFFFFF)
    return TicketPalette(
        paper = darken(s1.bgBottom, .45f),
        glowA = glowA, glowB = glowB, glowC = glowC, glowD = glowD,
        onTicket = Color(0xFFFFFFFF),
        onTicketSub = Color(0xE6FFFFFF),
        accent = line,
        accentSoft = Color(0x3DFFFFFF),
        btnBg = Color(0x1AFFFFFF),
        hideTint = Color(0xFFFFFFFF),
        avatarRing = glowA.copy(alpha = .55f),
        openBorder = Color(0x66FFFFFF)
    )
}

private fun Color.toArgbInt(): Int =
    android.graphics.Color.argb(
        (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
    )