package com.example.s1client.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * 统一触觉反馈（对标 s1er 的 S1Haptics）：语义分级 + 全局开关。
 *
 * 级别语义：
 * - SELECTION：高频点选/切换/菜单项（脆、短，EFFECT_TICK）
 * - LIGHT：轻操作/下拉刷新/复制（EFFECT_TICK）
 * - MEDIUM：写入/收藏/投票/回复/确认提交（明确一下）
 * - HEAVY：破坏操作确认/错误（明显一下）
 * - SUCCESS：成功组合震「哒哒—嗒」（签到成功/回复成功/收藏成功）
 * - ERROR：失败三连重震（加载失败/操作失败）
 *
 * 全局开关 [enabled] 默认开，可在设置页关闭。所有调用自动尊重开关。
 */
object Haptic {
    @Volatile
    var enabled = true

    private const val PREFS = "s1_client_prefs"
    private const val KEY_HAPTIC = "haptic_enabled"

    /** 应用启动读取一次持久化的总开关 */
    fun loadEnabled(context: Context) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        enabled = sp.getBoolean(KEY_HAPTIC, true)
    }

    /** 切换并持久化总开关（对标 s1er 的 enabled 全局开关） */
    fun setEnabled(context: Context, value: Boolean) {
        enabled = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_HAPTIC, value).apply()
    }

    enum class Level { SELECTION, LIGHT, MEDIUM, HEAVY, SUCCESS, ERROR }

    fun feedback(ctx: Context, level: Level) {
        if (!enabled) return
        val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (level) {
                Level.SELECTION, Level.LIGHT ->
                    runCatching { v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)) }
                Level.MEDIUM ->
                    runCatching { v.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE)) }
                Level.HEAVY ->
                    runCatching { v.vibrate(VibrationEffect.createOneShot(42, VibrationEffect.DEFAULT_AMPLITUDE)) }
                // SUCCESS「哒哒—嗒」：短-短-停-长
                Level.SUCCESS -> runCatching {
                    v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 28, 70, 28, 70, 90), -1))
                }
                // ERROR 三连重震
                Level.ERROR -> runCatching {
                    v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 46, 80, 46, 80, 46), -1))
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val ms = when (level) {
                Level.SELECTION, Level.LIGHT -> 10L
                Level.MEDIUM -> 20L
                Level.HEAVY -> 46L
                Level.SUCCESS -> 60L
                Level.ERROR -> 90L
            }
            runCatching { v.vibrate(ms) }
        }
    }

    fun selection(ctx: Context) = feedback(ctx, Level.SELECTION)
    fun light(ctx: Context) = feedback(ctx, Level.LIGHT)
    fun medium(ctx: Context) = feedback(ctx, Level.MEDIUM)
    fun heavy(ctx: Context) = feedback(ctx, Level.HEAVY)
    fun success(ctx: Context) = feedback(ctx, Level.SUCCESS)
    fun error(ctx: Context) = feedback(ctx, Level.ERROR)
}
