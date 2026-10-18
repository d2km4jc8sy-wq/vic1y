package com.example.s1client.share

import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.ui.graphics.toArgb

/**
 * 分享卡的一楼数据（从 ThreadModels.Post 投影而来，便于跨 Activity 传递 / 解耦解析）。
 * 评分、签名档在帖子模型里没有现成字段，本版诚实只带「头像/时间/楼层」三类可选项。
 */
data class ShareCardPost(
    val author: String,
    val floorLabel: String,   // "楼主" 或 "N楼"
    val timeText: String,
    val content: String,
    val isOP: Boolean,
    /** 作者 UID：用于拉真头像（avatarUrl(uid)）；null 降级为首字母圆 */
    val authorId: String? = null,
    /** 正文图片 URL（PostContentParser 解出）；离屏渲染前由 ShareCardImages 预加载 */
    val imageUrls: List<String> = emptyList()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt() != 0,
        parcel.readString(),
        parcel.createStringArrayList() ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(author)
        parcel.writeString(floorLabel)
        parcel.writeString(timeText)
        parcel.writeString(content)
        parcel.writeInt(if (isOP) 1 else 0)
        parcel.writeString(authorId)
        parcel.writeStringList(imageUrls)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<ShareCardPost> {
        override fun createFromParcel(parcel: Parcel) = ShareCardPost(parcel)
        override fun newArray(size: Int): Array<ShareCardPost?> = arrayOfNulls(size)
    }
}

/** 卡片样式：A 经典文字 / B 极简竖杠 / C 卡片描边（默认） */
enum class CardStyle { CLASSIC, BARS, EDGE }

/** 卡片背景来源 */
enum class BackgroundMode { THEME, WHITE, BLACK }

/** 导出图片格式 */
enum class ImageFormat { PNG, JPG }

/** 卡片配置（2026-10-18 二问增补：正文图片开关） */
data class ShareCardConfig(
    val style: CardStyle = CardStyle.EDGE,
    val showAvatar: Boolean = true,
    val showTime: Boolean = true,
    val showFloor: Boolean = true,
    val showImages: Boolean = true,
    val backgroundMode: BackgroundMode = BackgroundMode.THEME,
    val format: ImageFormat = ImageFormat.PNG,
    val widthPx: Int = 1080
)

/** 卡片配色（由 backgroundMode + 当前主题色板推导，宪法18条：配色从主题派生） */
data class CardColors(
    val textMain: Int,
    val textSub: Int,
    val accent: Int,
    val heart: Int,
    val cardBg: Int,
    val onAccent: Int,
    val isDark: Boolean
)

/**
 * 把服务端时间戳（秒）或原始串整理成可读时间；解析失败原样返回。
 * 帖子详情里 dateline 多为 unix 秒串，列表里可能是已格式化串。
 */
fun fmtDateline(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val t = raw.toLongOrNull()
    if (t != null && t > 0) {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA)
            sdf.format(java.util.Date(t * 1000L))
        } catch (_: Exception) {
            raw
        }
    }
    return raw
}

/** 由设置 + 当前主题色板推导卡片配色 */
fun computeColors(config: ShareCardConfig): CardColors {
    return when (config.backgroundMode) {
        BackgroundMode.WHITE -> CardColors(
            textMain = 0xFF222222.toInt(),
            textSub = 0xFF888888.toInt(),
            accent = 0xFFA0522D.toInt(),
            heart = 0xFFFF2C55.toInt(),
            cardBg = 0xFFFFFFFF.toInt(),
            onAccent = 0xFFFFF8DC.toInt(),
            isDark = false
        )
        BackgroundMode.BLACK -> CardColors(
            textMain = 0xFFEAEAEA.toInt(),
            textSub = 0xFF999999.toInt(),
            accent = 0xFFE0A85A.toInt(),
            heart = 0xFFFF2C55.toInt(),
            cardBg = 0xFF0E0E0E.toInt(),
            onAccent = 0xFF0E0E0E.toInt(),
            isDark = true
        )
        BackgroundMode.THEME -> {
            val p = com.example.s1client.ui.theme.s1PaletteFor(
                com.example.s1client.util.ThemeManager.currentTheme
            )
            CardColors(
                textMain = p.textMain.toArgb(),
                textSub = p.textSub.toArgb(),
                accent = p.accent.toArgb(),
                heart = p.heart.toArgb(),
                cardBg = p.cardBg.toArgb(),
                onAccent = p.onAccent.toArgb(),
                isDark = p.isDark
            )
        }
    }
}

/** 取带 alpha 的颜色（用于分隔线等弱装饰） */
fun withAlpha(color: Int, alpha: Float): Int {
    val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
    return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
}
