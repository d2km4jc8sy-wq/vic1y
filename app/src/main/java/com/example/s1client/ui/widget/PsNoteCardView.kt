package com.example.s1client.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import android.widget.LinearLayout

/**
 * PS1 手柄便签容器（回复楼卡片）。
 *
 * 本质是一个竖直 LinearLayout，唯一多做的事：把整卡裁剪成「左圆角 + 右锯齿」
 * 的票据形状——对齐参考 HTML 的 `clip-path: polygon(...)` 实现：
 *
 *   卡形 = 左上/左下 2dp 圆角 + 右缘在 [w-8dp, w-2dp] 之间以 11dp 齿距之字下行，
 *   绘制时 canvas.clipPath(卡形) 再走正常绘制流程。
 *
 * 为什么从「页面底色画三角贴片」改成整体裁剪：
 * - 贴片方案的齿口处会残留便签 drawable 的描边线，三角与卡体之间出现
 *   颜色断层（真机观感 = 生硬的三角形贴片，2026-10-16 用户反馈）；
 * - 裁剪方案里背景、描边、内容一起被裁进卡形，齿口直接透出下层页面，
 *   与 HTML 完全同构，过渡自然；齿口颜色 = 页面真实底色，任何主题自动正确。
 *
 * 性能：Path 在 onSizeChanged 缓存，draw 里只做一次 save/clip/restore，
 * 硬件加速下 clipPath 支持凹多边形（API 18+），120Hz 列表滚动不掉帧。
 *
 * 注意：锯齿最深处占右侧 8dp，卡片内已有 13dp padding，正文不会画到锯齿区。
 */
class PsNoteCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    /** 锯齿凹进深度（齿谷到卡右缘） */
    private val jagDepthPx = 8f * density
    /** 齿尖到卡右缘的留边（HTML 里齿尖不到达卡缘，留 2px 纸边） */
    private val jagLipPx = 2f * density
    /** 每个齿的高度 */
    private val jagPitchPx = 11f * density
    /** 左侧圆角半径（与便签 drawable 的 corners 一致） */
    private val cornerPx = 2f * density

    /** 完整卡形：左圆角 + 右锯齿（onSizeChanged 缓存） */
    private var jagClipPath = Path()

    /** 是否裁剪锯齿：回复楼 true，主楼 false（机身是完整矩形） */
    private var jagged = false

    init {
        orientation = VERTICAL
        clipChildren = false
        setWillNotDraw(false)
    }

    /** 切换主楼/回复楼外观时调用（由 PostAdapter 绑定） */
    fun setJagged(value: Boolean) {
        if (jagged == value) return
        jagged = value
        rebuildPath(width, height)
        invalidate()
    }

    /** 兼容保留：旧贴片方案需要随主题刷新齿口色，裁剪方案齿口透出页面、无需处理 */
    fun refreshEdgeColor() = Unit

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildPath(w, h)
    }

    private fun rebuildPath(w: Int, h: Int) {
        jagClipPath.reset()
        if (!jagged || w <= 0 || h <= 0) return
        val innerX = w - jagDepthPx      // 齿谷
        val outerX = w - jagLipPx        // 齿尖
        val path = Path()
        // 左上圆角 → 顶边 → 右缘锯齿之字下行 → 底边 → 左下圆角闭合
        path.moveTo(0f, cornerPx)
        path.quadTo(0f, 0f, cornerPx, 0f)
        path.lineTo(innerX, 0f)
        var y = 0f
        while (y + jagPitchPx <= h) {
            path.lineTo(outerX, y + jagPitchPx / 2f)
            y += jagPitchPx
            path.lineTo(innerX, y)
        }
        if (y < h) {
            // 余数不足一个整齿：半齿收底（齿尖落在右下角高度上）
            path.lineTo(outerX, h.toFloat())
        }
        path.lineTo(cornerPx, h.toFloat())
        path.quadTo(0f, h.toFloat(), 0f, h - cornerPx)
        path.close()
        jagClipPath = path
    }

    override fun draw(canvas: Canvas) {
        if (!jagged || jagClipPath.isEmpty) {
            super.draw(canvas)
            return
        }
        val save = canvas.save()
        canvas.clipPath(jagClipPath)
        super.draw(canvas)
        canvas.restoreToCount(save)
    }
}
