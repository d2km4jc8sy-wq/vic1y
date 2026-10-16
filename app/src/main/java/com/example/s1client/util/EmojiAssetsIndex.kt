package com.example.s1client.util

import android.content.Context

/**
 * 论坛表情的本地资产索引（性能优化 2026-10-16，优化报告 Phase1-⑤）。
 *
 * 原实现（PostAdapter.localEmojiUrl）对**每个表情、每次楼层绑定**都调
 * `ctx.assets.list("emoticon/<cat>")` —— 一次主线程磁盘枚举，长帖滚动时
 * 一个楼层有几个表情就做几次 I/O。
 *
 * 这里在首次使用时把全部分类的文件清单一次性扫进内存（8 个分类、千余个文件，
 * 一次扫描数毫秒，进程内只发生一次），之后所有命中判断都是纯集合查找。
 * 返回值语义与原实现逐字一致：本地命中返回 assets 路径，未命中返回 null（走网络兜底）。
 */
object EmojiAssetsIndex {

    private const val EMOJI_ROOT = "emoticon"

    /** 分类 -> 该分类下的文件名集合；懒加载，进程内只扫一次 */
    @Volatile
    private var index: Map<String, Set<String>>? = null

    private fun ensure(ctx: Context): Map<String, Set<String>> {
        index?.let { return it }
        synchronized(this) {
            index?.let { return it }
            val cats = ctx.assets.list(EMOJI_ROOT)?.toList().orEmpty()
            val map = HashMap<String, Set<String>>(cats.size)
            for (cat in cats) {
                map[cat] = ctx.assets.list("$EMOJI_ROOT/$cat")?.toSet().orEmpty()
            }
            index = map
            return map
        }
    }

    /**
     * 论坛 CDN 表情地址 → 本地 assets 路径。
     * 本地没有该文件时返回 null，调用方按原逻辑回落到 CDN 网络加载。
     */
    fun resolve(ctx: Context, url: String): String? {
        val (cat, file) = parseCdnUrl(url) ?: return null
        return resolveFrom(cat, file, ensure(ctx))
    }

    /** 从 CDN URL 解析 (分类, 文件名)；解析不出返回 null。纯函数，供单元测试覆盖 */
    internal fun parseCdnUrl(url: String): Pair<String, String>? {
        val m = Regex("""static\.stage1st\.com/image/smiley/([A-Za-z0-9_]+)/([^/?]+)""")
            .find(url) ?: return null
        return m.groupValues[1] to m.groupValues[2]
    }

    /** 按索引查本地路径；纯函数（不碰 Context），供单元测试覆盖 */
    internal fun resolveFrom(cat: String, file: String, index: Map<String, Set<String>>): String? =
        index[cat]?.takeIf { file in it }
            ?.let { "file:///android_asset/$EMOJI_ROOT/$cat/$file" }
}
