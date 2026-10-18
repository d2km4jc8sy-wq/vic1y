package com.example.s1client.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** dp → px（按调用方上下文密度，保证不同设备一致） */
fun Context.dip(v: Int): Int = (v * resources.displayMetrics.density).toInt()

private fun circleBg(color: Int): GradientDrawable =
    GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

/**
 * 构建分享卡（纯 Android View，便于离屏渲染成位图）。
 * 三样式：CLASSIC 经典文字 / BARS 极简竖杠 / EDGE 卡片描边。
 * 配色全部来自 [CardColors]（主题派生，宪法18条）。
 *
 * 2026-10-18 二问升级（对齐 S1er 观感，设计者拍板）：
 * - 真头像：bitmaps 里有 authorId 对应头像则贴图（圆形裁剪），否则降级首字母圆
 * - 楼层徽章移到作者行右侧（S1er 同款），左侧元信息只留时间
 * - 正文下方贴楼层图片（圆角，最多 3 张，更多计“还有 N 张图”）
 * - 页脚不再提“扫码”（不做真二维码，诚实文案）
 *
 * 比例（宪法15条）：内边距 21/13、间距 8/13、圆角 21 取自斐波那契；
 * 标题 21sp : 元信息 13sp ≈ φ。主角=正文（字号最大、色最深），元信息弱化。
 */
fun buildCardView(
    ctx: Context,
    config: ShareCardConfig,
    posts: List<ShareCardPost>,
    colors: CardColors,
    title: String,
    bitmaps: Map<String, Bitmap> = emptyMap()
): View {
    val pad = ctx.dip(21)
    val padSm = ctx.dip(13)
    val gap8 = ctx.dip(8)

    val card = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(pad, pad, pad, pad)
    }

    // —— 背景 + 圆角 + （EDGE）液态玻璃渐变描边 ——
    val radius = ctx.dip(21).toFloat()
    if (config.style == CardStyle.EDGE) {
        val edge = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            cornerRadius = radius
        }
        // 浅色含蓄：顶亮 → 底暗（在 apply 外用 edge.colors 赋值，避免与 colors 参数重名）
        edge.colors = intArrayOf(0x33FFFFFF, 0x1AFFFFFF, 0x14000000)
        val inner = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(colors.cardBg)
            cornerRadius = radius
        }
        val layer = LayerDrawable(arrayOf<android.graphics.drawable.Drawable>(edge, inner))
        val inset = ctx.dip(1)
        layer.setLayerInset(1, inset, inset, inset, inset)
        card.background = layer
    } else {
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(colors.cardBg)
            cornerRadius = radius
        }
        card.background = bg
    }

    // —— 头部 ——
    when (config.style) {
        CardStyle.BARS -> {
            val barRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.BOTTOM
            }
            val hs = listOf(8, 14, 18, 12, 9)
            val al = listOf(0.4f, 0.6f, 1f, 0.7f, 0.45f)
            hs.forEachIndexed { i, h ->
                val bar = View(ctx).apply {
                    background = GradientDrawable().apply {
                        setColor(colors.accent)
                        alpha = (al[i] * 255).toInt()
                    }
                    layoutParams = LinearLayout.LayoutParams(ctx.dip(3), ctx.dip(h)).apply {
                        if (i > 0) leftMargin = ctx.dip(3)
                    }
                }
                barRow.addView(bar)
            }
            card.addView(barRow, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, ctx.dip(18)))
        }
        else -> {
            val head = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val accentBar = View(ctx).apply {
                background = GradientDrawable().apply {
                    setColor(colors.accent)
                    cornerRadius = ctx.dip(2).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(ctx.dip(3), ctx.dip(18))
            }
            val brand = TextView(ctx).apply {
                text = "S1"
                textSize = 15f
                setTextColor(colors.accent)
                typeface = Typeface.DEFAULT_BOLD
            }
            val sub = TextView(ctx).apply {
                text = "Stage1st"
                textSize = 12f
                setTextColor(colors.textSub)
            }
            val spacer = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
            }
            val tag = TextView(ctx).apply {
                text = "长图分享"
                textSize = 11f
                setTextColor(colors.textSub)
                alpha = 0.7f
            }
            head.addView(accentBar)
            head.addView(brand)
            head.addView(sub)
            (brand.layoutParams as LinearLayout.LayoutParams).leftMargin = gap8
            (sub.layoutParams as LinearLayout.LayoutParams).leftMargin = gap8
            head.addView(spacer)
            head.addView(tag)
            card.addView(head)
        }
    }

    // —— 标题（主角级：21sp 粗体） ——
    val titleTv = TextView(ctx).apply {
        text = title
        textSize = 21f
        setTextColor(colors.textMain)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.START
        setLineSpacing(ctx.dip(6).toFloat(), 1.0f)
        setPadding(0, padSm, 0, 0)
    }
    card.addView(titleTv)

    // —— 分隔线 ——
    val divider = View(ctx).apply {
        background = GradientDrawable().apply { setColor(withAlpha(colors.accent, 0.18f)) }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, ctx.dip(1)
        ).apply {
            topMargin = padSm
            bottomMargin = padSm
        }
    }
    card.addView(divider)

    // —— 各楼层 ——
    posts.forEach { p ->
        val postWrap = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }

        // 作者行
        val authorRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        if (config.showAvatar) {
            val avSize = ctx.dip(34)
            val avWrap = FrameLayout(ctx)
            // 降级底：首字母圆（拉不到头像时始终可见）
            val initial = (p.author.firstOrNull()?.toString() ?: "?")
            val letter = TextView(ctx).apply {
                text = initial
                textSize = 15f
                setTextColor(colors.onAccent)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                background = circleBg(colors.accent)
            }
            avWrap.addView(letter, FrameLayout.LayoutParams(avSize, avSize))
            // 真头像（预加载到位则盖在首字母圆上，圆形裁剪）
            p.authorId?.let { com.example.s1client.util.avatarUrl(it, "small") }
                ?.let { url -> bitmaps[url] }?.let { bmp ->
                    val iv = ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        clipToOutline = true
                        outlineProvider = object : ViewOutlineProvider() {
                            override fun getOutline(view: View, outline: Outline) {
                                outline.setOval(0, 0, view.width, view.height)
                            }
                        }
                        setImageBitmap(bmp)
                    }
                    avWrap.addView(iv, FrameLayout.LayoutParams(avSize, avSize))
                }
            avWrap.layoutParams = LinearLayout.LayoutParams(avSize, avSize).apply {
                rightMargin = gap8
            }
            authorRow.addView(avWrap)
        }
        val meta = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val name = TextView(ctx).apply {
            text = p.author
            textSize = 13f
            setTextColor(colors.textMain)
            typeface = Typeface.DEFAULT_BOLD
        }
        val subParts = mutableListOf<String>()
        if (config.showTime && p.timeText.isNotBlank()) subParts += p.timeText
        meta.addView(name)
        if (subParts.isNotEmpty()) {
            val sub = TextView(ctx).apply {
                text = subParts.joinToString(" · ")
                textSize = 12f
                setTextColor(colors.textSub)
            }
            meta.addView(sub)
        }
        authorRow.addView(meta, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        // 楼层徽章移到右侧（S1er 同款）：左侧人名+时间，右侧“楼主/N楼”药丸
        if (config.showFloor) {
            val pill = TextView(ctx).apply {
                text = p.floorLabel
                textSize = 11f
                setTextColor(colors.accent)
                background = GradientDrawable().apply {
                    cornerRadius = ctx.dip(8).toFloat()
                    setColor(withAlpha(colors.accent, 0.14f))
                }
                setPadding(ctx.dip(8), ctx.dip(3), ctx.dip(8), ctx.dip(3))
            }
            authorRow.addView(pill, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = gap8 })
        }
        postWrap.addView(authorRow)

        // 正文（主角：15sp，行距 1.75）
        val bodyText = if (p.content.isBlank()) "（无文字内容）" else p.content
        val body = TextView(ctx).apply {
            text = bodyText
            textSize = 15f
            setTextColor(colors.textMain)
            setLineSpacing(ctx.dip(8).toFloat(), 1.0f)
            gravity = Gravity.START
        }
        if (config.style == CardStyle.CLASSIC) {
            // 经典款：正文左侧加一道竖杠（引用感）
            val q = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            val leftBar = View(ctx).apply {
                background = GradientDrawable().apply { setColor(colors.accent) }
                layoutParams = LinearLayout.LayoutParams(ctx.dip(3), LinearLayout.LayoutParams.MATCH_PARENT)
            }
            body.setPadding(ctx.dip(13), 0, 0, 0)
            q.addView(leftBar)
            q.addView(body, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            postWrap.addView(q, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = gap8 })
        } else {
            body.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = gap8 }
            postWrap.addView(body)
        }

        // —— 正文图片（预加载到位才贴；圆角裁剪，最多 3 张） ——
        if (config.showImages && p.imageUrls.isNotEmpty()) {
            val shown = p.imageUrls.take(3)
            shown.forEach { url ->
                bitmaps[url]?.let { bmp ->
                    val iv = ImageView(ctx).apply {
                        setImageBitmap(bmp)
                        adjustViewBounds = true
                        clipToOutline = true
                        outlineProvider = object : ViewOutlineProvider() {
                            override fun getOutline(view: View, outline: Outline) {
                                outline.setRoundRect(0, 0, view.width, view.height, ctx.dip(13).toFloat())
                            }
                        }
                    }
                    postWrap.addView(iv, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = gap8 })
                }
            }
            if (p.imageUrls.size > shown.size) {
                val more = TextView(ctx).apply {
                    text = "还有 ${p.imageUrls.size - shown.size} 张图，原帖可见"
                    textSize = 12f
                    setTextColor(colors.textSub)
                }
                postWrap.addView(more, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = gap8 })
            }
        }

        card.addView(postWrap, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = padSm })
    }

    // —— 页脚 ——
    val footer = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    if (config.style != CardStyle.BARS) {
        val mono = TextView(ctx).apply {
            text = "S1"
            textSize = 13f
            setTextColor(colors.onAccent)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = circleBg(colors.accent)
        }
        mono.layoutParams = LinearLayout.LayoutParams(ctx.dip(34), ctx.dip(34)).apply {
            rightMargin = gap8
        }
        footer.addView(mono)
    }
    val ft = TextView(ctx).apply {
        // 不做真二维码，文案诚实化（2026-10-18 设计者拍板：不做真二维码）
        text = "来自 S1Client · Stage1st"
        textSize = 12f
        setTextColor(colors.textSub)
    }
    if (config.style == CardStyle.BARS) {
        ft.gravity = Gravity.END
        footer.gravity = Gravity.END
    }
    footer.addView(ft, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    card.addView(footer, LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = padSm })

    return card
}
