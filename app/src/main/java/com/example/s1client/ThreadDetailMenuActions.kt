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

// ================= 帖子操作菜单（长按楼层） =================
internal fun ThreadDetailActivity.showPostMenu(post: Post) {
    val myUid = ForumDataCache.memberUid
    val isMine = myUid.isNotEmpty() && myUid != "0" && post.authorId == myUid

    // 方案②：底部圆形图标网格（对齐 menu_preview.html 的 .rw-sheet-grid）。
    // 常用操作放前面；屏蔽/隐藏类用红色（danger）单元格；举报置底红色。
    val normalActions = mutableListOf<ThreadDetailActivity.MenuAction>()
    normalActions.add(ThreadDetailActivity.MenuAction("只看该作者", R.drawable.ic_account_circle) { onlyHim(post) })
    normalActions.add(ThreadDetailActivity.MenuAction("复制链接", R.drawable.ic_share) { copyFloorLink(post) })
    normalActions.add(ThreadDetailActivity.MenuAction("多选分享", R.drawable.ic_multi_select) { enterShareSelectMode(post) })
    normalActions.add(ThreadDetailActivity.MenuAction("选择文字", R.drawable.ic_select_text) { showSelectTextSheet(post) })
    normalActions.add(ThreadDetailActivity.MenuAction("复制全文", R.drawable.ic_copy) { copyPostText(post) })
    if (isMine) normalActions.add(ThreadDetailActivity.MenuAction("编辑", R.drawable.ic_edit) { openEditor(post) })
    // 注：引用（回复）、评分、收藏已移到操作行外露，这里不再重复。

    // 屏蔽 / 隐藏类：参考 preview 的 danger 单元格，统一红色
    val blockActions = mutableListOf<ThreadDetailActivity.MenuAction>()
    blockActions.add(ThreadDetailActivity.MenuAction("屏蔽此人", R.drawable.ic_block, danger = true) { mutePostInThread(post) })
    blockActions.add(ThreadDetailActivity.MenuAction("加入黑名单", R.drawable.ic_block, danger = true) { addToBlacklist(post) })
    if (post.first == "1") blockActions.add(
        ThreadDetailActivity.MenuAction("隐藏该帖", R.drawable.ic_visibility_off, danger = true) { showHideThreadDialog() }
    )
    // 单楼屏蔽：按 pid 精确，只藏这一楼
    val floorHidden = post.pid != null && HiddenFloors.isHidden(tid, post.pid)
    blockActions.add(
        if (floorHidden)
            ThreadDetailActivity.MenuAction("取消屏蔽", R.drawable.ic_visibility, danger = true) { unhideFloor(post) }
        else
            ThreadDetailActivity.MenuAction("屏蔽此楼", R.drawable.ic_visibility_off, danger = true) { hideFloor(post) }
    )
    if (post.pid != null && ExpandedFloors.isExpanded(tid, post.pid)) {
        blockActions.add(ThreadDetailActivity.MenuAction("收起此楼", R.drawable.ic_collapse) { collapseFloor(post) })
    }

    val reportAction = ThreadDetailActivity.MenuAction("举报", R.drawable.ic_flag, danger = true) { reportPost(post) }

    val allActions = normalActions + blockActions + listOf(reportAction)

    val menuBinding = com.example.s1client.databinding.DialogPostMenuBinding.inflate(layoutInflater)
    val dialog = Dialog(this, R.style.S1DialogSlideUp)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setContentView(menuBinding.root)
    dialog.window?.apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setDimAmount(0.45f)
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setGravity(Gravity.BOTTOM)
        // Android 12+：面板背后的内容真正“磨砂”（用反射，避免低 SDK 编译期符号缺失）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                window.javaClass
                    .getMethod("setBlurBehindRadius", Int::class.javaPrimitiveType)
                    .invoke(window, 24)
            } catch (_: Throwable) {
            }
        }
        // 透明导航栏，避免手势条压住最后一行
        setNavigationBarColor(Color.TRANSPARENT)
    }
    dialog.setCancelable(true)
    dialog.setCanceledOnTouchOutside(true)

    val iconColor = resolveThemeColor(com.example.s1client.R.attr.s1TextSecondary)
    val chipColor = resolveThemeColor(com.example.s1client.R.attr.s1TextTertiary)
    val dangerColor = Color.parseColor("#E5484D")
    val chipAlpha = 0.10f
    val dangerChipAlpha = 0.10f

    // 抓手条：取 tertiary 降透明度，深浅主题都清晰
    val tertiary = resolveThemeColor(com.example.s1client.R.attr.s1TextTertiary)
    menuBinding.grab.background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 2f * resources.displayMetrics.density
        setColor(Color.argb(
            (0.5f * 255).toInt(),
            Color.red(tertiary), Color.green(tertiary), Color.blue(tertiary)
        ))
    }

    // 3 列等宽九宫格（对齐 preview；用 GridLayout 保证每列撑满、对齐）
    allActions.forEachIndexed { index, ma ->
        val cell = com.example.s1client.databinding.DialogPostMenuGridItemBinding
            .inflate(layoutInflater)
        cell.tvLabel.text = ma.label
        cell.ivIcon.setImageResource(ma.icon)
        val baseColor = if (ma.danger) dangerColor else iconColor
        cell.ivIcon.setColorFilter(baseColor)
        cell.tvLabel.setTextColor(baseColor)
        // 圆形 chip：普通=极淡 tertiary；danger=极淡红
        val chipBase = if (ma.danger) dangerColor else chipColor
        val chipA = if (ma.danger) dangerChipAlpha else chipAlpha
        cell.circleHost.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.argb(
                (chipA * 255).toInt(),
                Color.red(chipBase), Color.green(chipBase), Color.blue(chipBase)
            ))
        }
        cell.root.setOnClickListener {
            Haptic.light(this)
            dialog.dismiss()
            ma.run()
        }
        val params = GridLayout.LayoutParams().apply {
            width = 0
            height = GridLayout.LayoutParams.WRAP_CONTENT
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            if (index >= 3) {
                topMargin = (14 * resources.displayMetrics.density).toInt()
            }
        }
        menuBinding.llGrid.addView(cell.root, params)
    }

    dialog.show()
}

/** 提取楼层纯文本，去掉表情占位符等不可见字符 */

internal fun ThreadDetailActivity.extractPlainText(message: String?): String {
    if (message.isNullOrBlank()) return ""
    return com.example.s1client.util.PostContentParser.parse(message).text.toString().trim()
        .replace("\uFFFC", "")
}


internal fun ThreadDetailActivity.copyPostText(post: Post) {
    val plain = extractPlainText(post.message)
    if (plain.isBlank()) { toast("该楼层没有可复制的文字"); return }
    val cm = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    cm.setPrimaryClip(android.content.ClipData.newPlainText("post", plain))
    toast("已复制全文")
}

/** 打开独立弹窗，把楼层正文放到 selectable TextView 里，避免和列表滚动/长按冲突 */

internal fun ThreadDetailActivity.showSelectTextSheet(post: Post) {
    val selectBinding = com.example.s1client.databinding.DialogSelectTextBinding.inflate(layoutInflater)
    val dialog = Dialog(this)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setContentView(selectBinding.root)
    dialog.window?.apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setDimAmount(0.45f)
        setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setGravity(Gravity.CENTER)
    }
    dialog.setCancelable(true)
    dialog.setCanceledOnTouchOutside(true)

    val maxH = (resources.displayMetrics.heightPixels * 0.65).toInt()
    selectBinding.tvSelectable.maxHeight = maxH
    selectBinding.tvSelectable.text = extractPlainText(post.message).ifBlank { "（无文字内容）" }

    selectBinding.tvClose.setOnClickListener { dialog.dismiss() }
    dialog.show()
}

/** 全局黑名单：塞抹布 */

internal fun ThreadDetailActivity.addToBlacklist(post: Post) {
    val uid = post.authorId
    val name = post.author ?: "未知"
    if (uid.isNullOrBlank()) { toast("无法获取用户信息"); return }
    AlertDialog.Builder(this)
        .setTitle("加入黑名单")
        .setMessage("确认全局屏蔽「$name」？\n该作者在所有帖子中都会被折叠。")
        .setPositiveButton("确认") { _, _ ->
            BlockedAuthorManager.block(uid, name)
            S1Notify.show(this, "已将「$name」加入黑名单", S1Notify.Type.SUCCESS)
            pageAdapter.notifyBlockStateChanged()
        }
        .setNegativeButton("取消", null)
        .show()
}


internal fun ThreadDetailActivity.resolveThemeColor(attr: Int): Int {
    val tv = TypedValue()
    theme.resolveAttribute(attr, tv, true)
    return tv.data
}

/** 本帖屏蔽某人：二次确认后仅在本帖折叠其楼层（不影响信息流与其他帖子） */

internal fun ThreadDetailActivity.mutePostInThread(post: Post) {
    val uid = post.authorId
    val name = post.author ?: "未知"
    if (uid.isNullOrBlank()) { toast("无法获取用户信息"); return }
    AlertDialog.Builder(this)
        .setTitle("在本帖屏蔽此人")
        .setMessage("确认在本帖屏蔽「$name」的回复？\n仅在本帖折叠其楼层，不影响信息流与其他帖子。")
        .setPositiveButton("确认屏蔽") { _, _ ->
            Haptic.heavy(this)
            ThreadMutedUsers.mute(tid, uid, name)
            S1Notify.show(this, "已在本帖屏蔽「$name」", S1Notify.Type.SUCCESS)
            pageAdapter.notifyBlockStateChanged()
            updateMuteBanner()
        }
        .setNegativeButton("取消", null)
        .show()
}

/** 收回单楼展开：恢复这一楼的折叠（不影响该人其他楼层） */

internal fun ThreadDetailActivity.collapseFloor(post: Post) {
    val pid = post.pid ?: return
    ExpandedFloors.collapse(tid, pid)
    S1Notify.show(this, "已收起该楼", S1Notify.Type.INFO)
    pageAdapter.notifyBlockStateChanged()
}

/** 屏蔽这一楼：按 pid 精确藏掉，其余楼层不受影响；若此前被展开也一并折回 */

internal fun ThreadDetailActivity.hideFloor(post: Post) {
    val pid = post.pid ?: return
    HiddenFloors.hide(tid, pid)
    ExpandedFloors.collapse(tid, pid)
    S1Notify.show(this, "已屏蔽此楼", S1Notify.Type.SUCCESS)
    pageAdapter.notifyBlockStateChanged()
}

/** 取消屏蔽这一楼：恢复折叠判定（若该楼正被展开则保持展开） */

internal fun ThreadDetailActivity.unhideFloor(post: Post) {
    val pid = post.pid ?: return
    HiddenFloors.unhide(tid, pid)
    S1Notify.show(this, "已取消屏蔽此楼", S1Notify.Type.INFO)
    pageAdapter.notifyBlockStateChanged()
}

// ============ 举报 ============
internal data class ReportForm(val formhash: String, val options: List<String>)


internal fun ThreadDetailActivity.reportPost(post: Post) {
    if (!ForumDataCache.isLoggedIn()) { toast("请先登录"); return }
    val pid = post.pid
    if (pid.isNullOrBlank()) { toast("无法定位该楼层"); return }
    val floor = if (post.first == "1") "楼主" else "${post.position ?: "?"}楼"
    lifecycleScope.launch {
        val form = withContext(Dispatchers.IO) { fetchReportForm(pid) }
        if (form == null) { toast("获取举报表单失败"); return@launch }
        showReportDialog(pid, floor, form)
    }
}

/** 抓举报预备页：拿 formhash 和可选的举报类型 */

internal fun ThreadDetailActivity.fetchReportForm(pid: String): ReportForm? {
    return try {
        val url = "https://stage1st.com/2b/misc.php?mod=report" +
                "&rtype=post&rid=$pid&tid=$tid&inajax=1"
        val html = RetrofitClient.httpClient.newCall(
            okhttp3.Request.Builder().url(url).build()
        ).execute().use { it.body?.string().orEmpty() }
        val fh = Regex("""name="formhash"[^>]*value="([^"]+)"""").find(html)
            ?.groupValues?.get(1)
            ?: Regex("""value="([0-9a-zA-Z]{8,})"[^>]*name="formhash"""").find(html)
                ?.groupValues?.get(1)
            ?: Regex("""formhash=([0-9a-zA-Z]{8,})""").find(html)?.groupValues?.get(1)
        val options = Regex("""name="report_select"[^>]*value="([^"]+)"""")
            .findAll(html).map { it.groupValues[1] }.distinct().toList()
        if (fh == null) null
        else ReportForm(fh, options.ifEmpty {
            listOf("广告垃圾", "色情低俗", "人身攻击", "政治敏感", "其他违规")
        })
    } catch (e: Exception) {
        android.util.Log.e("S1Report", "fetch report form failed", e)
        null
    }
}


internal fun ThreadDetailActivity.showReportDialog(pid: String, floor: String, form: ReportForm) {
    AlertDialog.Builder(this)
        .setTitle("举报 · $floor")
        .setItems(form.options.toTypedArray()) { _, which ->
            askReportReason(pid, form, form.options[which])
        }
        .setNegativeButton("取消", null)
        .show()
}


internal fun ThreadDetailActivity.askReportReason(pid: String, form: ReportForm, reason: String) {
    val input = EditText(this).apply {
        hint = "补充说明（可留空）"
        val p = (16 * resources.displayMetrics.density).toInt()
        setPadding(p, p / 2, p, p / 2)
    }
    AlertDialog.Builder(this)
        .setTitle("举报类型：$reason")
        .setView(input)
        .setPositiveButton("提交") { _, _ ->
            submitReport(pid, form.formhash, reason, input.text.toString().trim())
        }
        .setNegativeButton("取消", null)
        .show()
}


internal fun ThreadDetailActivity.submitReport(pid: String, formhash: String, reason: String, message: String) {
    lifecycleScope.launch {
        val ok = withContext(Dispatchers.IO) {
            try {
                val fb = okhttp3.FormBody.Builder()
                    .add("formhash", formhash)
                    .add("reportsubmit", "true")
                    .add("rtype", "post")
                    .add("rid", pid)
                    .add("tid", tid)
                    .add("report_select", reason)
                    .add("message", message)
                    .build()
                val req = okhttp3.Request.Builder()
                    .url("https://stage1st.com/2b/misc.php?mod=report&inajax=1")
                    .post(fb).build()
                val body = RetrofitClient.httpClient.newCall(req).execute()
                    .use { it.body?.string().orEmpty() }
                android.util.Log.d("S1Report", "resp=" + body.take(200))
                body.contains("举报成功") || body.contains("succeed") ||
                        body.contains("操作成功") || body.contains("report_succeed")
            } catch (e: Exception) {
                android.util.Log.e("S1Report", "submit report failed", e)
                false
            }
        }
        toast(
            if (ok) "举报已提交，谢谢反馈" else "举报失败（可能没有权限）",
            if (ok) S1Notify.Type.SUCCESS else S1Notify.Type.ERROR
        )
    }
}

/** 只看某人：复用「只看楼主」的过滤机制，把 authorId 换成他 */

internal fun ThreadDetailActivity.onlyHim(post: Post) {
    val uid = post.authorId
    if (uid.isNullOrBlank()) { toast("无法获取用户信息"); return }
    authorId = uid
    isOnlyAuthor = true
    updateOnlyAuthorButton()
    resetAndReload()
}


internal fun ThreadDetailActivity.copyFloorLink(post: Post) {
    val pid = post.pid ?: return
    val url = "https://stage1st.com/2b/forum.php?mod=redirect&goto=findpost&ptid=$tid&pid=$pid"
    val cm = getSystemService(android.content.Context.CLIPBOARD_SERVICE)
            as android.content.ClipboardManager
    cm.setPrimaryClip(android.content.ClipData.newPlainText("floor", url))
    toast("已复制本层链接")
}

// ============ 评分 ============
/**
 * 抓网页版该页的评分聚合（ratelog）。移动版 API 不返回逐层评分，
 * 网页版和 API 都是每页 40 楼，页码对齐，按 pid 合并进 [rateMap]。
 */
