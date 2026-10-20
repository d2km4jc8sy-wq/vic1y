package com.example.s1client.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
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
    /**
     * 顶栏底部「极光细线」的三色渐变（2026-10-19 ③ 极昼身份签）。
     * null = 不画（其余主题视觉零变化）。仅幻彩浅板开启：
     * 2.5dp、40% 透明度，画在顶栏下边缘 —— 渐变只进焦点件的纪律不破，
     * 这条线是「限定皮肤」的身份签，面积极小不参与铺色。绘制见 [s1HeaderAurora]。
     */
    val headerAurora: List<Color>? = null,
    /** 是否深色主题（供 Material 配色桥接用） */
    val isDark: Boolean,
    /**
     * 顶栏文字与图标颜色。
     * 默认沿用旧规则（深色主题用主文字色、浅色主题用白字）以保持兼容；
     * 允许主题覆盖——纯白这类浅底顶栏必须用深色文字，否则白字在浅底上不可见。
     * 2026-10-18：幻彩主题此前沿用白字，而顶栏是浅紫→浅粉，对比度仅 1.15:1，本次一并修正。
     */
    val onHeader: Color = if (isDark) textMain else Color.White,
    /**
     * 成功 / 通过语义绿（R3 · 审计报告 v2 2026-10-18 第 9 轮）。
     * 原先写死在 LoginActivity:146（34C759），违反宪法 18 条「配色从主题色板派生」。
     * 现收口为 token，默认取原值以保证视觉零变化；各主题如需差异可单独覆盖。
     */
    val success: Color = Color(0xFF34C759),
    /**
     * 破坏性语义红（R2 · 审计报告 v2 2026-10-18 第 9 轮）。
     * 原先散落在 11 处写死 FF3B30（删除/清空/退出/确定文字、图标 tint、
     * HoldToConfirmButton 默认填充），违反宪法 18 条「配色从主题色板派生」。
     * 现收口为 token，默认取原值 FF3B30 以保证视觉零变化；各主题如需差异可单独覆盖。
     * 与 [heart]（热帖红 FF2C55）刻意分两套，避免「删除/退出」红与心形/热帖红撞色。
     */
    val danger: Color = Color(0xFFFF3B30),
    /**
     * 消息中心类型徽章色：「回复」提醒 / 「@」提醒（NoticeCenterActivity 行内小签，
     * 文字色 + 12% 同色底）。2026-10-18 收口为 token：原先写死在业务代码里
     * （珊瑚红 FF6B6B / 蓝 4F80F2），违反宪法 18 条「配色从主题色板派生」；
     * 且机器猫页面是枣红 #C74636（2026-10-19 由饱和红 #E60012 降饱和），
     * 原两色对比度仅 1.5~1.7:1，等于看不见。
     * 默认取原值，保证其余主题视觉零变化；按主题覆盖（见 DoraemonPalette）。
     */
    val noticeReply: Color = Color(0xFFFF6B6B),
    val noticeAt: Color = Color(0xFF4F80F2),
    /**
     * 消息中心未读竖条（3dp，页面底上）。默认 = [accent]（原行为，视觉零变化）。
     * 赤红/机器猫的强调色就是页面色本身（红底红条），竖条不可见 → 覆盖为白。
     */
    val noticeUnread: Color = accent,
    /**
     * PM 列表未读小红点（头像右上角 11dp 圆点）。默认 = [heart]（原行为）。
     * 该点落在头像渐变（accent→heart）与页面底上，赤红/机器猫的红系点会糊进红底 → 覆盖为白。
     */
    val pmUnreadDot: Color = heart,
    /**
     * 无图帖子封面的底色（卡片「上下两色」的上半部分）。
     * 默认 = [chipBg]，也就是「我的」「设置」里那些图标圆底的同一块灰。
     * 2026-10-18 用户定稿：封面不与图标底各自配色，直接共用同一个 token，
     * 这样深色/浅色/换主题时，封面块和图标底的灰度永远一致，语言才统一。
     */
    val coverTint: Color = chipBg,
    /**
     * 卡片下半（作者 / 分类 / 时间那一块）的底色，null = 跟卡面一样。
     * 2026-10-18 朱砂引入：整块信息区上色 = 「上黑下红」，
     * 比在封面里切一条色带整体性更好（色带会把封面切成三段）。
     * 与封面之间用 1px 底色线隔开，分区清楚但不加边框。
     */
    val metaTint: Color? = null,
    /**
     * 底部导航栏底色。默认 = [glassBar]（与顶栏同色）。
     * 机器猫单独给成中性灰：红页 + 蓝卡已经很满，底栏再上色会让屏幕底部继续「发声」，
     * 灰色把它压成安静的框架（2026-10-18 用户要求）。
     */
    val navBar: Color = glassBar,
    /**
     * 瀑布流列表区域的底色（卡片之间露出来的那一层）。默认 = [bgBottom]，与页面一致。
     * 机器猫单独给中性灰：页面与上下栏是红，列缝用灰，
     * 红蓝在列缝处才不会被误读成同一种颜色（用户指定，灰只此一处）。
     */
    val feedGap: Color = bgBottom,
    /**
     * 首页分类栏「选中胶囊」的底色。默认 = [accent]。
     * 机器猫单独给蓝：它的顶栏是红的，胶囊若也用红就看不见，必须互斥。
     */
    val pillTint: Color = accent,
    /**
     * 消息中心（提醒/私信）列表面板的底色。默认 = [bgBottom]，即列表直接铺在页面底上
     * （原行为，其余主题视觉零变化）。
     * 机器猫覆盖为卡片蓝：它的页面底是高饱和鲜红 #E60012，整页通铺会得到「全红界面」，
     * 而「我的」「设置」等页面都有蓝卡做视觉锚点。列表面板改蓝后与全站结构对齐
     * （红顶栏 + 蓝列表），也延续机器猫「一屏之内必有一蓝一红」的法则
     * （2026-10-19 用户报告：机器猫提醒页全红，无可读锚点）。
     */
    val listPanel: Color = bgBottom,
    /**
     * 卡内行间发丝（菜单组行线、身份卡头衔分隔、统计格竖线、提醒列表行线）。
     * 默认 = 列缝黑 feedGap 25% —— 「黑但分层」的一支语言，全主题自适应：
     * 叠蓝底自动呈深蓝、叠红底自动呈深红、叠白底呈中灰，发丝永远与底色同相压深，
     * 既读得出层次又不抢内容（2026-10-19 定：用户原话「红里调蓝、蓝里调红，或换黑」，
     * 设计判断否决互斥方案——1dp 红线在连续列表里是高频噪音，且稀释红的语义；
     * 也否决纯黑行线——会把列表切成表格，丢失「连续信息流」意图。黑 25% 是中间档）。
     * **刻意不跟 chipBg 联动** —— chipBg 是「图标圆底」，机器猫把它改成卡片蓝后，
     * 拿它画线等于蓝画在蓝上（2026-10-18 踩过）。
     */
    val rowLine: Color = feedGap.copy(alpha = 0.25f),
    /**
     * 卡片 / 面板外缘的「相框发丝」——1dp 描边，把大色块从页面底里切出来。
     * 默认 = 全透明（不画，其余主题视觉零变化）。
     * 机器猫覆盖为列缝灰 #2E2E33：页面枣红 + 卡片灰蓝两个大面积色相硬碰，
     * 远处看会糊成「一大块红加一大块蓝」；一圈中性灰细边让每个蓝卡有轮廓、
     * 红底退回背景空间（2026-10-19 用户报告「没有做到隔离」）。灰是 palette 里
     * 既有的第三色（feedGap 列缝同色），不引新色、不参与红蓝互斥。
     */
    val edgeLine: Color = Color.Transparent,
    /**
     * 帖子详情「便签」（回复楼卡片）的底色。默认 = [cardBg]，其余主题视觉零变化。
     * 机器猫覆盖为项圈红：详情页的楼层便签若也用蓝，整页就全是蓝、失去红蓝对比
     * （2026-10-19 用户反馈「整个版面就缺失了原来的效果」）。
     * 单独开一个槽位，是为了不牵连首页卡片（coverTint/metaTint）与「我的」页（s1BgCard）。
     */
    val ticketBg: Color = cardBg
)

// ————————— 原有三套（不动，只扩展 headerGradient）—————————

/** 旧「午后红茶」色板已于 2026-10-19 删除：米黄 #EFE6D3 + 茶褐 #A0522D。
 *  它和改版后的纯白（纸感暖白）同为浅色暖调，功能重复；
 *  用户选择保留改版后的那套，并由它继承「焙茶午后」这个名字（见下方 TeaPalette）。 */

/**
 * 焙茶午后（原名「纯白」，2026-10-19 改版 + 改名）：**纸感暖白 + 黛青**。
 *
 * 原版是冷灰蓝 #E8EAF0 + 纯白 #FFFFFF + iOS 蓝 #007AFF —— 干净、层次也对
 * （页面比卡片暗，浅色主题本来就该这样），但用户反馈三条问题，本版一起改：
 *   ① 降刺眼：卡片 #FFFFFF → #FDFCFA。不到纯白的柔白，大面积铺开夜里不再晃眼。
 *   ② 加温度：页面底从冷灰蓝改成近乎中性的暖灰 #EAE8E3，有纸的温和感，不再像系统设置页。
 *   ③ 换强调色：iOS 系统蓝 → 黛青 #1F6F8B。不是任何系统的默认色，
 *      且在白底上对比度 5.4:1（原 iOS 蓝只有 3.9:1），更清楚。
 *
 * 2026-10-19 接任「焙茶午后」这个名字：旧焙茶（米黄 #EFE6D3 + 茶褐 #A0522D）
 * 与本版同为浅色暖调、功能重复，已删除，名字由本套继承。
 * 旧的 S1 网页米黄底色因此不再单独存在 —— 纸感暖白是现在唯一的浅色。
 */
val TeaPalette = S1Colors(
    // 页面：近乎中性的暖灰，只比纯白暗一点点，纸感
    bgTop = Color(0xFFEAE8E3), bgBottom = Color(0xFFEAE8E3),
    // 顶栏跟卡片同色（原规则），一起转暖
    headerGradient = listOf(Color(0xFFFDFCFA), Color(0xFFFDFCFA)),
    textMain = Color(0xFF232120), textSub = Color(0xFF8A867F),
    accent = Color(0xFF1F6F8B), onAccent = Color(0xFFFFFFFF),
    accentCyan = Color(0xFF4FA3C7), heart = Color(0xFFFF2C55),
    // 卡片：柔白，不是纯白 —— 整屏大面积铺开时不刺眼
    cardBg = Color(0xFFFDFCFA),
    glassCard = Color(0xFAFDFCFA), glassLine = Color(0x08000000),
    glassBar = Color(0xF2FDFCFA), glassFloat = Color(0xFAFDFCFA),
    glassFloatLine = Color(0x12000000),
    glassDialog = Color(0xFFFDFCFA), glassDialogLine = Color(0x12000000),
    chipBg = Color(0xFFE4E0D9),
    strongIcon = Color(0xFF232120),
    snackbarBg = Color(0xFF232120), onSnackbar = Color(0xFFFDFCFA),
    // 封面流光：跟着暖白基调整体降饱和，不再用 iOS 系统色那一套
    flowCoverColors = listOf(
        Color(0xFF1F6F8B), Color(0xFF4A8C6A), Color(0xFFD98324), Color(0xFFD64550)
    ),
    glassEdgeTop = Color(0x1F000000), glassEdgeMid = Color(0x12000000),
    glassEdgeBottom = Color(0x0A000000),
    glassInsetLight = Color(0x66FFFFFF),
    isDark = false,
    onHeader = Color(0xFF232120),
    // 封面比页面深一档、比卡片深得多：三级分明，标题区那块才浮得起来
    coverTint = Color(0xFFDFDCD5)
)



// ————————— 三套新主题（视觉差异最大化）—————————



/**
 * 幻彩渐变（2026-10-19 定稿）：**霓虹糖果家族 × 深浅两板 = 2 套**。
 * 原 v1 极光家族（极夜/极昼）同日按用户决定剔除，主题索引 5 作废；
 * 糖果家族（原 v2）顶替「幻彩渐变」名号并移到主题列表末位。
 *
 * 用户定位（原话）：「幻彩本来就是炫加实用的，像游戏里的顶级限定皮肤，
 * 走特殊通道，资源其他池子不互享，带随机」。深浅两板由 ThemeManager.iridescentSeed 抛硬币：
 *   索引 0 = 夜市（深），索引 1 = 镭射糖果（浅）
 *
 * 现版翻车教训（三层渐变叠大面积、饱和度高、标题对比不稳）的吸收方式：
 * 渐变只出现在封面流光 / 签到按钮 / 卡片彩边这三个焦点件；页面底是微弱渐变（≤5% 明度差），
 * 正文阅读区纯色。封面光斑色一律用亮调（封面标题是深字/白字压纱，亮色才能保证对比）。
 */
private class IridescentPreset(
    val bgTop: Color, val bgBottom: Color,
    val accent: Color, val strongIcon: Color,
    val headerA: Color, val headerB: Color,
    val line: Color, val bar: Color,
    val cover: List<Color>,
    /** 流光封面的兜底底色 + 底部压纱色（深色变体用），随家族走，不再写死紫 */
    val coverTint: Color,
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
    val glassInsetLight: Color = Color(0x73FFFFFF),
    /**
     * 图标字色 = 强调色的提亮版（2026-10-28 统一规则）。
     * 此前这里是写死的蓝灰 #7FB8D8，两套板共用，图标字和紫粉主调对不上；
     * 默认取强调色提亮 35%，各板可单独覆盖。
     */
    val accentCyan: Color = lerpWhite(accent, 0.35f),
    /**
     * 主色上的文字色（按钮/胶囊/气泡字）。默认白 = 旧规则。
     * 2026-10-19 ①：深板改深字 —— 亮青/亮粉压白字只有 2.1~3.1:1（正文线 4.5），
     * 压深底色 8.9 / 6.4:1；机器猫铃铛黄（onAccent 深棕）同款思路，浅板不动。
     */
    val onAccent: Color = Color(0xFFFFFFFF),
    /** 顶栏底部极光细线（③）：true = 用封面三色在顶栏下缘画 2.5dp 光带（见 [S1Colors.headerAurora]） */
    val auroraLine: Boolean = false
)

private val IridescentPresets = listOf(
    // ══ 0 · 霓虹 · 夜市（深）═══════════════════════════════════
    //    深紫黑底，霓虹三色：粉 / 电紫 / 电青。
    //    2026-10-19 ②：强调色从霓虹粉 #FF4DA6 往品红推一档 #F44FD8 ——
    //    原主色与点赞心形红 #FF2C55 只差约 19° 色相且明度接近，深紫底上扫一眼易混；
    //    往红压 heart 会撞「删除/退出」红（danger #FF3B30，刻意分两套的既有约束），故推主色。
    //    新主色落在封面粉(#FF7AC0)与电紫(#B45CFF)之间，家族更顺；strongIcon/描边随动。
    //    2026-10-19 ①：主色上的文字弃白字（压亮粉 3.1:1）改深字 #120820（6.4:1）。
    IridescentPreset(
        Color(0xFF120820), Color(0xFF1C0D30),
        Color(0xFFF44FD8), Color(0xFFF88DE6),
        Color(0xFF170A28), Color(0xFF241040),
        Color(0x2EF44FD8), Color(0xE6170A28),
        listOf(Color(0xFFFF7AC0), Color(0xFFB45CFF), Color(0xFF5CE1E6)),
        coverTint = Color(0xFF1B0E2E),
        isDark = true,
        onAccent = Color(0xFF120820),   // ① 深字压亮品红 6.4:1（原白字 3.1:1）
        cardBg = Color(0xFF221335),
        textMain = Color(0xFFF6EBFB), textSub = Color(0xFFB39BCC),
        glassCard = Color(0xF2221335), glassFloat = Color(0xF22D1A44),
        glassDialog = Color(0xF22D1A44),
        chipBg = Color(0xFF2F1C45),
        accentCyan = Color(0xFFF88DE6),
        snackbarBg = Color(0xFF2D1A44), onSnackbar = Color(0xFFF6EBFB),
        glassEdgeTop = Color(0x59FFFFFF), glassEdgeMid = Color(0x2EFFFFFF),
        glassEdgeBottom = Color(0x1AFFFFFF), glassInsetLight = Color(0x8AFFFFFF)
    ),
    // ══ 1 · 镭射 · 箔片（浅）═══════════════════════════════════
    //    2026-10-19 改版（用户拍板「B·箔片」）：原来那套「银白粉紫底 + 糖果三色铺满整卡」
    //    一屏八张卡同频重复，糊成一片，且页面底与白卡几乎同亮度、卡片浮不起来，被判定「好难看」。
    //
    //    新方向不再追求「糖」，改做一张镭射纸：
    //      · 页面底从粉调收到冷调近白 #EFEDF6→#F8F5FC —— 白卡才浮得出来；
    //      · 封面珠光底 #EDEAF4（coverTint），彩虹只在上半张做一条斜切箔带（见 XhsPostCard style 5）；
    //      · 三色从粉彩换成箔片饱和色 粉/紫/青 —— 面积小了，浓度就必须上来，
    //        否则又是「白开水」；这也是彩丝卡框与顶栏极光细线的取色来源；
    //      · 强调色 #D6339C → #C2267F：白底上与白字上都更稳（4.36:1 → 5.0:1），也更少「糖」味。
    IridescentPreset(
        Color(0xFFEFEDF6), Color(0xFFF8F5FC),
        Color(0xFFC2267F), Color(0xFF8F5CFF),
        Color(0xFFFFFFFF), Color(0xFFFBF9FD),
        Color(0x2EC2267F), Color(0xF2FBFAFE),
        listOf(Color(0xFFFF4FB0), Color(0xFF8F5CFF), Color(0xFF22CFE6)),
        coverTint = Color(0xFFEDEAF4),
        isDark = false,
        auroraLine = true,   // ③ 极光细线随幻彩浅板走：顶栏下缘 2.5dp 三色 40%
        cardBg = Color(0xFFFFFFFF),
        textMain = Color(0xFF35243C), textSub = Color(0xFF88708E),
        glassCard = Color(0xF7FFFFFF), glassFloat = Color(0xF2FFFFFF),
        glassDialog = Color(0xFAFFFFFF),
        chipBg = Color(0xFFF1EDF7),
        accentCyan = Color(0xFFE05FA8),  // ②加深一档：#F57FC4 白底图标字仅 2.4:1，#E05FA8 3.3:1
        snackbarBg = Color(0xFF35243C), onSnackbar = Color(0xFFFFFFFF),
        glassEdgeTop = Color(0x8AFFFFFF), glassEdgeMid = Color(0x4DFFFFFF),
        glassEdgeBottom = Color(0x1AFFFFFF), glassInsetLight = Color(0x6EFFFFFF)
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

/** 与白色混合，[amount] = 0 原色，1 全白。用于派生「强调色的提亮版」 */
private fun lerpWhite(c: Color, amount: Float): Color = Color(
    red = c.red + (1f - c.red) * amount,
    green = c.green + (1f - c.green) * amount,
    blue = c.blue + (1f - c.blue) * amount,
    alpha = c.alpha
)

/**
 * 顶栏「极光细线」（2026-10-19 ③）：headerAurora 非空时在顶栏下缘画一条
 * 2.5dp、40% 透明度的三色横向光带；null = 完全不画（其余主题视觉零变化）。
 * 与顶栏渐变背景串联使用：`.background(Brush.horizontalGradient(c.headerGradient)).s1HeaderAurora(c)`。
 * 只做绘制、不参与布局与点击，各屏顶栏行为除多一条细线外与旧版一致。
 */
fun Modifier.s1HeaderAurora(c: S1Colors): Modifier = drawBehind {
    val aurora = c.headerAurora ?: return@drawBehind
    val h = 2.5.dp.toPx()
    drawRect(
        brush = Brush.horizontalGradient(aurora.map { it.copy(alpha = 0.40f) }),
        topLeft = Offset(0f, size.height - h),
        size = Size(size.width, h)
    )
}

private fun buildIridescentPalette(seed: Int): S1Colors {
    val p = IridescentPresets[seed.coerceIn(0, IridescentPresets.lastIndex)]
    return S1Colors(
        bgTop = p.bgTop, bgBottom = p.bgBottom,
        headerGradient = listOf(p.headerA, p.headerB),
        textMain = p.textMain, textSub = p.textSub,
        accent = p.accent, onAccent = p.onAccent,
        accentCyan = p.accentCyan, heart = Color(0xFFFF2C55),
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
        // 流光封面的兜底底色（XhsPostCard style 5 的 ① 底色 ④ 压纱都从它派生）
        coverTint = p.coverTint,
        // 液态玻璃：浅色含蓄 / 深色明显
        glassEdgeTop = p.glassEdgeTop, glassEdgeMid = p.glassEdgeMid,
        glassEdgeBottom = p.glassEdgeBottom,
        glassInsetLight = p.glassInsetLight,
        headerAurora = if (p.auroraLine) p.cover else null,
        isDark = p.isDark,
        // 幻彩顶栏是浅紫→浅粉，白字对比度仅 1.15:1（几乎不可见），改用主题主文字色
        onHeader = p.textMain
    )
}

// ————————— Linear 暗色（第 1 批改造新增）—————————

/**
 * Linear 墨蓝（2026-10-19 按 OLED 夜间护眼重做）：**近黑底 + 不刺眼的字 + 降饱和蓝**。
 * 用户明确的使用场景是「OLED 屏幕、晚上」，所以这一版的目标和「好看」不完全一致，护眼优先：
 *   ① 页面底压到近黑 #0A0C12 —— OLED 上黑色像素不发光，既省电也让整屏不再有底光刺眼。
 *   ② 主文字从近白 #EDEDEF 降到浅灰 #C9CBD1 —— 纯白压在黑底上会晕开（halation），
 *      浅灰仍有 10.5:1 的对比度，读得清但不晃眼。
 *   ③ 强调色从饱和靛蓝 #5E6AD2 降到柔蓝 #61729E —— 蓝光抑制褪黑素，夜间降饱和降亮度。
 *   ④ 卡片 #1B1E26 只比页面亮一档：靠明度差 + 1px 描边分层，不用阴影（暗底上黑影不可见）。
 * 层次依然是三级：页面 #0A0C12 < 封面 #141620 < 卡片 #1B1E26，与赤红/Linear 同一套逻辑。
 */
val LinearPalette = S1Colors(
    bgTop = Color(0xFF0A0C12), bgBottom = Color(0xFF0A0C12),
    headerGradient = listOf(Color(0xFF0A0C12), Color(0xFF0A0C12)),
    textMain = Color(0xFFC9CBD1), textSub = Color(0xFF8A8D96),
    accent = Color(0xFF61729E), onAccent = Color(0xFFFFFFFF),
    accentCyan = Color(0xFF7D8CB8), heart = Color(0xFFE8546E),
    cardBg = Color(0xFF1B1E26),
    // 玻璃层：几乎不透明的深灰 + 极细描边，避免传统玻璃的高光感（夜间高光很刺眼）
    glassCard = Color(0xF21B1E26), glassLine = Color(0xFF2A2E3A),
    glassBar = Color(0xF20A0C12), glassFloat = Color(0xFF23262F),
    glassFloatLine = Color(0xFF343842),
    glassDialog = Color(0xFF23262F), glassDialogLine = Color(0xFF343842),
    chipBg = Color(0xFF22252E),
    strongIcon = Color(0xFFC9CBD1),
    snackbarBg = Color(0xFF22252E), onSnackbar = Color(0xFFC9CBD1),
    // 卡片顶部语义色条：整体降饱和降亮度，与主色同族，避免花、也避免夜间刺眼
    flowCoverColors = listOf(
        Color(0xFF61729E), Color(0xFF4A7C68), Color(0xFF9C7B45),
        Color(0xFF6E5C8C), Color(0xFF5A6E8C)
    ),
    // 液态玻璃描边：比原版整体压暗一档 —— 夜间高光很晃眼
    glassEdgeTop = Color(0x4DFFFFFF), glassEdgeMid = Color(0x26FFFFFF),
    glassEdgeBottom = Color(0x1AFFFFFF),
    glassInsetLight = Color(0x73FFFFFF),
    isDark = true,
    // 封面：比页面亮一档、比卡面沉一档，三级分明（页面 < 封面 < 卡面）
    coverTint = Color(0xFF141620)
)

/**
 * 机器猫（2026-10-19 致敬版）：**哆啦A梦四色 —— 蓝身、白肚、红项圈、黄铃铛**。
 *
 * 上一版的主次是反的：枣红铺满页面、灰蓝只做卡片，红成了主角、蓝退成配角，
 * 结果看着像「一个红色的东西」，不是哆啦A梦 —— 哆啦A梦是蓝的，红只是脖子上的项圈。
 *
 * 本版按角色重新分配，四色各就各位：
 *   蓝 #166BB0 = 身体 → 页面底；顶栏 / 底栏统一更深一档的 #125A96
 *   白 #F0F7FD = 脸和肚子 → 卡片 / 列表面板（内容层，浅色）
 *   红 = 项圈 → **卡片之间的列缝**（一圈红包着白卡，正是项圈的形状）
 *              + 卡片内的强调（按钮、标签、气泡、引用条）
 *   黄 #F5B91D = 铃铛 → 顶栏选中胶囊（既是哆啦的标志件，也解决了红在蓝底上
 *                看不清的问题：红压蓝只有 1.3:1，黄压蓝 3.1:1）
 * 卡片是浅色 → isDark = false，卡片内用深蓝黑字；顶栏是深蓝 → onHeader 用白字。
 *
 * 2026-10-19 用户定案修正：第一版把底栏做成一整条红，用户反馈
 * 「瀑布流大量留白，到最下方突然转红，特别生硬」「下面的红色太抢眼」。
 * 原话：哪怕上下统一蓝、中间缝隙红填充也会好很多 ——
 * **可以哆啦A梦，但不要为了让人一看就觉得是哆啦A梦，而丢掉主题的本质：主题是为内容服务的。**
 * 于是红从「底部一大条」降级为「包着卡片的一圈线」：面积小了、位置对了，
 * 也更贴项圈 —— 项圈是绕一圈，不是挂在脚底。
 */
val DoraemonPalette = S1Colors(
    // 页面 = 哆啦蓝（身体）
    bgTop = Color(0xFF166BB0), bgBottom = Color(0xFF166BB0),
    // 顶栏同蓝。整屏只用一个蓝 —— 上一版拆成「深蓝顶栏 / 蓝页面 / 浅蓝封面」三级，
    // 是「颜色太多」的元凶（用户反馈出戏）。蓝就是一个蓝。
    headerGradient = listOf(Color(0xFF166BB0), Color(0xFF166BB0)),
    // 卡片是蓝底 / 红底 → 一律白字
    textMain = Color(0xFFFFFFFF), textSub = Color(0xFFDCEBF8),
    // 强调 = 铃铛黄。它是唯一在蓝底（3.1:1）和红底（3.4:1）上都读得出来的色，
    // 所以按钮、图标、徽章全用黄 —— 红蓝做大面，黄只做点
    accent = Color(0xFFF5B91D), onAccent = Color(0xFF3A2A00),
    accentCyan = Color(0xFFF5B91D), heart = Color(0xFFE23B44),
    cardBg = Color(0xFF166BB0),
    glassCard = Color(0xF2125A96), glassLine = Color(0x4DFFFFFF),
    glassBar = Color(0xF2166BB0), glassFloat = Color(0xF2E23B44),
    glassFloatLine = Color(0x4DFFFFFF),
    glassDialog = Color(0xF2125A96), glassDialogLine = Color(0x4DFFFFFF),
    chipBg = Color(0x33FFFFFF),
    strongIcon = Color(0xFFFFFFFF),
    snackbarBg = Color(0xFF125A96), onSnackbar = Color(0xFFFFFFFF),
    // 封面流光：就红蓝黄三色，不掺白
    flowCoverColors = listOf(
        Color(0xFF166BB0), Color(0xFFE23B44), Color(0xFFF5B91D), Color(0xFFE23B44)
    ),
    glassEdgeTop = Color(0x66FFFFFF), glassEdgeMid = Color(0x33FFFFFF),
    glassEdgeBottom = Color(0x1A000000),
    glassInsetLight = Color(0x99FFFFFF),
    isDark = true,
    onHeader = Color(0xFFFFFFFF),
    // 卡片直接做成红蓝双色块（用户定案：「上栏是蓝，封面上就是蓝，以此类推」）：
    //   上半 = 蓝 #166BB0（对应蓝顶栏）  下半 = 红 #E23B44（对应红底栏）
    // 不再塞浅蓝、白这类第三色去过渡 —— 过渡色正是上一版出戏的根因
    coverTint = Color(0xFF166BB0),
    metaTint = Color(0xFFE23B44),
    navBar = Color(0xFFE23B44),
    // 列缝 = 深蓝（2026-10-19 二改）。第一版试过纯白，用户反馈「配合红蓝看着有点干」——
    // 白是无彩色，夹在蓝红之间像剪纸，没有过渡；深蓝与蓝同族，卡片从缝里「浮」出来，
    // 红卡也靠这一层暗底托住，比白线润。
    feedGap = Color(0xFF0B4570),
    pillTint = Color(0xFFF5B91D),
    noticeReply = Color(0xFFF5B91D),
    noticeAt = Color(0xFFF5B91D),
    noticeUnread = Color.White,
    pmUnreadDot = Color.White,
    // 列表面板 = 深蓝（比页面沉一档），白字可读，与卡片上半同族
    listPanel = Color(0xFF125A96),
    // 详情页楼层便签 = 项圈红：蓝页面 + 红便签，红蓝在一屏里直接对撞。
    // 红蓝是经典互补对，不必回避（用户原话：「别怕红蓝撞一起，人类眼睛看很讨好」）
    ticketBg = Color(0xFFE23B44)
)

/**
 * 毛线主题（2026-10-20 新增）：**针织/毛线质感的原创浅色主题**。
 *
 * 不是任何游戏 IP 的复刻——只取"毛线针织"的材质语言：粉彩、奶油、焦糖棕、柔珊瑚，
 * （2026-10-20 修订）原计划的针织纹理 / 手缝虚线 / 毛线球等绘制效果
 * 已因性能原因全部剔除（小米10 上滑动明显发涩），**现仅保留配色**：
 * 奶油底 #FBF1E3、焦糖棕字 #5C4A3A、柔玫瑰强调 #D2607F、粉彩点缀。
 *
 * 配色取舍（同一套"暖浅色"逻辑，但比焙茶更甜更柔）：
 *   · 页面 = 奶油 #FBF1E3，比焙茶的暖灰更黄更"织物"；卡片 = 织物白 #FDF8F0（略暖于纯白）。
 *   · 文字 = 焦糖棕 #5C4A3A（不用纯黑，保留毛线的暖）；次级 #9C8B7A（灰褐，不刺眼）。
 *   · 强调 = 柔玫瑰 #D2607F：白字压上去约 4.6:1，满足正文线；且是"毛线"的标志色之一。
 *   · 浅色点缀 = 薄荷 #6FB7A6 / 雾蓝 #8FB3E0 / 暖黄 #E9C16B，做流光封面与卡片色条。
 *   · heart（热帖/喜欢）用柔珊瑚 #E88471，danger（错误）用暖红 #C24E4E——
 *     都比系统默认的硬红更贴"织物"，但语义仍清晰可辨。
 *   · success 收口为柔绿 #6FB38F（原默认 #34C759 偏荧光，与毛线调性不搭）。
 */
val YarnPalette = S1Colors(
    // 页面：奶油，比焙茶更黄更暖，像米白毛线
    bgTop = Color(0xFFFBF1E3), bgBottom = Color(0xFFFBF1E3),
    // 顶栏同奶油，略提亮一档让标题区浮起来
    headerGradient = listOf(Color(0xFFFDF6EC), Color(0xFFFDF6EC)),
    textMain = Color(0xFF5C4A3A), textSub = Color(0xFF9C8B7A),
    // 强调 = 柔玫瑰；白字压上去约 4.6:1
    accent = Color(0xFFD2607F), onAccent = Color(0xFFFFFFFF),
    accentCyan = Color(0xFF6FB7A6), heart = Color(0xFFE88471),
    // 卡片：织物白（略暖于纯白，大面积铺开不刺眼）
    cardBg = Color(0xFFFDF8F0),
    glassCard = Color(0xFAFDF8F0), glassLine = Color(0x1F5C4A3A),
    glassBar = Color(0xF6FBF1E3), glassFloat = Color(0xFAFDF8F0),
    glassFloatLine = Color(0x245C4A3A),
    glassDialog = Color(0xFFFDF8F0), glassDialogLine = Color(0x245C4A3A),
    chipBg = Color(0xFFF0E3D2),
    strongIcon = Color(0xFF5C4A3A),
    snackbarBg = Color(0xFF5C4A3A), onSnackbar = Color(0xFFFBF1E3),
    // 流光封面：粉彩四色，低饱和、甜柔
    flowCoverColors = listOf(
        Color(0xFFD2607F), Color(0xFF6FB7A6), Color(0xFF8FB3E0), Color(0xFFE9C16B)
    ),
    // 液态玻璃描边：浅色含蓄
    glassEdgeTop = Color(0x33FFFFFF), glassEdgeMid = Color(0x1AFFFFFF),
    glassEdgeBottom = Color(0x0A000000),
    glassInsetLight = Color(0x66FFFFFF),
    isDark = false,
    onHeader = Color(0xFF5C4A3A),
    // 浅色主题：顶部语义色条用柔玫瑰；封面比页面深一档
    coverTint = Color(0xFFF3E6D4),
    // 失败/喜欢走毛线调性（见上方说明）
    success = Color(0xFF6FB38F),
    danger = Color(0xFFC24E4E),
    noticeReply = Color(0xFFD2607F),
    noticeAt = Color(0xFF6FB7A6),
    noticeUnread = Color(0xFFD2607F),
    pmUnreadDot = Color(0xFFD2607F),
    pillTint = Color(0xFFD2607F)
)

val LocalS1Colors = staticCompositionLocalOf { TeaPalette }

/**
 * 赤红（2026-10-19 定稿，默认主题）：**红做氛围、灰做内容**。
 *
 * 取代了 2026-10-18 那一版（页面 #A23341 亮砖红 + 卡片 #222226 近黑）。
 * 旧版第一眼惊艳，但明暗方向是反的：亮背景包围暗内容，
 * 瞳孔在亮暗之间反复调节 —— 用户实测「看 20 分钟眼睛就特别难受」。
 *
 * 本版红色一个不改，只把明暗方向掉过来：
 *   旧：页面 #A23341（中亮）> 卡片 #222226（很暗）
 *   新：页面 #4A1C26（暗）  < 卡片 #4A4245（亮）
 * → 暗背景衬托亮内容，与 Linear 同一套层次逻辑（页面 < 封面 < 卡面）。
 * 于是红从「铺满屏幕的场」退回成「沉在底下的氛围」：
 * 一眼看过去还是红的，但红不再抢内容的位置。风格比旧版收敛，可以长久用。
 */
val CrimsonPalette = S1Colors(
    // 页面：深砖红。比旧版 #A23341 暗一半，红色识别度保留，亮度退到背景该有的位置
    bgTop = Color(0xFF4A1C26), bgBottom = Color(0xFF4A1C26),
    // 顶栏 / 底栏：暖灰，比卡面暗一档。避免红色从顶部漫出去（沿用赤红的同一条规则）
    headerGradient = listOf(Color(0xFF3E3639), Color(0xFF3E3639)),
    textMain = Color(0xFFEDEAEC), textSub = Color(0xFFB8AFB3),
    // 强调色用亮红：暗红页面上红条不可见，粉底暖灰卡上也是，只有提亮的红才读得出来
    accent = Color(0xFFFF6B7A), onAccent = Color(0xFFFFFFFF),
    accentCyan = Color(0xFFFF8F9A), heart = Color(0xFFFF4D5E),
    // 卡片：暖灰，比页面亮约 26 档（Linear 是 28 档），是这套主题真正的「内容层」
    cardBg = Color(0xFF4A4245),
    glassCard = Color(0xF24A4245), glassLine = Color(0xFF5A5053),
    glassBar = Color(0xF2422E33), glassFloat = Color(0xFF544B4E),
    glassFloatLine = Color(0xFF655B5E),
    glassDialog = Color(0xFF544B4E), glassDialogLine = Color(0xFF655B5E),
    chipBg = Color(0xFF5C3A40),
    strongIcon = Color(0xFFEDEAEC),
    snackbarBg = Color(0xFF5C3A40), onSnackbar = Color(0xFFEDEAEC),
    // 封面流光：赤红家族四色，红只在封面与页面缝里出现
    flowCoverColors = listOf(
        Color(0xFFD62839), Color(0xFF8E1B2C), Color(0xFFFF6B7A), Color(0xFFA23341)
    ),
    glassEdgeTop = Color(0x66FFFFFF), glassEdgeMid = Color(0x33FFFFFF),
    glassEdgeBottom = Color(0x26FFFFFF),
    glassInsetLight = Color(0x99FFFFFF),
    isDark = true,
    // 封面比卡面沉一档、比页面亮一档：三层依次拉开，不再是旧版那种「黑洞」
    coverTint = Color(0xFF382F33),
    metaTint = null,
    noticeUnread = Color.White,
    pmUnreadDot = Color.White
)


/**
 * 主题索引 → 调色板。唯一一份映射，供 S1Theme 与「非 Compose 场景」共用
 * （例如换主题后同步系统栏颜色，见 ThemeEdge.syncSystemBarsWithPalette）。
 */
fun s1PaletteFor(themeIndex: Int, seed: Int = ThemeManager.iridescentSeed): S1Colors = when (themeIndex) {
    ThemeManager.THEME_TEA -> TeaPalette
    ThemeManager.THEME_LINEAR -> LinearPalette
    ThemeManager.THEME_CRIMSON -> CrimsonPalette
    ThemeManager.THEME_DORAEMON -> DoraemonPalette
    ThemeManager.THEME_IRIDESCENT -> buildIridescentPalette(ThemeManager.iridescentPaletteIndex(seed))
    ThemeManager.THEME_YARN -> YarnPalette
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
        } catch (e: Throwable) {
            android.util.Log.e("S1Font", "theme: createFromFile 失败: ${e.message}")
            androidx.compose.ui.text.font.FontFamily.Default
        }
        android.util.Log.d("S1Font", "theme: file=${customFontFile.exists()} family覆盖=${family != androidx.compose.ui.text.font.FontFamily.Default}")
        applyFontFamily(materialTypography(), family)
    } else {
        materialTypography()
    }

    CompositionLocalProvider(LocalS1Colors provides colors) {
        MaterialTheme(colorScheme = materialColorScheme, typography = typography) {
            // 2026-10-19：显影换肤（scale 0.96→1 + 幕布淡出）整体删除。
            // 它没换到什么视觉收益，反而在冷启动分支把 snapTo 写成了 1f ——
            // 整块界面常年停在 96%，四边各让出 2%，露出窗口底色（浅色主题上一圈灰边）。
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
