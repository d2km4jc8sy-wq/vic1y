package com.example.s1client

import android.app.Dialog
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import android.util.Log
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.s1client.ui.theme.S1Theme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.data.model.Post
import com.example.s1client.databinding.ActivityThreadDetailBinding
import com.example.s1client.databinding.DialogReplySheetBinding
import com.example.s1client.databinding.DialogUserCardBinding
import com.example.s1client.ui.PageAdapter
import com.example.s1client.ui.Ps1Skin
import com.example.s1client.util.BbCode
import com.example.s1client.data.store.DraftStore
import com.example.s1client.data.store.FavoriteManager
import androidx.compose.ui.graphics.toArgb
import com.example.s1client.util.Haptic
import com.example.s1client.util.ImageUploader
import com.example.s1client.util.JsonParser
import com.example.s1client.util.S1Notify
import com.example.s1client.util.TimeUtil
import com.example.s1client.data.store.BlockedAuthorManager
import com.example.s1client.data.store.HiddenPostManager
import com.example.s1client.data.store.PostBookmarkManager
import com.example.s1client.data.store.ThreadMutedUsers
import com.example.s1client.data.store.ExpandedFloors
import com.example.s1client.data.store.HiddenFloors
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.s1client.ui.design.AccentPink
import com.example.s1client.ui.design.OnAccent
import com.example.s1client.ui.design.TextSub

// ================= 评分与 weighting =================
internal fun ThreadDetailActivity.loadRatings(page: Int) {
    if (page < 1 || ratingFetchedPages.contains(page)) return
    ratingFetchedPages.add(page)
    // 本页作者的战斗力（按 uid 从 Profile API 取，跨页缓存复用）
    pageAdapter.getPage(page)?.let { loadPowers(it) }
    lifecycleScope.launch {
        val fetched = withContext(Dispatchers.IO) { fetchRatingsHtml(page) }
        // put 覆盖旧值：自己评完分后要让汇总数字刷新
        fetched.forEach { (pid, info) -> rateMap.put(pid, info) }
        if (fetched.isNotEmpty()) pageAdapter.notifyRatingsChanged()
    }
}

internal fun ThreadDetailActivity.loadPowers(posts: List<Post>) {
    val uids = posts.mapNotNull { it.authorId }.distinct()
    if (uids.isEmpty()) return
    lifecycleScope.launch {
        com.example.s1client.data.store.PowerRepository.ensure(uids) { schedulePowerRefresh() }
    }
}

/**
 * 预取某页评分并填入 [rateMap]（在页面首次渲染前调用），避免评分行
 * GONE→VISIBLE 的竖向跳动（"打开帖子闪一下"的根因）。与页面内容加载串行，
 * 评分是一次独立 HTTP，开销很小。返回是否成功抓到（失败时由调用方决定标记）。
 */

internal suspend fun ThreadDetailActivity.preloadRatings(page: Int): Boolean = withContext(Dispatchers.IO) {
    try {
        val fetched = fetchRatingsHtml(page)
        fetched.forEach { (pid, info) -> rateMap.put(pid, info) }
        true
    } catch (e: Exception) {
        android.util.Log.e("S1Rate", "preload ratelog failed p$page", e)
        false
    }
}

/** 多条战斗力陆续到达：合并成一次 notify，避免频繁整页重绑 */

internal fun ThreadDetailActivity.schedulePowerRefresh() {
    binding.root.removeCallbacks(powerRefreshRunnable)
    binding.root.postDelayed(powerRefreshRunnable, 120)
}


internal fun ThreadDetailActivity.fetchRatingsHtml(page: Int): Map<String, com.example.s1client.util.RateInfo> {
    return try {
        val url = "https://stage1st.com/2b/forum.php?mod=viewthread" +
                "&tid=$tid&page=$page&mobile=2"
        val req = okhttp3.Request.Builder().url(url).build()
        val html = RetrofitClient.httpClient.newCall(req).execute()
            .use { it.body?.string().orEmpty() }
        com.example.s1client.util.RatingParser.parse(html)
    } catch (e: Exception) {
        android.util.Log.e("S1Rate", "fetch ratelog failed p$page", e)
        emptyMap()
    }
}

/** 点评分行：弹出完整评分明细列表（方案 A：紧凑长条，整行点按可看个人资料） */

internal fun ThreadDetailActivity.showRatingDetail(post: Post) {
    val pid = post.pid ?: return
    val info = rateMap[pid] ?: return
    val dialog = Dialog(this)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setContentView(R.layout.dialog_rating_list)
    dialog.window?.apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setDimAmount(0.4f)
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    dialog.setCancelable(true)
    dialog.setCanceledOnTouchOutside(true)

    val ctx = this
    val tvSummary = dialog.findViewById<android.widget.TextView>(R.id.tvSummary)
    val llRatings = dialog.findViewById<LinearLayout>(R.id.llRatings)
    val scroll = dialog.findViewById<android.widget.ScrollView>(R.id.scrollRatings)
    val llLoadFull = dialog.findViewById<LinearLayout>(R.id.llLoadFull)
    val pbLoadFull = dialog.findViewById<android.widget.ProgressBar>(R.id.pbLoadFull)
    val btnLoadFull = dialog.findViewById<android.widget.Button>(R.id.btnLoadFull)

    var currentInfo = info
    tvSummary.text = currentInfo.summary

    fun applyScrollHeight(listSize: Int) {
        val maxH = (resources.displayMetrics.heightPixels * 0.56).toInt()
        scroll.layoutParams.height =
            if (listSize > 6) maxH else ViewGroup.LayoutParams.WRAP_CONTENT
        scroll.requestLayout()
    }

    fun renderRows(list: List<com.example.s1client.util.RateEntry>) {
        llRatings.removeAllViews()
        if (list.isEmpty()) {
            val empty = android.widget.TextView(ctx).apply {
                text = "暂无逐条评分记录。"
                textSize = 13f
                setTextColor(resources.getColor(android.R.color.darker_gray))
                setPadding(0, 16, 0, 16)
            }
            llRatings.addView(empty)
        } else {
            list.forEachIndexed { idx, e ->
                val row = layoutInflater.inflate(R.layout.item_rating_row, llRatings, false)
                val ivAvatar = row.findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.ivAvatar)
                val tvLetter = row.findViewById<android.widget.TextView>(R.id.tvAvatarLetter)
                val tvName = row.findViewById<android.widget.TextView>(R.id.tvName)
                val tvScore = row.findViewById<android.widget.TextView>(R.id.tvScore)
                val tvReason = row.findViewById<android.widget.TextView>(R.id.tvReason)

                tvLetter.text = e.name.firstOrNull()?.toString() ?: "?"
                tvLetter.background = com.example.s1client.ui.PostAdapter.makeCircleGradient(e.uid.hashCode())
                tvName.text = e.name
                val sign = if (e.amount >= 0) "+" else "−"
                tvScore.text = "$sign${kotlin.math.abs(e.amount)}"
                // 加绿减红（与 PS1 评分抽屉/便签胶囊同一口径，替换原 A 股配色）
                tvScore.setTextColor(
                    if (e.amount >= 0) Ps1Skin.SCORE_UP else Ps1Skin.SCORE_DOWN
                )
                tvReason.text = e.reason.ifBlank { "—" }

                val avatarUrl = if (!e.avatar.isNullOrBlank()) e.avatar
                else com.example.s1client.ui.PostAdapter.buildAvatarUrl(e.uid)
                if (!avatarUrl.isNullOrBlank()) {
                    ivAvatar.alpha = 0f
                    ivAvatar.load(avatarUrl) {
                        listener(
                            onSuccess = { _, _ -> ivAvatar.alpha = 1f },
                            onError = { _, _ -> ivAvatar.alpha = 0f }
                        )
                    }
                }

                // 整行长条点按 → 打开个人资料卡片（绕过头像太小的问题）
                if (e.uid.isNotBlank()) {
                    row.setOnClickListener {
                        com.example.s1client.ui.showUserCardDialog(ctx, e.uid, e.name)
                    }
                } else {
                    row.isClickable = false
                    row.foreground = null
                }

                llRatings.addView(row)

                // 行间分隔线（最后一条不加）
                if (idx < list.lastIndex) {
                    llRatings.addView(android.view.View(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1
                        )
                        setBackgroundColor(resources.getColor(android.R.color.darker_gray))
                        alpha = 0.25f
                    })
                }
            }
        }
        applyScrollHeight(list.size)
    }

    renderRows(currentInfo.entries)

    // 服务端截断了内联 ratelog（entries 少于参与人数），提供「加载完整评分历史」
    val truncated = currentInfo.entries.size < currentInfo.count
    llLoadFull.visibility = if (truncated) View.VISIBLE else View.GONE
    if (truncated) {
        btnLoadFull.text = "加载完整评分历史 (共${currentInfo.count}人)"
        btnLoadFull.setOnClickListener {
            btnLoadFull.isEnabled = false
            pbLoadFull.visibility = View.VISIBLE
            btnLoadFull.text = "加载中…"
            lifecycleScope.launch {
                val full = withContext(Dispatchers.IO) { fetchFullRatings(pid) }
                if (full != null && full.entries.isNotEmpty()) {
                    currentInfo = full
                    rateMap.put(pid, full)
                    tvSummary.text = full.summary
                    renderRows(full.entries)
                    llLoadFull.visibility = View.GONE
                    pbLoadFull.visibility = View.GONE
                } else {
                    pbLoadFull.visibility = View.GONE
                    btnLoadFull.isEnabled = true
                    btnLoadFull.text = "加载失败，点击重试"
                }
            }
        }
    }

    dialog.findViewById<View>(R.id.ivClose).setOnClickListener { dialog.dismiss() }
    dialog.findViewById<View>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
    dialog.findViewById<View>(R.id.btnRate).setOnClickListener {
        dialog.dismiss()
        showRateDialog(post)
    }

    dialog.show()
}

/** 拉取某一层的完整评分历史（不受内联 ratelog 条数截断限制）。参考 s1er fetchFullRateLog */

internal fun ThreadDetailActivity.fetchFullRatings(pid: String): com.example.s1client.util.RateInfo? {
    return try {
        val url = "https://stage1st.com/2b/forum.php?mod=misc&action=viewratings" +
                "&tid=$tid&pid=$pid&mobile=2"
        val req = okhttp3.Request.Builder().url(url).build()
        val html = RetrofitClient.httpClient.newCall(req).execute()
            .use { it.body?.string().orEmpty() }
        android.util.Log.d("S1Rate", "viewratings pid=$pid len=${html.length}")
        com.example.s1client.util.RatingParser.parseViewRatings(html, pid)
    } catch (e: Exception) {
        android.util.Log.e("S1Rate", "fetch full ratings failed pid=$pid", e)
        null
    }
}

/**
 * 发起评分：拉评分表单 → 打开 PS1 评分抽屉。
 *
 * 与旧版 AlertDialog 列表的区别：分值在表单允许的离散档位里选择、只提交一次
 * （见 com.example.s1client.ui.RateDrawer 的类注释）；理由默认不填。
 * 表单拉取/提交/刷新链路（fetchRateForm / submitRate / loadRatings）保持不变。
 */
internal fun ThreadDetailActivity.showRateDialog(post: Post) {
    if (!ForumDataCache.isLoggedIn()) { toast("请先登录"); return }
    val pid = post.pid ?: return
    lifecycleScope.launch {
        S1Notify.show(this@showRateDialog, "正在打开评分面板…", S1Notify.Type.INFO)
        val form = withContext(Dispatchers.IO) { fetchRateForm(pid) }
        if (form.hasError) {
            toast("获取评分表单失败：${form.error}")
            return@launch
        }
        com.example.s1client.ui.RateDrawer.show(this@showRateDialog, post, form) { p, amount, reason, notify, f ->
            submitRate(p, amount, reason, notify, f)
        }
    }
}

/** 拉取 Discuz 评分表单（带 mobile=2 才会返回可提交的隐藏字段与分值选项） */

internal fun ThreadDetailActivity.fetchRateForm(pid: String): com.example.s1client.util.RateFormOptions {
    val fallback = com.example.s1client.util.RateFormOptions(
        scoreOptions = com.example.s1client.util.RateFormOptions.DEFAULT_SCORE_OPTIONS,
        reasonPresets = com.example.s1client.util.RateFormOptions.DEFAULT_REASON_PRESETS,
        formHash = ForumDataCache.formhash,
        tid = tid,
        pid = pid,
        referer = "https://stage1st.com/2b/thread-$tid-1-1.html",
        handleKey = "rate",
        minScore = -1,
        maxScore = 1,
        remainingToday = null,
        error = "获取评分表单失败"
    )
    return try {
        val url = "https://stage1st.com/2b/forum.php?mod=misc&action=rate" +
                "&tid=$tid&pid=$pid&mobile=2&inajax=1"
        val req = okhttp3.Request.Builder().url(url).build()
        val html = RetrofitClient.httpClient.newCall(req).execute()
            .use { it.body?.string().orEmpty() }
        com.example.s1client.util.RatingParser.parseRateForm(html, tid, pid)
    } catch (e: Exception) {
        fallback.copy(error = "获取评分表单失败：${e.message}")
    }.let { opts ->
        // formhash 缺失时回退到缓存的全局 formhash
        if (opts.formHash.isBlank() && ForumDataCache.formhash.isNotBlank()) {
            opts.copy(formHash = ForumDataCache.formhash)
        } else opts
    }
}


/**
 * 旧版"理由二次确认"流程，PS1 抽屉默认不填理由后已无调用方。
 * 暂保留：若以后要在抽屉里加"评分理由"入口，可直接复用它接 submitRate。
 */
internal fun ThreadDetailActivity.pickReasonThenSubmit(
    pid: String,
    amount: Int,
    form: com.example.s1client.util.RateFormOptions
) {
    val act = this
    val reasons = form.reasonPresets.toMutableList().apply {
        add("自定义理由")
        add("不填理由")
    }
    AlertDialog.Builder(act)
        .setTitle(if (amount > 0) "加 $amount 分" else "扣 ${-amount} 分")
        .setItems(reasons.toTypedArray()) { _, which ->
            when (val label = reasons[which]) {
                "不填理由" ->
                    lifecycleScope.launch { submitRate(pid, amount, "", form.notifyAuthorDefault, form) }
                "自定义理由" -> {
                    val input = android.widget.EditText(this).apply { hint = "输入评分理由" }
                    AlertDialog.Builder(act)
                        .setTitle("自定义理由")
                        .setView(input)
                        .setPositiveButton("确定") { _, _ ->
                            lifecycleScope.launch {
                                submitRate(
                                    pid, amount,
                                    input.text.toString().trim(),
                                    form.notifyAuthorDefault, form
                                )
                            }
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
                else ->
                    lifecycleScope.launch { submitRate(pid, amount, label, form.notifyAuthorDefault, form) }
            }
        }
        .setNegativeButton("取消", null)
        .show()
}

/**
 * 提交一次评分。
 * @return true=服务端确认成功（抽屉据此关闭）；false=失败，toast 已在内部提示，
 *         抽屉保留不关闭，允许用户重试。
 */
internal suspend fun ThreadDetailActivity.submitRate(
    pid: String,
    amount: Int,
    reason: String,
    notifyAuthor: Boolean,
    form: com.example.s1client.util.RateFormOptions
): Boolean {
    val fh = form.formHash.ifBlank { ForumDataCache.formhash }
    if (fh.isBlank()) {
        toast("获取 formhash 失败，请先登录", S1Notify.Type.ERROR)
        return false
    }
    val ok = withContext(Dispatchers.IO) {
        try {
            val url = "https://stage1st.com/2b/forum.php?mod=misc" +
                    "&action=rate&ratesubmit=yes&infloat=yes&inajax=1"
            val fb = okhttp3.FormBody.Builder()
                .add("formhash", fh)
                .add("tid", form.tid.ifBlank { tid })
                .add("pid", form.pid.ifBlank { pid })
                .add("score1", amount.toString())
                .add("reason", reason)
                .add("referer", form.referer.ifBlank { "https://stage1st.com/2b/thread-$tid-1-1.html" })
            if (form.handleKey.isNotBlank()) fb.add("handlekey", form.handleKey)
            if (notifyAuthor && !form.notifyAuthorDisabled) fb.add("sendreasonpm", "on")
            val req = okhttp3.Request.Builder().url(url).post(fb.build()).build()
            val resp = RetrofitClient.httpClient.newCall(req).execute()
            val body = resp.use { it.body?.string().orEmpty() }
            android.util.Log.d("S1Rate", "HTTP=${resp.code} body=${body.take(300)}")
            if (com.example.s1client.util.RatingParser.isRateSuccess(body)) {
                true
            } else {
                val err = com.example.s1client.util.RatingParser.extractMessagetextError(body)
                android.util.Log.e("S1Rate", "rate failed: ${err ?: body.take(200)}")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("S1Rate", "rate failed", e)
            false
        }
    }
    toast(
        if (ok) "评分成功" else "评分失败（可能没有权限）",
        if (ok) S1Notify.Type.SUCCESS else S1Notify.Type.ERROR
    )
    if (ok) {
        // 重新抓当前页评分，评分行汇总与便签胶囊分数都会跟着变
        ratingFetchedPages.remove(currentPage)
        loadRatings(currentPage)
    }
    return ok
}

// ============ 只看楼主 ============
