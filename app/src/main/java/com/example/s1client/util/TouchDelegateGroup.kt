package com.example.s1client.util

import android.graphics.Rect
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup

/**
 * 触达区外扩（阶段四 P15 无障碍整改，2026-10-17）。
 *
 * 问题：小尺寸可点击控件（如便签四小钮 22dp）周围存在「死区」——
 * 手指落在控件之外时事件穿透到卡片，什么都不发生。
 *
 * 方案：TouchDelegateGroup 把同一父容器内多个子控件的**隐形外扩触达区**
 * 组合挂到父容器上。事件落在哪个子控件的扩展矩形内，就转交给谁；
 * 落在所有扩展矩形之外时，走父容器原有分发（穿透到卡片）。
 *
 * 视觉零变化：只改事件路由，不改任何绘制/布局。
 */
class TouchDelegateGroup(private val parent: View) : TouchDelegate(Rect(), parent) {

    private data class Entry(val rect: Rect, val delegate: TouchDelegate)

    private val entries = mutableListOf<Entry>()

    fun addDelegate(rect: Rect, delegate: TouchDelegate) {
        entries.add(Entry(rect, delegate))
    }

    fun clear() = entries.clear()

    val isEmpty: Boolean
        get() = entries.isEmpty()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x.toInt()
        val y = event.y.toInt()
        // 最近邻路由（2026-10-17 防误触）：重叠带归矩形中心更近的钮，
        // 距离并列归先注册者；无命中走父容器原有分发（穿透到卡片）。
        val rects = entries.map { it.rect }
        val idx = TouchDelegateHelper.nearestHit(rects, x, y)
        return if (idx >= 0) entries[idx].delegate.onTouchEvent(event)
        else super.onTouchEvent(event)
    }
}
