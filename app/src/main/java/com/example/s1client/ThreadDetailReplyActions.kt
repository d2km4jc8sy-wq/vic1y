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

// ================= 回复 / 引用 / 收藏 =================
internal fun ThreadDetailActivity.openEditor(post: Post) {
        val pid = post.pid ?: return
        needReloadAfterEdit = true
        startActivity(Intent(this, PostEditorActivity::class.java).apply {
            putExtra("fid", threadFid)
            putExtra("editTid", tid)
            putExtra("editPid", pid)
        })
    }


    // ============ 回帖 ============

internal fun ThreadDetailActivity.showReplySheet() {
        val act = this
        if (!ForumDataCache.isLoggedIn()) { toast("请先登录"); return }
        val sheetBinding = DialogReplySheetBinding.inflate(layoutInflater)
        // 用普通 Dialog + 底部对齐：BottomSheetDialog 在这里既留不出四周边距，
        // 也不会给键盘让位，面板会被键盘整个盖住
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(sheetBinding.root)
        dialog.window?.let { w ->
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            w.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            w.setGravity(Gravity.BOTTOM)
            // 键盘弹起时窗口收缩，面板自动升到键盘上方
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            // 四周留边，让面板「缩一圈」浮起来，而不是贴着屏幕边
            val m = (14 * resources.displayMetrics.density).toInt()
            w.decorView.setPadding(m, m, m, m)
        }
        // BottomSheetDialog 不会给 ComposeView 提供 ViewTree owner，必须自己挂到 decorView 上，
        // 否则 ComposeView 一 attach 就抛 ViewTreeLifecycleOwner not found（回帖直接崩）
        dialog.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(this)
            decor.setViewTreeViewModelStoreOwner(this)
            decor.setViewTreeSavedStateRegistryOwner(this)
        }

        val quote = quotedPost
        if (quote != null) {
    val act = this
            sheetBinding.llQuotePreview.visibility = View.VISIBLE
            val author = quote.author ?: "未知"
            val floor = if (quote.first == "1") "楼主" else "${quote.position ?: "?"}楼"
            sheetBinding.tvQuoteAuthor.text = "引用 $author ($floor)"
            val plain = com.example.s1client.util.PostContentParser.parse(quote.message).text.toString()
                .replace(Regex("\\s+"), " ").trim()
            sheetBinding.tvQuoteContent.text = plain
        } else {
            sheetBinding.llQuotePreview.visibility = View.GONE
        }

        // 正文编辑器：与发帖共用 RichEditor（Compose 托管），草稿各 tid 一份
        val draftKey = DraftStore.keyForReply(tid)
        val replyValue = mutableStateOf(TextFieldValue(DraftStore.load(this, draftKey)))

        // 本地图片：上传成功后把 [attachimg] 插到光标位置
        replyAids = mutableListOf()
        onReplyImageUploaded = { aid ->
            val nv = BbCode.insert(replyValue.value, "[attachimg]$aid[/attachimg]")
            replyValue.value = nv
            DraftStore.save(act, draftKey, nv.text)
        }
        // 面板关了就断掉回调，避免上传回来时面板已销毁
        dialog.setOnDismissListener { onReplyImageUploaded = null }

        fun doSend() {
            android.util.Log.d("S1Reply", "doSend 触发, draftKey=$draftKey, 文本长度=${replyValue.value.text.length}")
            val msg = replyValue.value.text.trim()
            if (msg.isEmpty()) { toast("回复内容不能为空"); return }
            // 附件要显式声明「使用」，服务端 updateattach() 才会把它挂到这一楼层
            val attachFields = buildMap {
                replyAids.forEach { aid ->
                    put("attachnew[$aid][readperm]", "0")
                    put("attachnew[$aid][price]", "0")
                    put("attachnew[$aid][description]", "")
                }
            }
            sendReplyWithQuote(msg, quote, attachFields) { newPid ->
                quotedPost = null
                dialog.dismiss()
                jumpToNewReply(newPid)
            }
        }

        sheetBinding.cvReplyEditor.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnDetachedFromWindow
        )
        sheetBinding.cvReplyEditor.setContent {
            S1Theme {
                RichEditor(
                    value = replyValue.value,
                    onValueChange = {
                        replyValue.value = it
                        DraftStore.save(act, draftKey, it.text)
                    },
                    placeholder = "说点什么…",
                    fillToBottom = false,
                    onPickLocalImage = {
                        replyImagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    // 高度交给 RichEditor 的输入框自己定，这里只给宽度，
                    // 否则外层 min 高度会变成工具栏下方的空白
                    modifier = Modifier.fillMaxWidth(),
                    actions = {
                        Text(
                            text = "取消",
                            color = TextSub,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .clickable {
                                    quotedPost = null
                                    dialog.dismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(AccentPink)
                                .clickable { doSend() }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text(
                                "发送",
                                color = OnAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )
            }
        }

        dialog.show()
    }

    /**
     * 回帖成功后跳到自己新楼层所在的那一页，并滚到该页底部。
     * 不去猜总页数：一直往后翻，直到某一页里出现自己的新 pid 为止。
     */

internal fun ThreadDetailActivity.jumpToNewReply(newPid: String) {
        if (newPid.isBlank() || newPid == "0") return
        if (jumpLoading) return
        jumpLoading = true
        lifecycleScope.launch {
    val act = this
            try {
                // 翻页上限：已知总页数就以它为准（多留 2 页容错），拿不到才退回 60。
                // 原来固定 60 次意味着一个 200 页的帖子在弱网下要串行请求 60 次。
                var guard = 0
                val maxGuard = if (totalPages > 0) totalPages + 2 else 60
                while (guard++ < maxGuard) {
                    if (!morePagesToLoad()) break
                    val nextPage = loadedMaxPage + 1
                    val posts = fetchOnePage(nextPage)
                    syncPageSocketsOnMain()
                    if (posts.isEmpty()) break
                    // 显式绑定页码：跳过中间页后再追加时不能依赖隐式自增，否则会错位
                    pageAdapter.setPage(nextPage, posts)
                    loadedMaxPage = nextPage
                    if (posts.any { it.pid == newPid }) break
                    // 末页判断同样按服务端自报的每页数，不能写死 40
                    val cap = if (perPage > 0) perPage else PAGE_SIZE
                    if (posts.size < cap) break

                }

                var target = -1
                for (p in 1..loadedMaxPage) {
                    if (pageAdapter.getPage(p)?.any { it.pid == newPid } == true) {
                        target = p
                        break
                    }
                }
                // 兜底：回复落在最后一页时，本页缓存还是旧数据（没有新楼层），
                // 循环会因"没有更多页"提前退出而找不到 newPid → 表现为"只 toast 不跳转"。
                // 重取一次最后已加载页，把新楼层捞进缓存再定位。
                if (target == -1 && loadedMaxPage > 0) {
                    val refetched = fetchOnePage(loadedMaxPage)
                    if (refetched.any { it.pid == newPid }) {
                        pageAdapter.setPage(loadedMaxPage, refetched)
                        target = loadedMaxPage
                    }
                }
                if (target > 0) {
                    binding.vpPages.setCurrentItem(target - 1, true)
                    binding.vpPages.postDelayed({
                        val last = (pageAdapter.getPage(target)?.size ?: 1) - 1
                        pageAdapter.scrollToPositionInPage(target, if (last > 0) last else 0)
                    }, 400)
                }
            } finally {
                jumpLoading = false
            }
        }
    }


internal fun ThreadDetailActivity.sendReplyWithQuote(
        msg: String,
        quote: Post?,
        attachFields: Map<String, String> = emptyMap(),
        onDone: (String) -> Unit
    ) {
        val act = this
        lifecycleScope.launch {
            try {
                val fh = withContext(Dispatchers.IO) {
                    try { RetrofitClient.api.getThreadDetail(tid = tid).variables?.formhash }
                    catch (e: Exception) { null }
                } ?: ForumDataCache.formhash
                if (fh.isBlank()) { toast("获取 formhash 失败"); return@launch }

                var noticeAuthor = ""
                var noticeTrimStr = ""
                var noticeAuthorMsg = ""

                if (quote != null && quote.pid != null) {
                    val author = quote.author ?: ""
                    val pid = quote.pid
                    noticeAuthor = "=r_${author}_$pid"
                    val plain = com.example.s1client.util.PostContentParser.parse(quote.message)
                        .text.toString().replace(Regex("\\s+"), " ").trim()
                    val shortMsg = if (plain.length > 50) plain.take(50) + "..." else plain
                    val time = com.example.s1client.util.TimeUtil.formatDateTime(quote.dateline)
                    noticeTrimStr = "[quote][size=2][color=#999999]$author 发表于 $time[/color][/size]\n$shortMsg[/quote]\n\n"
                    noticeAuthorMsg = shortMsg
                }

                android.util.Log.d(
                    "S1Reply",
                    "sending reply: len=${msg.length}, quote=${quote?.pid}, fh=${fh.take(4)}…"
                )
                val resp = withContext(Dispatchers.IO) {
                    RetrofitClient.api.sendReply(
                        formhash = fh, tid = tid,
                        // 发帖小尾巴（对标 s1er）：回复路径同样在提交时拼接
                        message = com.example.s1client.data.store.SettingStore.appendPostTail(act, msg),
                        noticeauthor = noticeAuthor,
                        noticetrimstr = noticeTrimStr,
                        noticeauthormsg = noticeAuthorMsg,
                        attachFields = attachFields
                    )
                }
                // 注意：服务端把提示放在 Message.messagestr（不是 Variables.message），
                // 回帖成功时 Variables.pid 是新楼层 id，失败时是 "0"。
                val pid = resp.variables?.pid.orEmpty()
                val msgVal = resp.message?.messageVal.orEmpty()
                val msgStr = resp.message?.messageStr.orEmpty()
                android.util.Log.d("S1Reply", "pid=$pid, messageval=$msgVal, messagestr=$msgStr")
                val ok = pid.isNotBlank() && pid != "0"
                if (ok) {
                    DraftStore.clear(act, DraftStore.keyForReply(tid))
                    toast("回复成功", S1Notify.Type.SUCCESS)
                    onDone(pid)
                } else {
                    toast("回复失败：" + msgStr.ifBlank { msgVal.ifBlank { "pid=$pid" } })
                }
            } catch (e: Exception) {
                android.util.Log.e("S1Reply", "回帖链路异常", e)
                toast("发送失败：${e.message ?: "网络异常"}")
                android.util.Log.e("S1Reply", "reply exception", e)
                toast("回复失败：${e.message}")
            }
        }
    }

    // ============ 收藏 ============

internal fun ThreadDetailActivity.toggleFavorite() {
        if (!ForumDataCache.isLoggedIn()) { toast("请先登录", S1Notify.Type.INFO); return }
        // 取消收藏是移除操作，先二次确认（对标 s1er：取消收藏有确认）
        if (isFavorited) {
            AlertDialog.Builder(this)
                .setTitle("取消收藏")
                .setMessage("确定将本帖移出收藏列表吗？")
                .setPositiveButton("取消收藏") { _, _ -> performFavoriteToggle() }
                .setNegativeButton("保留", null)
                .show()
        } else {
            performFavoriteToggle()
        }
    }


internal fun ThreadDetailActivity.performFavoriteToggle() {
        val act = this
        lifecycleScope.launch {
            val result = FavoriteManager.toggleFavorite(tid)
            if (result.failed) {
                toast("操作失败：${result.message}", S1Notify.Type.ERROR)
                return@launch
            }
            isFavorited = result.isFavorited
            updateFavoriteButton()
            if (isFavorited) {
                // 加收藏：图标态变化 + medium 震（toast SUCCESS 自动配对）
                // 控件 05：爱心爆裂 —— 6 粒主题色粒子 + 心形弹跳
                (binding.root as? android.view.ViewGroup)?.let { root ->
                    com.example.s1client.ui.HeartBurstView.burstAt(binding.btnFavorite, root)
                }
                toast("已收藏", S1Notify.Type.SUCCESS)
            } else {
                // 取消收藏：确认后给一次 medium 确认感（toast 静音避免双震）
                Haptic.medium(act)
                S1Notify.show(act, "已取消收藏", S1Notify.Type.INFO, silent = true)
            }
        }
    }

    // ============ 楼层收藏（本地快照：保存作者、时间、标题、原文，原帖删除仍可看）============

internal fun ThreadDetailActivity.togglePostBookmark(post: Post) {
        val act = this
        val pid = post.pid ?: return
        if (PostBookmarkManager.isBookmarked(pid)) {
    val act = this
            AlertDialog.Builder(this)
                .setTitle("取消收藏楼层")
                .setMessage("确定将这条楼层移出本地收藏吗？")
                .setPositiveButton("移除") { _, _ ->
                    lifecycleScope.launch {
                        PostBookmarkManager.remove(act, pid)
                        pageAdapter.notifyBookmarksChanged()
                        S1Notify.show(act, "已取消楼层收藏", S1Notify.Type.INFO)
                    }
                }
                .setNegativeButton("保留", null)
                .show()
        } else {
            lifecycleScope.launch {
                PostBookmarkManager.add(act, post, tid, subject,
                    TimeUtil.formatSmart(post.dateline))
                pageAdapter.notifyBookmarksChanged()
                Haptic.light(act)
                S1Notify.show(act, "已收藏楼层", S1Notify.Type.SUCCESS)
            }
        }
    }

    // ===== 列表到底提示 =====
    internal val endDetectorAttached = java.util.WeakHashMap<RecyclerView, Boolean>()

    /** 末页滚到底部（或一屏装得下）时显示「已经到底」，否则隐藏；滚动与初始布局复用 */

