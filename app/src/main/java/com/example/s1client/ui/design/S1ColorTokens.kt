package com.example.s1client.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.s1client.ui.theme.LocalS1Colors

/**
 * 设计 token · 颜色快捷入口（从 MainActivity.kt 原样迁出，值与行为零变化）
 *
 * 原声明位置：MainActivity.kt 124-171 行（阶段 0 审计 1c：token 声明在功能文件里
 * 属分层违规）。所有消费方经 `import com.example.s1client.ui.design.<名>` 引用。
 */
val BgColor: Color
    @Composable get() = LocalS1Colors.current.bgTop
val BgColorBottom: Color
    @Composable get() = LocalS1Colors.current.bgBottom
val TextMain: Color
    @Composable get() = LocalS1Colors.current.textMain
val TextSub: Color
    @Composable get() = LocalS1Colors.current.textSub

/** 热帖红：P2 统一——全主题 palette.heart 恒为 FF2C55（视觉与统一前一致） */
val HeartRed: Color
    @Composable get() = LocalS1Colors.current.heart

/** 破坏性语义红：R2（第 9 轮审计 2026-10-18）收口为 token，默认 FF3B30（视觉零变化） */
val DangerRed: Color
    @Composable get() = LocalS1Colors.current.danger

/** 强调色：跟随主题（粉/棕/琥珀） */
val AccentPink: Color
    @Composable get() = LocalS1Colors.current.accent

/** accent 按钮上的文字色 */
val OnAccent: Color
    @Composable get() = LocalS1Colors.current.onAccent

val AccentCyan = Color(0xFF1FB8FF)

/** 卡片配色由调色板下发 */
val CardBg: Color
    @Composable get() = LocalS1Colors.current.cardBg

/** 玻璃质感：半透明底 + 亮描边，透出下方内容与页面渐变 */
val GlassCard: Color
    @Composable get() = LocalS1Colors.current.glassCard
val GlassLine: Color
    @Composable get() = LocalS1Colors.current.glassLine
val GlassBar: Color
    @Composable get() = LocalS1Colors.current.glassBar
/** 底部导航栏底色（默认同 GlassBar） */
val NavBar: Color
    @Composable get() = LocalS1Colors.current.navBar
/** 瀑布流列缝底色（默认同页面底，机器猫单独给灰） */
val FeedGap: Color
    @Composable get() = LocalS1Colors.current.feedGap
val GlassFloat: Color
    @Composable get() = LocalS1Colors.current.glassFloat
val GlassFloatLine: Color
    @Composable get() = LocalS1Colors.current.glassFloatLine
val GlassDialog: Color
    @Composable get() = LocalS1Colors.current.glassDialog
val GlassDialogLine: Color
    @Composable get() = LocalS1Colors.current.glassDialogLine

/** 未选中胶囊底 */
val ChipBg: Color
    @Composable get() = LocalS1Colors.current.chipBg

/** Snackbar 底/字 */
val SnackbarBg: Color
    @Composable get() = LocalS1Colors.current.snackbarBg
val OnSnackbar: Color
    @Composable get() = LocalS1Colors.current.onSnackbar

/** 悬浮条上的可点强图标（深色主题=白，浅色主题=主题深色） */
val StrongIcon: Color
    @Composable get() = LocalS1Colors.current.strongIcon

/** 帖子无图卡片的流动幻彩封面（首尾同色 + TileMode.Repeated 循环） */
val FlowCoverColors: List<Color>
    @Composable get() = LocalS1Colors.current.flowCoverColors

/** 消息中心「回复」类型徽章色（默认珊瑚红；机器猫红底覆盖为白） */
val NoticeReply: Color
    @Composable get() = LocalS1Colors.current.noticeReply

/** 消息中心「@」类型徽章色（默认蓝；机器猫红底覆盖为白） */
val NoticeAt: Color
    @Composable get() = LocalS1Colors.current.noticeAt

/** 消息中心未读竖条色（默认=强调色；赤红/机器猫红底覆盖为白） */
val NoticeUnread: Color
    @Composable get() = LocalS1Colors.current.noticeUnread

/** PM 列表未读小红点（默认=热帖红；赤红/机器猫覆盖为白） */
val PmUnreadDot: Color
    @Composable get() = LocalS1Colors.current.pmUnreadDot

/** 消息中心列表面板底色（默认=页面底；机器猫覆盖为卡片蓝，避免全红界面） */
val ListPanel: Color
    @Composable get() = LocalS1Colors.current.listPanel

/** 分类栏/筛选 chips「选中胶囊」底色（默认=强调色；机器猫覆盖为蓝，避免红叠红） */
val PillTint: Color
    @Composable get() = LocalS1Colors.current.pillTint

/** 卡内行间发丝（默认=textMain 12% 中性淡线；机器猫蓝卡面覆盖为白 14%） */
val RowLine: Color
    @Composable get() = LocalS1Colors.current.rowLine

/** 卡片/面板外缘相框发丝（默认透明不画；机器猫覆盖为列缝灰，把蓝卡从红页里切出来） */
val EdgeLine: Color
    @Composable get() = LocalS1Colors.current.edgeLine
