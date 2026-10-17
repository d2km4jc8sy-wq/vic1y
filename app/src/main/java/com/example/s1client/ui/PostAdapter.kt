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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.s1client.util.Haptic
import com.example.s1client.util.S1Notify
import com.example.s1client.data.store.BlockedAuthorManager
import com.example.s1client.data.store.ThreadMutedUsers
import com.example.s1client.data.store.ExpandedFloors
import com.example.s1client.data.store.HiddenFloors
import androidx.appcompat.app.AlertDialog
import androidx.core.text.util.LinkifyCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import coil.Coil
import coil.load
import coil.request.ImageRequest
import coil.target.Target
import com.example.s1client.ImageViewerActivity
import com.example.s1client.R
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.data.model.Post
import com.example.s1client.databinding.ItemPostBinding
import com.example.s1client.util.EmojiAssetsIndex
import com.example.s1client.util.ImageDownloader
import com.example.s1client.util.PostContentParser
import com.example.s1client.util.TimeUtil
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

/**
 * PS1 皮肤版楼层适配器（v3.1：票据 × 手柄融合最终确定版）。
 *
 * 与原版的差异仅在视觉绑定层：
 * - 主楼（first=="1"）：卡片整体是浅灰 PS1 金属机身（[Ps1Skin.bodyRes]），
 *   顶部品牌行 STAGE1 + 板块纯文字标签，标题/正文/图片包在"显示屏"里
 *   （[Ps1Skin.screenRes]），底部操作带 = 左评分 chip（46dp 虚线方章，点击看更多评分）
 *   + 右 △收藏 ○回复 ✕评分 □更多 四钮（34dp 带 9sp 功能标签，教学面）；
 * - 回复楼：卡片是右侧锯齿的手柄便签（[Ps1Skin.widget.PsNoteCardView] +
 *   [Ps1Skin.noteRes]），作者行右侧 = 26dp 圆头像（点击 → 资料卡）+ 40dp 倾斜评分
 *   stamp（rotate(-6deg)，点击 → 查看更多评分），底部 = 左回帖时间（楼层 · 日期）
 *   + 右纯图标四小钮（22dp，高效面无标签）；
 * - 数据回调、折叠/引用/投票/多选/长文/图片/表情等业务逻辑与原版完全一致。
 */
class PostAdapter(
    private val posts: List<Post>,
    private val subject: String = "",
    private val tid: String = "",
    /** 板块名（发帖时从 ForumDataCache 按 fid 解析）；为空则隐藏品牌行右侧标签 */
    private val sectionProvider: () -> String = { "" },
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
    private val onShareToggle: ((Post) -> Unit)? = null,
    /**
     * 投票卡（可空）。**照 s1er 的版式**：它不是顶部的独立区块，而是楼层列表里的
     * 一行，插在第 1 页主楼（楼层下标 0）之后 —— 也就是列表下标 1。
     * s1er: `if (_showsPollOnPage(state) && index == 1) PollCard(...)`。
     *
     * 必须是 var：投票数据通常比楼层晚到（两个请求），到货后由 PageAdapter.setPoll
     * 原地更新并 notifyItemInserted / notifyItemChanged。
     */
    var poll: com.example.s1client.data.model.ThreadPoll? = null,
    /** 投票提交：返回 null 表示成功，非 null 是错误文案（由 Activity 注入，内部走 IO） */
    private val onPollVote: (suspend (List<String>) -> String?)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 单楼展开状态改由 util.ExpandedFloors 统一管理（按 tid|pid 持久化），此处不再维护内存集合

    /**
     * 黑名单判定缓存：onBindViewHolder 每绑一次原本都要读 SP 解析，长帖滚动反复主线程读 SP 掉帧。
     * 这里在首个楼层绑定后缓存一次（整个 Adapter 生命周期内名单不变），后续只做集合查找。
     */
    private var _cachedBlockedUsers: Set<String>? = null
    private var _cachedBlockedWords: Set<String>? = null

    inner class VH(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)

    /** 投票卡的 ViewHolder：整块就是一个 ComposeView，内容高度自适应（不再被顶层卡槽压扁） */
    inner class PollVH(val composeView: androidx.compose.ui.platform.ComposeView)
        : RecyclerView.ViewHolder(composeView)

    private val showPoll: Boolean
        get() = poll != null && posts.isNotEmpty()

    /** 列表下标 → 楼层下标（投票卡那一行要扣掉） */
    private fun postIndexOf(listIndex: Int): Int =
        if (showPoll && listIndex > POLL_LIST_INDEX) listIndex - 1 else listIndex

    override fun getItemViewType(position: Int): Int =
        if (showPoll && position == POLL_LIST_INDEX) TYPE_POLL else TYPE_POST

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_POLL) {
            return PollVH(androidx.compose.ui.platform.ComposeView(parent.context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                // RecyclerView 里的 ComposeView 必须显式释放，否则滑出屏幕后 composition 泄漏
                setViewCompositionStrategy(
                    androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindow
                )
            })
        }
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PollVH) {
            val p = poll ?: return
            holder.composeView.setContent {
                com.example.s1client.ui.theme.S1Theme {
                    PollCard(
                        poll = p,
                        tid = tid,
                        onSubmit = { ids -> onPollVote?.invoke(ids) },
                        // 照 s1er：能投票还要"已登录"，未登录只给提示不给提交
                        isLoggedIn = ForumDataCache.isLoggedIn(),
                        modifier = Modifier
                            // 与 item_post 的外边距一致：左右 10dp、上下 8dp
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }
            return
        }
        val h = holder as VH
        val post = posts[postIndexOf(position)]
        val ctx = h.itemView.context
        val density = ctx.resources.displayMetrics.density
        val emojiSize = (18 * density).toInt()

        val authorName = post.author ?: "未知"
        h.binding.tvPostAuthor.text = authorName

        // 战斗力：金色小 pill，按 uid 从 Profile API 缓存取（楼层 JSON 无此字段）；
        // 取不到用 INVISIBLE 占位，避免 GONE→VISIBLE 引发重绘闪动。
        val powerStr = powerProvider(post.authorId)
        if (powerStr != null) {
            h.binding.tvPower.visibility = View.VISIBLE
            h.binding.tvPower.text = "⚔ $powerStr"
        } else {
            h.binding.tvPower.visibility = View.GONE
        }

        // 自己发的楼层昵称用强调色高亮（移动版 API 不回用户组，管理等用户组高亮暂时做不了）
        val myUid = ForumDataCache.memberUid
        val isSelf = myUid.isNotEmpty() && myUid != "0" && post.authorId == myUid
        val nameColorTv = TypedValue()
        ctx.theme.resolveAttribute(
            if (isSelf) com.example.s1client.R.attr.s1Accent
            else com.example.s1client.R.attr.s1TextSecondary,
            nameColorTv, true)
        h.binding.tvPostAuthor.setTextColor(nameColorTv.data)
        h.binding.tvPostAuthor.typeface =
            if (isSelf) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

        val timeStr = TimeUtil.formatSmart(post.dateline)

        val isFirst = post.first == "1"

        // ===== PS1 皮肤：主楼机身 / 回复楼便签 =====
        applyPs1Skin(h, isFirst, density)

        if (isFirst) {
            // 楼主层：时间挪到标题下的 meta 行，底部操作行不再重复显示
            h.binding.llOpMeta.visibility = View.VISIBLE
            h.binding.tvOpTime.text = timeStr
            h.binding.tvPostTime.visibility = View.GONE
        } else {
            h.binding.llOpMeta.visibility = View.GONE
            // v3.1：楼层号并入底部时间文本（「N楼 · 日期」），作者行只留昵称/战斗力/头像/stamp
            val floorNum = post.position ?: (position + 1).toString()
            h.binding.tvPostTime.text = "${floorNum}楼 · $timeStr"
            h.binding.tvPostTime.visibility =
                if (timeStr.isBlank()) View.GONE else View.VISIBLE
        }

        // 楼主层的作者信息已经上移到顶栏，楼层内作者行不重复
        h.binding.llAuthorRow.visibility = if (isFirst) View.GONE else View.VISIBLE
        if (isFirst) {
            if (subject.isNotBlank()) {
                h.binding.tvPostTitle.text = subject
                h.binding.tvPostTitle.visibility = View.VISIBLE
            } else {
                h.binding.tvPostTitle.visibility = View.GONE
            }
        } else {
            h.binding.tvPostTitle.visibility = View.GONE
        }

        val letter = authorName.firstOrNull()?.toString() ?: "?"
        h.binding.tvPostAvatarLetter.text = letter
        h.binding.tvPostAvatarLetter.background = makeCircleGradient(authorName.hashCode())

        h.binding.ivPostAvatar.alpha = 0f
        post.authorId?.let { buildAvatarUrl(it) }?.let { url ->
            h.binding.ivPostAvatar.load(url) {
                listener(
                    onSuccess = { _, _ -> h.binding.ivPostAvatar.alpha = 1f },
                    onError = { _, _ -> h.binding.ivPostAvatar.alpha = 0f }
                )
            }
        }

        if (shareSelectMode) {
            h.binding.avatarClickArea.setOnClickListener(null)
            // 关闭可点击性，避免头像区吞掉整卡点按（否则勾选不生效）
            h.binding.avatarClickArea.isClickable = false
            h.binding.avatarClickArea.isFocusable = false
        } else {
            h.binding.avatarClickArea.setOnClickListener {
                onAvatarClick?.invoke(post)
            }
        }

        // 四钮符号颜色在 vector 里固定（△绿○红✕蓝□品红），主楼 34dp 与便签 22dp 都不做统一 tint

        // ===== 卡片底：PS1 皮肤（多选时切换为强调色描边 + 浅填充，覆盖皮肤底） =====
        val accentTv = TypedValue()
        ctx.theme.resolveAttribute(com.example.s1client.R.attr.s1Accent, accentTv, true)
        val accent = accentTv.data

        if (shareSelectMode) {
            // 选中高亮画在 cardContent 上（绘制在子 View 之下，不遮挡右上角勾选徽标）
            h.binding.cardContent.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14 * density
                setStroke((2 * density).toInt(), accent)
                setColor(android.graphics.Color.argb(
                    26,
                    android.graphics.Color.red(accent),
                    android.graphics.Color.green(accent),
                    android.graphics.Color.blue(accent)))
            }
            h.binding.cardContent.setJagged(false)
            // 底部操作行整体收起（内含评分 chip 与主楼/便签两套钮）
            h.binding.llActions.visibility = View.GONE
            h.binding.ivSelectBadge.visibility = View.VISIBLE
            if (isShareSelected(post)) {
                h.binding.ivSelectBadge.setImageResource(R.drawable.ic_check_circle)
                h.binding.ivSelectBadge.setColorFilter(accent)
            } else {
                val tertiaryTv = TypedValue()
                ctx.theme.resolveAttribute(
                    com.example.s1client.R.attr.s1TextTertiary, tertiaryTv, true)
                h.binding.ivSelectBadge.setImageResource(R.drawable.ic_circle_outline)
                h.binding.ivSelectBadge.setColorFilter(tertiaryTv.data)
            }
            h.binding.root.isClickable = true
            h.binding.root.setOnClickListener { onShareToggle?.invoke(post) }
            h.binding.ivSelectBadge.setOnClickListener { onShareToggle?.invoke(post) }
            // 复用 ViewHolder 时清掉旧的非多选态链接点击，否则正文仍会吞掉整卡点按
            h.binding.tvPostMessage.movementMethod = null
        } else {
            h.binding.ivSelectBadge.visibility = View.GONE
            h.binding.root.setOnClickListener(null)
        }

        // 按压缩放反馈（不消费事件，点击/长按不受影响）
        h.binding.root.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN ->
                    v.animate().scaleX(0.985f).scaleY(0.985f).setDuration(110).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }
            false
        }

        // 四键统一震动语言（2026-10-16 用户要求）：此前四键的点击入口没有震动，
        // 震动散落在各功能内部（收藏/评分碰巧有、回复/更多没有），手感不统一。
        // 现在统一在**点击入口**给 selection 档轻震，功能内部不再重复给。
        h.binding.btnRate.setOnClickListener {
            Haptic.selection(it.context)
            onRateClick?.invoke(post)
        }
        h.binding.btnQuote.setOnClickListener {
            Haptic.selection(it.context)
            onQuoteClick?.invoke(post)
        }
        h.binding.btnFavoritePost.setOnClickListener {
            Haptic.selection(it.context)
            onFavoriteClick?.invoke(post)
        }
        h.binding.btnMore.setOnClickListener {
            Haptic.selection(it.context)
            onMoreClick?.invoke(post)
        }

        // 评分 chip（主楼）/ 倾斜 stamp（便签）：点击 → 查看更多评分（与评分汇总行同一入口）
        h.binding.btnScoreChip.setOnClickListener {
            if (!shareSelectMode) onRatingRowClick?.invoke(post)
        }
        h.binding.btnNoteStamp.setOnClickListener {
            if (!shareSelectMode) onRatingRowClick?.invoke(post)
        }

        // 回复楼胶囊行：与主楼四钮同一套回调
        // 便签四小键与主楼四钮同一套震动语言（同上）
        h.binding.btnNoteRate.setOnClickListener {
            Haptic.selection(it.context)
            if (!shareSelectMode) onRateClick?.invoke(post)
        }
        h.binding.btnNoteQuote.setOnClickListener {
            Haptic.selection(it.context)
            if (!shareSelectMode) onQuoteClick?.invoke(post)
        }
        h.binding.btnNoteMore.setOnClickListener {
            Haptic.selection(it.context)
            if (!shareSelectMode) onMoreClick?.invoke(post)
        }
        h.binding.btnNoteFav.setOnClickListener {
            Haptic.selection(it.context)
            if (!shareSelectMode) onFavoriteClick?.invoke(post)
        }

        // 触达区外扩（阶段四 P15 无障碍，2026-10-17）：便签四小钮 22dp 视觉不变，
        // 触达区向四周外扩 8dp 达到 38dp（宪法第 12 条）——解决「按钮周围点了没反应」的死区。
        // 组代理挂一次即可（相对位置不随绑定变化）；皮肤切换隐藏四钮时路由自然失效（GONE 不命中）。
        com.example.s1client.util.TouchDelegateHelper.expandHalo(
            parent = h.binding.llNoteButtons,
            children = listOf(h.binding.btnNoteFav, h.binding.btnNoteQuote, h.binding.btnNoteRate, h.binding.btnNoteMore),
            haloPx = (8f * h.itemView.resources.displayMetrics.density).toInt()
        )

        // 评分行：数据来自网页版 HTML 的 ratelog（移动版 API 没有）。
        // v3.1：列表层评分展示已由主楼评分 chip / 便签倾斜 stamp 承担；汇总行里的
        // 「N 人参与」等信息按 Q1 拍板收进详情层（点击 chip/stamp 进入），此处不再显示。
        val rate = rateProvider(post.pid)
        h.binding.llRating.visibility = View.GONE
        h.binding.llRating.setOnClickListener(null)

        bindRating(h, isFirst, post)

        val parsed = PostContentParser.parse(post.message)

        // ===== 折叠命中：整层折叠成一行，点一下可展开看原文 =====
        val blocked = run {
            val users = _cachedBlockedUsers
                ?: BlockedAuthorManager.allBlocked().keys
                    .also { _cachedBlockedUsers = it }
            val words = _cachedBlockedWords
                ?: com.example.s1client.data.store.BlacklistManager.blockedWords(ctx)
                    .also { _cachedBlockedWords = it }
            users.contains(post.authorId) ||
                    (parsed.text.isNotBlank() && words.any { w -> parsed.text.toString().contains(w, ignoreCase = true) })
        }
        val threadMuted = tid.isNotBlank() && post.authorId != null &&
                ThreadMutedUsers.isMuted(tid, post.authorId)
        val hiddenFloor = tid.isNotBlank() && post.pid != null &&
                HiddenFloors.isHidden(tid, post.pid)
        val expandedOnce = ExpandedFloors.isExpanded(tid, post.pid)
        val folded = (blocked || threadMuted || hiddenFloor) && !expandedOnce
        val foldMsg = when {
            folded && blocked -> "该楼层已被黑名单折叠（${authorName}）· 点击展开"
            folded && threadMuted -> "已在本帖屏蔽此人的回复（${authorName}）· 点击展开"
            folded && hiddenFloor -> "已屏蔽此楼 · 点击展开"
            else -> null
        }
        if (foldMsg != null) {
            val hint = h.binding.tvPostMessage
            h.binding.llTopImages.visibility = View.GONE
            h.binding.llImages.visibility = View.GONE
            h.binding.llRating.visibility = View.GONE
            h.binding.tvPostTitle.visibility = View.GONE
            h.binding.llOpMeta.visibility = View.GONE
            val spannable = android.text.SpannableString(foldMsg)
            val secTv = TypedValue()
            ctx.theme.resolveAttribute(
                com.example.s1client.R.attr.s1TextTertiary, secTv, true)
            spannable.setSpan(
                ForegroundColorSpan(secTv.data),
                0, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(RelativeSizeSpan(0.88f),
                0, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            hint.text = spannable
            hint.setOnClickListener {
                ExpandedFloors.expand(tid, post.pid)
                notifyItemChanged(position)
            }
            h.binding.llQuote.visibility = View.GONE
            h.binding.tvPower.visibility = View.GONE
            h.binding.tvExpand.visibility = View.GONE
            // PS1 皮肤装饰在折叠态全部收起，只剩一行提示
            // （底部操作行内含评分 chip 与两套钮，llActions 一并收起）
            h.binding.llActions.visibility = View.GONE
            h.binding.llBrandRow.visibility = View.GONE
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
        h.binding.tvPostMessage.text = ssb

        // 独立引用卡片：正文已剔除引用段，这里单独渲染被引内容；点击跳转到被引楼层
        val qText = parsed.quoteText.replace("\uFFFC", "").trim()
        if (qText.isNotBlank()) {
            h.binding.llQuote.visibility = View.VISIBLE
            val qFloor = parsed.quoteFloor
            h.binding.tvQuoteAuthor.text = if (qFloor != null) "引用 · ${qFloor}楼" else "引用"
            h.binding.tvQuoteBody.text = qText
            if (shareSelectMode) {
                h.binding.llQuote.isClickable = false
                h.binding.llQuote.setOnClickListener(null)
            } else if (qFloor != null) {
                h.binding.llQuote.isClickable = true
                h.binding.llQuote.setOnClickListener { onPostQuoteJump?.invoke(qFloor) }
            } else {
                h.binding.llQuote.isClickable = false
                h.binding.llQuote.setOnClickListener(null)
            }
        } else {
            h.binding.llQuote.visibility = View.GONE
        }

        // 长文折叠
        val tvMessage = h.binding.tvPostMessage
        val contentWpx = ((ctx.resources.displayMetrics.widthPixels / density) - 48f) * density
        tvMessage.maxLines = 12
        tvMessage.ellipsize = TextUtils.TruncateAt.END
        h.binding.tvExpand.setOnClickListener(null)
        h.binding.tvExpand.visibility = View.GONE
        // 自定义字体（阶段四 P10）：导入的字体应用到楼层正文（View 侧）
        com.example.s1client.data.store.SettingStore.customTypeface(h.itemView.context)?.let { tf ->
            tvMessage.typeface = tf
            android.util.Log.d("S1Font", "楼层正文已应用自定义字体")
        }
        val textWidth = (contentWpx - tvMessage.compoundPaddingLeft - tvMessage.compoundPaddingRight).toInt()
        // 性能优化 2026-10-16：测行数是只为「展开全文」按钮服务的一次完整排版，
        // 原来每次绑定都重算（长文楼层滚动时反复付出整段 StaticLayout 的成本）。
        // 按（宽度、字号、文本指纹）缓存进进程级 LruCache，重绑定直接查表。
        val lineKey = "$textWidth:${tvMessage.textSize}:${displayText.length}:${displayText.hashCode()}"
        val naturalLines = if (textWidth > 0) {
            val cached = lineCountCache.get(lineKey)
            if (cached != null) cached else {
                val measured = kotlin.runCatching {
                    android.text.StaticLayout.Builder.obtain(
                        displayText, 0, displayText.length, TextPaint(tvMessage.paint), textWidth
                    )
                        .setLineSpacing(tvMessage.lineSpacingExtra, tvMessage.lineSpacingMultiplier)
                        .build()
                        .lineCount
                }.getOrNull() ?: 0
                lineCountCache.put(lineKey, measured)
                measured
            }
        } else 0
        if (naturalLines > 12) {
            tvMessage.maxLines = 12
            tvMessage.ellipsize = TextUtils.TruncateAt.END
            h.binding.tvExpand.visibility = View.VISIBLE
            h.binding.tvExpand.text = "展开全文 ›"
            var expanded = false
            h.binding.tvExpand.setOnClickListener {
                expanded = !expanded
                tvMessage.maxLines = if (expanded) Int.MAX_VALUE else 12
                tvMessage.ellipsize = if (expanded) null else TextUtils.TruncateAt.END
                h.binding.tvExpand.text =
                    if (expanded) "收起 ‹" else "展开全文 ›"
            }
        } else {
            tvMessage.maxLines = Int.MAX_VALUE
            tvMessage.ellipsize = null
            h.binding.tvExpand.visibility = View.GONE
        }

        // ===== 链接识别 =====
        val accentLinkTv = TypedValue()
        ctx.theme.resolveAttribute(com.example.s1client.R.attr.s1Accent, accentLinkTv, true)
        h.binding.tvPostMessage.setLinkTextColor(accentLinkTv.data)
        h.binding.tvPostMessage.highlightColor =
            (accentLinkTv.data and 0x00FFFFFF.toInt()) or 0x33000000.toInt()
        if (!shareSelectMode && LinkifyCompat.addLinks(h.binding.tvPostMessage, Linkify.WEB_URLS)) {
            h.binding.tvPostMessage.movementMethod = LinkMovementMethod.getInstance()
        }

        val imageLoader = Coil.imageLoader(ctx)
        for ((startPos, url) in spansToLoad) {
            val emojiSrc = localEmojiUrl(ctx, url) ?: url
            val target = object : Target {
                override fun onStart(placeholder: Drawable?) {}
                override fun onError(error: Drawable?) {
                    if (emojiSrc != url) loadEmoji(ctx, ssb, startPos, emojiSize, url, h, position)
                }
                override fun onSuccess(result: Drawable) {
                    result.setBounds(0, 0, emojiSize, emojiSize)
                    ssb.setSpan(ImageSpan(result, ImageSpan.ALIGN_BASELINE),
                        startPos, startPos + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    if (h.bindingAdapterPosition == position) {
                        h.binding.tvPostMessage.text = ssb
                    }
                }
            }
            val req = ImageRequest.Builder(ctx)
                .data(emojiSrc).size(emojiSize, emojiSize).target(target).build()
            imageLoader.enqueue(req)
        }

        // ===== 图片：楼主层置顶，其余楼层放正文下方（维持整组展示，不做段落穿插） =====
        val topContainer = h.binding.llTopImages
        val container = h.binding.llImages
        // 每楼层图片上限（R1 低成本批）：0=不限；截断时在图组末尾补一行省略提示
        val imgCap = com.example.s1client.data.store.SettingStore.maxImagesPerFloor(ctx)
        val allUrls = if (imgCap > 0) parsed.imageUrls.take(imgCap) else parsed.imageUrls
        val omitted = parsed.imageUrls.size - allUrls.size
        val host = if (isFirst) topContainer else container
        topContainer.visibility = if (isFirst && allUrls.isNotEmpty()) View.VISIBLE else View.GONE
        container.visibility = if (!isFirst && allUrls.isNotEmpty()) View.VISIBLE else View.GONE
        // 阶段二③：图片区结构复用——同一楼层重绑（展开/评分回填/多选态切换等）时
        // url 集合未变就跳过整段视图重建（原实现每次绑定 removeAllViews + 逐图新建，
        // 是快速滚动与 notifyItemChanged 的 GC 大户）。键含 isFirst/shareSelectMode，
        // 任一变化即整组重建，行为与旧实现一致；折叠分支早退不经过这里，视图留在
        // 原容器（不可见），展开重绑时命中复用直接恢复。
        val imageKey = "$isFirst|$shareSelectMode|" + allUrls.joinToString("|")
        val needsBuild = allUrls.isNotEmpty() &&
                h.itemView.getTag(R.id.tag_image_key) != imageKey
        if (allUrls.isEmpty()) {
            h.itemView.setTag(R.id.tag_image_key, null)
        } else if (needsBuild) {
            h.itemView.setTag(R.id.tag_image_key, imageKey)
            topContainer.removeAllViews()
            container.removeAllViews()
        }
        if (needsBuild) {
            val gallery = isFirst && allUrls.size > 1
            val cellH = (150 * density).toInt()
            val gap = (8 * density).toInt()
            val radius = if (isFirst) 12 * density else 8 * density
            val screenWdp = ctx.resources.displayMetrics.widthPixels / density
            val contentWdp = (screenWdp - 48f).coerceAtLeast(120f)
            val imgHpx = (contentWdp * 0.7f * density).toInt()
            var row: LinearLayout? = null
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
            if (omitted > 0) {
                val hint = TextView(ctx).apply {
                    text = "已省略 $omitted 张图片（设置 → 数据管理 → 每楼层图片上限）"
                    textSize = 12f
                    setTextColor(phTextTv.data)
                    setPadding(0, (4 * density).toInt(), 0, 0)
                }
                host.addView(hint)
            }
        }

        h.binding.root.setOnLongClickListener(null)
    }

    /**
     * 应用 PS1 皮肤（v3.1）：主楼 = 金属机身 + 品牌行 + 显示屏 + 评分 chip + 四钮带标签；
     * 回复楼 = 锯齿便签 + 右上角头像/倾斜 stamp + 底部时间 + 纯图标四小钮。
     * 纯视觉绑定，任何业务状态都不在这里判断。
     */
    /**
     * 绑定评分 chip / 便签 stamp 的聚合分（细节轮 2026-10-16：未评分也显示控件，
     * 数字位显示「–」，消除已评/未评楼层视觉割裂）。
     *
     * 抽成独立方法是为了让 [onBindViewHolder] 与 payload 局部刷新共用同一份逻辑 ——
     * 评分数据异步到货时只需重刷评分控件，不必整层重绑（正文解析/表情/引用/四钮
     * 全部重跑一遍，长列表下是主要掉帧来源）。
     */
    private fun bindRating(h: VH, isFirst: Boolean, post: Post) {
        val rate = rateProvider(post.pid)
        if (rate != null && rate.count > 0) {
            val scoreTxt = (if (rate.total >= 0) "+" else "") + rate.total.toString()
            val scoreColor = if (rate.total >= 0) Ps1Skin.SCORE_UP else Ps1Skin.SCORE_DOWN
            if (isFirst) {
                h.binding.btnScoreChip.visibility = View.VISIBLE
                h.binding.tvChipScore.text = scoreTxt
                h.binding.tvChipScore.setTextColor(scoreColor)
            } else {
                h.binding.btnNoteStamp.visibility = View.VISIBLE
                h.binding.tvNoteScore.visibility = View.VISIBLE
                h.binding.tvNoteScore.text = scoreTxt
                h.binding.tvNoteScore.setTextColor(scoreColor)
            }
        } else {
            val emptyColor = tertiaryColor(h.binding.root)
            if (isFirst) {
                h.binding.btnScoreChip.visibility = View.VISIBLE
                h.binding.tvChipScore.text = "–"
                h.binding.tvChipScore.setTextColor(emptyColor)
            } else {
                h.binding.btnNoteStamp.visibility = View.VISIBLE
                h.binding.tvNoteScore.visibility = View.VISIBLE
                h.binding.tvNoteScore.text = "–"
                h.binding.tvNoteScore.setTextColor(emptyColor)
            }
        }
    }

    /**
     * 只刷新评分 chip / stamp（性能优化 2026-10-16）。
     * 供 [com.example.s1client.ui.PageAdapter.notifyRatingsChanged] 调用：
     * 评分数据异步到货时不必整层重绑，长列表下这是主要掉帧来源。
     */
    fun notifyRatingsOnly() {
        notifyItemRangeChanged(0, itemCount, PAYLOAD_RATING)
    }

    /**
     * payload 局部刷新：只重绑评分相关控件，不动其他任何部分。
     * 由 [notifyRatingsChanged] 调用（性能优化 2026-10-16）。
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.contains(PAYLOAD_RATING)) {
            val h = holder as? VH ?: return
            val idx = postIndexOf(position)
            // posts 可能在 notify 与实际重绑之间被换掉（换页/清空），
            // 这里必须取下标校验，不能让 payload 分支成为越界崩溃点。
            if (idx !in posts.indices) return
            bindRating(h, idx == 0, posts[idx])
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    private fun applyPs1Skin(h: VH, isFirst: Boolean, density: Float) {
        val b = h.binding
        val pad14 = (14 * density).toInt()
        // 主题次级文字色（时间/楼层号/引用标签）：便签与主楼共用
        val themeInk = tertiaryColor(b.root)
        if (isFirst) {
            // —— 主楼：厚重机身 ——
            b.cardContent.setJagged(false)
            b.cardContent.setBackgroundResource(Ps1Skin.bodyRes())
            // 细节轮2（2026-10-16）：去掉 elevation 阴影。
            // Android 的投影是直角矩形、不随 drawable 圆角裁剪 → 12dp 圆角机身
            // 四角会露出四块直角阴影（用户反馈"顶楼四角都有阴影"）。
            // 票据语言靠「描边 + 机身渐变」体现体积，不需要浮起投影；
            // 便签本来就没阴影（0f），两者也就齐平了。
            ViewCompat.setElevation(b.cardContent, 0f)
            // 显示屏
            b.llScreenBox.setBackgroundResource(Ps1Skin.screenRes())
            b.llScreenBox.setPadding(pad14, pad14, pad14, pad14)
            // 品牌行：STAGE1（左）+ 板块纯文字标签（右，解析不到则隐藏）
            b.llBrandRow.visibility = View.VISIBLE
            val section = sectionProvider()
            if (section.isNotBlank()) {
                b.tvSectionTag.text = section
                b.tvSectionTag.visibility = View.VISIBLE
            } else {
                b.tvSectionTag.visibility = View.GONE
            }
            // 操作带：评分 chip + 四钮带标签
            b.btnScoreChip.setBackgroundResource(Ps1Skin.chipRes())
            b.btnScoreChip.visibility = View.VISIBLE
            b.llPsButtons.visibility = View.VISIBLE
            b.llNoteButtons.visibility = View.GONE
            // 主楼与便签共用主题文字色（ViewHolder 复用时必须重刷）
            b.tvPostTime.setTextColor(themeInk)
            b.llQuote.setBackgroundResource(R.drawable.bg_quote_card)
        } else {
            // —— 回复楼：手柄便签（票据面，TicketPalette 深票纸）——
            b.cardContent.setJagged(true)
            b.cardContent.background = Ps1Skin.ticketDrawable()
            ViewCompat.setElevation(b.cardContent, 0f)
            // 内容直接落在票面上，无屏幕包边
            b.llScreenBox.background = null
            b.llScreenBox.setPadding(0, 0, 0, 0)
            b.llBrandRow.visibility = View.GONE
            // 操作带：底部时间 + 纯图标四小钮（评分 chip 不出现）
            b.btnScoreChip.visibility = View.GONE
            b.llPsButtons.visibility = View.GONE
            b.llNoteButtons.visibility = View.VISIBLE
            // stamp 虚线框随时跟随当前明暗皮肤（ViewHolder 复用时必须重刷）
            b.btnNoteStamp.setBackgroundResource(Ps1Skin.noteStampRes())
            // 细节轮4：便签改磨砂玻璃膜后，底色浅，文字必须回到**主题色**
            // （此前深票纸上的亮白字在浅磨砂上会看不清）。便签与主楼共用一套主题文字色。
            b.tvPostMessage.setTextColor(themeBodyColor(b.root))
            b.tvPostTime.setTextColor(themeInk)
            b.tvPostAuthor.setTextColor(themeBodyColor(b.root))
            // stamp 章内「评分」标签跟随主题次级色
            b.btnNoteStamp.findViewById<android.widget.TextView>(R.id.tvNoteStampLabel)
                ?.setTextColor(themeInk)
            // 引用卡回到主题内衬版
            b.llQuote.setBackgroundResource(R.drawable.bg_quote_card)
            b.tvQuoteAuthor.setTextColor(themeInk)
            b.tvQuoteBody.setTextColor(themeBodyColor(b.root))
        }
    }

    /** 本地未能命中时的兜底加载（回落到论坛 CDN） */
    private fun loadEmoji(
        ctx: android.content.Context,
        ssb: SpannableString,
        startPos: Int,
        emojiSize: Int,
        url: String,
        h: VH,
        position: Int
    ) {
        val target = object : Target {
            override fun onStart(placeholder: Drawable?) {}
            override fun onError(error: Drawable?) {}
            override fun onSuccess(result: Drawable) {
                result.setBounds(0, 0, emojiSize, emojiSize)
                ssb.setSpan(ImageSpan(result, ImageSpan.ALIGN_BASELINE),
                    startPos, startPos + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (h.bindingAdapterPosition == position) {
                    h.binding.tvPostMessage.text = ssb
                }
            }
        }
        Coil.imageLoader(ctx).enqueue(
            ImageRequest.Builder(ctx)
                .data(url).size(emojiSize, emojiSize).target(target).build()
        )
    }

    /**
     * 本地表情命中判断：论坛 CDN 地址映射到内置 assets。
     * 性能优化 2026-10-16：原实现对每个表情、每次绑定都跑一次
     * assets.list（主线程磁盘枚举）；现在走 [EmojiAssetsIndex]
     * 的进程内索引（首次使用扫一次，之后纯内存查找），返回值语义不变。
     */
    private fun localEmojiUrl(ctx: android.content.Context, url: String): String? =
        EmojiAssetsIndex.resolve(ctx, url)

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

    /** 楼层数 + 投票卡那一行（若这一页要显示投票） */
    override fun getItemCount() = posts.size + if (showPoll) 1 else 0

    /** 楼层数（不含投票卡），PageAdapter 插入/移除投票时需要它判断该不该 notify */
    fun postCount(): Int = posts.size

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is PollVH) holder.composeView.disposeComposition()
        super.onViewRecycled(holder)
    }

    /**
     * 失效黑名单/关键词缓存：外部（详情页）在「全局拉黑 / 本帖屏蔽」状态变化时调用。
     */
    fun invalidateBlockCaches() {
        _cachedBlockedUsers = null
        _cachedBlockedWords = null
    }

    /**
     * 阶段二：只重绑命中的楼层（屏蔽/折叠状态变化的精准刷新）。
     * notifyItemChanged 精确到行；黑名单缓存失效放在最前。
     * 评分缓存/正文解析均有 LruCache，命中的行重绑成本大幅低于整页重绑。
     */
    fun notifyFloorsChanged(predicate: (Post) -> Boolean) {
        invalidateBlockCaches()
        for (idx in posts.indices) {
            if (predicate(posts[idx])) {
                notifyItemChanged(idx + if (showPoll && idx >= POLL_LIST_INDEX) 1 else 0)
            }
        }
    }

    companion object {
        /** 楼层行的 viewType */
        /** payload：仅刷新评分 chip / stamp 文字 */
        private const val PAYLOAD_RATING = "payload_rating"

        /**
         * 长文行数测量缓存（性能优化 2026-10-16）：
         * key = 宽度:字号:文本长度:文本哈希。StaticLayout 全文排版只为数行数，
         * 缓存后同一楼层重复绑定零成本。512 条 ≈ 数页楼层，内存可忽略。
         */
        private val lineCountCache = android.util.LruCache<String, Int>(512)

        private const val TYPE_POST = 0
        /** 投票卡行的 viewType */
        private const val TYPE_POLL = 1
        /**
         * 投票卡在列表里的下标 —— **照 s1er**：`index == 1`，即紧跟主楼之后。
         */
        private const val POLL_LIST_INDEX = 1

        /** 头像地址：把 uid 补成 9 位再按 3/2/2/2 切段 */
        fun buildAvatarUrl(uid: String): String? {
            val n = uid.toIntOrNull() ?: return null
            if (n <= 0) return null
            val padded = String.format("%09d", n)
            val path = padded.substring(0, 3) + "/" + padded.substring(3, 5) + "/" +
                    padded.substring(5, 7) + "/" + padded.substring(7, 9)
            // 细节轮（2026-10-16）：small(48px) 拉到 54dp 抽屉头像发糊，统一改 middle
            return "https://avatar.stage1st.com/" + path + "_avatar_middle.jpg"
        }

        /** 从主题取正文色（s1TextBody）：便签/主楼正文共用 */
        fun themeBodyColor(view: View): Int {
            val tv = android.util.TypedValue()
            return if (view.context.theme.resolveAttribute(R.attr.s1TextBody, tv, true)) tv.data
            else 0xFF3A3630.toInt()
        }

        /** 从主题取次级文字色（评分控件未评分态的「–」用） */
        fun tertiaryColor(view: View): Int {
            val tv = android.util.TypedValue()
            return if (view.context.theme.resolveAttribute(R.attr.s1TextTertiary, tv, true)) {
                tv.data
            } else 0xFF888888.toInt()
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
