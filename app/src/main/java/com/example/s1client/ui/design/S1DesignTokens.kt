package com.example.s1client.ui.design

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 设计 token · 间距标尺（4pt 网格）
 *
 * 审计依据（AUDIT.md §2）：全项目 77 种 dp 值，主流落在 4/6/8/10/12/14/16/20/24。
 * 本标尺把常用值具名化；阶段 2 按组件逐个替换，未替换处行为不变。
 */
object S1Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
}

/**
 * 设计 token · 圆角标尺
 * 审计：18 种圆角值，99 与 999 混用同义（胶囊）。统一走 pill。
 */
object S1Radius {
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    /** 胶囊（原 99.dp / 999.dp 两种写法统一） */
    val pill: Dp = 999.dp
}

/** 设计 token · 阴影高程（审计：仅 6/18/24 三档在用） */
object S1Elevation {
    val e1: Dp = 6.dp
    val e2: Dp = 18.dp
    val e3: Dp = 24.dp
}
