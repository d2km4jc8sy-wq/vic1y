package com.example.s1client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.model.ForumDataCache
import com.example.s1client.ui.HoldToConfirmButton
import com.example.s1client.ui.S1SystemBarSync
import com.example.s1client.ui.SunMoonSwitch
import com.example.s1client.ui.ThemePickerSheet
import com.example.s1client.ui.ThemeSwatch
import com.example.s1client.ui.theme.LocalS1Colors
import com.example.s1client.ui.theme.S1Theme
import com.example.s1client.util.BlockedAuthorManager
import com.example.s1client.util.DataSaverStore
import com.example.s1client.util.Haptic
import com.example.s1client.util.ReadHistoryStore
import com.example.s1client.util.S1Notify
import com.example.s1client.util.SettingStore
import com.example.s1client.util.ThemeManager
import com.example.s1client.util.applyS1Theme
import com.example.s1client.util.dirSize
import com.example.s1client.util.formatSize
import com.example.s1client.util.imageCacheDir
import com.example.s1client.util.setupS1EdgeToEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 设置中心（对标 s1er settings_screen 十组架构，去重后七组）：
 *   外观（主题/文字大小/字体） · 交互（震动/省流） · 发帖（小尾巴） ·
 *   屏蔽（版块屏蔽/用户黑名单） · 数据管理（清历史/清图片缓存/重置外观） · 关于 · 退出登录
 *
 * 与既有功能去重：QuickActionsSheet 的省流/清缓存/关于/退出登录全部并入本页，
 * 「我的」页齿轮只进本页；黑名单管理从原快捷弹窗移入「屏蔽」组。
 *
 * 图标铁律：单线条 outlined 图标，统一 strongIcon 色。
 */
class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applyS1Theme()
        super.onCreate(savedInstanceState)
        setupS1EdgeToEdge()
        setContent {
            S1Theme {
                SettingsScreen(onFinish = { finish() })
            }
        }
    }
}

@Composable
fun SettingsScreen(onFinish: () -> Unit) {
    val c = LocalS1Colors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    S1SystemBarSync()

    var showThemeSheet by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showForumBlock by remember { mutableStateOf(false) }
    var showBlockedAuthors by remember { mutableStateOf(false) }
    var showClearHistory by remember { mutableStateOf(false) }
    var showResetAppearance by remember { mutableStateOf(false) }
    var showTailEdit by remember { mutableStateOf(false) }

    val loggedIn = ForumDataCache.isLoggedIn()

    // —— 可变设置状态（即时生效、即时持久化，无「保存」按钮，对标 s1er）——
    var hapticEnabled by remember { mutableStateOf(Haptic.enabled) }
    val dataSaver by DataSaverStore.enabled.collectAsState()
    var tail by remember { mutableStateOf(SettingStore.postTail(context)) }
    var fontSize by remember { mutableIntStateOf(SettingStore.fontSize(context)) }
    var fontName by remember { mutableStateOf(SettingStore.customFontName(context)) }
    var hiddenForums by remember { mutableStateOf(SettingStore.hiddenForumIds(context)) }
    var cacheText by remember { mutableStateOf("计算中…") }

    LaunchedEffect(Unit) {
        cacheText = withContext(Dispatchers.IO) { formatSize(dirSize(imageCacheDir(context))) }
    }

    // 字体导入选择器（导入后全局立即生效，见 S1Theme 的 settingsVersion 监听）
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            SettingStore.importFont(context, uri)
                .onSuccess {
                    fontName = it
                    S1Notify.show(context, "字体已导入：$it", S1Notify.Type.SUCCESS)
                }
                .onFailure {
                    S1Notify.show(context, it.message ?: "导入失败", S1Notify.Type.ERROR)
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
            .verticalScroll(rememberScrollState())
    ) {
        // —— 顶栏 ——
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(c.headerGradient))
                .padding(top = 48.dp, bottom = 10.dp, start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onFinish) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null,
                    tint = if (c.isDark) c.textMain else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                "设置", fontSize = 17.sp, fontWeight = FontWeight.Bold,
                color = if (c.isDark) c.textMain else Color.White,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(48.dp))
        }

        // ═══ 外观 ═══
        GroupTitle("外观")
        Group {
            SettingRow(
                Icons.Outlined.Palette, "主题",
                onClick = { showThemeSheet = true }
            ) {
                ThemeSwatch(ThemeManager.currentTheme, size = 22.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    themeName(), fontSize = 12.sp, color = c.textSub,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Chevron()
            }

            SettingRow(Icons.Outlined.FormatSize, "文字大小") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SettingStore.FONT_SIZES.forEach { size ->
                        val selected = size == fontSize
                        Text(
                            when (size) {
                                12 -> "小"; 14 -> "标准"; 16 -> "大"; else -> "超大"
                            },
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.White else c.textSub,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) c.accent.copy(alpha = 0.85f) else c.chipBg)
                                .clickable {
                                    fontSize = size
                                    SettingStore.setFontSize(context, size)
                                }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            SettingRow(
                Icons.Outlined.TextFields, "字体",
                sub = fontName ?: "默认字体",
                onClick = { fontPicker.launch(arrayOf("*/*")) }
            ) {
                Chevron()
            }

            if (fontName != null) {
                SettingRow(
                    Icons.Outlined.Refresh, "恢复默认字体",
                    sub = "移除自定义字体",
                    onClick = {
                        SettingStore.removeCustomFont(context)
                        fontName = null
                        S1Notify.show(context, "已恢复默认字体", S1Notify.Type.INFO)
                    }
                ) {}
            }
        }

        // ═══ 交互 ═══
        GroupTitle("交互")
        Group {
            SettingRow(Icons.Outlined.Vibration, "交互震动", sub = "全局触觉反馈") {
                SunMoonSwitch(
                    checked = hapticEnabled,
                    onCheckedChange = {
                        Haptic.setEnabled(context, it)
                        hapticEnabled = it
                        // 对标 s1er：开关自身切换给一次反馈
                        if (it) Haptic.selection(context)
                    }
                )
            }

            SettingRow(Icons.Outlined.DataUsage, "省流模式", sub = "图片不自动加载，点按才载入") {
                SunMoonSwitch(
                    checked = dataSaver,
                    onCheckedChange = { DataSaverStore.set(context, it) }
                )
            }
        }

        // ═══ 发帖 ═══
        GroupTitle("发帖")
        Group {
            SettingRow(
                Icons.Outlined.Edit, "发帖小尾巴",
                sub = if (tail.enabled) "发帖 / 回复 / 编辑提交时自动追加" else "已关闭"
            ) {
                SunMoonSwitch(
                    checked = tail.enabled,
                    onCheckedChange = {
                        tail = SettingStore.postTail(context).let { old ->
                            SettingStore.setPostTail(context, it, old.showDevice, old.custom)
                            SettingStore.postTail(context)
                        }
                    }
                )
            }

            if (tail.enabled) {
                // 去重（用户拍板）：机型/前缀/预览收进一个二级弹层，主列表只留一行
                SettingRow(
                    null, "小尾巴内容",
                    sub = "机型 · 前缀 · 预览",
                    onClick = { showTailEdit = true }
                ) {
                    Chevron()
                }
            }
        }

        // ═══ 屏蔽 ═══
        GroupTitle("屏蔽")
        Group {
            SettingRow(
                Icons.Outlined.Block, "版块屏蔽",
                sub = if (hiddenForums.isEmpty()) "不屏蔽任何版块" else "已屏蔽 ${hiddenForums.size} 个版块",
                onClick = { showForumBlock = true }
            ) {
                Chevron()
            }

            val blockedCount = BlockedAuthorManager.allBlocked().size
            SettingRow(
                Icons.Outlined.Person, "用户黑名单",
                sub = if (blockedCount == 0) "未屏蔽任何用户" else "已屏蔽 $blockedCount 位用户",
                onClick = { showBlockedAuthors = true }
            ) {
                Chevron()
            }
        }

        // ═══ 数据管理 ═══
        GroupTitle("数据管理")
        Group {
            SettingRow(
                Icons.Outlined.History, "清除阅读历史",
                sub = "本地最近浏览的帖子记录",
                onClick = { showClearHistory = true }
            ) {}

            SettingRow(
                Icons.Outlined.Delete, "清除图片缓存",
                sub = cacheText,
                onClick = {
                    scope.launch {
                        val cleared = withContext(Dispatchers.IO) {
                            val dir = imageCacheDir(context)
                            val size = dirSize(dir)
                            dir.deleteRecursively()
                            size
                        }
                        cacheText = formatSize(0L)
                        S1Notify.show(context, "已清除图片缓存", S1Notify.Type.SUCCESS)
                    }
                }
            ) {}

            SettingRow(
                Icons.Outlined.Refresh, "重置外观偏好",
                sub = "字号/字体恢复默认，不影响版块屏蔽与历史",
                onClick = { showResetAppearance = true }
            ) {}
        }

        // ═══ 关于 ═══
        GroupTitle("关于")
        Group {
            SettingRow(
                Icons.Outlined.Info, "关于",
                sub = "S1 Client · V1.0",
                onClick = { showAbout = true }
            ) {}
        }

        // ═══ 退出登录（长按 1s 确认，细节 27）═══
        if (loggedIn) {
            GroupTitle("账号")
            Box(Modifier.padding(horizontal = 14.dp)) {
                HoldToConfirmButton(
                    label = "长按退出登录",
                    onConfirm = {
                        // 与原逻辑一致：先关本页再清登录态，避免「我的」页闪出未登录空版面
                        onFinish()
                        RetrofitClient.clearCookies()
                        ForumDataCache.clearUser()
                    }
                )
            }
        }

        Spacer(Modifier.height(30.dp))
    }

    // ———————— 弹层 ————————
    if (showThemeSheet) {
        ThemePickerSheet(onDismiss = { showThemeSheet = false })
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("关于", color = c.textMain) },
            text = { Text("S1 Client\nV1.0\n\nStage1st 客户端", color = c.textMain) },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("知道了", color = c.accent) }
            }
        )
    }
    if (showClearHistory) {
        AlertDialog(
            onDismissRequest = { showClearHistory = false },
            title = { Text("清除阅读历史", color = c.textMain) },
            text = { Text("将删除本地保存的最近浏览记录，不影响论坛数据。确定吗？", color = c.textSub) },
            confirmButton = {
                TextButton(onClick = {
                    ReadHistoryStore.clear(context)
                    showClearHistory = false
                    S1Notify.show(context, "阅读历史已清除", S1Notify.Type.SUCCESS)
                }) { Text("确定", color = Color(0xFFFF3B30)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistory = false }) { Text("取消", color = c.textSub) }
            }
        )
    }
    if (showResetAppearance) {
        AlertDialog(
            onDismissRequest = { showResetAppearance = false },
            title = { Text("重置外观偏好", color = c.textMain) },
            text = { Text("字号与自定义字体会恢复默认（自定义字体将被删除）。确定吗？", color = c.textSub) },
            confirmButton = {
                TextButton(onClick = {
                    SettingStore.resetAppearance(context)
                    fontSize = SettingStore.fontSize(context)
                    fontName = null
                    showResetAppearance = false
                    S1Notify.show(context, "外观偏好已重置", S1Notify.Type.SUCCESS)
                }) { Text("确定", color = Color(0xFFFF3B30)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetAppearance = false }) { Text("取消", color = c.textSub) }
            }
        )
    }
    if (showForumBlock) {
        ForumBlockDialog(
            hidden = hiddenForums,
            onDismiss = { showForumBlock = false },
            onToggle = { fid, hide ->
                SettingStore.setForumHidden(context, fid, hide)
                hiddenForums = SettingStore.hiddenForumIds(context)
            },
            onClearAll = {
                SettingStore.clearHiddenForums(context)
                hiddenForums = emptySet()
                S1Notify.show(context, "已清空版块屏蔽", S1Notify.Type.SUCCESS)
            }
        )
    }
    if (showBlockedAuthors) {
        BlockedAuthorsDialog(
            blocked = BlockedAuthorManager.allBlocked(),
            onDismiss = { showBlockedAuthors = false },
            onUnblock = { uid ->
                BlockedAuthorManager.unblock(uid)
                S1Notify.show(context, "已取消屏蔽", S1Notify.Type.SUCCESS)
            },
            onClearAll = {
                BlockedAuthorManager.allBlocked().keys.toList()
                    .forEach { BlockedAuthorManager.unblock(it) }
                S1Notify.show(context, "已清空黑名单", S1Notify.Type.SUCCESS)
            }
        )
    }
    if (showTailEdit) {
        TailEditDialog(
            initial = tail.custom,
            showDevice = tail.showDevice,
            onDeviceChange = {
                SettingStore.setPostTail(context, tail.enabled, it, tail.custom)
                tail = SettingStore.postTail(context)
            },
            onDismiss = { showTailEdit = false },
            onSave = { text ->
                // setPostTail 内部会做清洗（去换行/方括号、截 20 字）
                SettingStore.setPostTail(context, tail.enabled, tail.showDevice, text)
                tail = SettingStore.postTail(context)
                showTailEdit = false
                S1Notify.show(context, "小尾巴前缀已保存", S1Notify.Type.SUCCESS)
            }
        )
    }
}

// ————————— 基础组件 —————————

@Composable
private fun GroupTitle(title: String) {
    Text(
        title,
        fontSize = 12.sp, fontWeight = FontWeight.Bold,
        color = LocalS1Colors.current.textSub,
        modifier = Modifier.padding(start = 26.dp, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    // 接入 FlowGlassCard：幻彩主题下外观/交互/发帖三组与「我的」页功能卡同规格（静态柔彩描边）；
    // 其他主题保持普通玻璃卡。之前没接是遗漏，不是主题设计限制。
    // 注意：FlowGlassCard 内容层是 Box，Group 的行必须显式包一层 Column 才能纵向排列。
    FlowGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        rotating = false,
        soft = true
    ) {
        Column { content() }
    }
}

/**
 * 设置行：左侧 28dp 圆底单线图标（icon=null 时仅缩进对齐），
 * 中间标题+可选副标题，右侧 trailing 自定义内容。onClick 为空则不可点。
 */
@Composable
private fun SettingRow(
    icon: ImageVector?,
    title: String,
    sub: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val c = LocalS1Colors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(c.chipBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = c.strongIcon, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(12.dp))
        } else {
            Spacer(Modifier.width(40.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.5.sp, color = c.textMain, fontWeight = FontWeight.Medium)
            if (sub != null) {
                Text(sub, fontSize = 11.sp, color = c.textSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing()
    }
}

@Composable
private fun Chevron() {
    val c = LocalS1Colors.current
    Icon(
        painter = painterResource(R.drawable.ic_chevron_right),
        contentDescription = null,
        tint = c.textSub.copy(alpha = 0.6f),
        modifier = Modifier.size(13.dp)
    )
}

private fun themeName(): String = when (ThemeManager.currentTheme) {
    ThemeManager.THEME_LIGHT -> "焙茶午后"
    ThemeManager.THEME_PINK -> "樱雾粉霜"
    ThemeManager.THEME_FC -> "朱砂红白"
    ThemeManager.THEME_RP5 -> "苔绿灰阶"
    ThemeManager.THEME_IRIDESCENT -> "幻彩渐变"
    ThemeManager.THEME_LINEAR -> "Linear 墨蓝"
    else -> "主题"
}

private fun tailPreview(custom: String, showDevice: Boolean): String {
    val device = if (showDevice) "Pixel 8" else null
    return when {
        device != null && custom.isNotBlank() -> "——$custom · 来自 $device 上的 S1Client 客户端"
        device != null -> "——来自 $device 上的 S1Client 客户端"
        else -> "——来自 S1Client 客户端"
    }
}

// ————————— 版块屏蔽弹窗（对标 s1er hidden_forums_screen：直接隐藏非置灰） —————————

@Composable
private fun ForumBlockDialog(
    hidden: Set<String>,
    onDismiss: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onClearAll: () -> Unit
) {
    val c = LocalS1Colors.current
    val forums = remember { ForumDataCache.getAllForums().filter { !it.fid.isNullOrBlank() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("版块屏蔽（${hidden.size}）", color = c.textMain) },
        text = {
            if (forums.isEmpty()) {
                Text("论坛数据尚未加载，请先进入一次首页", color = c.textSub, fontSize = 14.sp)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(forums.size) { i ->
                        val forum = forums[i]
                        val fid = forum.fid ?: return@items
                        val isHidden = hidden.contains(fid)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(fid, !isHidden) }
                                .padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 单线条复选框
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .border(
                                        1.5.dp,
                                        if (isHidden) c.accent else c.textSub.copy(alpha = 0.4f),
                                        RoundedCornerShape(5.dp)
                                    )
                                    .background(if (isHidden) c.accent.copy(alpha = 0.15f) else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isHidden) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = c.accent, modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                forum.name ?: fid,
                                fontSize = 13.sp,
                                color = if (isHidden) c.textSub else c.textMain,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                if (isHidden) "已屏蔽" else "显示中",
                                fontSize = 10.5.sp,
                                color = if (isHidden) c.accent else c.textSub.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (hidden.isNotEmpty()) {
                TextButton(onClick = onClearAll) { Text("全部恢复", color = Color(0xFFFF3B30)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭", color = c.textSub) }
        }
    )
}

// ————————— 小尾巴前缀编辑弹窗 —————————

@Composable
private fun TailEditDialog(
    initial: String,
    showDevice: Boolean,
    onDeviceChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val c = LocalS1Colors.current
    var text by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义前缀", color = c.textMain) },
        text = {
            Column {
                Text(
                    "将显示为「——{前缀} · 来自 {机型} 上的 S1Client 客户端」。\n自动去除换行与方括号，最长 20 字。",
                    fontSize = 12.sp, color = c.textSub
                )
                Spacer(Modifier.height(10.dp))
                BasicTextField(
                    value = text,
                    onValueChange = { if (it.length <= 20) text = it },
                    singleLine = true,
                    textStyle = TextStyle(color = c.textMain, fontSize = 14.sp),
                    cursorBrush = SolidColor(c.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.chipBg)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
                Text(
                    "${text.length}/20",
                    fontSize = 10.sp, color = c.textSub,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("附带机型信息", fontSize = 13.sp, color = c.textMain, modifier = Modifier.weight(1f))
                    SunMoonSwitch(
                        checked = showDevice,
                        onCheckedChange = onDeviceChange
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "预览：「——${text.ifBlank { "（无前缀）" }} · 来自 {机型} 上的 S1Client 客户端」",
                    fontSize = 11.sp, color = c.textSub.copy(alpha = 0.8f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) { Text("保存", color = c.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = c.textSub) }
        }
    )
}
