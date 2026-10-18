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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.ui.S1Switch
import com.example.s1client.ui.HoldToConfirmButton
import com.example.s1client.ui.S1SystemBarSync
import com.example.s1client.ui.SunMoonSwitch
import com.example.s1client.ui.ThemePickerSheet
import com.example.s1client.ui.ThemeSwatch
import com.example.s1client.ui.theme.LocalS1Colors
import com.example.s1client.ui.theme.S1Theme
import com.example.s1client.data.store.DataSaverStore
import com.example.s1client.data.store.ImageLoadPolicy
import com.example.s1client.util.Haptic
import com.example.s1client.data.store.ReadHistoryStore
import com.example.s1client.util.S1Notify
import com.example.s1client.data.store.SettingStore
import com.example.s1client.util.ThemeManager
import com.example.s1client.util.applyS1Theme
import com.example.s1client.feature_changelog.ChangelogActivity
import com.example.s1client.feature_update.UpdateChecker
import com.example.s1client.feature_network.NetworkParamsActivity
import com.example.s1client.util.applyS1Theme
import com.example.s1client.data.store.dirSize
import com.example.s1client.data.store.formatSize
import com.example.s1client.data.store.imageCacheDir
import com.example.s1client.util.setupS1EdgeToEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 设置中心（对标 s1er settings_screen，R6 去重后六组）：
 *   外观（主题/文字大小/字体） · 交互（震动/省流） · 发帖（小尾巴） ·
 *   数据管理（清历史/清图片缓存/重置外观） · 关于 · 退出登录
 *
 * R6 定位：**本页是全集**，头像右侧「快捷」弹窗只是其中 5 项（主题 / 文字大小 /
 * 字体 / 交互震动 / 省流）的快捷副本。之前这几项只在弹窗里、设置页找不到，
 * 「全部设置」名不副实、产生割裂感 —— 现在补回外观组，两边口径一致。
 *
 * 取消的组：屏蔽（版块屏蔽＝用户未做该功能、瀑布流也不需要；
 * 用户黑名单＝已上移到「我的」页三功能格，此处不重复）。
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


/** 诊断信息文本（设置→关于→诊断信息）：版本/机型/缓存/偏好 + 最近日志尾部（debug 构建可读） */
private fun buildDiagText(context: android.content.Context): String {
    val sb = StringBuilder()
    sb.appendLine("版本: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")")
    sb.appendLine("Android: " + android.os.Build.VERSION.RELEASE + " (SDK " + android.os.Build.VERSION.SDK_INT + ")")
    sb.appendLine("机型: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL)
    val dir = imageCacheDir(context)
    sb.appendLine("图片缓存: " + formatSize(dirSize(dir)) + "（上限 " + SettingStore.cacheCapMb(context) + " MB）")
    sb.appendLine("字号档: " + SettingStore.fontSize(context) + " · 阅读历史: " + (if (SettingStore.recordHistory(context)) "开" else "关"))
    sb.appendLine("楼层图片上限: " + SettingStore.maxImagesPerFloor(context).let { if (it > 0) "${'$'}it 张" else "不限" })
    sb.appendLine()
    sb.appendLine("—— 最近日志（尾部 60 行）——")
    runCatching {
        val proc = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "60"))
        val out = proc.inputStream.bufferedReader().readText()
        proc.waitFor()
        sb.append(out.takeLast(3000))
    }.onFailure { sb.append("无法读取日志（需要 debug 构建）: ").append(it.message) }
    return sb.toString()
}

@Composable
fun SettingsScreen(onFinish: () -> Unit) {
    val c = LocalS1Colors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    S1SystemBarSync()

    var showThemeSheet by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showClearHistory by remember { mutableStateOf(false) }
    var showResetAppearance by remember { mutableStateOf(false) }
    var showDiag by remember { mutableStateOf(false) }
    var showClearCache by remember { mutableStateOf(false) }
    var diagText by remember { mutableStateOf("") }
    var showTailEdit by remember { mutableStateOf(false) }
    // —— 检查更新状态（PROBLEMS P9）——
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }

    fun checkForUpdates() {
        if (checkingUpdate) return
        checkingUpdate = true
        S1Notify.show(context, "正在检查更新…")
        scope.launch {
            updateInfo = null
            when (val r = UpdateChecker.check(context, BuildConfig.VERSION_NAME)) {
                is UpdateChecker.CheckResult.Available -> updateInfo = r.info
                is UpdateChecker.CheckResult.UpToDate ->
                    S1Notify.show(context, "已是最新版本", S1Notify.Type.SUCCESS)
                is UpdateChecker.CheckResult.Failed ->
                    S1Notify.show(context, "检查失败：${r.message}", S1Notify.Type.ERROR)
            }
            checkingUpdate = false
        }
    }
    // 登录态：**改 resume 复检**（R9 同法 · 设计者裁定「这种看不到的地方，既然不合理就处理掉」）。
    // 原来是组合期一次性快照：若本页在登录态之外被打开（或登录后从其它入口返回），
    // 会停留在旧值 —— 表现为「退出登录」行该显示时不显示 / 不该显示时还在。
    // 机制与 MainActivity:366-368 的 homeLoggedIn 完全一致（宪法 23.A.3：状态由页面权威驱动）。
    var loggedIn by remember { mutableStateOf(ForumDataCache.isLoggedIn()) }
    val settingsLifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(settingsLifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                loggedIn = ForumDataCache.isLoggedIn()
            }
        }
        settingsLifecycle.lifecycle.addObserver(observer)
        onDispose { settingsLifecycle.lifecycle.removeObserver(observer) }
    }

    // —— 可变设置状态（即时生效、即时持久化，无「保存」按钮，对标 s1er）——
    var hapticEnabled by remember { mutableStateOf(Haptic.enabled) }
    var imgPolicyState by remember { mutableStateOf(DataSaverStore.policy()) }
    var avPolicyState by remember { mutableStateOf(DataSaverStore.avatarPolicy()) }
    var tail by remember { mutableStateOf(SettingStore.postTail(context)) }
    var cacheText by remember { mutableStateOf("计算中…") }

    // —— 一屏宫格所需：原分散在各组内的状态提升到顶层（卡片摘要与弹层共用）——
    var activeSheet by remember { mutableStateOf<String?>(null) }
    var followDark by remember { mutableStateOf(SettingStore.followSystemDark(context)) }
    var recordHist by remember { mutableStateOf(SettingStore.recordHistory(context)) }
    var camLever by remember { mutableStateOf(SettingStore.cameraLeverRefresh(context)) }
    var cacheCap by remember { mutableStateOf(SettingStore.cacheCapMb(context)) }
    var imgCap by remember { mutableStateOf(SettingStore.maxImagesPerFloor(context)) }
    var iconVariant by remember { mutableStateOf(com.example.s1client.util.AppIconManager.current(context)) }

    // —— 外观（R6 回归：让「全部设置」真的是全集，弹窗只是快捷副本）——
    var fontSize by remember { mutableIntStateOf(SettingStore.fontSize(context)) }
    var fontName by remember { mutableStateOf(SettingStore.customFontName(context)) }
    // —— 字体导入预览（点5 防破坏，2026-10-17）：选文件先预览+覆盖检测，应用才落盘 ——
    var fontPreview by remember { mutableStateOf<com.example.s1client.util.FontInspector.Preview?>(null) }
    // 备份 / 恢复（原定义在「数据管理」组内，改版后上提到顶层供弹层使用）
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            val r = runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    com.example.s1client.util.BackupManager.export(context, it).getOrThrow()
                } ?: error("无法写入所选位置")
            }
            r.onSuccess {
                S1Notify.show(context, "备份完成：$it 项设置与数据", S1Notify.Type.SUCCESS)
            }.onFailure {
                S1Notify.show(context, it.message ?: "备份失败", S1Notify.Type.ERROR)
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val r = runCatching {
                context.contentResolver.openInputStream(uri)?.use {
                    com.example.s1client.util.BackupManager.import(context, it).getOrThrow()
                } ?: error("无法读取所选文件")
            }
            r.onSuccess {
                S1Notify.show(context, "已恢复 $it 个模块 · 重启 App 后生效", S1Notify.Type.SUCCESS)
            }.onFailure {
                S1Notify.show(context, it.message ?: "恢复失败", S1Notify.Type.ERROR)
            }
        }
    }

    val fontPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            Thread {
                val result = runCatching {
                    val name = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                        val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
                    } ?: "custom.ttf"
                    if (!name.endsWith(".ttf", true) && !name.endsWith(".otf", true))
                        error("只支持 .ttf / .otf 字体文件")
                    val tmp = java.io.File(context.cacheDir, "font_preview.tmp")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tmp.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("无法读取所选文件")
                    val tf = runCatching { android.graphics.Typeface.createFromFile(tmp) }
                        .getOrNull() ?: error("字体文件损坏或不受支持")
                    val cov = com.example.s1client.util.FontInspector.coverage(tf)
                    com.example.s1client.util.FontInspector.Preview(name, tf, cov, tmp, uri)
                }
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    result.onSuccess { fontPreview = it }
                        .onFailure { S1Notify.show(context, it.message ?: "无法读取字体", S1Notify.Type.ERROR) }
                }
            }.start()
        }
    }

    LaunchedEffect(Unit) {
        cacheText = withContext(Dispatchers.IO) { formatSize(dirSize(imageCacheDir(context))) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
    ) {
        // —— 顶栏 ——
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(c.headerGradient))
                .padding(top = 48.dp, bottom = 8.dp, start = 3.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onFinish) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null,
                    tint = c.onHeader,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                "设置", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                color = c.onHeader,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(48.dp))
        }

        // ═══ 一屏六格 ═══
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 13.dp, vertical = 12.dp)
        ) {
            SettingsCard(
                modifier = Modifier.fillMaxWidth().weight(1.12f),
                icon = Icons.Outlined.Palette,
                title = "外观",
                summary = com.example.s1client.ui.themeNameShort() + " · " + fontSizeLabel(fontSize),
                wide = true,
                onClick = { activeSheet = "look" }
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = Icons.Outlined.DataUsage,
                    title = "图片与流量",
                    summary = imgPolicyState.label() + " · " + cacheCapLabel(cacheCap),
                    onClick = { activeSheet = "media" }
                )
                SettingsCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = Icons.Outlined.Vibration,
                    title = "交互与图标",
                    summary = (if (hapticEnabled) "震动开" else "震动关") + " · " +
                        com.example.s1client.util.AppIconManager.labelOf(iconVariant),
                    onClick = { activeSheet = "touch" }
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = Icons.Outlined.History,
                    title = "阅读与发帖",
                    summary = (if (tail.enabled) "小尾巴开" else "小尾巴关") +
                        " · 历史" + (if (recordHist) "开" else "关"),
                    onClick = { activeSheet = "read" }
                )
                SettingsCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = Icons.Outlined.Save,
                    title = "存储与备份",
                    summary = cacheText,
                    onClick = { activeSheet = "store" }
                )
            }
            Spacer(Modifier.height(10.dp))
            SettingsCard(
                modifier = Modifier.fillMaxWidth().weight(0.74f),
                icon = Icons.Outlined.Science,
                title = "实验功能",
                summary = if (camLever) "相机扳手已开启" else "相机扳手 · 未开启",
                wide = true,
                onClick = { activeSheet = "lab" }
            )
        }

        // ═══ 底部：关于与隐私 / 退出登录 ═══
        Box(Modifier.fillMaxWidth().padding(horizontal = 13.dp)) {
            FlowGlassCard(Modifier.fillMaxWidth(), rotating = false, soft = true) {
                Column {
                    SettingRow(
                        Icons.Outlined.Info, "关于与隐私",
                        sub = "关于 · 更新日志 · 检查更新 · 隐私政策",
                        onClick = { activeSheet = "about" }
                    ) { Chevron() }
                    if (loggedIn) {
                        SettingRow(
                            Icons.Outlined.Logout, "退出登录",
                            onClick = { showLogoutConfirm = true }
                        ) { Chevron() }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    // 字体预览（点5 防破坏）：预渲染 + 字形覆盖率，应用才落盘
    fontPreview?.let { pv ->
        AlertDialog(
            onDismissRequest = { pv.tmpFile.delete(); fontPreview = null },
            title = { Text("字体预览 · ${pv.name}", fontSize = 16.sp) },
            text = {
                Column {
                    val ffa = androidx.compose.ui.text.font.FontFamily(pv.typeface)
                    Text("Stage1st 岁月史书 0123", fontFamily = ffa, fontSize = 16.sp)
                    Text("今天也没有人盖楼，楼层 12345 层。", fontFamily = ffa, fontSize = 14.sp)
                    Text("ABCDEFGHIJKLMN abcdefghijklmn", fontFamily = ffa, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "字形覆盖 ${pv.coverage.coveragePercent}%（抽样 ${pv.coverage.total} 字 · 缺 ${pv.coverage.missingCount} 字）",
                        fontSize = 12.sp, color = c.textSub
                    )
                    if (pv.coverage.missingSample.isNotEmpty()) {
                        Text(
                            "缺字示例：${pv.coverage.missingSample.joinToString("")}",
                            fontSize = 12.sp, color = c.heart
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "应用后全 App 生效；取消不写入任何数据。",
                        fontSize = 11.sp, color = c.textSub.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    SettingStore.importFont(context, pv.uri)
                        .onSuccess {
                            fontName = it
                            S1Notify.show(
                                context,
                                "字体已导入：$it（字形覆盖 ${pv.coverage.coveragePercent}%）",
                                S1Notify.Type.SUCCESS
                            )
                        }
                        .onFailure { S1Notify.show(context, it.message ?: "导入失败", S1Notify.Type.ERROR) }
                    pv.tmpFile.delete()
                    fontPreview = null
                }) { Text("应用并替换", color = c.accent) }
            },
            dismissButton = {
                TextButton(onClick = { pv.tmpFile.delete(); fontPreview = null }) {
                    Text("取消", color = c.textSub)
                }
            }
        )
    }

    // ═════════ 六个设置弹层（一屏宫格的落点）═════════

    if (activeSheet == "look") {
        SettingsSheet("外观", onDismiss = { activeSheet = null }) {
            SettingRow(
                Icons.Outlined.Palette, "主题", sub = com.example.s1client.ui.themeNameShort(),
                onClick = { activeSheet = null; showThemeSheet = true }
            ) { Chevron() }
            SettingRow(Icons.Outlined.DarkMode, "深色跟随系统", sub = "跟随系统深浅色") {
                S1Switch(
                    checked = followDark,
                    onCheckedChange = {
                        SettingStore.setFollowSystemDark(context, it)
                        ThemeManager.setFollowSystemDark(context, it)
                        followDark = it
                    }
                )
            }
            SettingRow(Icons.Outlined.FormatSize, "文字大小") {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    SettingStore.FONT_SIZES.forEach { size ->
                        val selected = size == fontSize
                        Text(
                            text = fontSizeLabel(size),
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
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            SettingRow(
                Icons.Outlined.TextFields, "字体",
                sub = fontName ?: "默认字体",
                onClick = { fontPicker.launch(arrayOf("*/*")) }
            ) { Chevron() }
            SettingRow(
                Icons.Outlined.Refresh, "重置外观偏好",
                sub = "字号/字体恢复默认，不影响版块屏蔽与历史",
                onClick = { activeSheet = null; showResetAppearance = true }
            ) { Chevron() }
        }
    }

    if (activeSheet == "media") {
        SettingsSheet("图片与流量", onDismiss = { activeSheet = null }) {
            SheetChips(
                title = "图片加载",
                options = ImageLoadPolicy.values().map { it.label() },
                selected = imgPolicyState.label()
            ) { label ->
                val p = ImageLoadPolicy.values().first { it.label() == label }
                Haptic.selection(context)
                DataSaverStore.setPolicy(context, p)
                imgPolicyState = p
            }
            SheetChips(
                title = "头像加载",
                options = ImageLoadPolicy.values().map { it.label() },
                selected = avPolicyState.label()
            ) { label ->
                val p = ImageLoadPolicy.values().first { it.label() == label }
                Haptic.selection(context)
                DataSaverStore.setAvatarPolicy(context, p)
                avPolicyState = p
            }
            SheetChips(
                title = "图片缓存上限",
                options = listOf("100 MB", "256 MB", "512 MB", "1 GB", "2 GB"),
                selected = cacheCapLabel(cacheCap)
            ) { label ->
                val mb = cacheCapValue(label)
                SettingStore.setCacheCapMb(context, mb)
                cacheCap = mb
            }
            SheetChips(
                title = "每楼层图片上限",
                options = listOf("不限", "3 张", "6 张", "9 张"),
                selected = if (imgCap == 0) "不限" else "$imgCap 张"
            ) { label ->
                val n = if (label == "不限") 0 else label.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0
                SettingStore.setMaxImagesPerFloor(context, n)
                imgCap = n
            }
        }
    }

    if (activeSheet == "touch") {
        SettingsSheet("交互与图标", onDismiss = { activeSheet = null }) {
            SettingRow(Icons.Outlined.Vibration, "交互震动", sub = "全局触觉反馈") {
                S1Switch(
                    checked = hapticEnabled,
                    onCheckedChange = {
                        Haptic.setEnabled(context, it)
                        hapticEnabled = it
                        if (it) Haptic.selection(context)
                    }
                )
            }
            SheetChips(
                title = "应用图标",
                options = com.example.s1client.util.AppIconManager.VARIANTS.map { it.label },
                selected = com.example.s1client.util.AppIconManager.labelOf(iconVariant)
            ) { label ->
                val v = com.example.s1client.util.AppIconManager.VARIANTS.first { it.label == label }
                Haptic.selection(context)
                com.example.s1client.util.AppIconManager.apply(context, v.id)
                iconVariant = v.id
            }
        }
    }

    if (activeSheet == "read") {
        SettingsSheet("阅读与发帖", onDismiss = { activeSheet = null }) {
            SettingRow(Icons.Outlined.History, "记录阅读历史", sub = "关闭后不再保存最近浏览记录") {
                S1Switch(
                    checked = recordHist,
                    onCheckedChange = {
                        SettingStore.setRecordHistory(context, it)
                        recordHist = it
                    }
                )
            }
            SettingRow(
                Icons.Outlined.Edit, "发帖小尾巴",
                sub = if (tail.enabled) "发帖 / 回复 / 编辑提交时自动追加" else "已关闭"
            ) {
                S1Switch(
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
                SettingRow(
                    null, "小尾巴内容",
                    sub = "机型 · 前缀 · 预览",
                    onClick = { showTailEdit = true }
                ) { Chevron() }
            }
            SettingRow(
                Icons.Outlined.Delete, "清除阅读历史",
                sub = "本地最近浏览的帖子记录",
                onClick = { activeSheet = null; showClearHistory = true }
            ) {}
        }
    }

    if (activeSheet == "store") {
        SettingsSheet("存储与备份", onDismiss = { activeSheet = null }) {
            SettingRow(
                Icons.Outlined.Delete, "清除图片缓存", sub = cacheText,
                onClick = { activeSheet = null; showClearCache = true }
            ) {}
            SettingRow(
                Icons.Outlined.Save, "备份到文件",
                sub = "设置·历史·屏蔽·收藏·草稿 → ZIP",
                onClick = { exportLauncher.launch("s1client-backup.zip") }
            ) { Chevron() }
            SettingRow(
                Icons.Outlined.Restore, "从文件恢复",
                sub = "覆盖同名数据 · 重启生效",
                onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }
            ) { Chevron() }
            SettingRow(
                Icons.Outlined.Language, "网络参数",
                sub = "基础网址 / IP 直连（DNS 污染自救）",
                onClick = { context.startActivity(NetworkParamsActivity.intent(context)) }
            ) { Chevron() }
        }
    }

    if (activeSheet == "lab") {
        SettingsSheet("实验功能", onDismiss = { activeSheet = null }) {
            SettingRow(
                Icons.Outlined.PhotoCamera, "相机扳手刷新",
                sub = "开启后首页右上换成相机，下拉扳手松手触发刷新"
            ) {
                S1Switch(
                    checked = camLever,
                    onCheckedChange = {
                        SettingStore.setCameraLeverRefresh(context, it)
                        camLever = it
                    }
                )
            }
            Text(
                "这里专门放还没定型的新想法。做成开关的直接开，需要单独界面的会在这里多一行入口。",
                fontSize = 11.sp,
                color = c.textSub.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }

    if (activeSheet == "about") {
        SettingsSheet("关于与隐私", onDismiss = { activeSheet = null }) {
            SettingRow(
                Icons.Outlined.Info, "关于",
                sub = "S1 Client · V" + BuildConfig.VERSION_NAME,
                onClick = { activeSheet = null; showAbout = true }
            ) {}
            SettingRow(
                Icons.Outlined.History, "更新日志", sub = "版本与新特性",
                onClick = { context.startActivity(ChangelogActivity.intent(context)) }
            ) { Chevron() }
            SettingRow(
                Icons.Outlined.CloudDownload, "检查更新", sub = "从发布渠道获取最新版本",
                onClick = { checkForUpdates() }
            ) { Chevron() }
            SettingRow(
                Icons.Outlined.PrivacyTip, "隐私政策", sub = "我们不收集任何个人数据",
                onClick = { context.startActivity(com.example.s1client.feature_privacy.PrivacyActivity.intent(context)) }
            ) { Chevron() }
        }
    }

    // ———————— 弹层 ————————
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("退出登录", color = c.textMain) },
            text = { Text("退出后将返回登录界面，确认退出吗？", color = c.textSub) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    RetrofitClient.clearCookies()
                    ForumDataCache.clearUser()
                    // 登出后目标栈（2026-10-18 B 案治本，设计者点单）：
                    // 原 2026-10-14 拍板原话「游客版，我的、首页」= 未登录游客态的两个版面，
                    // 且不困死登录页。旧实现直接 startActivity(LoginActivity) 不清栈，
                    // 返回先落到设置页（登出发生地，已登录语境陈尸）——多出中间页，
                    // 违反 2026-10-11 登录页教训③。
                    // 修法：CLEAR_TOP 把栈里陈尸（我的/设置）精准清掉，只留游客态
                    // MainActivity 在栈底（MainActivity onResume 复检登录态切游客底栏），
                    // 再压登录页。返回路径：登录页 →(返回)→ 游客首页 ✓ 不困死 ✓
                    context.startActivity(
                        android.content.Intent(context, MainActivity::class.java).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                    )
                    context.startActivity(
                        android.content.Intent(context, LoginActivity::class.java)
                    )
                }) { Text("退出", color = c.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("取消", color = c.textSub)
                }
            }
        )
    }
    if (showThemeSheet) {
        ThemePickerSheet(onDismiss = { showThemeSheet = false })
    }
    updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            title = { Text("发现新版本", color = c.textMain) },
            text = {
                Text(
                    "发现新版本 ${info.version}（当前 V${BuildConfig.VERSION_NAME}）。" +
                        if (info.notes.isNotBlank()) "\n\n${info.notes.take(300)}" else "",
                    color = c.textMain
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    updateInfo = null
                    val url = info.apkUrl ?: info.pageUrl
                    if (url != null) {
                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                    } else {
                        S1Notify.show(context, "未提供下载链接", S1Notify.Type.INFO)
                    }
                }) { Text("立即更新", color = c.accent) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        UpdateChecker.setIgnoredVersion(context, info.version)
                        updateInfo = null
                    }) { Text("忽略此版", color = c.textSub) }
                    TextButton(onClick = { updateInfo = null }) { Text("稍后提醒", color = c.textSub) }
                }
            }
        )
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("关于", color = c.textMain) },
            text = { Text("S1 Client\nV" + BuildConfig.VERSION_NAME + "\n\nStage1st 客户端", color = c.textMain) },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("知道了", color = c.accent) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAbout = false
                    diagText = buildDiagText(context)
                    showDiag = true
                }) { Text("诊断信息", color = c.textSub) }
            }
        )
    }
    if (showDiag) {
        AlertDialog(
            onDismissRequest = { showDiag = false },
            title = { Text("诊断信息", color = c.textMain) },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(diagText, color = c.textSub, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDiag = false }) { Text("关闭", color = c.accent) }
            }
        )
    }
    if (showClearCache) {
        AlertDialog(
            onDismissRequest = { showClearCache = false },
            title = { Text("清除图片缓存", color = c.textMain) },
            text = { Text("将删除已缓存的图片文件（当前 $cacheText），不影响帖子和登录数据。确定吗？", color = c.textSub) },
            confirmButton = {
                TextButton(onClick = {
                    showClearCache = false
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            imageCacheDir(context).deleteRecursively()
                        }
                        cacheText = formatSize(0L)
                        Haptic.selection(context)
                        S1Notify.show(context, "已清除图片缓存", S1Notify.Type.SUCCESS)
                    }
                }) { Text("确定", color = c.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCache = false }) { Text("取消", color = c.textSub) }
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
                }) { Text("确定", color = c.danger) }
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
                    showResetAppearance = false
                    S1Notify.show(context, "外观偏好已重置", S1Notify.Type.SUCCESS)
                }) { Text("确定", color = c.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showResetAppearance = false }) { Text("取消", color = c.textSub) }
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

// ————————— 一屏宫格卡片 —————————

/**
 * 设置首页的卡片：wide = 整宽横排（外观 / 实验功能），否则为 2×2 方格。
 * 只用主题色板（glassCard 底 + glassLine 描边），不写死颜色。
 */
@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    summary: String,
    wide: Boolean = false,
    onClick: () -> Unit
) {
    val c = LocalS1Colors.current
    val shape = RoundedCornerShape(16.dp)
    if (wide) {
        Row(
            modifier = modifier
                .clip(shape)
                .background(c.glassCard)
                .border(0.5.dp, c.glassLine, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(c.chipBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = c.strongIcon, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, color = c.textMain, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(
                    summary, fontSize = 11.sp, color = c.textSub,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Chevron()
        }
    } else {
        Column(
            modifier = modifier
                .clip(shape)
                .background(c.glassCard)
                .border(0.5.dp, c.glassLine, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(c.chipBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = c.strongIcon, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.height(9.dp))
            Text(title, fontSize = 13.sp, color = c.textMain, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(
                summary, fontSize = 10.5.sp, color = c.textSub,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
            )
        }
    }
}

// ————————— 底部设置弹层 —————————

/**
 * 设置弹层外壳。按宪法 11/76：不放顶栏关闭按钮，靠 drag handle、点蒙层、系统返回关闭。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = LocalS1Colors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.cardBg,
        contentColor = c.textMain
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
        ) {
            Text(
                title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.textMain,
                modifier = Modifier.padding(start = 20.dp, bottom = 6.dp)
            )
            content()
        }
    }
}

// ————————— 弹层内「多选一」 —————————

/** 把原来的「点击循环盲选」换成能看见全部选项的 chip 行，每行最多 4 个自动折行。 */
@Composable
private fun SheetChips(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val c = LocalS1Colors.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            title, fontSize = 13.sp, color = c.textMain, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp, bottom = 7.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            options.chunked(4).forEach { rowOpts ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowOpts.forEach { opt ->
                        val on = opt == selected
                        Text(
                            opt, fontSize = 11.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                            color = if (on) Color.White else c.textSub,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (on) c.accent.copy(alpha = 0.85f) else c.chipBg)
                                .clickable { onSelect(opt) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

// ————————— 基础组件 —————————

@Composable
private fun GroupTitle(title: String) {
    Text(
        title,
        fontSize = 12.sp, fontWeight = FontWeight.Bold,
        color = LocalS1Colors.current.textSub,
        modifier = Modifier.padding(start = 26.dp, top = 21.dp, bottom = 5.dp)
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
            .padding(horizontal = 13.dp),
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

private fun fontSizeLabel(size: Int): String = when (size) {
    12 -> "小"
    14 -> "标准"
    16 -> "大"
    else -> "超大"
}

private fun cacheCapLabel(mb: Int): String = when (mb) {
    1024 -> "1 GB"
    2048 -> "2 GB"
    else -> "$mb MB"
}

private fun cacheCapValue(label: String): Int = when (label) {
    "1 GB" -> 1024
    "2 GB" -> 2048
    else -> label.filter { it.isDigit() }.toIntOrNull() ?: 100
}

private fun themeName(): String = when (ThemeManager.currentTheme) {
    ThemeManager.THEME_LIGHT -> "焙茶午后"
    ThemeManager.THEME_PURE -> "纯白"
    ThemeManager.THEME_PINK -> "樱雾粉霜"
    ThemeManager.THEME_FC -> "朱砂红白"
    ThemeManager.THEME_RP5 -> "苔绿灰阶"
    ThemeManager.THEME_IRIDESCENT -> "幻彩渐变"
    ThemeManager.THEME_LINEAR -> "Linear 墨蓝"
    ThemeManager.THEME_CRIMSON -> "朱砂"
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
                        .padding(horizontal = 13.dp, vertical = 8.dp)
                )
                Text(
                    "${text.length}/20",
                    fontSize = 10.sp, color = c.textSub,
                    modifier = Modifier.align(Alignment.End).padding(top = 3.dp)
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("附带机型信息", fontSize = 13.sp, color = c.textMain, modifier = Modifier.weight(1f))
                    S1Switch(
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
