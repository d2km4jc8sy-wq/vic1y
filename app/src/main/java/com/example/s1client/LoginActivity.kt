package com.example.s1client

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowInsetsControllerCompat
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.ui.design.S1Radius
import com.example.s1client.data.AccountHistory
import com.example.s1client.data.HistoryAccount
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.data.session.SessionRepository
import com.example.s1client.ui.theme.LocalS1Colors
import com.example.s1client.ui.theme.OledPalette
import com.example.s1client.ui.theme.S1Theme
import com.example.s1client.util.avatarUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 登录页。
 *
 * 颜色全部来自 LocalS1Colors —— 跟随首页主题（午后红茶 / OLED / 粉色佳人 / FC / RP5 / 幻彩），
 * 不再是独立的一套深色，避免进登录页时色彩落差。
 *
 * 版面（自上而下三段）：
 *   ① 中心大圆 —— 一颗会互动的「角色球」：
 *      · 主体是主题色渐变球（若 res/drawable 放入 s1_mascot.png 会自动改用图片，
 *        再放 s1_mascot_shy.png 则密码态自动换捂眼图）
 *      · 偶尔眨眼；输入用户名时低头"看"输入框；输密码时两只小手捂住眼睛
 *      · 外面一圈旋转细光环 + 环绕的小光点（小而淡，朦胧感）
 *   ② Stage1st 社区 + 副标题
 *   ③ 唯一一颗登录胶囊（玻璃底）
 *
 * 胶囊状态机：① 用户名 → ② 密码（← 返回 / 显示-隐藏切换）→
 *   （仅当账号设了安全提问时）③ 安全提问答案 → ④ 成功（✓ + 彩屑 + 自动收尾）。
 * 字段集合与校验文案对齐 S1er：只有 用户名 / 密码 / 安全提问，没有邮箱、没有忘记密码。
 * 登录为真实接口：getForumIndex() 取 formhash → S1Api.login(...) → 写入 ForumDataCache 并持久化。
 * 本页只负责「登录」这一件事，不再有已登录/退出登录的中间界面。
 */
class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        setContent {
            S1Theme {
                // 登录页跟随全局主题：原来的 LoginDarkPalette 覆盖已去掉。
                // 背景渐变 / 光斑 / 光点 / 角色球都读 LocalS1Colors，浅色主题自动变浅底。
                val c = LocalS1Colors.current
                SideEffect {
                    // 系统栏图标明暗跟着主题底色走（浅底深色图标 / 深底浅色图标）
                    controller.isAppearanceLightStatusBars = !c.isDark
                    controller.isAppearanceLightNavigationBars = !c.isDark
                }
                LoginScreen(onFinish = {
                    setResult(Activity.RESULT_OK)
                    finish()
                })
            }
        }
    }
}

/**
 * 成功绿：R3 修复（审计报告 v2 2026-10-18 第 9 轮）——原为本地写死常量，
 * 现改为从主题 token [com.example.s1client.ui.theme.S1Colors.success] 取，
 * 默认值与原来一致（34C759），故视觉零变化，但配色来源已收敛到色板。
 */
private val CpnGreenSuccess: Color
    @Composable get() = LocalS1Colors.current.success

// 注：原先这里有一套写死的深层 LoginDarkPalette，用 CompositionLocalProvider 覆盖 S1Theme，
// 让登录页固定走暖暗色调。现已移除 —— 登录页改为跟随全局主题（浅色主题就是浅底），
// 装饰元素（光斑/光点/角色球描边）按 isDark 做了明暗自适应补偿。

// ===== 装饰元素的数据类 =====

private data class BokehDot(val fx: Float, val fy: Float, val r: Float, val a: Float, val color: Color)

private data class AuroraBlob(val fx: Float, val fy: Float, val r: Float, val color: Color)

private data class OrbitDot(
    val ring: Int,
    val angle: Float,
    val radius: Float,
    val size: Float,
    val color: Color,
    val glass: Boolean
)

private data class ConfettiParticle(
    val color: Color,
    val angle: Float,
    val distance: Float,
    val size: Float,
    val delayMs: Int
)

@Composable
private fun LoginScreen(onFinish: () -> Unit) {
    val c = LocalS1Colors.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var celebrate by remember { mutableStateOf(false) }
    var compact by remember { mutableStateOf(false) }   // 输入框获焦 → 紧凑版面
    var step by remember { mutableStateOf(1) }          // 由 CapsuleLogin 回报
    val accounts = remember { AccountHistory.getAll(ctx) }
    var selected by remember { mutableStateOf(accounts.firstOrNull()) }
    var picked by remember { mutableStateOf(selected?.username ?: "") }
    var pickerOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
    ) {
        // 背景：大块朦胧光晕 + 淡散景小点
        BokehBackdrop()

        val heroSize = 148.dp   // 保持大圆一致：不再随键盘弹起缩小，消除大↔小球突兀切换

        // 键盘弹起时窗口高度会被压缩（各 ROM/输入法差异很大），
        // 所以紧凑态一律「顶部排列 + requiredHeightIn 保底」，完全不依赖 inset。
        val columnModifier = if (compact) {
            Modifier
                .fillMaxWidth()
                .requiredHeightIn(min = 480.dp)
                .padding(horizontal = 28.dp)
                .padding(top = 40.dp)
        } else {
            Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
        }

        Column(
            modifier = columnModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (compact) Arrangement.Top else Arrangement.Center
        ) {
            // ① 角色球（输入用户名时低头看，输密码时捂眼）
            Box(contentAlignment = Alignment.Center) {
                if (!compact) OrbitRings()
                HeroCircle(
                    size = heroSize,
                    lookDown = compact && step == 1,
                    shy = step == 2 || step == 3,
                    selectedAccount = selected,
                    accountsNotEmpty = accounts.isNotEmpty(),
                    onClick = { if (accounts.isNotEmpty()) pickerOpen = true }
                )
            }

            Spacer(Modifier.height(if (compact) 14.dp else 26.dp))

            // ② 标题
            Text(
                "Stage1st 社区",
                fontSize = if (compact) 24.sp else 30.sp,
                fontWeight = FontWeight.Bold,
                color = c.textMain,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(if (compact) 22.dp else 44.dp))

            // ③ 登录区：唯一一颗胶囊
            CapsuleLogin(
                onFocusChange = { compact = it },
                onStepChange = { step = it },
                onSuccess = {
                    celebrate = true
                    scope.launch {
                        delay(1500)
                        onFinish()
                    }
                },
                pickedUsername = picked
            )
        }

        // 历史账号切换面板（仿 QQ/微信 点头像切换）
        if (pickerOpen) {
            AccountPickerDialog(
                accounts = accounts,
                selected = selected,
                onPick = { acc ->
                    selected = acc
                    picked = acc?.username ?: ""
                    pickerOpen = false
                }
            )
        }

        // 登录成功特效
        if (celebrate) ConfettiBurst()
    }
}

// ================= 历史账号切换面板（仿 QQ/微信 点头像切换） =================

@Composable
private fun AccountPickerDialog(
    accounts: List<HistoryAccount>,
    selected: HistoryAccount?,
    onPick: (HistoryAccount?) -> Unit
) {
    val c = LocalS1Colors.current
    Dialog(
        onDismissRequest = { onPick(selected) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable { onPick(selected) },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(c.cardBg)
                    .border(1.dp, c.glassLine, RoundedCornerShape(24.dp))
                    .clickable { /* 吃掉点击，避免穿透关闭 */ }
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    "选择账号",
                    color = c.textSub,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                accounts.forEach { acc ->
                    val isSelected = selected?.uid == acc.uid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) c.accent.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { onPick(acc) }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = avatarUrl(acc.uid, "small"),
                            contentDescription = acc.username,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(c.chipBg),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            acc.username,
                            color = if (isSelected) c.accent else c.textMain,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Text("✓", color = c.accent, fontSize = 15.sp)
                        }
                    }
                }
                // 使用其他账号：清空选择，回到默认吉祥物
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPick(null) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(c.chipBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "+",
                            color = c.textSub,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        "使用其他账号",
                        color = c.textSub,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// ================= 背景：朦胧光晕 + 淡散景荧光点 =================

@Composable
private fun BokehBackdrop() {
    val c = LocalS1Colors.current
    val palette = c.flowCoverColors
    // 浅色主题上低透明度的彩色光点几乎看不见，按底色明暗补偿一档
    val deco = if (c.isDark) 1f else 1.6f

    // 几团大而极淡的光晕，铺出"朦胧"的底子
    val blobs = remember(palette) {
        List(4) {
            AuroraBlob(
                fx = Random.nextFloat(),
                fy = Random.nextFloat(),
                r = 130f + Random.nextFloat() * 120f,
                color = palette.random()
            )
        }
    }
    val dots = remember(palette) {
        List(40) {
            BokehDot(
                fx = Random.nextFloat(),
                fy = Random.nextFloat(),
                r = 1.2f + Random.nextFloat() * 2.6f,
                a = 0.05f + Random.nextFloat() * 0.11f,
                color = palette.random()
            )
        }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = maxWidth.value
        val h = maxHeight.value
        blobs.forEach { b ->
            Box(
                modifier = Modifier
                    .offset(x = (w * b.fx - b.r).dp, y = (h * b.fy - b.r).dp)
                    .size((b.r * 2).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(b.color.copy(alpha = 0.10f * deco), Color.Transparent)
                        )
                    )
            )
        }
        dots.forEach { d ->
            Box(
                modifier = Modifier
                    .offset(x = (w * d.fx).dp, y = (h * d.fy).dp)
                    .size(d.r.dp)
                    .clip(CircleShape)
                    .background(d.color)
                    .alpha(d.a * deco)
            )
        }
    }
}

// ================= 角色球 =================

@Composable
private fun HeroCircle(
    size: Dp,
    lookDown: Boolean,
    shy: Boolean,
    selectedAccount: HistoryAccount? = null,
    accountsNotEmpty: Boolean = false,
    onClick: () -> Unit = {}
) {
    val c = LocalS1Colors.current
    val inf = rememberInfiniteTransition()
    // 呼吸
    val breathe by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    // 外圈细光环缓慢自转
    val spin by inf.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Restart)
    )

    // —— 互动状态 ——
    val blink = remember { Animatable(1f) }      // 1 睁眼 → 0.1 闭眼
    val look = remember { Animatable(0f) }       // 0 平视 → 1 低头看输入框

    LaunchedEffect(lookDown) {
        look.animateTo(if (lookDown) 1f else 0f, tween(280, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(shy) {
        while (true) {
            delay(Random.nextLong(2200, 5200))
            if (!shy) {
                blink.animateTo(0.12f, tween(70))
                blink.animateTo(1f, tween(90))
            }
        }
    }

    val glowSize = size * 1.15f
    val ringSize = size * 0.77f
    val orbSize = size * 0.62f

    // 吉祥物图片位：res/drawable 放入 s1_mascot.png（睁眼）与 s1_mascot_shy.png（捂眼）
    // 自动启用两图切换 —— 同一人物、两种动作（密码态换捂眼图）。
    val ctx = LocalContext.current
    val mascotId = remember {
        ctx.resources.getIdentifier("s1_mascot", "drawable", ctx.packageName).takeIf { it != 0 }
    }
    val mascotShyId = remember {
        ctx.resources.getIdentifier("s1_mascot_shy", "drawable", ctx.packageName).takeIf { it != 0 }
    }

    Box(
        modifier = Modifier,
        contentAlignment = Alignment.Center
    ) {
        // 柔光晕（主题主色）
        Box(
            modifier = Modifier
                .size(glowSize + (breathe * 12).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            c.accent.copy(alpha = 0.22f),
                            c.accent.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .alpha(0.55f + breathe * 0.25f)
        )
        // 主圆：暗色玻璃球（与页面背景同系，去掉白色环、融入背景，不再"浮"在白圆里）
        Box(
            modifier = Modifier
                .size(size)
                .shadow(24.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(c.bgTop.copy(alpha = 0.72f), c.bgBottom.copy(alpha = 0.72f))
                    )
                )
                .border(
                    1.dp,
                    if (c.isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.07f),
                    CircleShape
                )
        )
        // 旋转细光环
        Box(
            modifier = Modifier
                .size(ringSize)
                .rotate(spin)
                .border(
                    2.dp,
                    Brush.sweepGradient(
                        listOf(Color.Transparent, c.accentCyan.copy(alpha = 0.8f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        // —— 内部：角色（历史头像优先；无历史/选其他账号时显示吉祥物）——
        val orbModifier = if (accountsNotEmpty) {
            Modifier
                .size(orbSize)
                .clickable { onClick() }
        } else {
            Modifier.size(orbSize)
        }
        Box(
            modifier = orbModifier,
            contentAlignment = Alignment.Center
        ) {
            // 底层圆盘：与页面背景一致，透明头像/吉祥物镂空处自然融入
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
            )
            if (selectedAccount != null) {
                // 已选历史账号：头像直接替换角色球（QQ/微信 风格）
                AsyncImage(
                    model = avatarUrl(selectedAccount.uid, "middle"),
                    contentDescription = selectedAccount.username,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else if (mascotId != null) {
                // 吉祥物：默认睁眼图；密码态自动切换为捂眼图（s1_mascot_shy.png）
                val painterId = if (shy && mascotShyId != null) mascotShyId else mascotId
                Image(
                    painter = painterResource(painterId),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 主题色渐变球 + 眼睛
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    c.accentCyan.copy(alpha = 0.95f),
                                    c.accent,
                                    c.accent.copy(alpha = 0.65f)
                                )
                            )
                        )
                )
                // 眼睛（眨眼 / 低头看）
                val eyeW = orbSize * 0.15f
                val eyeH = orbSize * 0.19f * blink.value
                val pupilY = eyeH * 0.24f * look.value
                Row(
                    modifier = Modifier.offset(y = (-orbSize * 0.06f)),
                    horizontalArrangement = Arrangement.spacedBy(orbSize * 0.10f)
                ) {
                    listOf(true, false).forEach { left ->
                        Box(
                            modifier = Modifier.size(eyeW, eyeH.coerceAtLeast(1.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // 眼白
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.94f))
                            )
                            // 瞳孔
                            Box(
                                modifier = Modifier
                                    .size(eyeW * 0.52f)
                                    .offset(y = pupilY)
                                    .clip(CircleShape)
                                    .background(Color(0xFF232833))
                            )
                        }
                    }
                }
            }
        }

    }
}

// ================= 环绕的小光点（小而淡，朦胧） =================

@Composable
private fun OrbitRings() {
    val c = LocalS1Colors.current
    val palette = c.flowCoverColors
    // "玻璃点"要有对比才看得见：深色底用白、浅色底用黑
    val glassTint = if (c.isDark) Color.White else Color.Black
    val deco = if (c.isDark) 1f else 1.7f
    val inf = rememberInfiniteTransition()
    val r0 by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart))
    val r1 by inf.animateFloat(360f, 0f, infiniteRepeatable(tween(44000, easing = LinearEasing), RepeatMode.Restart))
    val r2 by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(62000, easing = LinearEasing), RepeatMode.Restart))
    val rots = listOf(r0, r1, r2)

    val dots = remember(palette) {
        val out = mutableListOf<OrbitDot>()
        val rings = listOf(11 to 106f, 13 to 134f, 15 to 164f)
        rings.forEachIndexed { ri, (count, radius) ->
            for (i in 0 until count) {
                out.add(
                    OrbitDot(
                        ring = ri,
                        angle = (360f / count) * i + Random.nextFloat() * 14f,
                        radius = radius + Random.nextFloat() * 10f - 5f,
                        size = 1.3f + Random.nextFloat() * 1.9f,
                        color = palette.random(),
                        glass = Random.nextFloat() < 0.32f
                    )
                )
            }
        }
        out
    }

    Box(modifier = Modifier.size(344.dp), contentAlignment = Alignment.Center) {
        dots.forEach { d ->
            val rad = Math.toRadians((d.angle + rots[d.ring]).toDouble())
            val x = (cos(rad) * d.radius).dp
            val y = (sin(rad) * d.radius).dp
            val depth = 1f - d.ring * 0.26f   // 越外圈越淡
            Box(
                modifier = Modifier
                    .offset(x = x, y = y)
                    .size((d.size * 3.4f).dp),
                contentAlignment = Alignment.Center
            ) {
                // 柔光晕
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(d.color)
                        .alpha((if (d.glass) 0.05f else 0.09f) * depth * deco)
                )
                if (d.glass) {
                    Box(
                        modifier = Modifier
                            .size(d.size.dp)
                            .clip(CircleShape)
                            .background(glassTint.copy(alpha = 0.10f))
                            .border(0.8.dp, glassTint.copy(alpha = 0.34f), CircleShape)
                            .alpha(depth)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(d.size.dp)
                            .clip(CircleShape)
                            .background(d.color)
                            .alpha(0.48f * depth)
                    )
                }
            }
        }
    }
}

// ================= 登录区：唯一一颗胶囊 =================

/** Discuz 登录安全提问选项（与论坛语言包一致，对齐 S1er）。id 即 POST 的 questionid，0 = 未设置。 */
private val SecurityQuestions = listOf(
    0 to "未设置",
    1 to "母亲的名字",
    2 to "爷爷的名字",
    3 to "父亲出生的城市",
    4 to "您其中一位老师的名字",
    5 to "您个人计算机的型号",
    6 to "您最喜欢的餐馆名称",
    7 to "驾驶执照最后四位数字"
)

/**
 * 登录区：唯一一颗胶囊（玻璃底）。
 *
 * 字段集合对齐 S1er —— 只有 用户名 / 密码 / 安全提问，**没有邮箱、没有忘记密码**。
 * 步骤：1 用户名 → 2 密码 →（仅当服务端要求时）3 安全提问答案 → 4 成功。
 * 账号 / 密码 / 答案各自独立存放，成功态不会残留（更不会明文回显）密码。
 */
@Composable
private fun CapsuleLogin(
    onFocusChange: (Boolean) -> Unit,
    onStepChange: (Int) -> Unit,
    onSuccess: () -> Unit,
    pickedUsername: String = ""
) {
    val c = LocalS1Colors.current
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var step by remember { mutableStateOf(1) }             // 1 用户名 / 2 密码 / 3 安全提问 / 4 成功
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }            // 当前步骤输入框里的值
    var questionId by remember { mutableStateOf(0) }
    var pickerOpen by remember { mutableStateOf(false) }   // 安全提问选择面板
    var pwdVisible by remember { mutableStateOf(false) }   // 密码明文 / 密文
    var isBusy by remember { mutableStateOf(false) }       // 请求中（防重复）
    var btnState by remember { mutableStateOf("normal") }  // normal / error / success
    var toast by remember { mutableStateOf("") }
    var toastType by remember { mutableStateOf("") }
    var toastKey by remember { mutableStateOf(0) }
    var formhash by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val shakeX = remember { Animatable(0f) }

    LaunchedEffect(step) { onStepChange(step) }

    // 历史账号选择：回填用户名并聚焦；选"其他账号"时清空
    LaunchedEffect(pickedUsername) {
        text = pickedUsername
        if (pickedUsername.isNotBlank()) {
            step = 1
            focus.requestFocus()
        }
    }

    // 进页面就静默预取 formhash，点登录时不用再等一次请求
    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) { RetrofitClient.api.getForumIndex().variables?.formhash }
        }.getOrNull()?.let { if (it.isNotBlank()) formhash = it }
    }

    // 切步骤：先把当前输入写回对应字段，再载入目标步骤的内容
    fun goto(target: Int) {
        when (step) {
            1 -> account = text
            2 -> password = text
            3 -> answer = text
        }
        text = when (target) {
            1 -> account
            2 -> password
            3 -> answer
            else -> ""
        }
        step = target
        btnState = "normal"
    }

    fun showToast(msg: String, type: String) {
        toast = msg
        toastType = type
        toastKey++
    }
    LaunchedEffect(toastKey) {
        if (toastKey > 0 && step != 4) {
            delay(2600)
            toast = ""
            toastType = ""
        }
    }
    fun shake() {
        scope.launch {
            listOf(-8f, 8f, -6f, 6f, -4f, 4f, 0f).forEach { shakeX.animateTo(it, tween(45)) }
        }
    }

    fun doLogin() {
        isBusy = true
        btnState = "normal"
        val name = account
        val pwd = password
        val qid = questionId
        val ans = answer
        scope.launch {
            try {
                var fh = formhash
                if (fh.isBlank()) {
                    fh = withContext(Dispatchers.IO) {
                        RetrofitClient.api.getForumIndex().variables?.formhash.orEmpty()
                    }
                    formhash = fh
                }
                val resp = withContext(Dispatchers.IO) {
                    RetrofitClient.api.login(
                        username = name,
                        password = pwd,
                        formhash = fh,
                        questionid = qid.toString(),
                        answer = ans
                    )
                }
                val vs = resp.variables
                val uid = vs?.memberUid
                if (!uid.isNullOrEmpty() && uid != "0") {
                    // 成功：写入缓存并持久化（写点收敛进 SessionRepository，阶段二）
                    SessionRepository.adoptLogin(
                        uid = uid,
                        username = vs.memberUsername?.takeIf { it.isNotBlank() } ?: name,
                        formhash = vs.formhash?.takeIf { it.isNotBlank() } ?: fh,
                        credits = vs.memberCredits ?: "0",
                        groupId = vs.groupId ?: "",
                        groupTitle = vs.group?.groupTitle ?: ""
                    )
                    // 记录历史登录账号（头像由 uid 动态生成，不落盘）
                    AccountHistory.add(ctx, ForumDataCache.memberUid, ForumDataCache.memberUsername)

                    // 成功态：输入框里什么敏感内容都不留
                    password = ""
                    answer = ""
                    text = ""
                    step = 4
                    btnState = "success"
                    isBusy = false
                    delay(1200)
                    onSuccess()
                } else {
                    val raw = resp.message?.messageStr.orEmpty()
                    isBusy = false
                    btnState = "error"
                    text = ""
                    shake()
                    val needQuestion = step == 2 && (raw.contains("安全提问") || raw.contains("提问"))
                    if (needQuestion) {
                        // 该账号设了安全提问 → 追加一步：选提问 + 填答案
                        password = pwd
                        showToast("该账号设置了安全提问，请选择并填写答案", "info")
                        goto(3)
                        focus.requestFocus()
                    } else {
                        showToast(
                            raw.takeIf { it.isNotBlank() && it.length < 40 } ?: "用户名或密码错误",
                            "error"
                        )
                    }
                    delay(1400)
                    if (btnState == "error") btnState = "normal"
                }
            } catch (e: Exception) {
                isBusy = false
                btnState = "error"
                text = ""
                shake()
                showToast("网络异常，请稍后重试", "error")
                delay(1400)
                if (btnState == "error") btnState = "normal"
            }
        }
    }

    fun handleAction() {
        if (isBusy || step == 4) return
        val v = text.trim()
        when (step) {
            1 -> {
                if (v.isBlank()) {
                    shake()
                    showToast("请输入用户名", "error")
                    return
                }
                account = v
                goto(2)
                focus.requestFocus()
            }
            2 -> {
                if (text.isBlank()) {
                    shake()
                    showToast("请输入密码", "error")
                    return
                }
                password = text          // 密码不 trim，原样提交
                doLogin()
            }
            3 -> {
                if (questionId != 0 && v.isBlank()) {
                    shake()
                    showToast("请填写安全提问的答案", "error")
                    return
                }
                answer = v
                doLogin()
            }
        }
    }

    val placeholder = when (step) {
        1 -> "请输入用户名"
        2 -> "请输入密码"
        3 -> "请输入答案"
        else -> ""
    }
    val btnText = when {
        step == 4 -> "✓"
        isBusy -> "登录中..."
        step == 1 -> "下一步"
        else -> "登录"
    }
    val disabled = isBusy || step == 4
    val btnColor = when (btnState) {
        "error" -> c.heart
        "success" -> CpnGreenSuccess
        else -> if (disabled) c.chipBg else c.accent
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // ===== 安全提问（仅当账号设置了安全提问时才出现）=====
        if (step == 3) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(S1Radius.pill))
                    .background(c.chipBg)
                    .clickable(enabled = !isBusy) { pickerOpen = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("安全提问 · ", color = c.textSub, fontSize = 12.5.sp)
                Text(
                    SecurityQuestions.first { it.first == questionId }.second,
                    color = c.accent,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(6.dp))
                Text("▾", color = c.textSub, fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
        }

        // ===== 唯一胶囊（玻璃底）=====
            Box(modifier = Modifier.offset(x = shakeX.value.dp)) {
                // 细节 06：聚焦时胶囊边框变主题双色渐变 + 微微加粗（外圈柔光用低透明描边叠加）
                var fieldFocused by remember { mutableStateOf(false) }
                val focusBrush = if (fieldFocused) Brush.linearGradient(
                    listOf(
                        c.flowCoverColors.getOrElse(1) { c.accent },
                        c.accent
                    )
                ) else SolidColor(c.glassLine)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    // 控件 06：聚焦外圈柔光（三层递减描边，画在裁剪外才可见）
                    .drawBehind {
                        if (fieldFocused) {
                            listOf(0.20f, 0.10f, 0.05f).forEachIndexed { i, a ->
                                drawRoundRect(
                                    color = c.accent.copy(alpha = a),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = (4f + i * 3f) * density
                                    )
                                )
                            }
                        }
                    }
                    .shadow(18.dp, RoundedCornerShape(S1Radius.pill))
                    .clip(RoundedCornerShape(S1Radius.pill))
                    .background(c.cardBg.copy(alpha = 0.66f))
                    .border(if (fieldFocused) 1.5.dp else 1.dp, focusBrush, RoundedCornerShape(S1Radius.pill))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：返回箭头 + 输入框 +（密码步骤）显示/隐藏
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step == 2 || step == 3) {
                        Text(
                            "←",
                            fontSize = 20.sp,
                            color = c.textSub,
                            modifier = Modifier.clickable(enabled = !isBusy) {
                                goto(if (step == 3) 2 else 1)
                                focus.requestFocus()
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        enabled = step != 4 && !(step == 3 && questionId == 0),
                        textStyle = TextStyle(color = c.textMain, fontSize = 16.sp),
                        visualTransformation = if (step == 2 && !pwdVisible) PasswordVisualTransformation()
                        else VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (step == 2) KeyboardType.Password else KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { handleAction() }),
                        decorationBox = { inner ->
                            Box(
                                Modifier.fillMaxWidth().fillMaxHeight(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (step == 4) {
                                    Text("登录成功", color = CpnGreenSuccess, fontSize = 15.sp, maxLines = 1)
                                } else if (text.isEmpty()) {
                                    Text(
                                        if (step == 3 && questionId == 0) "未设置安全提问（无需答案）" else placeholder,
                                        color = c.textSub,
                                        fontSize = 15.sp,
                                        maxLines = 1
                                    )
                                } else inner()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focus)
                            .onFocusChanged {
                                onFocusChange(it.isFocused)
                                fieldFocused = it.isFocused
                            }
                    )
                    if (step == 2) {
                        Spacer(Modifier.width(8.dp))
                        // 显示/隐藏密码：睁眼 = 当前明文可见，闭眼带斜杠 = 已遮码
                        Icon(
                            imageVector = if (pwdVisible) Icons.Outlined.Visibility
                            else Icons.Outlined.VisibilityOff,
                            contentDescription = if (pwdVisible) "隐藏密码" else "显示密码",
                            tint = c.textSub,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { pwdVisible = !pwdVisible }
                        )
                    }
                }

                // 右侧：内嵌胶囊按钮
                Box(
                    modifier = Modifier
                        .height(52.dp)
                        .clip(RoundedCornerShape(S1Radius.pill))
                        .background(btnColor)
                        .clickable(enabled = !disabled) { handleAction() }
                        .padding(horizontal = 26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        btnText,
                        color = when (btnState) {
                            "success", "error" -> Color.White
                            else -> if (disabled) c.textSub else c.onAccent
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        // 固定高度提示槽，避免 toast 出现/消失时布局跳动
        Box(modifier = Modifier.height(20.dp), contentAlignment = Alignment.Center) {
            if (toast.isNotEmpty()) {
                Text(
                    toast,
                    color = when (toastType) {
                        "error" -> c.heart
                        "success" -> CpnGreenSuccess
                        else -> c.textSub
                    },
                    fontSize = 12.5.sp,
                    maxLines = 1
                )
            }
        }
    }

    // ===== 安全提问选择面板 =====
    if (pickerOpen) {
        Dialog(
            onDismissRequest = { pickerOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .clickable { pickerOpen = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(c.cardBg)
                        .border(1.dp, c.glassLine, RoundedCornerShape(24.dp))
                        .clickable { /* 吃掉点击，避免穿透关闭 */ }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        "选择安全提问",
                        color = c.textSub,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                    SecurityQuestions.forEach { (id, label) ->
                        val selected = id == questionId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) c.accent.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable {
                                    questionId = id
                                    pickerOpen = false
                                    if (id == 0) text = ""
                                    focus.requestFocus()
                                }
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                label,
                                color = if (selected) c.accent else c.textMain,
                                fontSize = 15.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (selected) Text("✓", color = c.accent, fontSize = 15.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// 密码「显示 / 隐藏」图标已改为 Material Icons（Icons.Outlined.Visibility / VisibilityOff），
// 实现位于 CapsuleLogin 内（step == 2 分支），不再使用手绘 Canvas 版本。

// ================= 登录成功彩屑特效 =================

@Composable
private fun ConfettiBurst() {
    val c = LocalS1Colors.current
    val palette = c.flowCoverColors
    val particles = remember(palette) {
        List(34) {
            ConfettiParticle(
                color = palette.random(),
                angle = Random.nextFloat() * 360f,
                distance = 70f + Random.nextFloat() * 130f,
                size = 4f + Random.nextFloat() * 4f,
                delayMs = Random.nextInt(0, 220)
            )
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(1200))
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        particles.forEach { p ->
            val t = ((progress.value - p.delayMs / 1200f) / (1f - p.delayMs / 1200f)).coerceIn(0f, 1f)
            if (t > 0f) {
                val rad = Math.toRadians(p.angle.toDouble())
                val x = (sin(rad) * p.distance * t).dp
                val y = (cos(rad) * p.distance * t + 60 * t * t).dp
                Box(
                    modifier = Modifier
                        .offset(x = x, y = y)
                        .size(p.size.dp)
                        .clip(RoundedCornerShape(50))
                        .background(p.color)
                        .alpha(1f - t)
                )
            }
        }
    }
}
