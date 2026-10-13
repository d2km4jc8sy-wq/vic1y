package com.example.s1client.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

/**
 * 专门为 ViewPager2 内部设计的垂直 RecyclerView。
 *
 * 关键：当用户垂直滑动时，禁止父级 ViewPager2 拦截触摸事件；
 * 当用户水平滑动时，让 ViewPager2 接管，触发翻页。
 *
 * 这样就能避免"垂直列表滚到底时，多余的滑动手势被 ViewPager2 当翻页"的问题。
 */
class VerticalRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {

    private var initialX = 0f
    private var initialY = 0f

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = e.x
                initialY = e.y
                // 恢复父级拦截能力
                parent?.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(e.x - initialX)
                val dy = abs(e.y - initialY)

                // 只在第一次判定方向时决定谁接管
                if (dx > 8 || dy > 8) {
                    if (dy > dx) {
                        // 垂直方向：禁止 ViewPager2 拦截
                        parent?.requestDisallowInterceptTouchEvent(true)
                    } else {
                        // 水平方向：允许 ViewPager2 拦截
                        parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onInterceptTouchEvent(e)
    }
}
