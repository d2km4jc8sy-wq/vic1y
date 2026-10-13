package com.example.s1client.util

import com.example.s1client.data.model.DarkRoomEntry
import com.google.gson.JsonParser

/**
 * 小黑屋（公开处罚列表）解析。
 *
 * 接口口径抄自开源 s1er：
 * - URL  `forum.php?mod=misc&action=showdarkroom&ajaxdata=json[&cid=游标]`
 * - 解析 `lib/services/forum_tools_service.dart` 的 `parseDarkRoomJson`
 *
 * ⚠️ **最大的坑**：S1 返回的 `data` 是**裸数字键对象**，形如
 * ```json
 * {"message":"1|79003","data":{579096:{...},579209:{...}}}
 * ```
 * 数字键没加引号，**这不是合法 JSON**，Gson / org.json 解析必然抛异常。
 * s1er 在 Dart 侧专门写了 `ensureJson` 来兜这个坑（见其源码注释）。
 * 这里用正则把裸键补上引号后再交给 Gson —— 见 [normalizeNumericKeys]。
 */
object DarkRoomParser {

    data class Page(
        val items: List<DarkRoomEntry> = emptyList(),
        val nextCursor: String = "",
        /** 服务端用 "0|..." 表示已到底 */
        val dataExist: String = "1",
        val hasMore: Boolean = false,
        /** true 表示服务端要求登录 */
        val loginRequired: Boolean = false,
        val parseError: String = ""
    )

    /** 顶层外壳解析（登录校验在 [parse] 里做，这里只管规范化） */
    fun parse(rawBody: String, requestCursor: String = ""): Page {
        val body = rawBody.trim()
        if (body.isEmpty()) return Page(parseError = "无数据")

        // 未登录时服务端可能直接返回登录提示页
        if (body.contains("to_login") || body.contains("login_before_enter")) {
            return Page(loginRequired = true, parseError = "请先登录")
        }

        val root = try {
            JsonParser.parseString(normalizeNumericKeys(body))
        } catch (e: Exception) {
            return Page(parseError = "解析失败: ${e.javaClass.simpleName}")
        }
        if (!root.isJsonObject) return Page(parseError = "响应格式异常")

        val obj = root.asJsonObject

        // data 既可能是数组，也可能是「裸数字键对象」，两边都要收
        val items = mutableListOf<DarkRoomEntry>()
        val dataEl = obj.get("data")
        if (dataEl != null && dataEl.isJsonObject) {
            for ((_, v) in dataEl.asJsonObject.entrySet()) {
                if (v != null && v.isJsonObject) {
                    runCatching {
                        items.add(GsonHolder.gson.fromJson(v, DarkRoomEntry::class.java))
                    }
                }
            }
        } else if (dataEl != null && dataEl.isJsonArray) {
            for (v in dataEl.asJsonArray) {
                if (v != null && v.isJsonObject) {
                    runCatching {
                        items.add(GsonHolder.gson.fromJson(v, DarkRoomEntry::class.java))
                    }
                }
            }
        }

        // message 形如 "1|79003"：前段是"还有数据"标记，后段是下一页游标
        var dataExist = "1"
        var nextCursor = ""
        val message = obj.get("message")?.takeIf { !it.isJsonNull }?.asString
        if (!message.isNullOrBlank()) {
            val parts = message.split("|")
            dataExist = parts.getOrNull(0) ?: "1"
            nextCursor = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: ""
        }

        val hasMore = dataExist != "0" && items.isNotEmpty() &&
                nextCursor.isNotEmpty() && nextCursor != requestCursor

        return Page(
            items = items,
            nextCursor = nextCursor,
            dataExist = dataExist,
            hasMore = hasMore
        )
    }

    /**
     * 把 JSON 里的**裸数字键**补成合法键。
     *
     * 只处理两种位置：对象开头 `{` 之后、以及逗号 `,` 之后。
     * 真实响应里两者都有（`{579096:{...},579209:{...}}`），
     * 只处理 `{` 会漏掉第二个及之后的条目。
     *
     * 不做全文替换，避免误伤字符串内容里的 `{123` 之类。
     */
    fun normalizeNumericKeys(raw: String): String {
        val sb = StringBuilder(raw.length + 64)
        var i = 0
        val n = raw.length
        while (i < n) {
            val c = raw[i]
            // 对象起始 { 或键值分隔 ,
            if (c == '{' || c == ',') {
                var j = i + 1
                while (j < n && raw[j].isWhitespace()) j++
                if (j < n && raw[j].isDigit()) {
                    var k = j
                    while (k < n && raw[k].isDigit()) k++
                    if (k < n && raw[k] == ':') {
                        // 确实是裸数字键 → 补引号。
                        // 注意：只跳到冒号本身（i = k），冒号要照常输出；
                        // 早前写成 i = k + 1 会把 ':' 一起吞掉，产出 {"579096"{... 这种坏 JSON。
                        sb.append(c).append('"')
                        sb.append(raw, j, k)
                        sb.append('"')
                        i = k
                        continue
                    }
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }
}

/** 复用项目里配置好的 Gson（避免各处 new Gson 导致配置不一致） */
internal object GsonHolder {
    val gson: com.google.gson.Gson by lazy {
        com.google.gson.GsonBuilder()
            .setLenient()
            .create()
    }
}
