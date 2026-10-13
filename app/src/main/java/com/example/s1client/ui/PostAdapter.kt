package com.example.s1client.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.RelativeSizeSpan
import android.text.TextPaint
import android.text.TextUtils
import android.text.util.Linkify
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.s1client.util.S1Notify
import com.example.s1client.util.BlockedAuthorManager
import com.example.s1client.util.ThreadMutedUsers
import com.example.s1client.util.ExpandedFloors
import com.example.s1client.util.HiddenFloors
import androidx.appcompat.app.AlertDialog
import androidx.core.text.util.LinkifyCompat
import androidx.recyclerview.widget.RecyclerView
import coil.Coil
import coil.load
import coil.request.ImageRequest
import coil.target.Target
import com.example.s1client.ImageViewerActivity
import com.example.s1client.R
import com.example.s1client.data.model.ForumDataCache
import com.example.s1client.data.model.Post
import com.example.s1client.databinding.ItemPostBinding
import com.example.s1client.util.ImageDownloader
import com.example.s1client.util.PostContentParser
import com.example.s1client.util.TimeUtil
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

class PostAdapter(
    private val posts: List<Post>,
    private val subject: String = "",
    private val tid: String = "",
    private val onAvatarClick: ((Post) -> Unit)? = null,
    private val onEditClick: ((Post) -> Unit)? = null,
    private val onRateClick: ((Post) -> Unit)? = null,
    private val onQuoteClick: ((Post) -> Unit)? = null,
    /** 点收藏图标：楼层收藏/取消收藏（回调里由 Activity 操作本地快照） */
    private val onFavoriteClick: ((Post) -> Unit)? = null,
    /** 楼层「⋯」菜单：整菜单交给 Activity 组织（它才知道 tid/是否本人） */
    private val onMoreClick: ((Post) -> Unit)? = null,
    /** pid -> 评分聚合（网页版 ratelog），取不到就隐藏评分行 */
    private val rateProvider: (String?) -> com.example.s1client.util.RateInfo? = { null },
    /** 点评分行：展示逐条评分明细 */
    private val onRatingRowClick: ((Post) -> Unit)? = null,
    /** pid -> 战斗力（网页版 viewthread HTML 解析回填），取不到就隐藏 */
    private val powerProvider: (String?) -> String? = { null },
    /** 点独立引用卡片：传出被引楼号（字符串数字）用于同页定位跳转 */
    private val onPostQuoteJump: ((String?) -> Unit)? = null,
    /** pid -> 是否已被本地楼层收藏 */
    private val bookmarkProvider: (String?) -> Boolean = { false },
    /** 多选分享：是否处于多选模式（开启后整卡点按=勾选/取消，操作行与图片点击屏蔽）。
     *  必须是可更新的 var：进入/退出多选时由 PageAdapter 同步给已创建的各页 PostAdapter，
     *  否则构造时捕获的 false 会让多选分支永不执行。 */
    var shareSelectMode: Boolean = false,
    /** pid 是否已在选中集合 */
    private val isShareSelected: (Post) -> Boolean = { false },
    /** 点按卡片的回调（勾选/取消），由 Activity 注入 */
    private val onShareToggle: ((Post) -> Unit)? = null
) : RecyclerView.Adapter<PostAdapter.VH>() {

    // 单楼展开状态改由 util.ExpandedFloors 统一管理（按 tid|pid 持久化），此处不再维护内存集合

    /**
     * 黑名单判定缓存：onBindViewHolder 每绑一次原本都要读 SP 解析，长帖滚动反复主线程读 SP 掉帧。
     * 这里在首个楼层绑定后缓存一次（整个 Adapter 生命周期内名单不变），后续只做集合查找。
     */
    private var _cachedBlockedUsers: Set<String>? = null
    private var _cachedBlockedWords: Set<String>? = null

    inner class VH(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val post = posts[position]
        val ctx = holder.itemView.context
        val density = ctx.resources.displayMetrics.density
        val emojiSize = (18 * density).toInt()

        val authorName = post.author ?: "未知"
        holder.binding.tvPostAuthor.text = authorName

        // 战斗力：金色小 pill，按 uid 从 Profile API 缓存取（楼层 JSON 无此字段）；
        // 取不到用 INVISIBLE 占位，避免 GONE→VISIBLE 引发重绘闪动。
        val powerStr = powerProvider(post.authorId)
        if (powerStr != null) {
            holder.binding.tvPower.visibility = View.VISIBLE
            holder.binding.tvPower.text = "⚔ $powerStr"
        } else {
            holder.binding.tvPower.visibility = View.GONE
        }

        // 自己发的楼层昵称用强调色高亮（移动版 API 不回用户组，管理等用户组高亮暂时做不了）
        val myUid = ForumDataCache.memberUid
        val isSelf = myUid.isNotEmpty() && myUid != "0" && post.authorId == myUid
        val nameColorTv = TypedValue()
        ctx.theme.resolveAttribute(
            if (isSelf) com.example.s1client.R.attr.s1Accent
            else com.example.s1client.R.attr.s1TextSecondary,
            nameColorTv, true)
        holder.binding.tvPostAuthor.setTextColor(nameColorTv.data)
        holder.binding.tvPostAuthor.typeface =
            if (isSelf) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

        val timeStr = TimeUtil.formatSmart(post.dateline)
        holder.binding.tvPostTime.text = timeStr

        val isFirst = post.first == "1"
        if (isFirst) {
            // 楼主层：时间挪到标题下的 meta 行，底部操作行不再重复显示
            holder.binding.llOpMeta.visibility = View.VISIBLE
            holder.binding.tvOpTime.text = timeStr
            holder.binding.tvPostTime.visibility = View.GONE
        } else {
            holder.binding.llOpMeta.visibility = View.GONE
            holder.binding.tvPostTime.visibility =
                if (timeStr.isBlank()) View.GONE else View.VISIBLE
        }

        // 楼主层的作者信息已经上移到顶栏，楼层内不再重复
        holder.binding.llAuthorRow.visibility = if (isFirst) View.GONE else View.VISIBLE
        if (isFirst) {
            holder.binding.tvPostFloor.text = "楼主"
            if (subject.isNotBlank()) {
                holder.binding.tvPostTitle.text = subject
                holder.binding.tvPostTitle.visibility = View.VISIBLE
            } else {
                holder.binding.tvPostTitle.visibility = View.GONE
            }
        } else {
            holder.binding.tvPostTitle.visibility = View.GONE
            val floorNum = post.position ?: (position + 1).toString()
            holder.binding.tvPostFloor.text = floorNum + "楼"
        }

        val letter = authorName.firstOrNull()?.toString() ?: "?"
        holder.binding.tvPostAvatarLetter.text = letter
        holder.binding.tvPostAvatarLetter.background = makeCircleGradient(authorName.hashCode())

        holder.binding.ivPostAvatar.alpha = 0f
        post.authorId?.let { buildAvatarUrl(it) }?.let { url ->
            holder.binding.ivPostAvatar.load(url) {
                listener(
                    onSuccess = { _, _ -> holder.binding.ivPostAvatar.alpha = 1f },
                    onError = { _, _ -> holder.binding.ivPostAvatar.alpha = 0f }
                )
            }
        }

        if (shareSelectMode) {
            holder.binding.avatarClickArea.setOnClickListener(null)
            // 关闭可点击性，避免头像区吞掉整卡点按（否则勾选不生效）
            holder.binding.avatarClickArea.isClickable = false
            holder.binding.avatarClickArea.isFocusable = false
        } else {
            holder.binding.avatarClickArea.setOnClickListener {
                onAvatarClick?.invoke(post)
            }
        }

        // 操作行：评分 / 引用 / 收藏 / ⋯（编辑已挪进「⋯」菜单）。
        // 图标统一走 s1ActionIcon，避免各矢量自带的颜色（ic_star 是金色）不一致
        val actionTv = TypedValue()
        ctx.theme.resolveAttribute(com.example.s1client.R.attr.s1ActionIcon, actionTv, true)
        val actionTint = android.content.res.ColorStateList.valueOf(actionTv.data)
        holder.binding.btnRate.imageTintList = actionTint
        holder.binding.btnQuote.imageTintList = actionTint
        holder.binding.btnFavoritePost.imageTintList = actionTint
        holder.binding.btnMore.imageTintList = actionTint

        // ===== 楼层卡片化：圆角卡片底 + 描边；选中态切换为强调色描边 + 浅填充 =====
        val cardTv = TypedValue()
        ctx.theme.resolveAttribute(com.example.s1client.R.attr.s1BgCard, cardTv, true)
        val borderTv = TypedValue()
        ctx.theme.resolveAttribute(com.example.s1client.R.attr.s1BorderCard, borderTv, true)
        val accentTv = TypedValue()
        ctx.theme.resolveAttribute(com.example.s1client.R.attr.s1Accent, accentTv, true)
        val accent = accentTv.data

        if (shareSelectMode) {
            // 选中高亮直接画在 background（绘制在子 View 之下，绝不遮挡右上角勾选徽标）
            holder.binding.root.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16 * density
                setStroke((2 * density).toInt(), accent)   // 加粗到 2dp 更醒目
                setColor(android.graphics.Color.argb(
                    26,  // ≈10% 强调色填充，明显但不刺眼
                    android.graphics.Color.red(accent),
                    android.graphics.Color.green(accent),
                    android.graphics.Color.blue(accent)))
            }
            holder.binding.llActions.visibility = View.GONE
            holder.binding.ivSelectBadge.visibility = View.VISIBLE
            if (isShareSelected(post)) {
                // 选中：accent 实心圆盘 + 白色对勾（白色底衬透出对勾，清晰落在卡片右上角）
                holder.binding.ivSelectBadge.setImageResource(R.drawable.ic_check_circle)
                holder.binding.ivSelectBadge.setColorFilter(accent)
            } else {
                // 未选：tertiary 圆环（白色底衬透出空心圈，像未勾选的复选框）
                holder.binding.ivSelectBadge.setImageResource(R.drawable.ic_circle_outline)
                val tertiaryTv = TypedValue()
                ctx.theme.resolveAttribute(
                    com.example.s1client.R.attr.s1TextTertiary, tertiaryTv, true)
                holder.binding.ivSelectBadge.setColorFilter(tertiaryTv.data)
            }
            holder.binding.root.isClickable = true
            holder.binding.root.setOnClickListener { onShareToggle?.invoke(post) }
            holder.binding.ivSelectBadge.setOnClickListener { onShareToggle?.invoke(post) }
            // 复用 ViewHolder 时清掉旧的非多选态链接点击，否则正文仍会吞掉整卡点按
            holder.binding.tvPostMessage.movementMethod = null
        } else {
            // 普通态：s1BgCard 圆角底 + 1dp 描边，徽标隐藏
            holder.binding.root.background = GradientDrawable().apply {
                setColor(cardTv.data)
                cornerRadius = 16 * density
                setStroke((1 * density).toInt(), borderTv.data)
            }
            holder.binding.ivSelectBadge.visibility = View.GONE
            holder.binding.root.setOnClickListener(null)
            holder.binding.llActions.visibility = View.VISIBLE
        }

        // 按压缩放反馈（不消费事件，点击/长按不受影响）
        holder.binding.root.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN ->
                    v.animate().scaleX(0.985f).scaleY(0.985f).setDuration(110).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }
            false
        }

        holder.binding.btnRate.setOnClickListener { onRateClick?.invoke(post) }
        holder.binding.btnQuote.setOnClickListener { onQuoteClick?.invoke(post) }
        holder.binding.btnFavoritePost.setOnClickListener { onFavoriteClick?.invoke(post) }
        holder.binding.btnMore.setOnClickListener { onMoreClick?.invoke(post) }

        // 评分行：数据来自网页版 HTML 的 ratelog（移动版 API 没有）。
        // 没加载到用 INVISIBLE 占位，避免 GONE→VISIBLE 把下方整段往下推造成闪动。
        val rate = rateProvider(post.pid)
        if (rate != null) {
            holder.binding.llRating.visibility = View.VISIBLE
            holder.binding.tvRatingInfo.text = rate.summary
            if (shareSelectMode) {
                // 多选模式：评分行不可点，整卡点按统一走勾选
                holder.binding.llRating.isClickable = false
                holder.binding.llRating.setOnClickListener(null)
            } else {
                holder.binding.llRating.setOnClickListener { onRatingRowClick?.invoke(post) }
            }
        } else {
            holder.binding.llRating.visibility = View.GONE
            holder.binding.llRating.setOnClickListener(null)
        }

        // 操作行外露：引用 / 评分 / 收藏 常驻（评分行汇总仅在确有评分时出现，
        // 但发起评分的按钮对任意楼层都可用，所以这里常驻可见）
        holder.binding.btnQuote.visibility = View.VISIBLE
        holder.binding.btnFavoritePost.visibility = View.VISIBLE
        holder.binding.btnRate.visibility = View.VISIBLE

        // 楼层收藏：固定用书签图标，**不做实心/空心状态变化**。
        // 理由（用户拍板）：楼层收藏与帖子收藏都用星星会让人分不清自己在收藏什么，
        // 而实心/空心切换又要求用户"看着图标找状态"，反而更费解。
        // 现在：书签 = 收藏这条楼层；星星 = 收藏整个帖子。形状不同，含义就一目了然，
        // 是否已收藏由点击后的 toast 告知即可。
        holder.binding.btnFavoritePost.setImageResource(R.drawable.ic_bookmark_outline)

        val parsed = PostContentParser.parse(post.message)

        // ===== 折叠命中：整层折叠成一行，点一下可展开看原文 =====
        // 命中判定走缓存（首绑后只查集合，不再每次读 SP），消除长帖滚动掉帧
        // 三类折叠作用域：全局黑名单（作者 / 关键词）、本帖屏蔽某人（仅本 tid）
        val blocked = run {
            val users = _cachedBlockedUsers
                ?: com.example.s1client.util.BlockedAuthorManager.allBlocked().keys
                    .also { _cachedBlockedUsers = it }
            val words = _cachedBlockedWords
                ?: com.example.s1client.util.BlacklistManager.blockedWords(ctx)
                    .also { _cachedBlockedWords = it }
            users.contains(post.authorId) ||
                    (parsed.text.isNotBlank() && words.any { w -> parsed.text.toString().contains(w, ignoreCase = true) })
        }
        val threadMuted = tid.isNotBlank() && post.authorId != null &&
                com.example.s1client.util.ThreadMutedUsers.isMuted(tid, post.authorId)
        // 单楼屏蔽：仅在本帖藏掉这一层（pid 精确），其余楼层不受影响
        val hiddenFloor = tid.isNotBlank() && post.pid != null &&
                com.example.s1client.util.HiddenFloors.isHidden(tid, post.pid)
        // 单楼展开：按 pid 精确、持久化；只恢复这一楼，不影响该人其他楼层
        val expandedOnce = com.example.s1client.util.ExpandedFloors.isExpanded(tid, post.pid)
        // 折叠优先级：全局拉黑 / 本帖屏蔽此人 / 屏蔽此楼 > 单楼手动展开（例外）
        val folded = (blocked || threadMuted || hiddenFloor) && !expandedOnce
        val foldMsg = when {
            folded && blocked -> "该楼层已被黑名单折叠（${authorName}）· 点击展开"
            folded && threadMuted -> "已在本帖屏蔽此人的回复（${authorName}）· 点击展开"
            folded && hiddenFloor -> "已屏蔽此楼 · 点击展开"
            else -> null
        }
        if (foldMsg != null) {
            val hint = holder.binding.tvPostMessage
            holder.binding.llTopImages.visibility = View.GONE
            holder.binding.llImages.visibility = View.GONE
            holder.binding.llRating.visibility = View.GONE
            holder.binding.tvPostTitle.visibility = View.GONE
            holder.binding.llOpMeta.visibility = View.GONE
            holder.binding.tvPostFloor.text = if (isFirst) "楼主" else "${post.position ?: (position + 1)}楼"
            val spannable = android.text.SpannableString(foldMsg)
            val secTv = TypedValue()
            ctx.theme.resolveAttribute(
                com.example.s1client.R.attr.s1TextTertiary, secTv, true)
            spannable.setSpan(
                android.text.style.ForegroundColorSpan(secTv.data),
                0, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(android.text.style.RelativeSizeSpan(0.88f),
                0, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            hint.text = spannable
            hint.setOnClickListener {
                // 只恢复这一楼：按 pid 持久化记录，该人其余楼层仍保持折叠
                com.example.s1client.util.ExpandedFloors.expand(tid, post.pid)
                notifyItemChanged(position)
            }
            holder.binding.llQuote.visibility = View.GONE
            holder.binding.tvPower.visibility = View.GONE
            holder.binding.tvExpand.visibility = View.GONE
            return
        }

        val displayText = parsed.text.toString()
        val ssb = SpannableString(displayText)

        // ===== 代码块：等宽字体 + 底色块 =====
        if (parsed.codeBlocks.isNotEmpty()) {
            val codeBgTv = TypedValue()
            ctx.theme.resolveAttribute(
                com.example.s1client.R.attr.s1QuoteBg, codeBgTv, true)
            for ((cs, ce) in parsed.codeBlocks) {
                if (ce <= cs || ce > ssb.length) continue
                ssb.setSpan(android.text.style.TypefaceSpan("monospace"),
                    cs, ce, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                ssb.setSpan(
                    com.example.s1client.util.CodeBlockSpan(codeBgTv.data, ctx),
                    cs, ce, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                ssb.setSpan(RelativeSizeSpan(0.9f),
                    cs, ce, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        // ===== 列表项：圆点项目符号 =====
        if (parsed.listItems.isNotEmpty()) {
            val bulletTv = TypedValue()
            ctx.theme.resolveAttribute(
                com.example.s1client.R.attr.s1TextSecondary, bulletTv, true)
            val gap = (6 * density).toInt()
            for ((ls, le) in parsed.listItems) {
                if (le <= ls || le > ssb.length) continue
                ssb.setSpan(android.text.style.BulletSpan(gap, bulletTv.data),
                    ls, le, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        var emojiIdx = 0
        val spansToLoad = mutableListOf<Pair<Int, String>>()
        for (i in 0 until ssb.length) {
            if (ssb[i].code == 0xFFFC) {
                val url = parsed.emojiUrls.getOrNull(emojiIdx) ?: continue
                emojiIdx++
                val ph = ColorDrawable(0x00000000)
                ph.setBounds(0, 0, emojiSize, emojiSize)
                ssb.setSpan(ImageSpan(ph, ImageSpan.ALIGN_BASELINE),
                    i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spansToLoad.add(Pair(i, url))
            }
        }
        holder.binding.tvPostMessage.text = ssb

        // 独立引用卡片：正文已剔除引用段，这里单独渲染被引内容；点击跳转到被引楼层
        val qText = parsed.quoteText.replace("\uFFFC", "").trim()
        if (qText.isNotBlank()) {
            holder.binding.llQuote.visibility = View.VISIBLE
            val qFloor = parsed.quoteFloor
            holder.binding.tvQuoteAuthor.text = if (qFloor != null) "引用 · ${qFloor}楼" else "引用"
            holder.binding.tvQuoteBody.text = qText
            if (shareSelectMode) {
                holder.binding.llQuote.isClickable = false
                holder.binding.llQuote.setOnClickListener(null)
            } else if (qFloor != null) {
                holder.binding.llQuote.isClickable = true
                holder.binding.llQuote.setOnClickListener { onPostQuoteJump?.invoke(qFloor) }
            } else {
                holder.binding.llQuote.isClickable = false
                holder.binding.llQuote.setOnClickListener(null)
            }
        } else {
            holder.binding.llQuote.visibility = View.GONE
        }

        // 长文折叠：同步测自然行数（用卡片可用宽度），首帧即定稿，
        // 避免 GONE→VISIBLE 在布局完成后再切换造成的「闪一下」。
        val tvMessage = holder.binding.tvPostMessage
        val contentWpx = ((ctx.resources.displayMetrics.widthPixels / density) - 48f) * density
        tvMessage.maxLines = 12
        tvMessage.ellipsize = TextUtils.TruncateAt.END
        holder.binding.tvExpand.setOnClickListener(null)
        holder.binding.tvExpand.visibility = View.GONE
        val textWidth = (contentWpx - tvMessage.compoundPaddingLeft - tvMessage.compoundPaddingRight).toInt()
        val naturalLines = if (textWidth > 0) kotlin.runCatching {
            val layout = android.text.StaticLayout.Builder.obtain(
                displayText, 0, displayText.length, TextPaint(tvMessage.paint), textWidth
            )
                .setLineSpacing(tvMessage.lineSpacingExtra, tvMessage.lineSpacingMultiplier)
                .build()
            layout.lineCount
        }.getOrNull() ?: 0 else 0
        if (naturalLines > 12) {
            tvMessage.maxLines = 12
            tvMessage.ellipsize = TextUtils.TruncateAt.END
            holder.binding.tvExpand.visibility = View.VISIBLE
            holder.binding.tvExpand.text = "展开全文 ›"
            var expanded = false
            holder.binding.tvExpand.setOnClickListener {
                expanded = !expanded
                tvMessage.maxLines = if (expanded) Int.MAX_VALUE else 12
                tvMessage.ellipsize = if (expanded) null else TextUtils.TruncateAt.END
                holder.binding.tvExpand.text =
                    if (expanded) "收起 ‹" else "展开全文 ›"
            }
        } else {
            tvMessage.maxLines = Int.MAX_VALUE
            tvMessage.ellipsize = null
            holder.binding.tvExpand.visibility = View.GONE
        }

        // ===== 链接识别：URL 自动高亮成强调色 + 可点击跳浏览器 =====
        val accentLinkTv = TypedValue()
        ctx.theme.resolveAttribute(com.example.s1client.R.attr.s1Accent, accentLinkTv, true)
        holder.binding.tvPostMessage.setLinkTextColor(accentLinkTv.data)
        // 选区高亮色：用 20% 强调色，拖拽选区才看得见；点链接高亮也不会太刺眼
        holder.binding.tvPostMessage.highlightColor =
            (accentLinkTv.data and 0x00FFFFFF.toInt()) or 0x33000000.toInt()
        if (!shareSelectMode && LinkifyCompat.addLinks(holder.binding.tvPostMessage, Linkify.WEB_URLS)) {
            holder.binding.tvPostMessage.movementMethod = LinkMovementMethod.getInstance()
        }
        // 正文长按 = 系统文本选择（拖拽复制部分段落）；整卡长按不再复制（避免误触）

        val imageLoader = Coil.imageLoader(ctx)
        for ((startPos, url) in spansToLoad) {
            // 表情本地优先：assets/emoticon 里有的直接用本地，秒出且断网可用；
            // 缺的才回落到论坛 CDN
            val emojiSrc = localEmojiUrl(ctx, url) ?: url
            val target = object : Target {
                override fun onStart(placeholder: Drawable?) {}
                override fun onError(error: Drawable?) {
                    // 本地没命中、网络也挂了，再试一次 CDN
                    if (emojiSrc != url) loadEmoji(ctx, ssb, startPos, emojiSize, url, holder, position)
                }
                override fun onSuccess(result: Drawable) {
                    result.setBounds(0, 0, emojiSize, emojiSize)
                    ssb.setSpan(ImageSpan(result, ImageSpan.ALIGN_BASELINE),
                        startPos, startPos + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    if (holder.bindingAdapterPosition == position) {
                        holder.binding.tvPostMessage.text = ssb
                    }
                }
            }
            val req = ImageRequest.Builder(ctx)
                .data(emojiSrc).size(emojiSize, emojiSize).target(target).build()
            imageLoader.enqueue(req)
        }

        // ===== 图片：楼主层置顶（像小红书那样放最上面），其余楼层放正文下方 =====
        val topContainer = holder.binding.llTopImages
        val container = holder.binding.llImages
        topContainer.removeAllViews()
        container.removeAllViews()
        val allUrls = parsed.imageUrls
        val host = if (isFirst) topContainer else container
        topContainer.visibility = if (isFirst && allUrls.isNotEmpty()) View.VISIBLE else View.GONE
        container.visibility = if (!isFirst && allUrls.isNotEmpty()) View.VISIBLE else View.GONE
        if (allUrls.isNotEmpty()) {
            // 楼主层多图 → 两列画廊格子；单图和普通楼层仍走整行宽
            val gallery = isFirst && allUrls.size > 1
            val cellH = (150 * density).toInt()
            val gap = (8 * density).toInt()
            val radius = if (isFirst) 12 * density else 8 * density
            // 预留图片高度：卡片可用宽度 * 固定比例，首帧即占好位，
            // 避免图片异步加载完成后把下方内容顶下去（"打开帖子闪一下"的主因）。
            val screenWdp = ctx.resources.displayMetrics.widthPixels / density
            val contentWdp = (screenWdp - 48f).coerceAtLeast(120f)
            val imgHpx = (contentWdp * 0.7f * density).toInt()
            var row: LinearLayout? = null
            // 图片占位底色 / 加载失败文字跟随主题，避免浅色主题下露出深灰块
            val phBgTv = TypedValue()
            ctx.theme.resolveAttribute(com.example.s1client.R.attr.s1BgCardLight, phBgTv, true)
            val phTextTv = TypedValue()
            ctx.theme.resolveAttribute(com.example.s1client.R.attr.s1TextTertiary, phTextTv, true)
            for ((idx, url) in allUrls.withIndex()) {
                val wrap = FrameLayout(ctx)
                if (gallery) {
                    if (idx % 2 == 0) {
                        val newRow = LinearLayout(ctx).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { topMargin = (12 * density).toInt() }
                        }
                        host.addView(newRow)
                        row = newRow
                    }
                    // 两列 weight 均分，中间留 gap
                    wrap.layoutParams = LinearLayout.LayoutParams(0, cellH, 1f).apply {
                        marginStart = if (idx % 2 == 1) gap else 0
                        marginEnd = if (idx % 2 == 0) gap else 0
                    }
                } else {
                    wrap.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = (12 * density).toInt() }
                }

                val placeholder = TextView(ctx).apply {
                    text = "图片加载失败\n点击用浏览器打开"
                    textSize = 13f
                    setTextColor(phTextTv.data)
                    gravity = Gravity.CENTER
                    setBackgroundColor(phBgTv.data)
                    setPadding((16 * density).toInt(), (16 * density).toInt(),
                        (16 * density).toInt(), (16 * density).toInt())
                    minHeight = (100 * density).toInt()
                    visibility = View.GONE
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        if (gallery) FrameLayout.LayoutParams.MATCH_PARENT
                        else imgHpx
                    )
                }

                val iv = ShapeableImageView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        if (gallery) FrameLayout.LayoutParams.MATCH_PARENT
                        else imgHpx
                    )
                    // 固定高度 + 中心裁切：首帧即占好位，加载完成不再改变布局（消除闪动）
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    shapeAppearanceModel = ShapeAppearanceModel.builder()
                        .setAllCorners(CornerFamily.ROUNDED, radius)
                        .build()
                    setBackgroundColor(phBgTv.data)
                }

                val isS1 = url.contains("stage1st.com")
                Log.d("S1IMG", "开始加载 (S1=" + isS1 + "): " + url)
                iv.load(url) {
                    listener(
                        onSuccess = { _, result ->
                            Log.d("S1IMG", "✓ 成功 [" + url + "] 数据源=" + result.dataSource)
                            placeholder.visibility = View.GONE
                        },
                        onError = { _, err ->
                            Log.e("S1IMG", "✗ 失败 [" + url + "] -> " +
                                    err.throwable.javaClass.simpleName + ": " +
                                    err.throwable.message)
                            placeholder.visibility = View.VISIBLE
                        }
                    )
                }

                if (shareSelectMode) {
                    iv.setOnClickListener(null)
                    iv.setOnLongClickListener(null)
                    iv.isClickable = false
                    placeholder.setOnClickListener(null)
                    placeholder.isClickable = false
                } else {
                    iv.setOnClickListener {
                        val intent = Intent(ctx, ImageViewerActivity::class.java).apply {
                            putStringArrayListExtra("urls", ArrayList(allUrls))
                            putExtra("position", idx)
                        }
                        ctx.startActivity(intent)
                    }
                    iv.setOnLongClickListener {
                        showImageMenu(ctx, url)
                        true
                    }
                    placeholder.setOnClickListener {
                        // 用系统浏览器打开
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                            ctx.startActivity(intent)
                        } catch (e: Exception) {
                            S1Notify.show(ctx, "无法打开浏览器", S1Notify.Type.ERROR)
                        }
                    }
                }

                wrap.addView(iv)
                wrap.addView(placeholder)
                if (gallery) {
                    (row ?: host).addView(wrap)
                } else {
                    host.addView(wrap)
                }
            }
        }

        // 整卡长按不再复制（避免误触）；正文选区已在上方开启
        holder.binding.root.setOnLongClickListener(null)

        Log.d("S1FLICK", "BIND pos=$position pid=${post.pid} pow=${visName(holder.binding.tvPower.visibility)} rate=${visName(holder.binding.llRating.visibility)} exp=${visName(holder.binding.tvExpand.visibility)} img=${visName(holder.binding.llImages.visibility)} top=${visName(holder.binding.llTopImages.visibility)}")

        // 诊断：监听 item 整体高度变化，直接抓住 bind 之后哪项把下方顶下去了
        val ll = View.OnLayoutChangeListener { _, _, _, _, b, _, _, _, ob ->
            if (ob > 0 && b != ob) {
                Log.d("S1LAYOUT", "HEIGHT ${ob}->${b} pid=${post.pid} pow=${visName(holder.binding.tvPower.visibility)} rate=${visName(holder.binding.llRating.visibility)} exp=${visName(holder.binding.tvExpand.visibility)} img=${visName(holder.binding.llImages.visibility)} top=${visName(holder.binding.llTopImages.visibility)}")
            }
        }
        holder.itemView.getTag(R.id.tag_post_layout_listener)?.let {
            holder.itemView.removeOnLayoutChangeListener(it as View.OnLayoutChangeListener)
        }
        holder.itemView.setTag(R.id.tag_post_layout_listener, ll)
        holder.itemView.addOnLayoutChangeListener(ll)
    }

    private fun visName(v: Int): String = when (v) {
        View.VISIBLE -> "V"
        View.INVISIBLE -> "I"
        else -> "G"
    }

    /** 本地未能命中时的兜底加载（回落到论坛 CDN） */
    private fun loadEmoji(
        ctx: android.content.Context,
        ssb: SpannableString,
        startPos: Int,
        emojiSize: Int,
        url: String,
        holder: VH,
        position: Int
    ) {
        val target = object : Target {
            override fun onStart(placeholder: Drawable?) {}
            override fun onError(error: Drawable?) {}
            override fun onSuccess(result: Drawable) {
                result.setBounds(0, 0, emojiSize, emojiSize)
                ssb.setSpan(ImageSpan(result, ImageSpan.ALIGN_BASELINE),
                    startPos, startPos + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (holder.bindingAdapterPosition == position) {
                    holder.binding.tvPostMessage.text = ssb
                }
            }
        }
        Coil.imageLoader(ctx).enqueue(
            ImageRequest.Builder(ctx)
                .data(url).size(emojiSize, emojiSize).target(target).build()
        )
    }

    /**
     * 表情本地优先：把论坛 CDN 地址映射成已内置的 assets 路径。
     * 命中：https://static.stage1st.com/image/smiley/face2017/044.png
     *   ->  file:///android_asset/emoticon/face2017/044.png
     * 未内置该文件则返回 null，由调用方回落到网络。
     */
    private fun localEmojiUrl(ctx: android.content.Context, url: String): String? {
        val m = Regex("""static\.stage1st\.com/image/smiley/([A-Za-z0-9_]+)/([^/?]+)""")
            .find(url) ?: return null
        val cat = m.groupValues[1]
        val file = m.groupValues[2]
        val exists = runCatching {
            ctx.assets.list("emoticon/$cat")?.contains(file) == true
        }.getOrDefault(false)
        return if (exists) "file:///android_asset/emoticon/$cat/$file" else null
    }

    private fun showImageMenu(ctx: android.content.Context, url: String) {
        val items = arrayOf("保存到相册", "复制链接", "分享")
        AlertDialog.Builder(ctx)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> saveImage(ctx, url)
                    1 -> {
                        val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("url", url))
                        S1Notify.show(ctx, "已复制链接", S1Notify.Type.SUCCESS)
                    }
                    2 -> {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, url)
                        }
                        ctx.startActivity(Intent.createChooser(intent, "分享链接"))
                    }
                }
            }
            .show()
    }

    private fun saveImage(ctx: android.content.Context, url: String) {
        S1Notify.show(ctx, "正在保存...")
        // 跟随 Activity 生命周期：退出后协程自动取消，不会泄漏 ctx、
        // 也不会向已销毁的 Activity 弹窗（BadTokenException）
        val scope = (ctx as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope
            ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope.launch {
            val filename = withContext(Dispatchers.IO) { ImageDownloader.downloadToGallery(ctx, url) }
            val act = (ctx as? android.app.Activity)
            if (act != null && (act.isFinishing || act.isDestroyed)) return@launch
            if (filename != null) {
                S1Notify.show(ctx, "已保存到相册：" + filename, S1Notify.Type.SUCCESS)
            } else {
                S1Notify.show(ctx, "保存失败", S1Notify.Type.ERROR)
            }
        }
    }

    override fun getItemCount() = posts.size

    /**
     * 失效黑名单/关键词缓存：外部（详情页）在「全局拉黑 / 本帖屏蔽」状态变化时调用，
     * 配合 notifyDataSetChanged 让已绑定楼层重新判定折叠，做到即时折楼/展开。
     */
    fun invalidateBlockCaches() {
        _cachedBlockedUsers = null
        _cachedBlockedWords = null
    }

    private fun resolveQuoteTextColor(ctx: android.content.Context): Int {
        val tv = TypedValue()
        val found = ctx.theme.resolveAttribute(
            com.example.s1client.R.attr.s1QuoteText, tv, true)
        return if (found) tv.data else 0xFF888888.toInt()
    }

    private fun resolveQuoteBgColor(ctx: android.content.Context): Int {
        val tv = TypedValue()
        val found = ctx.theme.resolveAttribute(
            com.example.s1client.R.attr.s1QuoteBg, tv, true)
        return if (found) tv.data else 0x22000000
    }

    private fun resolveQuoteBarColor(ctx: android.content.Context): Int {
        val tv = TypedValue()
        val found = ctx.theme.resolveAttribute(
            com.example.s1client.R.attr.s1QuoteBar, tv, true)
        return if (found) tv.data else 0xFF888888.toInt()
    }

    companion object {
        /** 头像地址：把 uid 补成 9 位再按 3/2/2/2 切段 */
        fun buildAvatarUrl(uid: String): String? {
            val n = uid.toIntOrNull() ?: return null
            if (n <= 0) return null
            val padded = String.format("%09d", n)
            val path = padded.substring(0, 3) + "/" + padded.substring(3, 5) + "/" +
                    padded.substring(5, 7) + "/" + padded.substring(7, 9)
            return "https://avatar.stage1st.com/" + path + "_avatar_small.jpg"
        }

        /** 无头像/加载失败时的兜底：按名字哈希取一组渐变圆底 */
        fun makeCircleGradient(seed: Int): GradientDrawable {
            val palettes = listOf(
                intArrayOf(0xFFC08060.toInt(), 0xFFD0A080.toInt()),
                intArrayOf(0xFFA08868.toInt(), 0xFFC0A888.toInt()),
                intArrayOf(0xFF8090A0.toInt(), 0xFFA0B0C0.toInt()),
                intArrayOf(0xFF80A080.toInt(), 0xFFA0C0A0.toInt()),
                intArrayOf(0xFFC0A040.toInt(), 0xFFD0B870.toInt())
            )
            val p = palettes[Math.floorMod(seed, palettes.size)]
            return GradientDrawable().apply { shape = GradientDrawable.OVAL; colors = p }
        }
    }
}
