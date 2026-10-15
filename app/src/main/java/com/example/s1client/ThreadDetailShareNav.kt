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

// ================= 多楼层分享 + 页码导航 =================
internal fun ThreadDetailActivity.enterShareSelectMode(post: Post) {
        val act = this
        shareSelectMode = true
        shareSelected.clear()
        post.pid?.let { shareSelected.add(it) }
        pageAdapter.applyShareSelectMode(true)
        binding.bottomBar.visibility = View.GONE
        binding.shareSelectBar.visibility = View.VISIBLE
        updateShareSelectBar()
        Haptic.light(act)
    }

    /** 点按楼层：在集合中切换该楼（受上限约束） */

internal fun ThreadDetailActivity.toggleShareFloor(post: Post) {
        val act = this
        val pid = post.pid ?: return
        if (shareSelected.contains(pid)) {
            shareSelected.remove(pid)
        } else {
            if (shareSelected.size >= SHARE_MAX) {
                toast("最多选择 $SHARE_MAX 个楼层")
                return
            }
            shareSelected.add(pid)
        }
        Haptic.light(act)
        updateShareSelectBar()
        pageAdapter.applyShareSelectMode(true)
    }

    /** 退出多选模式：清空选中、恢复原回复条 */

internal fun ThreadDetailActivity.exitShareSelectMode() {
        shareSelectMode = false
        shareSelected.clear()
        pageAdapter.applyShareSelectMode(false)
        binding.shareSelectBar.visibility = View.GONE
        binding.bottomBar.visibility = View.VISIBLE
    }

    /** 刷新底部分享条：已选数量 + 分享按钮可用态 */

internal fun ThreadDetailActivity.updateShareSelectBar() {
        binding.tvShareCount.text = "已选 ${shareSelected.size} / $SHARE_MAX"
        val has = shareSelected.isNotEmpty()
        binding.btnShareSelected.isEnabled = has
        binding.btnShareSelected.alpha = if (has) 1f else 0.5f
    }

    /**
     * 把选中的楼层拼成纯文本，调起系统分享面板（可发微信/QQ/备忘录等）。
     * 顺序：按页码 + 页内顺序遍历已加载页并筛出选中楼层，天然等于楼层顺序。
     */

internal fun ThreadDetailActivity.shareSelectedFloors() {
        if (shareSelected.isEmpty()) { toast("请先选择楼层"); return }
        val picked = mutableListOf<Post>()
        for (p in 1..loadedMaxPage) {
            pageAdapter.getPage(p)?.forEach { post ->
                if (post.pid != null && shareSelected.contains(post.pid)) picked.add(post)
            }
        }
        picked.sortBy { it.position?.toIntOrNull() ?: Int.MAX_VALUE }

        val subjectText = subject.ifBlank { "Stage1st 帖子" }
        val url = "https://stage1st.com/2b/thread-$tid-1-1.html"
        val sb = StringBuilder()
        sb.append(subjectText).append("\n").append(url).append("\n\n")
        sb.append("—— 多选分享（${picked.size} 楼）——\n\n")
        picked.forEachIndexed { i, post ->
            val floorLabel = if (post.first == "1") "楼主" else "${post.position ?: "?"}楼"
            sb.append("${post.author ?: "未知"} ($floorLabel):\n")
            val body = extractPlainText(post.message)
            sb.append(if (body.isBlank()) "（无文字内容）" else body)
            if (i < picked.lastIndex) sb.append("\n\n")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subjectText)
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        try {
            startActivity(Intent.createChooser(intent, "分享选中的楼层"))
        } catch (e: Exception) {
            toast("无法分享")
        }
    }

    /** 返回键/顶栏返回：多选模式下优先退出多选，否则关闭详情页 */

internal fun ThreadDetailActivity.handleBack() {
        if (shareSelectMode) {
            exitShareSelectMode()
            return
        }
        finish()
    }



    // ============ 页码指示 ============

internal fun ThreadDetailActivity.updatePageIndicator() {
        val total = if (totalPages < 1) 1 else totalPages
        binding.tvPageInfo.text = "$currentPage / $total"
    }

    /**
     * 点底部页码：在屏幕左侧中部展开贴边浮条（左手拇指易够到）。
     * 紧凑滚轮选页 + 进度填充 + 咔哒声，顶部确认块提交，无操作自动淡出。
     */

internal fun ThreadDetailActivity.showPagePicker() {
        val total = (if (totalPages > 0) totalPages else loadedMaxPage).coerceAtLeast(1)
        if (total <= 1) { toast("本贴只有一页"); return }
        // 统一设计语言：详情页与首页共用 WheelJumpSheetView（EdgePageBar 弃用）
        val root = findViewById<ViewGroup>(android.R.id.content)
        if (jumpSheet == null) {
            val scrim = android.view.View(this).apply {
                setBackgroundColor(0x73000000.toInt())
                visibility = android.view.View.GONE
                setOnClickListener { hideJumpSheet() }
            }
            root.addView(scrim, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            sheetScrim = scrim
            val sheet = com.example.s1client.ui.WheelJumpSheetView(this).apply {
                visibility = android.view.View.GONE
            }
            root.addView(sheet, android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.BOTTOM))
            jumpSheet = sheet
        }
        val accent = run {
            val tv = android.util.TypedValue()
            theme.resolveAttribute(com.example.s1client.R.attr.s1Accent, tv, true)
            if (tv.type != android.util.TypedValue.TYPE_NULL) tv.data else 0xFF5E6AD2.toInt()
        }
        jumpSheet?.setup("跳转到楼层", 1, total, currentPage, accent, 0x295E6AD2)
        jumpSheet?.setS1Colors(
            com.example.s1client.ui.theme.s1PaletteFor(
                com.example.s1client.util.ThemeManager.currentTheme
            )
        )
        jumpSheet?.onJump = { target ->
            hideJumpSheet()
            if (target != currentPage) {
                pendingFlashPage = target   // 原型规格 7：跳转落点高亮
                if (pageAdapter.hasPage(target)) {
                    binding.vpPages.setCurrentItem(target - 1, false)
                    updatePageIndicator()
                } else {
                    gotoPage(target)
                }
            }
        }
        jumpSheet?.onRequestClose = { hideJumpSheet() }
        sheetScrim?.visibility = android.view.View.VISIBLE
        jumpSheet?.visibility = android.view.View.VISIBLE
        jumpSheet?.alpha = 0f
        jumpSheet?.animate()?.alpha(1f)?.setDuration(220)?.start()
    }

    private fun ThreadDetailActivity.resolveAttrColor(attr: Int, fallback: Int): Int {
        val tv = android.util.TypedValue()
        theme.resolveAttribute(attr, tv, true)
        return if (tv.type != android.util.TypedValue.TYPE_NULL) tv.data else fallback
    }

    private var jumpSheet: com.example.s1client.ui.WheelJumpSheetView? = null
    private var sheetScrim: android.view.View? = null

    internal fun ThreadDetailActivity.hideJumpSheet() {
        jumpSheet?.animate()?.alpha(0f)?.setDuration(200)?.withEndAction {
            jumpSheet?.visibility = android.view.View.GONE
            sheetScrim?.visibility = android.view.View.GONE
        }?.start()
    }

    /**
     * 滚轮震动：速度越快震得越明显（模拟 iOS 滚轮的段落感）。
     * 优先用系统预置的 EFFECT_TICK（脆、短、系统级），设备支持振幅控制时
     * 再按速度放大振幅。
     */
    internal var lastHapticMs = 0L

internal fun ThreadDetailActivity.tickHaptic(speed: Float) {
        val now = android.os.SystemClock.elapsedRealtime()
        // 快滚时间隔稍长一点，避免震成一片糊音
        val gap = if (speed > 1.2f) 26L else 34L
        if (now - lastHapticMs < gap) return
        lastHapticMs = now

        runCatching {
            val v = getSystemService(android.content.Context.VIBRATOR_SERVICE)
                    as? android.os.Vibrator ?: return
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // 系统级「刻度」反馈，最接近 iOS 滚轮的段落感
                v.vibrate(android.os.VibrationEffect.createPredefined(
                    android.os.VibrationEffect.EFFECT_TICK))
            } else if (v.hasAmplitudeControl()) {
                // 慢滚轻、快滚重
                val amp = (90 + (speed.coerceIn(0f, 2.5f) / 2.5f) * 100).toInt().coerceIn(60, 235)
                v.vibrate(android.os.VibrationEffect.createOneShot(12, amp))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(12)
            }
        }
    }

    /**
     * 跳到指定楼层：只请求目标页一次（不再把中间页逐页拉一遍），
     * 拿到后直接切页并滚到该楼在本页内的位置。
     */

internal fun ThreadDetailActivity.goToFloor(floor: Int) {
        if (floor < 1) { toast("楼层从 1 开始"); return }
        val (targetPage, indexInPage) =
            com.example.s1client.util.Paging.locateFloor(floor, perPage)

        val jump = {
            binding.vpPages.setCurrentItem(targetPage - 1, false)
            // 切页后该页 RecyclerView 才完成绑定；延迟到绑定完成再滚（用按页定位，不依赖 attachedRv）
            binding.vpPages.postDelayed({
                pageAdapter.scrollToPositionInPage(
                    targetPage,
                    pageAdapter.listIndexFromPostIndex(targetPage, indexInPage)
                )
            }, 220)
        }

        if (pageAdapter.hasPage(targetPage)) {
            jump()
            toast("已跳到第 ${floor} 楼")
            return
        }
        if (isLoading) { toast("正在加载，请稍后"); return }

        // 关键：单次请求目标页，不等中间页
        isLoading = true
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val posts = fetchOnePage(targetPage)
                syncPageSocketsOnMain()
                if (posts.isEmpty()) {
                    toast("没有第 ${floor} 楼")
                    return@launch
                }
                val ratingsReady = preloadRatings(targetPage)
                pageAdapter.setPage(targetPage, posts)
                if (targetPage > loadedMaxPage) loadedMaxPage = targetPage
                hasMore = morePagesToLoad()
                if (ratingsReady) ratingFetchedPages.add(targetPage)
                pageAdapter.getPage(targetPage)?.let { loadPowers(it) }
                jump()
                toast("已跳到第 ${floor} 楼")
            } finally {
                binding.progressBar.visibility = View.GONE
                isLoading = false
            }
        }
    }

    /** 滑到尚未加载的页码（拖拽/翻页越过了中间页）时按需补拉 */

internal fun ThreadDetailActivity.onPageMissing(page: Int) {
        // pageInRange：预留槽位后，滑过界会回调到不存在的页码（如第 0 页或超出总页数），
        // 不挡住的话会一直发无意义的请求。
        if (!pageInRange(page)) return
        // 已经拿到内容就别重复请求（预留槽位会在每次 bind 时回调这里）
        if (pageAdapter.hasPage(page)) return
        //
        // ⚠️ 原来这里还有一道 `if (isLoading) return`，是"部分网页永久空白"的根因：
        //   loadFirstPage 从发请求到纠正页码**全程占着 isLoading**，而 ViewPager2 会在
        //   首屏就把左右相邻页一起 bind —— 那两次 onPageMissing 全部被挡掉，
        //   占位 VH 拿不到数据；等用户真正滑过去时它**不会重新 bind**（VH 已存在），
        //   于是那两页永远空白。
        // 改成"按页去重"：同一页并发只会请求一次，不再依赖全局 isLoading。
        if (!missingPages.add(page)) return
        lifecycleScope.launch {
            try {
                val posts = fetchOnePage(page)
                syncPageSocketsOnMain()
                if (posts.isNotEmpty()) {
                    val ratingsReady = preloadRatings(page)
                    pageAdapter.setPage(page, posts)
                    if (page > loadedMaxPage) loadedMaxPage = page
                    hasMore = morePagesToLoad()
                    if (ratingsReady) ratingFetchedPages.add(page)
                    pageAdapter.getPage(page)?.let { loadPowers(it) }
                }
            } finally {
                missingPages.remove(page)
            }
        }
    }

    /**
     * 顺序往下还能不能继续加载。
     *
     * 注意：不能用「某一页返回的楼层数 < PAGE_SIZE」来判断到头了——最后一页本来就
     * 不满一页，而且跳到最后一页后再往回跳时，这个标志还是 false，会误报"已是最后一页"。
     * 一律按帖子总页数 [totalPages] 判断（拿不到总页数时保守放行）。
     */

internal fun ThreadDetailActivity.morePagesToLoad(): Boolean =
        com.example.s1client.util.Paging.morePagesToLoad(loadedMaxPage, totalPages)

    /** 目标页码是否在帖子范围内 */

internal fun ThreadDetailActivity.pageInRange(page: Int): Boolean =
        com.example.s1client.util.Paging.pageInRange(page, totalPages)

    // ============ 数据加载：一页 40 楼 ============
    /**
     * 直接请求 API 一页（40 楼），超出截取，少于 40 就全部返回。
     */

internal suspend fun ThreadDetailActivity.fetchOnePage(page: Int): List<Post> = withContext(Dispatchers.IO) {
        try {
            val r = RetrofitClient.api.getThreadDetail(
                tid = tid, page = page,
                authorid = if (isOnlyAuthor) authorId else null
            )
            r.variables?.formhash?.let { ForumDataCache.formhash = it }
            r.variables?.thread?.fid?.let { if (it.isNotBlank()) threadFid = it }
            // 总页数 = (回复数 + 楼主层) / 每页楼层数
            r.variables?.thread?.let { t ->
                val replies = t.replies?.toIntOrNull() ?: -1
                val ppp = t.ppp?.toIntOrNull()?.takeIf { it > 0 } ?: PAGE_SIZE
                perPage = ppp
                if (replies >= 0) {
                    // ⚠️ 这里只算数字，**绝对不能碰 UI**：fetchOnePage 跑在 Dispatchers.IO 上，
                    // 而 reserveUpTo（内部 notifyItemRangeInserted）与 updatePageIndicator
                    //（改 TextView）都必须在主线程，否则抛 CalledFromWrongThreadException；
                    // 该异常又会被下面的 catch 吞成空列表 → 表现为「进任何帖子都加载失败」。
                    totalPages = com.example.s1client.util.Paging.totalPages(replies, ppp)
                }
            }
            val posts = r.variables?.postList ?: emptyList()
            // 投票数据顺手带走：整个帖子的属性，第 1 页的响应里就有，
            // 不必再为它单独发一次请求（原来 loadPollIfAny() 每次开帖都多发一次）。
            if (page == 1) {
                capturedPollSpecial = r.variables?.thread?.special
                capturedPollJson = r.variables?.specialPoll
            }
            // 瀑布流封面缓存：服务端只对约 1/4 帖子返回 attachmentImagePreviewList，
            // 用户进详情页看到图就顺手记下来，下次瀑布流这张卡就有封面（零额外请求）
            if (page == 1) {
                posts.firstOrNull { it.first == "1" }?.message?.let { msg ->
                    com.example.s1client.util.CoverResolver
                        .pickFirstContentImage(msg)
                        ?.let { url ->
                            com.example.s1client.data.store.CoverCacheStore
                                .get(applicationContext).put(tid, url)
                        }
                }
            }
            // take() 返回新列表副本；subList 只是原列表视图，上游修改会连带变化。
            //
            // 关键：截断上限必须用**本次响应自报的 ppp**，不能写死 PAGE_SIZE(40)。
            // 服务端每页数（perPage/ppp）不是 40 时（如被论坛设置成 20 或 50），
            // 按 40 截断会凭空吞掉楼层 —— 表现就是"部分贴子缺页/楼层错位"。
            val cap = if (perPage > 0) perPage else PAGE_SIZE
            if (posts.size > cap) posts.take(cap) else posts
        } catch (e: Exception) {
            // 原来这里直接 emptyList()，把超时/解析失败/网络异常全吞成"没有数据"，
            // 表现是"进入帖子提示加载失败"但日志里什么都查不到。必须留下类型和页码。
            //
            // 特别注意「子线程碰 View」这类编程错误：曾真的出现过 HTTP 200 成功，
            // 却因为在 IO 线程里调 adapter 通知 / 改 TextView 而失败，
            // 异常被这个 catch 吞成空列表 → 表现为「进任何帖子都加载失败」。
            // 该异常类在 SDK 桩里没有独立 class（属于 framework 内部），只能按类名匹配。
            if (e.javaClass.simpleName == "CalledFromWrongThreadException") {
                Log.e("S1Thread", "fetchOnePage(page=$page) 在子线程碰了 View —— 编程错误，看栈", e)
            } else {
                Log.w(
                    "S1Thread",
                    "fetchOnePage(page=$page) failed: ${e.javaClass.simpleName}: ${e.message}"
                )
            }
            emptyList()
        }
    }

    /**
     * [fetchOnePage] 之后、**必须在主线程**调用：把新算出的总页数同步进 UI。
     *
     * 两件事：
     * 1. 给尚未加载的页码预留槽位 —— 不预留的话 ViewPager2 的 itemCount 只到
     *    "已加载的最大页"，最后一页的位置不存在，用户滑不过去（永远卡在 n-1 页）。
     * 2. 刷新页码指示。
     *
     * 为什么独立成函数：预留动作需要在 fetchOnePage 的 6 个调用点都做一遍，
     * 而它又必须离开 IO 线程，集中一处比复制 6 遍更不容易出错。
     */

internal fun ThreadDetailActivity.syncPageSocketsOnMain() {
        val total = totalPages
        if (total > 0 && pageAdapter.getPageCount() < total) {
            pageAdapter.reserveUpTo(total)
        }
        updatePageIndicator()
    }

