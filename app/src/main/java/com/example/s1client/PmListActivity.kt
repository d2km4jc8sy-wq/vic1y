package com.example.s1client

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.s1client.data.api.ApiResult
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.api.safeApiCall
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.data.model.PmListItem
import com.example.s1client.databinding.ActivityPmListBinding
import com.example.s1client.databinding.ItemPmListBinding
import com.example.s1client.ui.showUserCardDialog
import com.example.s1client.util.avatarUrl
import com.example.s1client.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.s1client.util.PmParser

class PmListActivity : BaseActivity() {

    private lateinit var binding: ActivityPmListBinding
    /** 从私信列表页 HTML 里提取的 formhash，删除会话用 */
    private var pmFormhash: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPmListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdge()
        applyTopInset(binding.topBar)

        binding.btnBack.setOnClickListener { finish() }
        binding.rvPm.layoutManager = LinearLayoutManager(this)
        loadPmList()
    }

    override fun onResumeInternal() {
        if (::binding.isInitialized) loadPmList()
    }

    private fun loadPmList() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val url = "https://stage1st.com/2b/home.php?mod=space&do=pm"
            when (val r = safeApiCall { RetrofitClient.fetchHtml(url) }) {
                is ApiResult.Success -> {
                    pmFormhash = Regex("""formhash[=']+([a-f0-9]{6,})""")
                        .find(r.data)?.groupValues?.get(1)
                        ?: ForumDataCache.formhash
                    handleList(r.data)
                }
                is ApiResult.Failure -> {
                    toast(r.error.userMessage)
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            }
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun handleList(html: String) {
        val list = PmParser.parsePmList(html)
        if (list.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvPm.adapter = null
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvPm.adapter = PmAdapter(
                items = list,
                onClick = { item ->
                    startActivity(Intent(this, PmDetailActivity::class.java).apply {
                        putExtra("touid", item.touid)
                        putExtra("username", item.username)
                    })
                },
                onAvatarClick = { item ->
                    showUserCardDialog(this, item.touid, item.username)
                },
                onLongClick = { item -> confirmDelete(item) }
            )
        }
    }

    /** 长按会话 → 确认 → 调 Discuz 删除接口 → 刷新 */
    private fun confirmDelete(item: PmListItem) {
        AlertDialog.Builder(this)
            .setTitle("删除私信")
            .setMessage("确定删除与「${item.username}」的私信会话吗？删除后不可恢复。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ -> deleteConversation(item) }
            .show()
    }

    private fun deleteConversation(item: PmListItem) {
        val fh = pmFormhash.ifBlank { ForumDataCache.formhash }
        if (fh.isBlank()) {
            toast("登录已过期，请重新登录")
            return
        }
        lifecycleScope.launch {
            val r = safeApiCall {
                val body = RetrofitClient.api.deletePmConversation(
                    plid = item.plid,
                    touid = item.touid,
                    formhash = fh
                ).string()
                withContext(Dispatchers.Default) {
                    body.contains("succeedhandle", true) ||
                        body.contains("操作成功", true) ||
                        (!body.contains("error", true) && !body.contains("失败", true))
                }
            }
            when (r) {
                is ApiResult.Success -> {
                    if (r.data) {
                        toast("已删除")
                        loadPmList()
                    } else {
                        toast("删除失败，请重试")
                    }
                }
                is ApiResult.Failure -> toast("删除失败：${r.error.userMessage}")
            }
        }
    }
}

class PmAdapter(
    private val items: List<PmListItem>,
    private val onClick: (PmListItem) -> Unit,
    private val onAvatarClick: (PmListItem) -> Unit,
    private val onLongClick: (PmListItem) -> Unit
) : RecyclerView.Adapter<PmAdapter.VH>() {

    inner class VH(val binding: ItemPmListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemPmListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvUsername.text = item.username
        holder.binding.tvTime.text = formatTime(item.lastTime)
        holder.binding.tvLastMessage.text = cleanMessage(item.lastMessage)

        // 头像
        val letter = item.username.firstOrNull()?.toString()?.uppercase() ?: "?"
        holder.binding.tvAvatarLetter.text = letter
        holder.binding.tvAvatarLetter.background = makeCircle(item.username.hashCode())
        holder.binding.ivAvatar.alpha = 0f
        avatarUrl(item.touid, "middle")?.let { url ->
            holder.binding.ivAvatar.load(url) {
                listener(
                    onSuccess = { _, _ -> holder.binding.ivAvatar.alpha = 1f },
                    onError = { _, _ -> holder.binding.ivAvatar.alpha = 0f }
                )
            }
        }

        // 未读小红点（页面 cur1/new 标记）
        holder.binding.tvUnreadBadge.visibility = if (item.isNew) View.VISIBLE else View.GONE

        holder.binding.root.setOnClickListener { onClick(item) }
        holder.binding.root.setOnLongClickListener { onLongClick(item); true }
        holder.binding.avatarWrap.setOnClickListener { onAvatarClick(item) }
    }

    override fun getItemCount() = items.size

    /** 去掉 Discuz 尾巴：时间、"删除 忽略 共X条 菜单|回复" */
    private fun cleanMessage(s: String): String {
        var r = s.replace(Regex("""<[^>]+>"""), "")
        r = r.replace(Regex("""\s*\d{4}-\d{1,2}-\d{1,2}\s+\d{1,2}:\d{2}.*$"""), "")
        r = r.replace(Regex("""\s*(删除|忽略|共\s*\d+\s*条|菜单\|?回复|菜单|回复)\s*"""), " ")
        r = r.replace(Regex("""\s+"""), " ").trim()
        return r.ifEmpty { "（无内容）" }
    }

    /** 今天只留 HH:mm，昨天/更早显示 MM-dd */
    private fun formatTime(t: String): String {
        if (t.isBlank()) return ""
        val m = Regex("""(\d{4})-(\d{1,2})-(\d{1,2})\s+(\d{1,2}):(\d{2})""").find(t)
        if (m != null) {
            val now = java.util.Calendar.getInstance()
            val msg = java.util.Calendar.getInstance().apply {
                set(m.groupValues[1].toInt(), m.groupValues[2].toInt() - 1, m.groupValues[3].toInt())
            }
            val sameDay = now.get(java.util.Calendar.YEAR) == msg.get(java.util.Calendar.YEAR) &&
                now.get(java.util.Calendar.DAY_OF_YEAR) == msg.get(java.util.Calendar.DAY_OF_YEAR)
            return if (sameDay) {
                "%02d:%02d".format(m.groupValues[4].toInt(), m.groupValues[5].toInt())
            } else {
                "%02d-%02d".format(m.groupValues[2].toInt(), m.groupValues[3].toInt())
            }
        }
        return t
    }

    private fun makeCircle(seed: Int): android.graphics.drawable.GradientDrawable {
        val palettes = listOf(
            intArrayOf(0xFFE94A72.toInt(), 0xFFF48FB1.toInt()),
            intArrayOf(0xFFC8788E.toInt(), 0xFFE8A0B0.toInt()),
            intArrayOf(0xFF8B7355.toInt(), 0xFFC0A888.toInt()),
            intArrayOf(0xFF8090A0.toInt(), 0xFFA0B0C0.toInt()),
            intArrayOf(0xFF80A080.toInt(), 0xFFA0C0A0.toInt())
        )
        val p = palettes[Math.floorMod(seed, palettes.size)]
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL; colors = p
        }
    }
}
