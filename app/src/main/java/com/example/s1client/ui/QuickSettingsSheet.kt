package com.example.s1client.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s1client.data.store.DataSaverStore
import com.example.s1client.data.store.ImageLoadPolicy
import com.example.s1client.data.store.SettingStore
import com.example.s1client.ui.theme.LocalS1Colors
import com.example.s1client.util.Haptic
import com.example.s1client.util.S1Notify
import com.example.s1client.util.ThemeManager

/**
 * 二级弹窗 A：头像右侧入口的「快捷」面板。
 *
 * 入选铁律（R5 层级化）：**即时生效 + 零确认**。
 *   - 主题 / 文字大小 / 字体 / 交互震动 / 省流模式
 * 落选的不在这里：小尾巴（要打字）、版块屏蔽/黑名单（要浏览列表增删）、
 * 清缓存/重置/退出登录（危险确认型）——全部留设置页（二级 B）。
 *
 * 「全部设置」行是通往二级 B 的桥；主题行跳 [ThemePickerSheet]（由宿主切换，避免嵌套 ModalBottomSheet）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSettingsSheet(
    onDismiss: () -> Unit,
    onOpenThemePicker: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val c = LocalS1Colors.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var fontSize by remember { mutableIntStateOf(SettingStore.fontSize(context)) }
    var fontName by remember { mutableStateOf(SettingStore.customFontName(context)) }
    var hapticOn by remember { mutableStateOf(Haptic.enabled) }
    val imagePolicy by DataSaverStore.policy.collectAsState()
    val avatarPolicy by DataSaverStore.avatarPolicy.collectAsState()

    val fontPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.glassDialog,
        contentColor = c.textMain,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = c.glassDialogLine) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 26.dp)
        ) {
            Text(
                text = "快捷",
                color = c.textMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 12.dp)
            )

            // —— 主题（跳二级主题面板，由宿主切换 sheet）——
            SheetNavRow(
                icon = Icons.Outlined.Palette,
                title = "主题",
                value = themeNameShort()
            ) { onOpenThemePicker() }

            // —— 文字大小：行内 chips 即点即换（R5 预览形态）——
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(c.chipBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.FormatSize, contentDescription = null,
                        tint = c.strongIcon, modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    "文字大小", color = c.textMain, fontSize = 14.sp,
                    fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                    SettingStore.FONT_SIZES.forEach { size ->
                        val selected = size == fontSize
                        Text(
                            text = when (size) { 12 -> "小"; 14 -> "标准"; 16 -> "大"; else -> "超大" },
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.White else c.textSub,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) c.accent.copy(alpha = 0.85f) else c.chipBg
                                )
                                .clickable {
                                    fontSize = size
                                    SettingStore.setFontSize(context, size)
                                }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // —— 字体（导入）——
            SheetNavRow(
                icon = Icons.Outlined.TextFields,
                title = "字体",
                value = fontName ?: "默认字体"
            ) { fontPicker.launch(arrayOf("*/*")) }

            SheetDivider()

            // —— 交互震动（开关）——
            SheetSwitchRow(
                icon = Icons.Outlined.Vibration,
                title = "交互震动",
                sub = "全局触觉反馈",
                checked = hapticOn
            ) { on ->
                Haptic.setEnabled(context, on)
                hapticOn = on
                // 对标 s1er：开关自身切换给一次反馈
                if (on) Haptic.selection(context)
            }

            // —— 图片加载三档（点1/点2，2026-10-17）：点击循环 ——
            SheetNavRow(
                icon = Icons.Outlined.DataUsage,
                title = "图片加载",
                sub = "始终 / 仅 Wi-Fi / 手动（手动=原省流，进详情看图）",
                value = imagePolicy.label(),
                onClick = {
                    com.example.s1client.util.Haptic.selection(context)
                    DataSaverStore.setPolicy(context, imagePolicy.next())
                }
            )

            // —— 头像加载三档（独立于图片）——
            SheetNavRow(
                icon = Icons.Outlined.AccountCircle,
                title = "头像加载",
                sub = "独立于图片加载 · 关闭后显示字母头像",
                value = avatarPolicy.label(),
                onClick = {
                    com.example.s1client.util.Haptic.selection(context)
                    DataSaverStore.setAvatarPolicy(context, avatarPolicy.next())
                }
            )

            SheetDivider()

            // —— 全部设置（通往二级 B）——
            SheetNavRow(
                icon = Icons.Outlined.Settings,
                title = "全部设置",
                sub = "发帖 · 屏蔽 · 数据 · 账号",
                iconTint = c.accent,
                titleColor = c.accent,
                highlighted = true
            ) { onOpenSettings() }
        }
    }
}

@Composable
private fun SheetNavRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    sub: String? = null,
    iconTint: Color? = null,
    titleColor: Color? = null,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    val c = LocalS1Colors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (highlighted) c.accent.copy(alpha = 0.14f) else c.chipBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, contentDescription = null,
                tint = iconTint ?: c.strongIcon, modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, color = titleColor ?: c.textMain, fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (sub != null) {
                Text(sub, color = c.textSub, fontSize = 11.5.sp)
            }
        }
        if (value != null) {
            Text(value, color = c.textSub, fontSize = 12.sp)
            Spacer(Modifier.width(6.dp))
        }
        Text("›", color = c.textSub, fontSize = 15.sp)
    }
}

@Composable
private fun SheetSwitchRow(
    icon: ImageVector,
    title: String,
    sub: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val c = LocalS1Colors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(c.chipBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, contentDescription = null,
                tint = c.strongIcon, modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, color = c.textMain, fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (sub != null) {
                Text(sub, color = c.textSub, fontSize = 11.5.sp)
            }
        }
        // 统一用 S1Switch（Material You 形制），与设置页长得完全一样
        S1Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SheetDivider() {
    Box(
        Modifier
            .padding(horizontal = 22.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.07f))
    )
}

/** 当前主题短名（从 MineActivity 的外观卡原样迁入） */
internal fun themeNameShort(): String = when (ThemeManager.currentTheme) {
    ThemeManager.THEME_LIGHT -> "焙茶午后"
    ThemeManager.THEME_PURE -> "纯白"
    ThemeManager.THEME_OLED -> "曜石玄夜"
    ThemeManager.THEME_PINK -> "樱雾粉霜"
    ThemeManager.THEME_FC -> "朱砂红白"
    ThemeManager.THEME_RP5 -> "苔绿灰阶"
    ThemeManager.THEME_IRIDESCENT -> "幻彩渐变"
    ThemeManager.THEME_LINEAR -> "Linear 墨蓝"
    else -> "主题"
}
