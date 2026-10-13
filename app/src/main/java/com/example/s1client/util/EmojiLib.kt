package com.example.s1client.util

import android.content.Context

/**
 * 内置表情素材库。
 *
 * 素材放在 assets/emoticon/<分类>/<文件>，分类与文件名跟 S1 服务器
 * （static.stage1st.com/image/smiley/<分类>/<文件>）完全一致，
 * 所以插入时用 [img] 指向线上同一张图，发出去和官方表情显示一致，
 * 而面板里预览走本地 assets，断网也能翻。
 */
object EmojiLib {

    private const val ROOT = "emoticon"
    private const val CDN = "https://static.stage1st.com/image/smiley/"

    @Volatile
    private var cache: List<Pair<String, List<String>>>? = null

    /** 返回 [分类名 -> 该分类下文件名列表] */
    fun categories(context: Context): List<Pair<String, List<String>>> {
        cache?.let { return it }
        val result = mutableListOf<Pair<String, List<String>>>()
        val cats = runCatching { context.assets.list(ROOT) }.getOrNull().orEmpty().sorted()
        for (c in cats) {
            val files = runCatching { context.assets.list("$ROOT/$c") }
                .getOrNull().orEmpty().filter { it.isNotBlank() }.sorted()
            if (files.isNotEmpty()) result.add(c to files)
        }
        cache = result
        return result
    }

    /** 面板里的本地预览地址 */
    fun assetPath(cat: String, file: String): String = "file:///android_asset/$ROOT/$cat/$file"

    /** 线上地址（S1 服务器上的同一张图） */
    fun cdn(cat: String, file: String): String = "$CDN$cat/$file"

    /** 插入正文的 BBCode */
    fun bbcode(cat: String, file: String): String = "[img]${cdn(cat, file)}[/img]"

    /** 按编号找该分类下的真实文件名（含扩展名），找不到返回 null */
    fun fileForNumber(context: Context, cat: String, num: Int): String? {
        val files = categories(context).firstOrNull { it.first == cat }?.second ?: return null
        return files.firstOrNull { it.substringBefore('.').toIntOrNull() == num }
    }
}
