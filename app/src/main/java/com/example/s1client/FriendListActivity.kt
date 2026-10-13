package com.example.s1client

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.s1client.util.S1Notify
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.data.model.FriendItem
import com.example.s1client.databinding.ActivityFriendListBinding
import com.example.s1client.databinding.ItemFriendBinding
import com.example.s1client.ui.showUserCardDialog
import kotlinx.coroutines.launch
import com.example.s1client.util.FriendParser

class FriendListActivity : BaseActivity() {

    private lateinit var binding: ActivityFriendListBinding
    private var uid: String = ""
    private var username: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFriendListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdge()
        applyTopInset(binding.topBar)

        uid = intent.getStringExtra("uid") ?: ""
        username = intent.getStringExtra("username") ?: ""
        binding.tvTitle.text = if (username.isBlank()) "好友" else "$username 的好友"
        binding.btnBack.setOnClickListener { finish() }
        binding.rvFriends.layoutManager = LinearLayoutManager(this)

        if (uid.isEmpty()) { finish(); return }
        loadFriends()
    }

    private fun loadFriends() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val url = "https://stage1st.com/2b/home.php?mod=space&uid=$uid&do=friend&from=space"
                Log.d("S1HTML", "=== FriendListActivity ===")
                Log.d("S1HTML", "URL: $url")
                val html = RetrofitClient.fetchHtml(url)
                Log.d("S1HTML", "HTML 长度: ${html.length}")

                val isRedirected = html.contains("的个人资料") && !html.contains("的好友")
                if (isRedirected) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = "该用户没有公开好友列表"
                    binding.progressBar.visibility = View.GONE
                    return@launch
                }

                val rawFriends = FriendParser.parseFriendList(html)

                // 过滤掉：当前被查看用户自己 + 当前登录用户自己
                val viewerUid = ForumDataCache.memberUid  // 当前登录用户
                val ownerUid = uid                        // 被查看的好友列表所有者

                val friends = rawFriends.filter { item ->
                    item.uid != viewerUid && item.uid != ownerUid
                }

                Log.d("S1HTML", "原始好友: ${rawFriends.size}, 过滤后: ${friends.size}")
                Log.d("S1HTML", "过滤条件: viewerUid=$viewerUid, ownerUid=$ownerUid")

                if (friends.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = "暂无好友"
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.rvFriends.adapter = FriendAdapter(friends) { item ->
                        showUserCardDialog(this@FriendListActivity, item.uid, item.username)
                    }
                }
            } catch (e: Exception) {
                Log.e("S1HTML", "好友加载异常", e)
                toast("加载失败：${e.message}", S1Notify.Type.ERROR)
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "加载失败：${e.message}"
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}

class FriendAdapter(
    private val items: List<FriendItem>,
    private val onClick: (FriendItem) -> Unit
) : RecyclerView.Adapter<FriendAdapter.VH>() {

    inner class VH(val binding: ItemFriendBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFriendBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvUsername.text = item.username
        holder.binding.tvLastVisit.text =
            if (item.note.isNotBlank()) item.note else "UID: ${item.uid}"
        val letter = item.username.firstOrNull()?.toString() ?: "?"
        holder.binding.tvAvatarLetter.text = letter

        val uidInt = item.uid.toIntOrNull() ?: 0
        if (uidInt > 0) {
            val padded = String.format("%09d", uidInt)
            val path = "${padded.substring(0,3)}/${padded.substring(3,5)}/" +
                       "${padded.substring(5,7)}/${padded.substring(7,9)}"
            val url = "https://avatar.stage1st.com/${path}_avatar_middle.jpg"
            holder.binding.ivAvatar.alpha = 0f
            holder.binding.ivAvatar.load(url) {
                listener(
                    onSuccess = { _, _ -> holder.binding.ivAvatar.alpha = 1f },
                    onError = { _, _ -> holder.binding.ivAvatar.alpha = 0f }
                )
            }
        }
        holder.binding.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}