package com.example.s1client.ui

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.s1client.util.Haptic
import com.example.s1client.util.S1Notify
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.s1client.FriendListActivity
import com.example.s1client.PmDetailActivity
import com.example.s1client.UserReplyListActivity
import com.example.s1client.UserThreadListActivity
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.model.ForumDataCache
import com.example.s1client.databinding.DialogUserCardBinding
import com.example.s1client.util.BlockedAuthorManager
import com.example.s1client.util.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 全局统一的用户卡片：点楼层头像、点首页卡片头像都走这一个。
 * 居中弹窗（非 BottomSheet），无顶部拖拽线，圆角玻璃卡片。
 */
fun showUserCardDialog(
    activity: ComponentActivity,
    uid: String,
    username: String,
    /** 塞抹布后的回调：宿主页面用它弹出「撤销」条（不传则只弹系统通知） */
    onBlocked: ((String, String) -> Unit)? = null
) {
    if (uid.isBlank()) return

    // 关键：Dialog 不会继承 Activity 的主题，?attr/s1* 会解析到默认浅色主题，
    // 深色主题下就会弹出一块白卡。这里包一层带当前主题的 Context。
    val themedCtx = android.view.ContextThemeWrapper(activity, com.example.s1client.util.ThemeManager.getThemeRes())
    val cardBinding = DialogUserCardBinding.inflate(android.view.LayoutInflater.from(themedCtx))
    val dialog = Dialog(themedCtx)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setContentView(cardBinding.root)
    dialog.window?.apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setDimAmount(0.4f)
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    dialog.setCancelable(true)
    dialog.setCanceledOnTouchOutside(true)

    cardBinding.tvCardUsername.text = username
    cardBinding.tvCardAvatarLetter.text = username.firstOrNull()?.toString() ?: "?"

    val uidInt = uid.toIntOrNull() ?: 0
    if (uidInt > 0) {
        val padded = String.format("%09d", uidInt)
        val path = "${padded.substring(0, 3)}/${padded.substring(3, 5)}/" +
                "${padded.substring(5, 7)}/${padded.substring(7, 9)}"
        val url = "https://avatar.stage1st.com/${path}_avatar_middle.jpg"
        cardBinding.ivCardAvatar.alpha = 0f
        cardBinding.ivCardAvatar.load(url) {
            listener(
                onSuccess = { _, _ -> cardBinding.ivCardAvatar.alpha = 1f },
                onError = { _, _ -> cardBinding.ivCardAvatar.alpha = 0f }
            )
        }
    }

    // ===== 三个统计区点击跳转（不关卡片，返回时卡片还在） =====
    cardBinding.btnCardFriends.setOnClickListener {
        activity.startActivity(Intent(activity, FriendListActivity::class.java).apply {
            putExtra("uid", uid)
            putExtra("username", username)
        })
    }
    cardBinding.btnCardThreads.setOnClickListener {
        activity.startActivity(Intent(activity, UserThreadListActivity::class.java).apply {
            putExtra("uid", uid)
            putExtra("username", username)
        })
    }
    cardBinding.btnCardPosts.setOnClickListener {
        activity.startActivity(Intent(activity, UserReplyListActivity::class.java).apply {
            putExtra("uid", uid)
            putExtra("username", username)
        })
    }

    cardBinding.tvCardClose.setOnClickListener { dialog.dismiss() }

    // ===== 操作栏：私信 / 加好友 / 塞抹布（看别人时显示，看自己时隐藏） =====
    val isSelf = uid == ForumDataCache.memberUid
    if (!isSelf) {
        cardBinding.llCardActions.visibility = View.VISIBLE
        cardBinding.btnCardPm.setOnClickListener {
            if (!ForumDataCache.isLoggedIn()) {
                S1Notify.show(activity, "请先登录")
            } else {
                activity.startActivity(Intent(activity, PmDetailActivity::class.java).apply {
                    putExtra("touid", uid)
                    putExtra("username", username)
                })
            }
        }
        cardBinding.btnCardFriend.setOnClickListener {
            val fh = ForumDataCache.formhash
            if (fh.isBlank()) {
                S1Notify.show(activity, "登录已过期，请重新登录", S1Notify.Type.ERROR)
                return@setOnClickListener
            }
            cardBinding.btnCardFriend.isEnabled = false
            activity.lifecycleScope.launch {
                try {
                    val body = withContext(Dispatchers.IO) {
                        RetrofitClient.api.addFriend(uid = uid, formhash = fh).string()
                    }
                    val msg = parseFriendResult(body)
                    S1Notify.show(
                        activity, msg,
                        if (msg.contains("请先登录")) S1Notify.Type.ERROR else S1Notify.Type.SUCCESS
                    )
                } catch (e: Exception) {
                    S1Notify.show(activity, "发送失败：${e.message}", S1Notify.Type.ERROR)
                } finally {
                    cardBinding.btnCardFriend.isEnabled = true
                }
            }
        }
        cardBinding.btnCardBlock.setOnClickListener {
            // 不再二次确认：直接执行，由宿主页面的「撤销」条兜底误操作
            Haptic.heavy(activity)
            BlockedAuthorManager.block(uid, username)
            if (onBlocked != null) {
                onBlocked(uid, username)
                dialog.dismiss()
            } else {
                S1Notify.show(activity, "已对「$username」塞抹布", S1Notify.Type.SUCCESS, silent = true)
            }
        }
    }

    // 协程跟随 Dialog 而不是 Activity：用户快速关闭弹窗后网络回来时，
    // 不会向已销毁的视图写数据（幽灵更新）/ 不会向已 finish 的 Activity 弹窗。
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    dialog.setOnDismissListener { scope.cancel() }

    scope.launch {
        try {
            val resp = withContext(Dispatchers.IO) {
                RetrofitClient.api.getProfile(uid = uid)
            }
            val p = withContext(Dispatchers.IO) {
                JsonParser.parseProfile(resp.variables?.space, resp.variables?.extCredits, uid)
            }
            // 弹窗已关闭 → 直接放弃这次更新，避免写已废弃的视图
            if (!dialog.isShowing) return@launch
            cardBinding.tvCardGroup.text = p.groupName.ifBlank { "--" }
            cardBinding.tvCardFriends.text = p.friends
            cardBinding.tvCardThreads.text = p.threads
            cardBinding.tvCardPosts.text = p.posts
            // 签名：S1 对无签名者返回 "0"/"null"/"-"，过滤后隐藏
            val sig = p.customStatus.trim()
            if (sig.isEmpty() || sig == "0" || sig.equals("null", true) || sig == "-") {
                cardBinding.tvCardSignature.visibility = View.GONE
            } else {
                cardBinding.tvCardSignature.text = sig
                cardBinding.tvCardSignature.visibility = View.VISIBLE
            }
            cardBinding.tvCardCredits.text = p.credits
            cardBinding.tvCardRegDate.text = p.regDate
            // 原 ProfileActivity 搬入字段
            cardBinding.tvCardOltime.text = if (p.oltime.isBlank()) "--" else "${p.oltime} 小时"
            cardBinding.tvCardLastVisit.text = p.lastVisit.ifBlank { "--" }
            cardBinding.tvCardLastPost.text = p.lastPost.ifBlank { "--" }
            val power = p.creditItems.firstOrNull { it.title.contains("战斗力") }
            cardBinding.tvCardPower.text = power?.let {
                if (it.unit.isBlank()) it.value else "${it.value} ${it.unit}"
            } ?: "--"

            // 积分明细：除战斗力外的全部条目动态填充（原 ProfileActivity 积分卡搬入）
            val detailItems = p.creditItems.filter { !it.title.contains("战斗力") }
            if (detailItems.isNotEmpty()) {
                cardBinding.llCardCreditItems.visibility = View.VISIBLE
                cardBinding.llCardCreditItems.removeAllViews()
                val density = activity.resources.displayMetrics.density
                detailItems.forEach { item ->
                    val row = LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, (4 * density).toInt(), 0, (4 * density).toInt())
                    }
                    row.addView(TextView(activity).apply {
                        text = item.title
                        textSize = 13f
                        setTextColor(activity.attrColor(android.R.attr.textColorSecondary))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    row.addView(TextView(activity).apply {
                        text = if (item.unit.isBlank()) item.value else "${item.value} ${item.unit}"
                        textSize = 13f
                        setTextColor(activity.attrColor(android.R.attr.textColorPrimary))
                    })
                    cardBinding.llCardCreditItems.addView(row)
                }
            }
        } catch (e: Exception) {
            S1Notify.show(activity, "资料加载失败：${e.message}", S1Notify.Type.ERROR)
        }
    }

    dialog.show()
}

/** 主题属性取色（跟随当前主题的积分明细文字色） */
private fun ComponentActivity.attrColor(attr: Int): Int {
    val tv = android.util.TypedValue()
    return if (theme.resolveAttribute(attr, tv, true)) tv.data else 0
}

/** 解析 Discuz inajax 加好友返回文本 */
private fun parseFriendResult(body: String): String = when {
    body.contains("已经是好友", true) -> "你们已经是好友了"
    body.contains("succeedhandle", true) -> "好友申请已发送，等待对方确认"
    body.contains("好友请求", true) || body.contains("好友申请", true) ->
        "好友申请已发送，等待对方确认"
    body.contains("login", true) || body.contains("请先登录") -> "请先登录后再操作"
    else -> "好友申请已发送"
}
