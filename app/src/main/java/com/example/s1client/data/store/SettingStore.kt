package com.example.s1client.data.store

import android.content.Context
import android.net.Uri
import android.os.Build
import org.json.JSONArray
import java.io.File

/**
 * 设置存储（对标 s1er 的 AppSettings / settings_store.dart）。
 *
 * key 沿用 s1er 命名，方便对照：
 * - post_tail_*   ← postSignatureEnabled / postSignatureShowDevice / postSignatureCustom
 * - font_size     ← fontSize（12/14/16/18 四档，默认 14）
 * - custom_font_* ← customFontFileName（字体文件固定存 filesDir/fonts/custom.ttf）
 * - hidden_forums ← hiddenForums（fid 集合，首页直接隐藏）
 *
 * 全部即时生效、即时持久化，无「保存」按钮（与 s1er 一致）。
 */
object SettingStore {

    private const val PREFS = "s1_client_prefs"

    /**
     * 设置变更版本号：任何影响全局渲染的设置（字号/字体）变化时 +1。
     * S1Theme 读取它触发整 App 重组，实现「改设置秒生效」。
     */
    val changes = androidx.compose.runtime.mutableIntStateOf(0)
    private fun bump() { changes.intValue++ }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ————————— 发帖小尾巴 —————————

    data class PostTail(
        val enabled: Boolean = true,
        val showDevice: Boolean = true,
        val custom: String = ""
    )

    fun postTail(context: Context): PostTail {
        val sp = prefs(context)
        return PostTail(
            enabled = sp.getBoolean("post_tail_enabled", true),
            showDevice = sp.getBoolean("post_tail_device", true),
            custom = sp.getString("post_tail_custom", "").orEmpty()
        )
    }

    fun setPostTail(context: Context, enabled: Boolean, showDevice: Boolean, custom: String) {
        prefs(context).edit()
            .putBoolean("post_tail_enabled", enabled)
            .putBoolean("post_tail_device", showDevice)
            .putString("post_tail_custom", cleanCustom(custom))
            .apply()
    }

    /** 自定义前缀清洗：去换行、去 BBCode 方括号、截 20 字（对标 s1er maxCustomLength） */
    private fun cleanCustom(raw: String): String =
        raw.replace("\n", " ").replace("[", "").replace("]", "").trim().take(20)

    private const val CLIENT_NAME = "S1Client"
    private const val CLIENT_URL = "https://stage1st.com/2b/"

    /**
     * 提交时拼接小尾巴（对标 PostSignature.appendIfEnabled）：
     * 正文 + 空一行 + [size=1][color=gray]——…[/color][/size]
     * 发帖 / 回复 / 编辑三个提交路径共用这一个函数。
     */
    fun appendPostTail(context: Context, message: String): String {
        val t = postTail(context)
        if (!t.enabled) return message
        val device = if (t.showDevice) Build.MODEL?.takeIf { it.isNotBlank() } ?: "Android" else null
        val line = when {
            device != null && t.custom.isNotBlank() ->
                "——${t.custom} · 来自 $device 上的 [url=$CLIENT_URL]$CLIENT_NAME 客户端[/url]"
            device != null ->
                "——来自 $device 上的 [url=$CLIENT_URL]$CLIENT_NAME 客户端[/url]"
            else ->
                "——来自 [url=$CLIENT_URL]$CLIENT_NAME 客户端[/url]"
        }
        val sig = "[size=1][color=gray]$line[/color][/size]"
        val body = message.trimEnd()
        if (body.isEmpty()) return sig
        return "$body\n\n$sig"
    }

    /** 编辑回填时剥离旧尾巴（对标 PostSignature.stripTrailing），输入框不显示旧落款 */
    fun stripTail(message: String): String =
        message.replace(Regex("""\[size=1]\[color=gray]——.*?\[/color]\[/size]\s*$"""), "").trimEnd()

    // ————————— 文字大小（四档，对标 fontSize 12/14/16/18） —————————

    val FONT_SIZES = intArrayOf(12, 14, 16, 18)
    private const val DEFAULT_BODY_SIZE = 14

    fun fontSize(context: Context): Int =
        prefs(context).getInt("font_size", DEFAULT_BODY_SIZE)

    fun setFontSize(context: Context, size: Int) {
        prefs(context).edit().putInt("font_size", size).apply()
        bump()
    }

    /** 实验功能：转盘翻页（详情页翻页每档 tick 触觉）——默认关 */
    fun dialPaging(context: Context): Boolean = prefs(context).getBoolean("dial_paging", false)
    fun setDialPaging(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean("dial_paging", on).apply()
        bump()
    }

    /** 全局文字缩放系数 = fontSize / 14 */
    fun fontScale(context: Context): Float = fontSize(context) / DEFAULT_BODY_SIZE.toFloat()

    // ————————— 字体导入（对标 FontImportService） —————————

    fun fontFile(context: Context): File {
        val dir = File(context.filesDir, "fonts")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "custom.ttf")
    }

    /** 当前自定义字体的展示名（原文件名），null = 用默认字体 */
    fun customFontName(context: Context): String? =
        prefs(context).getString("custom_font_file", null)
            ?.takeIf { fontFile(context).exists() }

    /**
     * 导入字体：只收 .ttf/.otf，复制到 filesDir/fonts/custom.ttf 覆盖保存。
     * 返回展示名（原文件名）。
     */
    fun importFont(context: Context, uri: Uri): Result<String> = runCatching {
        val name = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        } ?: "custom.ttf"
        if (!name.endsWith(".ttf", true) && !name.endsWith(".otf", true)) {
            error("只支持 .ttf / .otf 字体文件")
        }
        context.contentResolver.openInputStream(uri)?.use { input ->
            fontFile(context).outputStream().use { output -> input.copyTo(output) }
        } ?: error("无法读取所选文件")
        prefs(context).edit().putString("custom_font_file", name).apply()
        bump()
        name
    }

    /** 恢复默认字体：删文件 + 清 key（对标 removeCustomFont） */
    fun removeCustomFont(context: Context) {
        fontFile(context).delete()
        prefs(context).edit().remove("custom_font_file").apply()
        bump()
    }

    // ————————— 版块屏蔽（对标 hiddenForums：fid 集合，首页直接隐藏） —————————

    fun hiddenForumIds(context: Context): Set<String> {
        val raw = prefs(context).getString("hidden_forums", null) ?: return emptySet()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }.toSet()
        }.getOrDefault(emptySet())
    }

    fun setForumHidden(context: Context, fid: String, hidden: Boolean) {
        val cur = hiddenForumIds(context).toMutableSet()
        if (hidden) cur.add(fid) else cur.remove(fid)
        prefs(context).edit()
            .putString("hidden_forums", JSONArray(cur.toList()).toString())
            .apply()
    }

    fun clearHiddenForums(context: Context) {
        prefs(context).edit().remove("hidden_forums").apply()
    }

    /** 重置外观与功能偏好（对标 resetAppearanceSettings）：字号/字体恢复默认，不动版块屏蔽与历史数据 */
    fun resetAppearance(context: Context) {
        removeCustomFont(context)
        prefs(context).edit()
            .putInt("font_size", DEFAULT_BODY_SIZE)
            .apply()
        bump()
    }
}
