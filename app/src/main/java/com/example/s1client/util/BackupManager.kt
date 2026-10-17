package com.example.s1client.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 本地备份/恢复（2026-10-17 点4，对标 s1er G15）。
 *
 * 范围 = 列入 [FILES] 的 SharedPreferences 快照（JSON in ZIP）：
 * 设置 / 首页版面与已读 / 阅读历史 / 五级屏蔽体系 / 收藏 / 签到 / 草稿 /
 * 账号历史（仅 uid/用户名） / 网络参数。
 *
 * 明确不含：Cookie 与登录态（s1_user_prefs）、图片缓存、投票缓存
 * （投票标记是内存态，无持久化，重启即清——无数据可备份）。
 *
 * 恢复采用「覆盖写回 SP」：写完提示重启 App，各 Store 重 init 后生效。
 */
object BackupManager {

    private val FILES = listOf(
        "s1_client_prefs",      // SettingStore：字号/字体/小尾巴/上限类/阅读历史开关
        "s1_home_tabs",         // 首页版面配置 + 图片/头像三档 + 塞抹布名单 + 已读
        "s1_read_history",      // ReadHistoryStore
        "s1_blacklist",         // 关键词折叠
        "s1_thread_mutes",      // 本帖屏蔽
        "s1_hidden_floors",     // 单楼屏蔽
        "s1_hidden_posts",      // 整帖隐藏
        "fav_local",            // 本地收藏
        "s1_attendance",        // 签到
        "s1_editor_drafts",     // 草稿
        "s1_account_history",   // 账号历史（uid/用户名）
        "network_params"        // 网络参数页（强制网址/IP 直连）
    )

    /** 导出为 .zip（内含 backup.json）；返回备份的键总数 */
    fun export(context: Context, out: OutputStream): Result<Int> = runCatching {
        val root = JSONObject()
        var keys = 0
        for (name in FILES) {
            val sp = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val obj = JSONObject()
            for ((k, v) in sp.all) {
                when (v) {
                    is Boolean -> obj.put(k, v)
                    is Int -> obj.put(k, v)
                    is Long -> obj.put(k, v)
                    is Float -> obj.put(k, v.toDouble())
                    is String -> obj.put(k, v)
                    is Set<*> -> obj.put(k, JSONArray(v))
                }
            }
            if (obj.length() > 0) {
                root.put(name, obj)
                keys += obj.length()
            }
        }
        if (keys == 0) error("没有可备份的数据")
        ZipOutputStream(out).use { zos ->
            zos.putNextEntry(ZipEntry("backup.json"))
            zos.write(root.toString().toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        keys
    }

    /** 从 .zip 恢复；返回覆盖的 SP 模块数。恢复后需重启 App 生效 */
    fun import(context: Context, input: InputStream): Result<Int> = runCatching {
        var files = 0
        var found = false
        ZipInputStream(input).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "backup.json") {
                    found = true
                    val root = JSONObject(zis.readBytes().toString(Charsets.UTF_8))
                    for (name in FILES) {
                        if (!root.has(name)) continue
                        val obj = root.getJSONObject(name)
                        val ed = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit()
                        for (k in obj.keys()) {
                            when (val v = obj.get(k)) {
                                is Boolean -> ed.putBoolean(k, v)
                                is Int -> ed.putInt(k, v)
                                is Long -> ed.putLong(k, v)
                                is Double -> ed.putFloat(k, v.toFloat())
                                is String -> ed.putString(k, v)
                                is JSONArray -> ed.putStringSet(
                                    k,
                                    (0 until v.length()).mapNotNull { j ->
                                        v.optString(j).takeIf { s -> s.isNotBlank() }
                                    }.toSet()
                                )
                            }
                        }
                        ed.apply()
                        files++
                    }
                    break
                }
                entry = zis.nextEntry
            }
        }
        if (!found) error("备份文件缺少 backup.json（请选择本应用导出的 .zip）")
        files
    }
}
