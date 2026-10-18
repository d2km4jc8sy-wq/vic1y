package com.example.s1client

import android.graphics.drawable.GradientDrawable
import android.content.Context
import android.content.Intent
import com.example.s1client.util.PmRichText
import com.example.s1client.util.S1Notify
import com.example.s1client.ui.showUserCardDialog
import androidx.activity.ComponentActivity
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.s1client.data.api.ApiResult
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.api.safeApiCall
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.data.model.PmMessage
import com.example.s1client.databinding.ActivityPmDetailBinding
import com.example.s1client.databinding.ItemPmMessageBinding
import com.example.s1client.ui.showUserCardDialog
import com.example.s1client.util.avatarUrl
import com.example.s1client.util.toast
import com.example.s1client.util.unescapeHtmlEntities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.s1client.util.PmParser

class PmDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityPmDetailBinding
    private lateinit var adapter: PmMessageAdapter
    private var touid: String = ""
    private var username: String = "私信"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPmDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // edge-to-edge：顶栏垫状态栏、输入栏垫手势条/键盘，保证导航栏透明不露出主题色条
        setupEdgeToEdge()
        applyTopInset(binding.topBar)
        applyImeBottomInset(binding.bottomBar)

        touid = intent.getStringExtra("touid") ?: ""
        username = intent.getStringExtra("username") ?: "私信"
        binding.tvTitle.text = username
        binding.btnBack.setOnClickListener { finish() }
        if (touid.isEmpty()) { finish(); return }

        // 顶栏头像：点击看对方资料
        binding.ivTopAvatar.setOnClickListener {
            showUserCardDialog(this, touid, username)
        }
        binding.tvTopAvatarLetter.setOnClickListener {
            showUserCardDialog(this, touid, username)
        }

        // 顶栏头像
        val letter = username.firstOrNull()?.toString()?.uppercase() ?: "?"
        binding.tvTopAvatarLetter.text = letter
        binding.tvTopAvatarLetter.background = makeCircle(username.hashCode())
        binding.ivTopAvatar.alpha = 0f
        avatarUrl(touid, "middle")?.let { url ->
            binding.ivTopAvatar.load(url) {
                listener(
                    onSuccess = { _, _ -> binding.ivTopAvatar.alpha = 1f },
                    onError = { _, _ -> binding.ivTopAvatar.alpha = 0f }
                )
            }
        }

        adapter = PmMessageAdapter(
            touid = touid,
            touName = username,
            onAvatarClick = { isSelf ->
                if (isSelf) {
                    showUserCardDialog(this, ForumDataCache.memberUid, ForumDataCache.memberUsername)
                } else {
                    showUserCardDialog(this, touid, username)
                }
            }
        )
        val lm = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvMessages.layoutManager = lm
        binding.rvMessages.adapter = adapter

        binding.btnSend.setOnClickListener { sendReply() }
        loadConversation()
    }

    private fun loadConversation() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val url = "https://stage1st.com/2b/home.php?mod=space&do=pm&subop=view&touid=$touid"
            when (val r = safeApiCall { RetrofitClient.fetchHtml(url) }) {
                is ApiResult.Success -> {
                    val list = PmParser.parsePmConversation(r.data)
                    // 提案簿 10：乐观更新对账 —— 服务端未收录乐观消息时保留现状不覆盖
                    adapter.submitRespectingOptimistic(list)
                    if (list.isNotEmpty()) {
                        binding.rvMessages.scrollToPosition(list.size - 1)
                    }
                }
                is ApiResult.Failure -> toast(r.error.userMessage)
            }
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun sendReply() {
        val msg = binding.etReply.text.toString().trim()
        if (msg.isEmpty()) { toast("内容不能为空"); return }
        binding.btnSend.isEnabled = false

        // R10 修复（审计报告 v2 2026-10-18 第 9 轮）：
        // 原来这里直读 ForumDataCache.formhash，缓存空/过期即放弃并弹死路提示
        // 「请先刷新主页获取登录令牌」——既无兜底、也把责任推给用户（设计者实测差点连发）。
        // 同项目已有 4 处现成兜底实现：AttendanceManager.fetchFormhash /
        // FavoriteManager.fetchFormhash / SearchManager.fetchFormhash / 回帖 inline
        // （ThreadDetailReplyActions:391-394）—— 私信是第 5 个写路径却一个都没用上。
        // 此处补上，口径对齐 AttendanceManager：forumindex 现拉 → 失败回退缓存。
        // 取令牌期间显示 progressBar 作「请求中可见反馈」，不再让用户盲等。
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val fh = withContext(Dispatchers.IO) {
                try { RetrofitClient.api.getForumIndex().variables?.formhash } catch (_: Exception) { null }
            } ?: ForumDataCache.formhash
            binding.progressBar.visibility = View.GONE
            if (fh.isBlank()) {
                toast("登录令牌获取失败，请稍候再试")
                binding.btnSend.isEnabled = true
                return@launch
            }
            when (val r = safeApiCall {
                RetrofitClient.api.sendPm(touid = touid, formhash = fh, message = msg)
            }) {
                is ApiResult.Success -> {
                    binding.etReply.setText("")
                    toast("已发送")
                    // 提案簿 10（设计者点单「私信发送刷新」）：乐观更新 —— 服务端 PM
                    // 详情页对刚发的消息有可见性时序延迟（同 URL 同解析器，发完立刻拉
                    // 不含新消息、退回重进才有，设计者 13T 实测），故先本地插入这条
                    // 零等待显示；随后 loadConversation() 对账，服务端收录后覆盖为
                    // 带真实时间戳的版本（submitRespectingOptimistic 防中途抹掉）。
                    adapter.appendOptimistic(
                        PmMessage(pmid = "", author = "您", message = msg, dateline = "刚刚", isSelf = true)
                    )
                    binding.rvMessages.scrollToPosition(adapter.itemCount - 1)
                    loadConversation()
                }
                is ApiResult.Failure -> toast(r.error.userMessage)
            }
            binding.progressBar.visibility = View.GONE
            binding.btnSend.isEnabled = true
        }
    }

    companion object {
        fun makeCircle(seed: Int): GradientDrawable {
            val palettes = listOf(
                intArrayOf(0xFFE94A72.toInt(), 0xFFF48FB1.toInt()),
                intArrayOf(0xFFC8788E.toInt(), 0xFFE8A0B0.toInt()),
                intArrayOf(0xFF8B7355.toInt(), 0xFFC0A888.toInt()),
                intArrayOf(0xFF8090A0.toInt(), 0xFFA0B0C0.toInt()),
                intArrayOf(0xFF80A080.toInt(), 0xFFA0C0A0.toInt())
            )
            val p = palettes[Math.floorMod(seed, palettes.size)]
            return GradientDrawable().apply {
                shape = GradientDrawable.OVAL; colors = p
            }
        }
    }
}

class PmMessageAdapter(
    private val touid: String,
    private val touName: String,
    private val onAvatarClick: (isSelf: Boolean) -> Unit
) : RecyclerView.Adapter<PmMessageAdapter.VH>() {

    private val items = mutableListOf<PmMessage>()

    fun submit(list: List<PmMessage>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    /**
     * 乐观更新（提案簿 10 · 2026-10-18 设计者点单）：发送成功后立刻在本地追加刚发的
     * 消息，让用户零等待。根因：服务端 PM 详情页（subop=view）对刚发送的消息有可见性
     * 时序延迟 —— 同一 URL 同一解析器，发送后立刻拉取不含新消息，退回重进（几秒后）
     * 就能看到（设计者 13T 实测）。与 [submitRespectingOptimistic] 配对使用。
     */
    fun appendOptimistic(m: PmMessage) {
        items.add(m)
        notifyDataSetChanged()
    }

    /**
     * 服务端对账刷新：会话消息单调递增（Discuz PM 无删除入口），
     * 若服务端返回条数少于当前显示条数（乐观那条尚未被服务端收录），
     * 保留当前列表不覆盖，防止乐观消息"闪现又消失"；服务端收录后自然覆盖。
     * 首次载入（items 为空）时恒覆盖，行为与原 submit 一致。
     */
    fun submitRespectingOptimistic(list: List<PmMessage>) {
        if (items.isNotEmpty() && list.size < items.size) return
        submit(list)
    }

    inner class VH(val binding: ItemPmMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemPmMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        val b = holder.binding
        // 私信正文是 HTML 原文，表情/@/链接本来都在（PmParser 取的 msgEl.html()），
        // 这里做富文本渲染而不是剥成纯文本
        b.tvMessage.text = PmRichText.render(
            m.message,
            onImageClick = { src ->
                val url = if (src.startsWith("http")) src else "https://stage1st.com/2b/$src"
                runCatching {
                    b.root.context.startActivity(
                        Intent(b.root.context, ImageViewerActivity::class.java).apply {
                            putStringArrayListExtra("urls", arrayListOf(url))
                            putExtra("position", 0)
                        }
                    )
                }.onFailure {
                    S1Notify.show(b.root.context, "图片无法打开", S1Notify.Type.INFO)
                }
            },
            onUserClick = { targetUid ->
                (b.root.context as? ComponentActivity)?.let { act ->
                    runCatching { showUserCardDialog(act, targetUid, ForumDataCache.memberUsername) }
                }
            }
        )
        b.tvMessage.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        b.tvAuthor.text = if (m.isSelf) "我" else touName
        b.tvTime.text = m.dateline

        // 头像 URL
        val avatarUid = if (m.isSelf) ForumDataCache.memberUid else touid
        val letterName = if (m.isSelf) ForumDataCache.memberUsername else touName
        val letter = letterName.firstOrNull()?.toString()?.uppercase() ?: "?"

        if (m.isSelf) {
            // 自己的消息：头像在右边，显示自头像；隐藏对方头像
            b.avatarWrap.visibility = View.GONE
            b.avatarWrapSelf.visibility = View.VISIBLE
            b.avatarWrapSelf.setOnClickListener { onAvatarClick(true) }

            b.tvAvatarLetterSelf.text = letter
            b.tvAvatarLetterSelf.background = PmDetailActivity.makeCircle(letterName.hashCode())
            b.ivAvatarSelf.alpha = 0f
            avatarUrl(avatarUid, "middle")?.let { url ->
                b.ivAvatarSelf.load(url) {
                    listener(
                        onSuccess = { _, _ -> b.ivAvatarSelf.alpha = 1f },
                        onError = { _, _ -> b.ivAvatarSelf.alpha = 0f }
                    )
                }
            }

            // 气泡颜色：accent
            b.tvMessage.setBackgroundResource(com.example.s1client.R.drawable.bg_pm_bubble_self)
            b.tvMessage.backgroundTintList = null
            val tv = android.util.TypedValue()
            holder.itemView.context.theme.resolveAttribute(
                com.example.s1client.R.attr.s1TextOnAccent, tv, true)
            b.tvMessage.setTextColor(tv.data)
            // 气泡靠右
            val parent = b.tvMessage.parent as LinearLayout
            parent.gravity = Gravity.END
            b.tvAuthor.gravity = Gravity.END
            b.tvTime.gravity = Gravity.END
        } else {
            b.avatarWrap.visibility = View.VISIBLE
            b.avatarWrapSelf.visibility = View.GONE
            b.avatarWrap.setOnClickListener { onAvatarClick(false) }

            b.tvAvatarLetter.text = letter
            b.tvAvatarLetter.background = PmDetailActivity.makeCircle(touName.hashCode())
            b.ivAvatar.alpha = 0f
            avatarUrl(touid, "middle")?.let { url ->
                b.ivAvatar.load(url) {
                    listener(
                        onSuccess = { _, _ -> b.ivAvatar.alpha = 1f },
                        onError = { _, _ -> b.ivAvatar.alpha = 0f }
                    )
                }
            }

            b.tvMessage.setBackgroundResource(com.example.s1client.R.drawable.bg_pm_bubble_other)
            b.tvMessage.backgroundTintList = null
            val tv = android.util.TypedValue()
            holder.itemView.context.theme.resolveAttribute(
                com.example.s1client.R.attr.s1TextBody, tv, true)
            b.tvMessage.setTextColor(tv.data)
            val parent = b.tvMessage.parent as LinearLayout
            parent.gravity = Gravity.START
            b.tvAuthor.gravity = Gravity.START
            b.tvTime.gravity = Gravity.START
        }
    }

    override fun getItemCount() = items.size

    private fun stripBbcode(s: String): String {
        // R11 收敛（2026-10-18 第 9 轮审计）：原来此处私有一份 4 实体 replace 链，
        // 且 &amp; 前置（`&amp;lt;` 会被双重解码）。公共函数顺序正确且覆盖全，换之。
        return unescapeHtmlEntities(
            s.replace(Regex("""<br\s*/?>"""), "\n")
                .replace(Regex("""<[^>]+>"""), "")
                .replace(Regex("""\[/?[a-zA-Z0-9=]+]"""), "")
        )?.trim() ?: s.trim()
    }
}
