package com.example.s1client.util

import android.content.Context

/**
 * S1 表情短码。
 *
 * 官方对「*2017」系列表情定义了短码 `[前缀:三位编号]`，例如 `[f:044]`
 * 就是 face2017/044 那张图。前缀与官方 EmoticonFactory 一致：
 *   a=animal2017  b=bundam2017  c=carton2017
 *   d=device2017  f=face2017    g=goose2017
 *
 * 服务端能直接识别这些短码（发帖页 BBCode 模式插入的就是它），
 * 所以正文里写短码即可，不需要任何转换。
 * 老表情（face / carton / animal …）没有短码，退回 `[img]` 长链接。
 */
object EmojiCode {

    private val CAT_TO_PREFIX = mapOf(
        "animal2017" to "a",
        "bundam2017" to "b",
        "carton2017" to "c",
        "device2017" to "d",
        "face2017" to "f",
        "goose2017" to "g",
    )

    private val PREFIX_TO_CAT: Map<String, String> =
        CAT_TO_PREFIX.entries.associate { (cat, p) -> p to cat }

    /** 正文里出现的短码（也用于预览解析） */
    private val CODE_REGEX = Regex("""\[[a-g]:\d{1,4}]""")

    /** 生成插入正文的短码；该分类不支持则返回 null（调用方回退 [img]） */
    fun shortCode(cat: String, file: String): String? {
        val prefix = CAT_TO_PREFIX[cat] ?: return null
        val num = file.substringBefore('.').toIntOrNull() ?: return null
        return "[$prefix:" + num.toString().padStart(3, '0') + "]"
    }

    /**
     * 把短码还原成可显示的图片地址（预览用）：
     * 本地 assets 里有就用本地（快、离线可用），否则回退到线上。
     * 不是可识别的短码则返回 null。
     */
    fun resolve(context: Context, code: String): String? {
        val m = CODE_REGEX.matchEntire(code) ?: return null
        val cat = PREFIX_TO_CAT[m.value.substring(1, 2)] ?: return null
        val num = m.value.substring(3, m.value.length - 1).toIntOrNull() ?: return null
        val file = EmojiLib.fileForNumber(context, cat, num)
        return if (file != null) EmojiLib.assetPath(cat, file)
        else EmojiLib.cdn(cat, num.toString().padStart(3, '0') + ".png")
    }
}
