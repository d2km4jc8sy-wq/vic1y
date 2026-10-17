package com.example.s1client.ui

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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s1client.R
import com.example.s1client.ui.theme.LocalS1Colors
import com.example.s1client.data.store.BlockedAuthorManager
import com.example.s1client.data.store.DataSaverStore
import com.example.s1client.data.store.ImageLoadPolicy
import com.example.s1client.util.S1Notify
import com.example.s1client.data.store.dirSize
import com.example.s1client.data.store.formatSize
import com.example.s1client.data.store.imageCacheDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 齿轮 → 快捷操作（底部弹窗，无标题）。
 *
 * 原来这里是个叫「更多」的居中对话框，里面塞了 设置 / 帮助 / 关于 / 退出登录：
 * 「更多」这个标题没有任何信息量，「帮助」又只是个「即将上线」的 toast 占位。
 * 现在收缩成三件真正能做的事，并且不再有二级「设置」页面：
 *   清理图片缓存（原先在设置页里）/ 关于 / 退出登录
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionsSheet(
    loggedIn: Boolean,
    onDismiss: () -> Unit,
    onAbout: () -> Unit,
    onLogout: () -> Unit,
    onBlockedManager: () -> Unit = {}
) {
    val c = LocalS1Colors.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 省流模式：与首页瀑布流共用 DataSaverStore，改这里首页立即生效
    val imagePolicy by com.example.s1client.data.store.DataSaverStore.policy.collectAsState()
    val blockedCount by BlockedAuthorManager.blockedFlow.collectAsState(initial = BlockedAuthorManager.allBlocked())

    var cacheText by remember { mutableStateOf("") }
    var clearing by remember { mutableStateOf(false) }

    // 打开时算一次缓存体积：让用户知道要清掉的到底是什么、有多大
    LaunchedEffect(Unit) {
        cacheText = withContext(Dispatchers.IO) {
            formatSize(dirSize(imageCacheDir(context)))
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
            ActionRow(
                iconRes = R.drawable.ic_clean_cache,
                label = "清理图片缓存",
                trailing = when {
                    clearing -> "清理中…"
                    cacheText.isBlank() -> ""
                    else -> cacheText
                },
                tint = c.textMain
            ) {
                if (clearing) return@ActionRow
                clearing = true
                // 只清 Coil 磁盘缓存（image_cache 目录），不动其他缓存
                coil.Coil.imageLoader(context).diskCache?.clear()
                cacheText = formatSize(0)
                clearing = false
                S1Notify.show(context, "已清理图片缓存", S1Notify.Type.SUCCESS)
            }

            // 图片加载三档（点1/点2，2026-10-17）：点击循环 始终→仅Wi-Fi→手动
            ActionRow(
                iconRes = R.drawable.ic_clean_cache,
                label = "图片加载",
                trailing = imagePolicy.label(),
                tint = c.textMain
            ) {
                com.example.s1client.util.Haptic.selection(context)
                val nextP = imagePolicy.next()
                DataSaverStore.setPolicy(context, nextP)
                S1Notify.show(context, "图片加载：${nextP.label()}", S1Notify.Type.SUCCESS)
            }

            // 吐抹布（黑名单管理，原在首页锤子菜单里）
            ActionRow(
                iconRes = R.drawable.ic_block,
                label = if (blockedCount.isNotEmpty()) "吐抹布（${blockedCount.size}）" else "吐抹布",
                tint = c.textMain
            ) {
                onDismiss()
                onBlockedManager()
            }

            ActionRow(
                iconRes = R.drawable.ic_info,
                label = "关于",
                tint = c.textMain
            ) {
                onDismiss()
                onAbout()
            }

            if (loggedIn) {
                Spacer(Modifier.height(2.dp))
                ActionRow(
                    iconRes = R.drawable.ic_exit,
                    label = "退出登录",
                    tint = Color(0xFFFF3B30)
                ) {
                    onDismiss()
                    onLogout()
                }
            }
        }
    }
}

/** 弹窗里的一行：圆底图标 + 文案 +（可选）右侧说明。 */
@Composable
private fun ActionRow(
    iconRes: Int,
    label: String,
    tint: Color,
    trailing: String = "",
    onClick: () -> Unit
) {
    val c = LocalS1Colors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(c.chipBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(13.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 15.sp,
            fontWeight = if (label == "退出登录") FontWeight.Medium else FontWeight.Normal
        )
        Spacer(Modifier.weight(1f))
        if (trailing.isNotBlank()) {
            Text(text = trailing, color = c.textSub, fontSize = 12.sp)
        }
    }
}
