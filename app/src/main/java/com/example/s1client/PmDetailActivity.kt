package com.example.s1client

import android.graphics.drawable.GradientDrawable
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
import kotlinx.coroutines.launch
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
                    adapter.submit(list)
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
        lifecycleScope.launch {
            val fh = ForumDataCache.formhash
            if (fh.isBlank()) {
                toast("请先刷新主页获取登录令牌")
                binding.btnSend.isEnabled = true
                return@launch
            }
            when (val r = safeApiCall {
                RetrofitClient.api.sendPm(touid = touid, formhash = fh, message = msg)
            }) {
                is ApiResult.Success -> {
                    binding.etReply.setText("")
                    toast("已发送")
                    loadConversation()
                }
                is ApiResult.Failure -> toast(r.error.userMessage)
            }
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

    inner class VH(val binding: ItemPmMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemPmMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        val b = holder.binding
        b.tvMessage.text = stripBbcode(m.message)
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
        return s.replace(Regex("""<br\s*/?>"""), "\n")
            .replace(Regex("""<[^>]+>"""), "")
            .replace(Regex("""\[/?[a-zA-Z0-9=]+]"""), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }
}
