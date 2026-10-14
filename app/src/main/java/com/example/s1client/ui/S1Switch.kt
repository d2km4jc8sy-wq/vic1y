package com.example.s1client.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.s1client.ui.theme.LocalS1Colors

/**
 * 项目统一的开关 —— **Material You 形制**，自绘，不用 Material3 `Switch`。
 *
 * 为什么自己画：Material3 `Switch` 在浅色主题下是「灰轨道 + 白圆钮」，
 * 和本项目的玻璃卡片、强调色体系不搭；而旧的 `SunMoonSwitch` 带日月动画、
 * 74×38dp，在设置列表里显得笨重。本组件按 Material You 规范重做：
 *
 * - 轨道 52×32dp、圆角 16dp；开 = 强调色，关 = chipBg；
 * - 圆钮随状态变形：**开启 24dp 白色 + 对勾**，关闭 16dp 灰钮、无图标；
 * - 圆钮用 `offset` 滑动而不是 `padding`，切换时是平移而不是挤压。
 *
 * 设置页与快捷面板共用它，保证两处开关长得完全一样。
 */
@Composable
fun S1Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalS1Colors.current
    val press = remember { MutableInteractionSource() }

    val knobSize by animateDpAsState(if (checked) 24.dp else 16.dp, label = "knobSize")
    val knobOffset by animateDpAsState(if (checked) 20.dp else 4.dp, label = "knobOffset")

    Box(
        modifier = modifier
            .width(52.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (checked) c.accent else c.chipBg)
            .clickable(
                interactionSource = press,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(knobSize)
                .clip(CircleShape)
                .background(if (checked) Color.White else c.textSub),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = c.accent,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }
        }
    }
}
