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

class ThreadDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityThreadDetailBinding
    private lateinit var progressLineView: android.view.View
    private var tid: String = ""
    private var subject: String = ""
    private var isFavorited: Boolean = false
    private var currentPage: Int = 1
    private var loadedMaxPage: Int = 0
    private var hasMore: Boolean = true
    private var isLoading: Boolean = false
    /**
     * 「回帖后跳到自己新楼层」的专用标志。
     *
     * 不能复用 [isLoading]：jumpToNewReply 会连续翻最多几十页，整个过程占着 isLoading
     * 的话，期间用户在 OnPageChangeCallback 触发的预加载、gotoPage、跳楼层全被门禁挡掉，
     * 弱网下表现就是"整页假死、什么都点不动"。两者语义不同，各管各的。
     */
    private var jumpLoading: Boolean = false

    private var authorId: String? = null
    private var isOnlyAuthor: Boolean = false
    private var quotedPost: Post? = null
    private var threadFid: String = ""

    private var attachedRv: RecyclerView? = null
    /** 总页数：由 thread.replies + ppp 算出。
     *  @Volatile：它在 IO 线程写（fetchOnePage）、主线程读（UI 与翻页判断），必须保证可见性 */
    @Volatile
    private var totalPages: Int = 1
    /** 实际每页楼层数：论坛返回 t.ppp；与 totalPages 用同一口径，避免跳页落错页。
     *  同样跨线程读写 → @Volatile */
    @Volatile
    private var perPage: Int = 40

    // ===== 多选分享：选中态 + 已选 pid（按加入顺序，便于按楼层序拼接） =====
    private var shareSelectMode = false
    private val shareSelected = java.util.LinkedHashSet<String>()
    private val SHARE_MAX = 10 // 对齐 s1er 的 shareMaxSelectedFloors

    // ===== 回帖面板的本地图片 =====
    /** 本次回帖上传成功的附件 aid，提交时要带上 attachnew[...] */
    private var replyAids: MutableList<String> = mutableListOf()
    /** 上传成功后的回调：把 [attachimg] 插进回帖输入框；面板关闭时置空 */
    private var onReplyImageUploaded: ((String) -> Unit)? = null

    // 用 Activity 级 launcher：面板 ComposeView 在独立 Dialog 窗口里，
    // 拿不到 ActivityResultRegistryOwner，用不到 rememberLauncherForActivityResult
    private val replyImagePicker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val callback = onReplyImageUploaded
        if (uri == null || callback == null) return@registerForActivityResult
        toast("正在上传图片…")
        lifecycleScope.launch {
            runCatching {
                ImageUploader.upload(
                    this@ThreadDetailActivity, uri,
                    listOf(
                        ImageUploader.replyPage(tid, threadFid),
                        ImageUploader.newthreadPage(threadFid)
                    )
                )
            }
                .onSuccess { aid ->
                    replyAids.add(aid)
                    callback(aid)
                    toast("图片已插入")
                }
                .onFailure {
                    android.util.Log.e("S1Upload", "reply pick/upload failed", it)
                    toast("图片上传失败：" + (it.message ?: it.javaClass.simpleName))
                }
        }
    }

    private lateinit var pageAdapter: PageAdapter
    private val PAGE_SIZE = 40  // 每页最多 40 楼

    /** pid -> 评分聚合，来自网页版 viewthread HTML 的 ratelog（移动版 API 没有） */
    private val rateMap = java.util.concurrent.ConcurrentHashMap<String, com.example.s1client.util.RateInfo>()
    /** 已经抓过评分的页码，避免重复请求。
     *  注意：loadFirstPage / 多个 preloadPage 会并发调用 loadRatings，
     *  这里必须是并发安全集合（普通 mutableSetOf 会 ConcurrentModificationException）。 */
    private val ratingFetchedPages = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()

    /** 战斗力数据到达后防抖刷新（多条 uid 陆续返回，合并成一次列表重绑） */
    private val powerRefreshRunnable = Runnable {
        Log.d("S1FLICK", "POWER_ARRIVED")
        pageAdapter.notifyRatingsChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 细节 02：页面转场触觉 —— 进详情页 LIGHT 一震（"进来了"），返回在 finish 处 SELECTION
        Haptic.light(this)
        binding = ActivityThreadDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdge()
        applyTopInset(binding.topBar)
        applyBottomInset(binding.bottomBar)
        applyBottomInset(binding.shareSelectBar)

        tid = intent.getStringExtra("tid") ?: ""
        subject = intent.getStringExtra("subject") ?: ""
        isFavorited = intent.getBooleanExtra("favorited", false)

        binding.btnBack.setOnClickListener { handleBack() }
        if (tid.isEmpty()) { finish(); return }

        binding.btnShareCancel.setOnClickListener { exitShareSelectMode() }
        binding.btnShareSelected.setOnClickListener { shareSelectedFloors() }

        // ===== 用 ViewPager2 内置手势处理水平翻页 =====
        binding.vpPages.isUserInputEnabled = true

        setupPageAdapter()
        setupHideAndMute()

        // 控件 04：换页加载珠子注入主题三色（原版三色追逐）
        val orbitPalette = com.example.s1client.ui.theme.s1PaletteFor(
            com.example.s1client.util.ThemeManager.currentTheme
        )
        binding.progressBar.setDotColors(
            orbitPalette.flowCoverColors.take(3).map { it.toArgb() }
        )

        // ===== 细节 15：顶部阅读进度细线（2.5dp，随楼层页推进渐变生长）=====
        // 强调色从主题属性取（与 S1Notify 同套解析），7 套主题自动适配
        val accentTyped = android.util.TypedValue()
        val hasAccent = theme.resolveAttribute(R.attr.s1Accent, accentTyped, true)
        val accentColor = if (hasAccent) accentTyped.data else 0xFFFF6FB5.toInt()
        val progressLine = android.view.View(this).apply {
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    accentColor,
                    ColorUtils.blendARGB(accentColor, android.graphics.Color.WHITE, 0.35f)
                )
            )
        }
        progressLine.layoutParams = androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT, (2.5f * resources.displayMetrics.density).toInt()
        ).apply { gravity = android.view.Gravity.TOP }
        progressLine.pivotX = 0f
        progressLine.scaleX = 0f
        progressLineView = progressLine
        (binding.root as? android.view.ViewGroup)?.addView(progressLine)
        binding.vpPages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPage = position + 1
                hideEndFooter()
                updatePageIndicator()
                loadRatings(currentPage)
                // 细节 15：推进进度线（按已加载的最大页数归一化，避免后期页数突变）
                val maxPage = maxOf(loadedMaxPage, currentPage, 1)
                progressLine.pivotX = 0f
                progressLine.animate().scaleX(currentPage.toFloat() / maxPage).setDuration(220).start()
                if (currentPage + 1 > loadedMaxPage && morePagesToLoad() && !isLoading) {
                    preloadPage(currentPage + 1)
                }
            }
        })

        binding.btnReplyInput.setOnClickListener { showReplySheet() }
        binding.tvPageInfo.setOnClickListener { showPagePicker() }
        binding.btnFavorite.setOnClickListener { toggleFavorite() }
        binding.btnShare.setOnClickListener { shareCurrentThread() }
        binding.btnOnlyAuthor.setOnClickListener { toggleOnlyAuthor() }


        updateFavoriteButton()
        refreshFavoriteState()
        registerDebugProbe()
        loadFirstPage()
    }

    /**
     * 仅 debug 构建可用的自检入口：让 adb 能直接触发本页的数据加载。
     *
     * 背景：MIUI 默认禁止 `adb shell input tap`（INJECT_EVENTS 被拒），无法自动化点进
     * 帖子，于是「进帖子是否还加载失败」只能靠人肉验证。装了这个探针后，
     * 一条广播就能真实跑一遍 fetchOnePage —— 之前那种"改了但没法验证"的情况能避免。
     *
     * release 构建里 registerReceiver 返回 null，什么都不做。
     */
    private fun registerDebugProbe() {
        if (!BuildConfig.DEBUG) return
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                val page = intent?.getIntExtra("page", 1) ?: 1
                Log.i("S1Probe", "probe load page=$page tid=$tid")
                lifecycleScope.launch {
                    val posts = fetchOnePage(page)
                    syncPageSocketsOnMain()
                    Log.i(
                        "S1Probe",
                        "probe done page=$page posts=${posts.size} totalPages=$totalPages " +
                            "perPage=$perPage adapterPages=${pageAdapter.getPageCount()} " +
                            "firstPid=${posts.firstOrNull()?.pid}"
                    )
                }
            }
        }
        val filter = android.content.IntentFilter("com.example.s1client.PROBE_THREAD")
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, android.content.Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(receiver, filter)
        }
    }

    /**
     * 进页面时拉一次真实收藏状态来点亮星星。
     *
     * 原来只依赖 Intent 的 `favorited` extra，但全项目没有一处 putExtra 传它，
     * 初始值恒为 false → 已收藏的帖也显示空心星。查询失败时保持原状（不误显示未收藏）。
     */
    private fun refreshFavoriteState() {
        if (!ForumDataCache.isLoggedIn()) return
        lifecycleScope.launch {
            val fav = com.example.s1client.data.store.FavoriteManager.isFavorited(tid) ?: return@launch
            if (fav != isFavorited) {
                isFavorited = fav
                updateFavoriteButton()
            }
        }
    }

    private fun setupPageAdapter() {
        pageAdapter = PageAdapter(
            onPageScroll = { position, rv, dy ->
                // 底部栏常驻，不再随滚动显隐；这里只记住当前页的列表用于回帖后定位
                if (position == currentPage - 1) {
                    attachedRv = rv
                    attachEndDetector(rv)
                }
            },
            subject = subject,
            tid = tid,
            onPostAvatarClick = { post -> showUserCard(post) },
            onPostEdit = { post -> openEditor(post) },
            onPostRateClick = { post -> showRateDialog(post) },
            onPostQuoteClick = { post ->
                quotedPost = post
                showReplySheet()
            },
            onPostFavoriteClick = { post -> togglePostBookmark(post) },
            onPostMoreClick = { post -> showPostMenu(post) },
            rateProvider = { pid -> pid?.let { rateMap[it] } },
            onPostRatingRowClick = { post -> showRatingDetail(post) },
            powerProvider = { uid -> com.example.s1client.data.store.PowerRepository.get(uid) },
            onPostQuoteJump = { floor -> floor?.toIntOrNull()?.let { goToFloor(it) } },
            bookmarkProvider = { pid -> com.example.s1client.data.store.PostBookmarkManager.isBookmarked(pid) },
            onPageMissing = { page -> onPageMissing(page) }
        )
        // 多选分享：把选中判定与切换回调注入 PageAdapter，供各页 PostAdapter 取用
        pageAdapter.shareIsSelected = { p -> p.pid != null && shareSelected.contains(p.pid) }
        pageAdapter.shareOnToggle = { p -> toggleShareFloor(p) }
        binding.vpPages.adapter = pageAdapter
    }

    // ============ 整帖隐藏（1楼菜单「隐藏该帖」）+ 本帖屏蔽实时刷新 ============
    private fun setupHideAndMute() {
        binding.tvThreadTitle.text = subject
        binding.tvMuteBanner.setOnClickListener { showMuteManageDialog() }

        // 本帖屏蔽状态变化：刷新楼层折叠 + 提示条
        lifecycleScope.launch {
            ThreadMutedUsers.mutedFlow.collect {
                updateMuteBanner()
                pageAdapter.notifyBlockStateChanged()
            }
        }
        // 全局拉黑变化：已打开的帖子也要即时折楼
        lifecycleScope.launch {
            BlockedAuthorManager.blockedFlow.collect {
                pageAdapter.notifyBlockStateChanged()
            }
        }
        // 单楼屏蔽变化：屏蔽/取消某一楼，即时重绑让该层折叠/展开
        lifecycleScope.launch {
            HiddenFloors.flow.collect {
                pageAdapter.notifyBlockStateChanged()
            }
        }
        updateMuteBanner()
    }

    /** 长按标题隐藏整帖：二次确认后隐藏并退出详情页（列表已通过 StateFlow 即时移除） */
    private fun showHideThreadDialog() {
        val titleHint = subject.takeIf { it.isNotBlank() } ?: "该帖"
        AlertDialog.Builder(this)
            .setTitle("隐藏该帖")
            .setMessage("确认隐藏「$titleHint」？\n隐藏后首页不再显示这条帖子，可在「我的 → 抹布贴」恢复。")
            .setPositiveButton("确认隐藏") { _, _ ->
                Haptic.heavy(this@ThreadDetailActivity)
                HiddenPostManager.hide(tid, subject, "", "")
                S1Notify.show(this, "已隐藏该帖", S1Notify.Type.SUCCESS)
                finish()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 本帖屏蔽提示条：有屏蔽则显示「本帖已屏蔽 N 人 · 点击管理」 */
    private fun updateMuteBanner() {
        val muted = ThreadMutedUsers.mutedInThread(tid)
        if (muted.isEmpty()) {
            binding.tvMuteBanner.visibility = View.GONE
            return
        }
        binding.tvMuteBanner.visibility = View.VISIBLE
        binding.tvMuteBanner.text = "本帖已屏蔽 ${muted.size} 人 · 点击管理"
    }

    /** 管理本帖屏蔽的人：逐条恢复 / 全部恢复 */
    private fun showMuteManageDialog() {
        val muted = ThreadMutedUsers.mutedInThread(tid).toList()
        if (muted.isEmpty()) { updateMuteBanner(); return }
        val names = muted.map { (_, n) -> n.ifBlank { "未知" } }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("本帖已屏蔽（${muted.size}）")
            .setItems(names) { _, which ->
                val (uid, _) = muted[which]
                ThreadMutedUsers.unmute(tid, uid)
                S1Notify.show(this, "已恢复该人回复", S1Notify.Type.SUCCESS)
                updateMuteBanner()
                pageAdapter.notifyBlockStateChanged()
            }
            .setPositiveButton("全部恢复") { _, _ ->
                ThreadMutedUsers.unmuteAll(tid)
                S1Notify.show(this, "已恢复全部", S1Notify.Type.SUCCESS)
                updateMuteBanner()
                pageAdapter.notifyBlockStateChanged()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ============ 顶栏的楼主信息（头像 + 昵称）============
    private fun bindTopAuthor(post: Post?) {
        val name = post?.author ?: return
        binding.tvTopAuthor.text = name
        binding.tvTopAvatarLetter.text = name.firstOrNull()?.toString() ?: "?"
        binding.tvTopAvatarLetter.background =
            com.example.s1client.ui.PostAdapter.makeCircleGradient(name.hashCode())
        binding.ivTopAvatar.alpha = 0f
        post.authorId?.let { com.example.s1client.ui.PostAdapter.buildAvatarUrl(it) }?.let { url ->
            binding.ivTopAvatar.load(url) {
                listener(
                    onSuccess = { _, _ -> binding.ivTopAvatar.alpha = 1f },
                    onError = { _, _ -> binding.ivTopAvatar.alpha = 0f }
                )
            }
        }
        if (!post.authorId.isNullOrBlank()) {
            val open = View.OnClickListener { showUserCard(post) }
            binding.topAvatarArea.setOnClickListener(open)
            binding.tvTopAuthor.setOnClickListener(open)
        }
    }

    // ============ 用户卡片（与首页头像点击共用同一套界面）============
    private fun showUserCard(post: Post) {
        val uid = post.authorId
        if (uid.isNullOrBlank()) { toast("无法获取用户信息"); return }
        com.example.s1client.ui.showUserCardDialog(this, uid, post.author ?: "未知")
    }

    // ============ 楼层菜单（仅行尾「⋯」；长按已改复制/选区）============
    /** 菜单动作：图标 + 文字标签；danger=true 用红色（屏蔽/隐藏/举报类） */
    private data class MenuAction(
        val label: String,
        val icon: Int,
        val danger: Boolean = false,
        val run: () -> Unit
    )

    private fun showPostMenu(post: Post) {
        val myUid = ForumDataCache.memberUid
        val isMine = myUid.isNotEmpty() && myUid != "0" && post.authorId == myUid

        // 方案②：底部圆形图标网格（对齐 menu_preview.html 的 .rw-sheet-grid）。
        // 常用操作放前面；屏蔽/隐藏类用红色（danger）单元格；举报置底红色。
        val normalActions = mutableListOf<MenuAction>()
        normalActions.add(MenuAction("只看该作者", R.drawable.ic_account_circle) { onlyHim(post) })
        normalActions.add(MenuAction("复制链接", R.drawable.ic_share) { copyFloorLink(post) })
        normalActions.add(MenuAction("多选分享", R.drawable.ic_multi_select) { enterShareSelectMode(post) })
        normalActions.add(MenuAction("选择文字", R.drawable.ic_select_text) { showSelectTextSheet(post) })
        normalActions.add(MenuAction("复制全文", R.drawable.ic_copy) { copyPostText(post) })
        if (isMine) normalActions.add(MenuAction("编辑", R.drawable.ic_edit) { openEditor(post) })
        // 注：引用（回复）、评分、收藏已移到操作行外露，这里不再重复。

        // 屏蔽 / 隐藏类：参考 preview 的 danger 单元格，统一红色
        val blockActions = mutableListOf<MenuAction>()
        blockActions.add(MenuAction("屏蔽此人", R.drawable.ic_block, danger = true) { mutePostInThread(post) })
        blockActions.add(MenuAction("加入黑名单", R.drawable.ic_block, danger = true) { addToBlacklist(post) })
        if (post.first == "1") blockActions.add(
            MenuAction("隐藏该帖", R.drawable.ic_visibility_off, danger = true) { showHideThreadDialog() }
        )
        // 单楼屏蔽：按 pid 精确，只藏这一楼
        val floorHidden = post.pid != null && HiddenFloors.isHidden(tid, post.pid)
        blockActions.add(
            if (floorHidden)
                MenuAction("取消屏蔽", R.drawable.ic_visibility, danger = true) { unhideFloor(post) }
            else
                MenuAction("屏蔽此楼", R.drawable.ic_visibility_off, danger = true) { hideFloor(post) }
        )
        if (post.pid != null && ExpandedFloors.isExpanded(tid, post.pid)) {
            blockActions.add(MenuAction("收起此楼", R.drawable.ic_collapse) { collapseFloor(post) })
        }

        val reportAction = MenuAction("举报", R.drawable.ic_flag, danger = true) { reportPost(post) }

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
                Haptic.light(this@ThreadDetailActivity)
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
    private fun extractPlainText(message: String?): String {
        if (message.isNullOrBlank()) return ""
        return com.example.s1client.util.PostContentParser.parse(message).text.toString().trim()
            .replace("\uFFFC", "")
    }

    private fun copyPostText(post: Post) {
        val plain = extractPlainText(post.message)
        if (plain.isBlank()) { toast("该楼层没有可复制的文字"); return }
        val cm = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("post", plain))
        toast("已复制全文")
    }

    /** 打开独立弹窗，把楼层正文放到 selectable TextView 里，避免和列表滚动/长按冲突 */
    private fun showSelectTextSheet(post: Post) {
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
    private fun addToBlacklist(post: Post) {
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

    private fun resolveThemeColor(attr: Int): Int {
        val tv = TypedValue()
        theme.resolveAttribute(attr, tv, true)
        return tv.data
    }

    /** 本帖屏蔽某人：二次确认后仅在本帖折叠其楼层（不影响信息流与其他帖子） */
    private fun mutePostInThread(post: Post) {
        val uid = post.authorId
        val name = post.author ?: "未知"
        if (uid.isNullOrBlank()) { toast("无法获取用户信息"); return }
        AlertDialog.Builder(this)
            .setTitle("在本帖屏蔽此人")
            .setMessage("确认在本帖屏蔽「$name」的回复？\n仅在本帖折叠其楼层，不影响信息流与其他帖子。")
            .setPositiveButton("确认屏蔽") { _, _ ->
                Haptic.heavy(this@ThreadDetailActivity)
                ThreadMutedUsers.mute(tid, uid, name)
                S1Notify.show(this, "已在本帖屏蔽「$name」", S1Notify.Type.SUCCESS)
                pageAdapter.notifyBlockStateChanged()
                updateMuteBanner()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 收回单楼展开：恢复这一楼的折叠（不影响该人其他楼层） */
    private fun collapseFloor(post: Post) {
        val pid = post.pid ?: return
        ExpandedFloors.collapse(tid, pid)
        S1Notify.show(this, "已收起该楼", S1Notify.Type.INFO)
        pageAdapter.notifyBlockStateChanged()
    }

    /** 屏蔽这一楼：按 pid 精确藏掉，其余楼层不受影响；若此前被展开也一并折回 */
    private fun hideFloor(post: Post) {
        val pid = post.pid ?: return
        HiddenFloors.hide(tid, pid)
        ExpandedFloors.collapse(tid, pid)
        S1Notify.show(this, "已屏蔽此楼", S1Notify.Type.SUCCESS)
        pageAdapter.notifyBlockStateChanged()
    }

    /** 取消屏蔽这一楼：恢复折叠判定（若该楼正被展开则保持展开） */
    private fun unhideFloor(post: Post) {
        val pid = post.pid ?: return
        HiddenFloors.unhide(tid, pid)
        S1Notify.show(this, "已取消屏蔽此楼", S1Notify.Type.INFO)
        pageAdapter.notifyBlockStateChanged()
    }

    // ============ 举报 ============
    private data class ReportForm(val formhash: String, val options: List<String>)

    private fun reportPost(post: Post) {
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
    private fun fetchReportForm(pid: String): ReportForm? {
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

    private fun showReportDialog(pid: String, floor: String, form: ReportForm) {
        AlertDialog.Builder(this)
            .setTitle("举报 · $floor")
            .setItems(form.options.toTypedArray()) { _, which ->
                askReportReason(pid, form, form.options[which])
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun askReportReason(pid: String, form: ReportForm, reason: String) {
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

    private fun submitReport(pid: String, formhash: String, reason: String, message: String) {
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
    private fun onlyHim(post: Post) {
        val uid = post.authorId
        if (uid.isNullOrBlank()) { toast("无法获取用户信息"); return }
        authorId = uid
        isOnlyAuthor = true
        updateOnlyAuthorButton()
        resetAndReload()
    }

    private fun copyFloorLink(post: Post) {
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
    private fun loadRatings(page: Int) {
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

    /** 拉取一页所有作者的战斗力（去重 + 限并发，新数据到达时防抖刷新列表） */
    private fun loadPowers(posts: List<Post>) {
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
    private suspend fun preloadRatings(page: Int): Boolean = withContext(Dispatchers.IO) {
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
    private fun schedulePowerRefresh() {
        binding.root.removeCallbacks(powerRefreshRunnable)
        binding.root.postDelayed(powerRefreshRunnable, 120)
    }

    private fun fetchRatingsHtml(page: Int): Map<String, com.example.s1client.util.RateInfo> {
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
    private fun showRatingDetail(post: Post) {
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
                    // 加分红、扣分红绿（A 股惯例：红涨绿跌）
                    tvScore.setTextColor(
                        if (e.amount >= 0) 0xFFF6536B.toInt() else 0xFF36C27C.toInt()
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
    private fun fetchFullRatings(pid: String): com.example.s1client.util.RateInfo? {
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

    private fun showRateDialog(post: Post) {
        if (!ForumDataCache.isLoggedIn()) { toast("请先登录"); return }
        val pid = post.pid ?: return
        val floor = if (post.first == "1") "楼主" else "${post.position ?: "?"}楼"
        lifecycleScope.launch {
            // 先看表单，按服务端给出的分值范围与选项展示（正数=加分、负数=扣分）。
            // 没权限扣分的用户组，表单里就只有正数，这里自然只出现加分。
            val form = withContext(Dispatchers.IO) { fetchRateForm(pid) }
            if (form.hasError) {
                toast("获取评分表单失败：${form.error}")
                return@launch
            }
            val amounts = form.buildScoreOptions()
                .ifEmpty { com.example.s1client.util.RateFormOptions.DEFAULT_SCORE_OPTIONS }
            val options = amounts.map { if (it > 0) "加 $it 分" else "扣 ${-it} 分" }.toTypedArray()
            val remainText = form.remainingToday?.let { "（今日剩余 $it 分）" }.orEmpty()
            AlertDialog.Builder(this@ThreadDetailActivity)
                .setTitle("评分 · $floor $remainText")
                .setItems(options) { _, which -> pickReasonThenSubmit(pid, amounts[which], form) }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    /** 拉取 Discuz 评分表单（带 mobile=2 才会返回可提交的隐藏字段与分值选项） */
    private fun fetchRateForm(pid: String): com.example.s1client.util.RateFormOptions {
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

    private fun pickReasonThenSubmit(
        pid: String,
        amount: Int,
        form: com.example.s1client.util.RateFormOptions
    ) {
        val reasons = form.reasonPresets.toMutableList().apply {
            add("自定义理由")
            add("不填理由")
        }
        AlertDialog.Builder(this)
            .setTitle(if (amount > 0) "加 $amount 分" else "扣 ${-amount} 分")
            .setItems(reasons.toTypedArray()) { _, which ->
                when (val label = reasons[which]) {
                    "不填理由" ->
                        submitRate(pid, amount, "", form.notifyAuthorDefault, form)
                    "自定义理由" -> {
                        val input = android.widget.EditText(this).apply { hint = "输入评分理由" }
                        AlertDialog.Builder(this)
                            .setTitle("自定义理由")
                            .setView(input)
                            .setPositiveButton("确定") { _, _ ->
                                submitRate(
                                    pid, amount,
                                    input.text.toString().trim(),
                                    form.notifyAuthorDefault, form
                                )
                            }
                            .setNegativeButton("取消", null)
                            .show()
                    }
                    else ->
                        submitRate(pid, amount, label, form.notifyAuthorDefault, form)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun submitRate(
        pid: String,
        amount: Int,
        reason: String,
        notifyAuthor: Boolean,
        form: com.example.s1client.util.RateFormOptions
    ) {
        lifecycleScope.launch {
            val fh = form.formHash.ifBlank { ForumDataCache.formhash }
            if (fh.isBlank()) { toast("获取 formhash 失败，请先登录"); return@launch }
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
                // 重新抓当前页评分，评分行的汇总会跟着变
                ratingFetchedPages.remove(currentPage)
                loadRatings(currentPage)
            }
        }
    }

    // ============ 只看楼主 ============
    private fun toggleOnlyAuthor() {
        if (authorId.isNullOrEmpty()) { toast("正在加载楼主信息..."); loadFirstPage(); return }
        isOnlyAuthor = !isOnlyAuthor
        updateOnlyAuthorButton()
        resetAndReload()
    }

    private fun updateOnlyAuthorButton() {
        if (isOnlyAuthor) {
            binding.btnOnlyAuthor.text = "全部楼层"
            binding.btnOnlyAuthor.isSelected = true
            val tv = TypedValue()
            theme.resolveAttribute(com.example.s1client.R.attr.s1TextOnAccent, tv, true)
            binding.btnOnlyAuthor.setTextColor(tv.data)
        } else {
            binding.btnOnlyAuthor.text = "只看楼主"
            binding.btnOnlyAuthor.isSelected = false
            val tv = TypedValue()
            theme.resolveAttribute(com.example.s1client.R.attr.s1TextSecondary, tv, true)
            binding.btnOnlyAuthor.setTextColor(tv.data)
        }
    }

    private fun resetAndReload() {
        loadedMaxPage = 0
        hasMore = true
        currentPage = 1
        isLoading = false
        setupPageAdapter()
        updatePageIndicator()
        loadFirstPage()
    }

    // ============ 收藏按钮（星标）============
    /**
     * 收藏帖子的星标。
     *
     * **固定实心星，不随收藏状态变化**（用户拍板）。
     * 楼层收藏已改用书签图标，两者形状不同、含义一目了然；
     * 若这里再叠"实心/空心"状态切换，用户就得盯着图标猜状态，反而更费解。
     * 收藏与否由点击后的 toast 明确告知。
     *
     * 保留这个函数是因为 toggle 后仍要统一重绘一次（例如恢复 tint）。
     */
    private fun updateFavoriteButton() {
        binding.btnFavorite.setImageResource(R.drawable.ic_star)
        val tv = TypedValue()
        theme.resolveAttribute(com.example.s1client.R.attr.s1Accent, tv, true)
        binding.btnFavorite.imageTintList =
            android.content.res.ColorStateList.valueOf(tv.data)
    }

    // ============ 分享 ============
    private fun shareCurrentThread() {
        val title = subject.ifBlank { "Stage1st 帖子" }
        val url = "https://stage1st.com/2b/thread-$tid-1-1.html"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title\n$url")
        }
        try {
            startActivity(Intent.createChooser(intent, "分享帖子"))
        } catch (e: Exception) {
            toast("无法分享")
        }
    }

    // ============ 多选分享（抄 s1er：进入多选模式 → 点楼层勾选 → 底部栏分享） ============
    /** 从某楼菜单进入多选模式：预选当前楼，显示底部分享条、隐藏原回复条 */
    private fun enterShareSelectMode(post: Post) {
        shareSelectMode = true
        shareSelected.clear()
        post.pid?.let { shareSelected.add(it) }
        pageAdapter.applyShareSelectMode(true)
        binding.bottomBar.visibility = View.GONE
        binding.shareSelectBar.visibility = View.VISIBLE
        updateShareSelectBar()
        Haptic.light(this@ThreadDetailActivity)
    }

    /** 点按楼层：在集合中切换该楼（受上限约束） */
    private fun toggleShareFloor(post: Post) {
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
        Haptic.light(this@ThreadDetailActivity)
        updateShareSelectBar()
        pageAdapter.applyShareSelectMode(true)
    }

    /** 退出多选模式：清空选中、恢复原回复条 */
    private fun exitShareSelectMode() {
        shareSelectMode = false
        shareSelected.clear()
        pageAdapter.applyShareSelectMode(false)
        binding.shareSelectBar.visibility = View.GONE
        binding.bottomBar.visibility = View.VISIBLE
    }

    /** 刷新底部分享条：已选数量 + 分享按钮可用态 */
    private fun updateShareSelectBar() {
        binding.tvShareCount.text = "已选 ${shareSelected.size} / $SHARE_MAX"
        val has = shareSelected.isNotEmpty()
        binding.btnShareSelected.isEnabled = has
        binding.btnShareSelected.alpha = if (has) 1f else 0.5f
    }

    /**
     * 把选中的楼层拼成纯文本，调起系统分享面板（可发微信/QQ/备忘录等）。
     * 顺序：按页码 + 页内顺序遍历已加载页并筛出选中楼层，天然等于楼层顺序。
     */
    private fun shareSelectedFloors() {
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
    private fun handleBack() {
        if (shareSelectMode) {
            exitShareSelectMode()
            return
        }
        finish()
    }

    override fun onBackPressed() {
        if (shareSelectMode) {
            exitShareSelectMode()
            return
        }
        super.onBackPressed()
    }

    // ============ 页码指示 ============
    private fun updatePageIndicator() {
        val total = if (totalPages < 1) 1 else totalPages
        binding.tvPageInfo.text = "$currentPage / $total"
    }

    /**
     * 点底部页码：在屏幕左侧中部展开贴边浮条（左手拇指易够到）。
     * 紧凑滚轮选页 + 进度填充 + 咔哒声，顶部确认块提交，无操作自动淡出。
     */
    private fun showPagePicker() {
        val total = (if (totalPages > 0) totalPages else loadedMaxPage).coerceAtLeast(1)
        if (total <= 1) { toast("本贴只有一页"); return }
        binding.edgePageBar.onConfirm = { target ->
            binding.edgePageBar.hide()
            if (target != currentPage) {
                if (pageAdapter.hasPage(target)) {
                    binding.vpPages.setCurrentItem(target - 1, false)
                    updatePageIndicator()
                } else {
                    gotoPage(target)
                }
            }
        }
        binding.edgePageBar.show(currentPage, total)
    }

    /**
     * 滚轮震动：速度越快震得越明显（模拟 iOS 滚轮的段落感）。
     * 优先用系统预置的 EFFECT_TICK（脆、短、系统级），设备支持振幅控制时
     * 再按速度放大振幅。
     */
    private var lastHapticMs = 0L
    private fun tickHaptic(speed: Float) {
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
    private fun goToFloor(floor: Int) {
        if (floor < 1) { toast("楼层从 1 开始"); return }
        val (targetPage, indexInPage) =
            com.example.s1client.util.Paging.locateFloor(floor, perPage)

        val jump = {
            binding.vpPages.setCurrentItem(targetPage - 1, false)
            // 切页后该页 RecyclerView 才完成绑定；延迟到绑定完成再滚（用按页定位，不依赖 attachedRv）
            binding.vpPages.postDelayed({
                pageAdapter.scrollToPositionInPage(targetPage, indexInPage)
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
    private fun onPageMissing(page: Int) {
        // pageInRange：预留槽位后，滑过界会回调到不存在的页码（如第 0 页或超出总页数），
        // 不挡住的话会一直发无意义的请求。
        if (isLoading || !pageInRange(page)) return
        // 已经拿到内容就别重复请求（预留槽位会在每次 bind 时回调这里）
        if (pageAdapter.hasPage(page)) return
        isLoading = true
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
                isLoading = false
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
    private fun morePagesToLoad(): Boolean =
        com.example.s1client.util.Paging.morePagesToLoad(loadedMaxPage, totalPages)

    /** 目标页码是否在帖子范围内 */
    private fun pageInRange(page: Int): Boolean =
        com.example.s1client.util.Paging.pageInRange(page, totalPages)

    // ============ 数据加载：一页 40 楼 ============
    /**
     * 直接请求 API 一页（40 楼），超出截取，少于 40 就全部返回。
     */
    private suspend fun fetchOnePage(page: Int): List<Post> = withContext(Dispatchers.IO) {
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
    private fun syncPageSocketsOnMain() {
        val total = totalPages
        if (total > 0 && pageAdapter.getPageCount() < total) {
            pageAdapter.reserveUpTo(total)
        }
        updatePageIndicator()
    }

    private fun loadFirstPage() {
        isLoading = true
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val posts = fetchOnePage(1)
                // fetchOnePage 刚算出的总页数要在主线程同步进 UI（预留槽位 + 刷新页码）
                syncPageSocketsOnMain()
                if (posts.isEmpty()) { toast("加载失败", S1Notify.Type.ERROR); return@launch }
                hasMore = morePagesToLoad()

                if (authorId.isNullOrEmpty()) {
                    val opPost = posts.firstOrNull { it.first == "1" }
                    authorId = opPost?.authorId
                }
                // 楼主头像/昵称放顶栏（小红书式），楼层里不再重复作者行
                bindTopAuthor(posts.firstOrNull { it.first == "1" })

                val ratingsReady = preloadRatings(1)
                // 必须显式写页码：syncPageSocketsOnMain 已把 maxPage 推到总页数，
                // 用 addPage（写进 maxPage+1）会把首页内容写到错误的页码上
                pageAdapter.setPage(1, posts)
                loadedMaxPage = 1
                updatePageIndicator()
                if (ratingsReady) ratingFetchedPages.add(1)
                pageAdapter.getPage(1)?.let { loadPowers(it) }
                if (hasMore) preloadPage(2)
            } finally {
                binding.progressBar.visibility = View.GONE
                isLoading = false
            }
        }
    }

    private fun preloadPage(page: Int) {
        if (page <= loadedMaxPage || isLoading || !morePagesToLoad()) return
        isLoading = true
        lifecycleScope.launch {
            try {
                val posts = fetchOnePage(page)
                syncPageSocketsOnMain()
                // 空响应不锁死 hasMore（可能只是这次网络抖动），下次滚动会自然重试
                if (posts.isEmpty()) return@launch
                hasMore = morePagesToLoad()
                val ratingsReady = preloadRatings(page)
                // 必须用 setPage(page, ...) 而不是 addPage()：
                // addPage 是「写进 maxPage+1」，而 reserveUpTo 之后 maxPage 已经等于总页数，
                // 用 addPage 会把这一页的内容写到错误的页码上。
                pageAdapter.setPage(page, posts)
                loadedMaxPage = page
                if (ratingsReady) ratingFetchedPages.add(page)
                pageAdapter.getPage(page)?.let { loadPowers(it) }
            } finally {
                isLoading = false
            }
        }
    }

    private fun gotoPage(page: Int) {
        if (page < 1) { toast("已经是第一页"); return }
        if (page == currentPage) return
        if (pageAdapter.hasPage(page)) {
            // 相邻页用平滑滚动，跳远页直接切（否则会掠过空白页造成闪烁）
            val smooth = kotlin.math.abs(page - currentPage) == 1
            binding.vpPages.setCurrentItem(page - 1, smooth)
            return
        }
        // 边界按「帖子总页数」判断，不能用 hasMore：hasMore 会在跳到最后一页
        // （楼层不满 40）时被置 false，导致之后往回跳也被当成"最后一页"而拒绝
        if (totalPages > 0 && page > totalPages) { toast("没有第 $page 页"); return }
        if (isLoading) { toast("正在加载，请稍后"); return }

        // 只请求目标页本身，不再把中间的页逐页拉一遍
        isLoading = true
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val posts = fetchOnePage(page)
                syncPageSocketsOnMain()
                if (posts.isEmpty()) { toast("加载失败", S1Notify.Type.ERROR); return@launch }
                hasMore = morePagesToLoad()
                val ratingsReady = preloadRatings(page)
                pageAdapter.setPage(page, posts)
                if (page > loadedMaxPage) loadedMaxPage = page
                if (ratingsReady) ratingFetchedPages.add(page)
                pageAdapter.getPage(page)?.let { loadPowers(it) }
                // 关键：跳远页不要用平滑滚动，否则会高速掠过中间没加载的空白页（视觉上就是连闪几下）
                binding.vpPages.setCurrentItem(page - 1, false)
            } finally {
                binding.progressBar.visibility = View.GONE
                isLoading = false
            }
        }
    }

    // ============ 编辑自己的帖子 ============
    private var needReloadAfterEdit = false

    private fun openEditor(post: Post) {
        val pid = post.pid ?: return
        needReloadAfterEdit = true
        startActivity(Intent(this, PostEditorActivity::class.java).apply {
            putExtra("fid", threadFid)
            putExtra("editTid", tid)
            putExtra("editPid", pid)
        })
    }

    // ============ 细节 22：音量键翻页（阅读模式，前台接管退出即恢复） ============
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        when (keyCode) {
            android.view.KeyEvent.KEYCODE_VOLUME_UP -> {
                if (binding.vpPages.currentItem > 0) {
                    binding.vpPages.currentItem -= 1
                    Haptic.selection(this)
                }
                return true
            }
            android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val max = (binding.vpPages.adapter?.itemCount ?: 1) - 1
                if (binding.vpPages.currentItem < max) {
                    binding.vpPages.currentItem += 1
                    Haptic.selection(this)
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        // A 类修复：View 取色一次冻结 → onResume 重取（切主题回页即生效）
        if (::progressLineView.isInitialized) {
            val tv = android.util.TypedValue()
            val has = theme.resolveAttribute(R.attr.s1Accent, tv, true)
            val a = if (has) tv.data else 0xFFFF6FB5.toInt()
            progressLineView.background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    a,
                    ColorUtils.blendARGB(a, android.graphics.Color.WHITE, 0.35f)
                )
            )
        }
        binding.progressBar.refreshThemeColors()
        super.onResume()
        // 从编辑页回来要刷新，才能看到改后的内容
        if (needReloadAfterEdit) {
            needReloadAfterEdit = false
            resetAndReload()
        }
    }

    // ============ 回帖 ============
    private fun showReplySheet() {
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
            DraftStore.save(this@ThreadDetailActivity, draftKey, nv.text)
        }
        // 面板关了就断掉回调，避免上传回来时面板已销毁
        dialog.setOnDismissListener { onReplyImageUploaded = null }

        fun doSend() {
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
                        DraftStore.save(this@ThreadDetailActivity, draftKey, it.text)
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
    private fun jumpToNewReply(newPid: String) {
        if (newPid.isBlank() || newPid == "0") return
        if (jumpLoading) return
        jumpLoading = true
        lifecycleScope.launch {
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

    private fun sendReplyWithQuote(
        msg: String,
        quote: Post?,
        attachFields: Map<String, String> = emptyMap(),
        onDone: (String) -> Unit
    ) {
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
                        message = com.example.s1client.data.store.SettingStore.appendPostTail(this@ThreadDetailActivity, msg),
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
                    DraftStore.clear(this@ThreadDetailActivity, DraftStore.keyForReply(tid))
                    toast("回复成功", S1Notify.Type.SUCCESS)
                    onDone(pid)
                } else {
                    toast("回复失败：" + msgStr.ifBlank { msgVal.ifBlank { "pid=$pid" } })
                }
            } catch (e: Exception) {
                android.util.Log.e("S1Reply", "reply exception", e)
                toast("回复失败：${e.message}")
            }
        }
    }

    // ============ 收藏 ============
    private fun toggleFavorite() {
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

    private fun performFavoriteToggle() {
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
                Haptic.medium(this@ThreadDetailActivity)
                S1Notify.show(this@ThreadDetailActivity, "已取消收藏", S1Notify.Type.INFO, silent = true)
            }
        }
    }

    // ============ 楼层收藏（本地快照：保存作者、时间、标题、原文，原帖删除仍可看）============
    private fun togglePostBookmark(post: Post) {
        val pid = post.pid ?: return
        if (PostBookmarkManager.isBookmarked(pid)) {
            AlertDialog.Builder(this)
                .setTitle("取消收藏楼层")
                .setMessage("确定将这条楼层移出本地收藏吗？")
                .setPositiveButton("移除") { _, _ ->
                    lifecycleScope.launch {
                        PostBookmarkManager.remove(this@ThreadDetailActivity, pid)
                        pageAdapter.notifyBookmarksChanged()
                        S1Notify.show(this@ThreadDetailActivity, "已取消楼层收藏", S1Notify.Type.INFO)
                    }
                }
                .setNegativeButton("保留", null)
                .show()
        } else {
            lifecycleScope.launch {
                PostBookmarkManager.add(this@ThreadDetailActivity, post, tid, subject,
                    TimeUtil.formatSmart(post.dateline))
                pageAdapter.notifyBookmarksChanged()
                Haptic.light(this@ThreadDetailActivity)
                S1Notify.show(this@ThreadDetailActivity, "已收藏楼层", S1Notify.Type.SUCCESS)
            }
        }
    }

    // ===== 列表到底提示 =====
    private val endDetectorAttached = java.util.WeakHashMap<RecyclerView, Boolean>()

    /** 末页滚到底部（或一屏装得下）时显示「已经到底」，否则隐藏；滚动与初始布局复用 */
    private fun checkEndFooter(r: RecyclerView) {
        if (totalPages <= 0 || currentPage < totalPages) { hideEndFooter(); return }
        val lm = r.layoutManager as? LinearLayoutManager ?: return
        if (lm.findLastVisibleItemPosition() >= (r.adapter?.itemCount ?: 0) - 1) {
            showEndFooter()
        }
    }

    /** 给当前页 RecyclerView 挂滚动监听：末页滚到底 → 浮出「已经到底」 */
    private fun attachEndDetector(rv: RecyclerView?) {
        rv ?: return
        if (endDetectorAttached[rv] == true) return
        endDetectorAttached[rv] = true
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(r: RecyclerView, dx: Int, dy: Int) = checkEndFooter(r)
        })
        // 末页内容一屏装得下、无需滚动时也能显示
        rv.post { checkEndFooter(rv) }
    }

    private fun showEndFooter() {
        binding.tvEndFooter.visibility = View.VISIBLE
        binding.tvEndFooter.alpha = 0f
        binding.tvEndFooter.animate().alpha(1f).setDuration(200).start()
    }

    private fun hideEndFooter() {
        if (binding.tvEndFooter.visibility == View.VISIBLE) {
            binding.tvEndFooter.animate().alpha(0f).setDuration(200)
                .withEndAction { binding.tvEndFooter.visibility = View.GONE }.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
