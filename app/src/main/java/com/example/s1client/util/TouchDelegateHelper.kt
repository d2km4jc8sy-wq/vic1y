package com.example.s1client.util

import android.graphics.Rect
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup

/**
 * 触达区外扩助手（阶段四 P15，2026-10-17）。
 *
 * [expandHalo]：给 [children] 里每个子 view 的**隐形触达区**向四周外扩
 * [haloPx]（视觉零变化——不改绘制/布局/大小），并把组合代理挂到 [parent]。
 *
 * 典型场景：22dp 的小圆钮外扩 8dp 达到 38dp（Android 无障碍推荐最小 48dp、
 * 本项目宪法第 12 条约定 38dp）——解决「按钮周围点了没反应」的死区问题。
 *
 * 纯函数 [expandRect] 可单测（外扩 + 裁剪到父容器边界的数学）。
 */
object TouchDelegateHelper {

    /** 外扩并裁剪到父容器范围内（纯函数，可单测） */
    fun expandRect(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        halo: Int,
        parentWidth: Int,
        parentHeight: Int
    ): Rect {
        val r = Rect(left, top, right, bottom)
        r.inset(-halo, -halo)
        if (!r.intersect(0, 0, parentWidth, parentHeight)) {
            return Rect()
        }
        return r
    }

    /**
     * 给 [children] 的每个 view 外扩 [haloPx] 的隐形触达区并挂组合代理。
     * parent 尚未布局（宽高为 0）时通过 post 延迟到布局完成后再挂。
     */
    fun expandHalo(parent: ViewGroup, children: List<View>, haloPx: Int) {
        parent.post {
            if (parent.width == 0 || parent.height == 0) return@post
            val group = TouchDelegateGroup(parent)
            for (v in children) {
                val r = Rect()
                v.getHitRect(r)
                val expanded = expandRect(r.left, r.top, r.right, r.bottom, haloPx, parent.width, parent.height)
                if (!expanded.isEmpty) {
                    group.addDelegate(expanded, TouchDelegate(expanded, v))
                }
            }
            parent.touchDelegate = if (group.isEmpty) null else group
        }
    }
}
