package com.example.s1client.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.s1client.util.Haptic
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    val angleDeg: Float,      // 抛射角度
    val distancePx: Float,    // 抛射距离
    val color: Color,
    val widthPx: Float,
    val heightPx: Float,
    val spin: Float           // 自旋角速度
)

/**
 * 全屏彩带爆发（细节 23）：签到成功等里程碑时刻的仪式感演出。
 *
 * - 粒子颜色取自主题 flowCoverColors 色板，自动适配 7 套主题
 * - 1.3s 抛射 + 旋转 + 淡出后自动回调 onDone 移除
 * - 触发时配一次 SUCCESS 组合震（哒哒—嗒）
 *
 * 用法：在页面最外层 Box 最后放置：
 *   if (confettiKey > 0) ConfettiBurst(trigger = confettiKey, onDone = { confettiKey = 0 })
 */
@Composable
fun ConfettiBurst(
    trigger: Int,
    modifier: Modifier = Modifier,
    particleCount: Int = 24,
    onDone: () -> Unit = {}
) {
    val c = com.example.s1client.ui.theme.LocalS1Colors.current
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current

    // trigger 变化时重建粒子并起爆
    val particles = remember(trigger) {
        val colors = c.flowCoverColors
        List(particleCount) {
            Particle(
                angleDeg = Random.nextFloat() * 360f,
                distancePx = with(density) { Random.nextInt(140, 320).dp.toPx() },
                color = colors[Random.nextInt(colors.size)],
                widthPx = with(density) { Random.nextInt(5, 8).dp.toPx() },
                heightPx = with(density) { Random.nextInt(9, 15).dp.toPx() },
                spin = Random.nextFloat() * 500f - 250f
            )
        }
    }

    var progress by remember(trigger) { mutableFloatStateOf(0f) }

    LaunchedEffect(trigger) {
        Haptic.success(context)
        val start = System.nanoTime()
        val durationNs = 1_300_000_000L
        while (progress < 1f) {
            awaitFrame()
            progress = ((System.nanoTime() - start).toFloat() / durationNs).coerceIn(0f, 1f)
        }
        onDone()
    }

    if (progress < 1f) {
        Box(modifier = modifier.fillMaxSize()) {
            // 从屏幕中上部（签到行位置附近）抛出
            Canvas(Modifier.fillMaxSize()) {
                val origin = Offset(size.width / 2f, size.height * 0.34f)
                val t = progress
                // 缓出位移 + 后段重力下坠
                val ease = 1f - (1f - t) * (1f - t)
                particles.forEach { p ->
                    val rad = Math.toRadians(p.angleDeg.toDouble())
                    val dx = (cos(rad) * p.distancePx * ease).toFloat()
                    val dyBase = (sin(rad) * p.distancePx * ease).toFloat()
                    val gravity = 260f * t * t * density.density   // 抛物线下坠
                    val alpha = when {
                        t < 0.7f -> 1f
                        else -> (1f - (t - 0.7f) / 0.3f).coerceIn(0f, 1f)
                    }
                    rotate(degrees = p.spin * t, pivot = origin + Offset(dx, dyBase)) {
                        drawRoundRect(
                            color = p.color.copy(alpha = alpha),
                            topLeft = origin + Offset(dx, dyBase) + gravity.let { Offset(0f, it) } -
                                    Offset(p.widthPx / 2f, p.heightPx / 2f),
                            size = androidx.compose.ui.geometry.Size(p.widthPx, p.heightPx),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(p.widthPx / 3f)
                        )
                    }
                }
            }
        }
    }
}
