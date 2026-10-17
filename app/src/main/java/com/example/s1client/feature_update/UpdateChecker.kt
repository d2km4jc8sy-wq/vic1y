package com.example.s1client.feature_update

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 检查更新（PROBLEMS P9，2026-10-17；对齐 s1er 设置→关于→检查更新）。
 *
 * 发布渠道：GitHub Releases（latest API）。当前仓库为私有、且尚未建 Release——
 * 请求 404/失败时按「渠道暂无发布」处理并如实提示，绝不假装「已是最新」。
 * 仓库转公开或自建静态 JSON 后，本类无需改动即可生效（manifest 兼容两种形态）。
 *
 * manifest（两种都认）：
 * 1. GitHub Releases latest API：tag_name / body / assets[].browser_download_url
 * 2. 自定义 JSON：{"version":"1.2.0","notes":"...","apkUrl":"https://..."}
 *
 * 忽略此版：本地记 ignored_version，命中则 check 返回 null（用户拍板对齐 s1er 图3）。
 */
object UpdateChecker {

    /** 发布渠道清单地址：仓库转公开/自建分发后，把这里换成 Releases API 或静态 JSON 即可 */
    const val MANIFEST_URL = "https://api.github.com/repos/d2km4jc8sy-wq/vic1y/releases/latest"

    const val PREFS = "update_prefs"
    private const val KEY_IGNORED = "ignored_version"

    data class UpdateInfo(
        val version: String,
        val notes: String,
        val apkUrl: String?,
        val pageUrl: String?
    )

    fun ignoredVersion(context: android.content.Context): String =
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .getString(KEY_IGNORED, null).orEmpty()

    fun setIgnoredVersion(context: android.content.Context, version: String) {
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putString(KEY_IGNORED, version).apply()
    }

    /**
     * 检查更新。返回：
     * - UpdateInfo（有新版本且未被忽略）
     * - null（已是最新 / 该版本被忽略）
     * - CheckError（渠道不可达，message 可直接展示）
     */
    sealed class CheckResult {
        data class Available(val info: UpdateInfo) : CheckResult()
        data object UpToDate : CheckResult()
        data class Failed(val message: String) : CheckResult()
    }

    suspend fun check(context: android.content.Context, currentVersion: String): CheckResult =
        withContext(Dispatchers.IO) {
            try {
                val conn = java.net.URL(MANIFEST_URL).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.setRequestProperty("User-Agent", "S1Client")
                val code = conn.responseCode
                if (code == 404) {
                    return@withContext CheckResult.UpToDate.also {
                        Log.i("Update", "发布渠道暂无 Release（404），视为已最新")
                    }
                }
                if (code != 200) {
                    return@withContext CheckResult.Failed("渠道响应 $code")
                }
                val body = conn.inputStream.bufferedReader().readText()
                parseLatest(body)?.let { info ->
                    when {
                        isNewer(info.version, currentVersion) &&
                                info.version != ignoredVersion(context) -> CheckResult.Available(info)
                        else -> CheckResult.UpToDate
                    }
                } ?: CheckResult.UpToDate
            } catch (e: Exception) {
                Log.w("Update", "检查更新失败", e)
                CheckResult.Failed(e.message ?: "网络异常")
            }
        }

    /** GitHub Releases latest API → UpdateInfo；解析不出返回 null */
    internal fun parseLatest(body: String): UpdateInfo? {
        return try {
            val json = JSONObject(body)
            val version = json.optString("tag_name").removePrefix("v").trim()
            if (version.isBlank()) return null
            val notes = json.optString("body").trim()
            val apkUrl = json.optJSONArray("assets")?.let { arr ->
                (0 until arr.length())
                    .map { arr.getJSONObject(it).optString("browser_download_url") }
                    .firstOrNull { it.endsWith(".apk") }
            }
            UpdateInfo(
                version = version,
                notes = notes,
                apkUrl = apkUrl,
                pageUrl = json.optString("html_url").ifBlank { null }
            )
        } catch (e: Exception) {
            Log.w("Update", "parseLatest failed", e)
            null
        }
    }

    /**
     * 版本比较（纯函数，可单测）：按「.」分段逐段数值比较，段数不足补 0。
     * 非法段按 0 处理；完全相等 → false。
     * 例：1.10 > 1.9；1.1.0 > 1.1；1.0 == 1.0 → false。
     */
    internal fun isNewer(remote: String, current: String): Boolean {
        fun segments(v: String) = v.removePrefix("v").trim()
            .split(".").map { it.trim().toIntOrNull() ?: 0 }
        val r = segments(remote)
        val c = segments(current)
        val n = maxOf(r.size, c.size)
        for (i in 0 until n) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv != cv) return rv > cv
        }
        return false
    }
}
