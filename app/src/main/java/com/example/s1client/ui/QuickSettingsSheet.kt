package com.example.s1client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s1client.data.store.DataSaverStore
import com.example.s1client.data.store.ImageLoadPolicy
import com.example.s1client.data.store.SettingStore
import com.example.s1client.ui.theme.LocalS1Colors
import com.example.s1client.util.Haptic
import com.example.s1client.util.ThemeManager

/**
 * 二级弹窗 A：头像右侧入口的「快捷」面板。
 *
 * **2026-10-19 改版（用户拍板 B·磁贴）**：原来是 7 行纵向列表，占屏 3/4，
 * 一行里混了四种交互（进二级 / 开关 / 分段 / 点一下换一档），还靠三条副标题去解释。
 * 现在改成：
 *   ① 顶部 2×2 磁贴 —— 主题 / 字号 / 震动 / 头像，每格 = 图标 + 名称 + **当前值**，
 *      一眼能读出状态，不再需要副标题；
 *   ② 多档位一律用**分段控件**（字号、头像、图片加载）—— 有几档、现在第几档全都看得见，
 *      取代原来「点一下换一档」的猜谜交互；
 *   ③ 图片加载是改得最勤的一项，常驻；字号/头像点磁贴才就地展开（同一时间只开一个）；
 *   ④ 底部一条「全部设置」通往二级 B。
 *
 * 入选铁律没变（R5 层级化）：**即时生效 + 零确认**。小尾巴、屏蔽、清缓存这类
 * 要打字 / 要确认 / 要浏览列表的，仍然全部留在设置页。
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
    var hapticOn by remember { mutableStateOf(Haptic.enabled) }
    val imagePolicy by DataSaverStore.policy.collectAsState()
    val avatarPolicy by DataSaverStore.avatarPolicy.collectAsState()
    /** 当前展开的分段区：null / "font" / "avatar" —— 同一时间只开一个 */
    var expanded by remember { mutableStateOf<String?>(null) }

    val imageOptions = listOf(
        ImageLoadPolicy.ALWAYS, ImageLoadPolicy.WIFI_ONLY, ImageLoadPolicy.MANUAL
    )

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
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "快捷",
                color = c.textMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 12.dp)
            )

            // ————————— 2×2 磁贴 —————————
            Column(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickTile(
                        icon = Icons.Outlined.Palette,
                        label = "主题",
                        value = themeNameShort(),
                        modifier = Modifier.weight(1f)
                    ) { onOpenThemePicker() }
                    QuickTile(
                        icon = Icons.Outlined.FormatSize,
                        label = "字号",
                        value = fontSizeLabel(fontSize),
                        active = expanded == "font",
                        modifier = Modifier.weight(1f)
                    ) { expanded = if (expanded == "font") null else "font" }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickTile(
                        icon = Icons.Outlined.Vibration,
                        label = "震动",
                        value = if (hapticOn) "开" else "关",
                        modifier = Modifier.weight(1f)
                    ) {
                        val on = !hapticOn
                        Haptic.setEnabled(context, on)
                        hapticOn = on
                        if (on) Haptic.selection(context)
                    }
                    QuickTile(
                        icon = Icons.Outlined.AccountCircle,
                        label = "头像",
                        value = avatarPolicy.label(),
                        active = expanded == "avatar",
                        modifier = Modifier.weight(1f)
                    ) { expanded = if (expanded == "avatar") null else "avatar" }
                }
            }

            // ————————— 就地展开的分段（同一时间只开一个）—————————
            if (expanded == "font") {
                QuickSegment(
                    title = "文字大小",
                    options = SettingStore.FONT_SIZES.map { fontSizeLabel(it) },
                    selected = SettingStore.FONT_SIZES.indexOf(fontSize).coerceAtLeast(0)
                ) { i ->
                    Haptic.selection(context)
                    fontSize = SettingStore.FONT_SIZES[i]
                    SettingStore.setFontSize(context, fontSize)
                }
            }
            if (expanded == "avatar") {
                QuickSegment(
                    title = "头像加载",
                    options = imageOptions.map { it.label() },
                    selected = imageOptions.indexOf(avatarPolicy).coerceAtLeast(0)
                ) { i ->
                    Haptic.selection(context)
                    DataSaverStore.setAvatarPolicy(context, imageOptions[i])
                }
            }

            // ————————— 图片加载：改得最勤，常驻 —————————
            QuickSegment(
                title = "图片加载",
                options = imageOptions.map { it.label() },
                selected = imageOptions.indexOf(imagePolicy).coerceAtLeast(0)
            ) { i ->
                Haptic.selection(context)
                DataSaverStore.setPolicy(context, imageOptions[i])
            }

            Spacer(Modifier.height(10.dp))
            SheetDivider()

            // ————————— 全部设置（通往二级 B）—————————
            SheetNavRow(
                icon = Icons.Outlined.Settings,
                title = "全部设置",
                sub = "字体 · 发帖 · 屏蔽 · 数据 · 账号",
                iconTint = c.accent,
                titleColor = c.accent,
                highlighted = true
            ) { onOpenSettings() }
        }
    }
}

/** 磁贴：图标 + 名称在上，当前值在下。active = 该格的分段正展开着。 */
@Composable
private fun QuickTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val c = LocalS1Colors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) c.accent.copy(alpha = 0.18f) else c.textMain.copy(alpha = 0.07f))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, contentDescription = null,
                tint = c.strongIcon, modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(7.dp))
            Text(label, color = c.textSub, fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            color = c.textMain,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 分段控件：一档一段，全部档位同时可见，当前档用强调色填实。
 * 取代原来「点一下换一档」的循环交互 —— 那玩意儿看不见有几档，全靠试。
 */
@Composable
private fun QuickSegment(
    title: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val c = LocalS1Colors.current
    Column(modifier = Modifier.padding(horizontal = 18.dp).padding(top = 11.dp)) {
        Text(
            text = title,
            color = c.textSub,
            fontSize = 11.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(c.textMain.copy(alpha = 0.07f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            options.forEachIndexed { i, text ->
                val on = i == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (on) c.accent.copy(alpha = 0.9f) else Color.Transparent)
                        .clickable { onSelect(i) }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        color = if (on) Color.White else c.textSub,
                        fontSize = 12.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
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
private fun SheetDivider() {
    Box(
        Modifier
            .padding(horizontal = 22.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.07f))
    )
}

/** 字号档位 → 文案（快捷面板与设置页共用同一套） */
private fun fontSizeLabel(size: Int): String = when (size) {
    12 -> "小"; 14 -> "标准"; 16 -> "大"; else -> "超大"
}

/** 当前主题短名（从 MineActivity 的外观卡原样迁入） */
internal fun themeNameShort(): String = when (ThemeManager.currentTheme) {
    ThemeManager.THEME_TEA -> "焙茶午后"
    ThemeManager.THEME_IRIDESCENT -> "幻彩渐变"
    ThemeManager.THEME_LINEAR -> "Linear 墨蓝"
    ThemeManager.THEME_CRIMSON -> "Red赤红"
    ThemeManager.THEME_DORAEMON -> "机器猫"
    ThemeManager.THEME_YARN -> "毛线主题"
    else -> "主题"
}
