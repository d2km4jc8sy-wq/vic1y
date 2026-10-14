package com.example.s1client.ui

import android.view.View

/**
 * L2 级成功反馈（宪法第 16 条）。
 *
 * 只让"被点的那个元素"原地脉冲一圈：不做全屏彩带（L1 留给里程碑）、
 * 不做位移（避免页面抖动）。时长与幅度都刻意很小，属于"确认"而不是"表演"。
 *
 * **震动不在这里发**：按项目现有约定，动效与 toast `SUCCESS` 成对出现 ——
 * `S1Notify` 的 SUCCESS 类型会自动走 `Haptic.success`，由它负责触觉，
 * 避免同一件事震两下（参考收藏成功的爱心爆裂写法）。
 *
 * 现状审计：登录彩带 / 签到彩带 / 签到描边脉冲 / 收藏爱心爆裂这 4 处特效
 * 都已自带震动，全部符合第 16 条；本工具补的是**还缺视觉反馈的操作**。
 */
object PulseFeedback {

    /**
     * @param scale 峰值缩放，默认 1.22（够看见又不夸张）
     */
    fun once(view: View, scale: Float = 1.22f) {
        view.animate().cancel()
        // 从当前值归位，避免连续点击时残留缩放
        view.scaleX = 1f
        view.scaleY = 1f
        view.animate()
            .scaleX(scale).scaleY(scale)
            .setDuration(130)
            .withEndAction {
                view.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(190)
                    .start()
            }
            .start()
    }
}
