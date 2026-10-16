package com.example.s1client.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.s1client.R
import com.example.s1client.ThreadDetailActivity
import com.example.s1client.data.model.Post
import com.example.s1client.util.Haptic
import com.example.s1client.util.RateFormOptions
import com.example.s1client.util.S1Notify
import kotlinx.coroutines.launch

/**
 * PS1 评分抽屉。
 *
 * 交互语义（重要，与纯视觉稿不同 —— 是与 Stage1st/Discuz 真实评分规则对齐后的结果）：
 * - 分值**离散选值、一次提交**：可选值来自评分表单 [RateFormOptions.buildScoreOptions]
 *   （普通用户组通常只有 −1 / +1，部分用户组只有加分），不是连点连改服务器总分；
 * - 大分数显示的是"本次选择的加扣值"（初始 0 = 未选），不是该楼层的聚合总分；
 * - 连点 + / − 只在本地可选档之间循环（如 +1 → +2 → +1），确定时才发**一次**请求；
 * - RESET 回到未选；点遮罩 / 返回键 = 放弃（提交前不落任何数据，无需回滚）；
 * - 理由默认不填（服务端允许），通知作者沿用表单默认勾选态；
 * - 今日剩余额度为 0 时禁用 + 键。
 *
 * 视觉：金属机身面板 + 深色/暖白显示屏 + 电源 LED 呼吸 + 浮动 +1/−1 +
 * 弹簧入场，震动全部走 [Haptic]。无实时模糊，阴影全用渐变/elevation，保证高刷不掉帧。
 */
class RateDrawer private constructor(
    private val act: ThreadDetailActivity,
    private val post: Post,
    private val form: RateFormOptions,
    private val onSubmit: suspend (
        pid: String,
        amount: Int,
        reason: String,
        notifyAuthor: Boolean,
        form: RateFormOptions
    ) -> Boolean
) {
    private val dialog = Dialog(act)

    private lateinit var root: View
    private lateinit var overlay: View
    private lateinit var floatLayer: FrameLayout
    private lateinit var panel: View
    private lateinit var scoreValue: TextView
    private lateinit var btnPlus: View
    private lateinit var btnMinus: View
    private lateinit var inputValue: EditText
    private lateinit var btnDone: TextView

    /** 本次选择的分值，0 = 未选 */
    private var selected = 0
    private var submitting = false
    private var closing = false

    private val positives: List<Int> = form.buildScoreOptions().filter { it > 0 }.sorted()
    private val negatives: List<Int> = form.buildScoreOptions().filter { it < 0 }.sortedDescending()


    private fun show() {
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_rate_drawer)
        dialog.window?.let { w ->
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            w.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            // 自绘遮罩，不要系统 dim
            w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            w.setDimAmount(0f)
        }
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        root = dialog.findViewById(R.id.rateRoot)
        overlay = dialog.findViewById(R.id.rateOverlay)
        floatLayer = dialog.findViewById(R.id.rateFloatLayer)
        panel = dialog.findViewById(R.id.ratePanel)
        scoreValue = dialog.findViewById(R.id.tvScoreValue)
        btnPlus = dialog.findViewById(R.id.btnPlus)
        btnMinus = dialog.findViewById(R.id.btnMinus)
        inputValue = dialog.findViewById(R.id.btnReset)
        btnDone = dialog.findViewById(R.id.btnDone)

        // 皮肤：面板机身 + 屏幕跟随当前主题明暗
        panel.setBackgroundResource(Ps1Skin.bodyRes())
        dialog.findViewById<View>(R.id.rateScreen)
            .setBackgroundResource(Ps1Skin.screenRes())
        // 细节轮4：XML 里写死的黑字统一改为运行时取色（深色皮肤 → 白字）。
        // 输入框压黑键面 → 白字；确定键压白键面 → 深色皮肤下改黑字。
        val inkOnScreen = if (Ps1Skin.isLight()) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        val inkOnWhiteFace = if (Ps1Skin.isLight()) 0xFF000000.toInt() else 0xFF000000.toInt()
        inputValue.setTextColor(inkOnDarkFace())
        dialog.findViewById<TextView>(R.id.btnDone).setTextColor(inkOnWhiteFace)
        dialog.findViewById<android.widget.CheckBox>(R.id.rateNotifyAuthor).apply {
            setTextColor(inkOnScreen)
            buttonTintList = android.content.res.ColorStateList.valueOf(inkOnScreen)
        }

        // 用户信息
        val name = post.author ?: "未知"
        dialog.findViewById<TextView>(R.id.tvDrawerName).text = name
        val letterView = dialog.findViewById<TextView>(R.id.tvDrawerAvatarLetter)
        letterView.text = name.firstOrNull()?.toString() ?: "?"
        letterView.background = PostAdapter.makeCircleGradient(name.hashCode())
        val avatar = dialog.findViewById<com.google.android.material.imageview.ShapeableImageView>(
            R.id.ivDrawerAvatar
        )
        avatar.alpha = 0f
        post.authorId?.let { PostAdapter.buildAvatarUrl(it) }?.let { url ->
            avatar.load(url) {
                listener(
                    onSuccess = { _, _ -> avatar.alpha = 1f },
                    onError = { _, _ -> avatar.alpha = 0f }
                )
            }
        }

        // 战斗力：USER 占位行改显示当前战斗力（鹅），取不到时显示占位符
        val power = com.example.s1client.data.store.PowerRepository.get(post.authorId)
        dialog.findViewById<TextView>(R.id.tvDrawerPower).text =
            if (power != null) "⚔ $power" else "⚔ 战斗力未知"

        // 今日额度：用尽则置灰 + 键。注意不能用 isEnabled=false —— 那样禁用原因的
        // 点击提示永远收不到；改为可点击但在回调里拦截提示。
        val remain = form.remainingToday
        val plusEnabled = positives.isNotEmpty() && (remain == null || remain > 0)
        val minusEnabled = negatives.isNotEmpty()
        btnPlus.alpha = if (plusEnabled) 1f else 0.35f
        btnMinus.alpha = if (minusEnabled) 1f else 0.35f
        btnPlus.setOnClickListener {
            if (!plusEnabled) {
                Haptic.heavy(act)
                val msg = if (positives.isEmpty()) "当前用户组没有加分权限"
                else "今日评分额度已用完"
                S1Notify.show(act, msg, S1Notify.Type.INFO)
                return@setOnClickListener
            }
            bump(positive = true)
        }
        btnMinus.setOnClickListener {
            if (!minusEnabled) {
                Haptic.heavy(act)
                S1Notify.show(act, "当前用户组没有扣分权限", S1Notify.Type.INFO)
                return@setOnClickListener
            }
            bump(positive = false)
        }
        attachPressScale(btnPlus)
        attachPressScale(btnMinus)

        // 分值输入（细节轮2：RESET 按钮改为可直接敲数字）
        inputValue.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) commitInput()
        }
        inputValue.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                commitInput()
                v.clearFocus()
                true
            } else false
        }
        // 点击键帽区的分数大数字也能唤起输入（等价于点输入框）
        scoreValue.setOnClickListener { inputValue.requestFocus() }

        // —— 评分理由区（细节轮3）：预置 chips + 自由输入 + 通知作者开关 ——
        val reasonChips = dialog.findViewById<com.google.android.material.chip.ChipGroup>(
            R.id.rateReasonChips
        )
        val reasonInput = dialog.findViewById<EditText>(R.id.rateReasonInput)
        val notifyAuthor = dialog.findViewById<android.widget.CheckBox>(R.id.rateNotifyAuthor)
        var pickedReason: String? = null
        val presets = form.reasonPresets.ifEmpty { RateFormOptions.DEFAULT_REASON_PRESETS }
        presets.forEach { preset ->
            // Chip 需要 Activity context（主题解析走 act，Dialog 拿不到主题）
            val chip = com.google.android.material.chip.Chip(act).apply {
                text = preset
                isCheckable = true
                isClickable = true
                textSize = 12f
                // 票据语汇：chip 底取票据纸色、选中态取 accent（随主题自适应）
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    if (isChecked) inkOnLightFace() else android.R.color.transparent
                )
                setTextColor(inkOnLightFace())
            }
            chip.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    pickedReason = preset
                    // 选中即回填输入框：chips 是快捷方式，自由输入是补充而非二选一
                    reasonInput.setText(preset)
                } else if (pickedReason == preset) {
                    pickedReason = null
                    reasonInput.setText("")
                }
            }
            reasonChips.addView(chip)
        }
        notifyAuthor.isChecked = form.notifyAuthorDefault
        notifyAuthor.isEnabled = !form.notifyAuthorDisabled
        notifyAuthor.alpha = if (form.notifyAuthorDisabled) 0.4f else 1f

        btnDone.setOnClickListener {
            commitInput()
            // 理由取「输入框内容」优先（chips 点选已回填），空则不传（服务端允许不填）
            val reason = reasonInput.text?.toString()?.trim().orEmpty()
            confirm(reason, notifyAuthor.isChecked)
        }

        overlay.setOnClickListener { close() }
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                close()
            }
            keyCode == KeyEvent.KEYCODE_BACK
        }
        dialog.setOnShowListener {
            Haptic.medium(act)
            // 细节轮（2026-10-16）：移除 LED 呼吸动效（用户反馈），保留静态电源灯装饰
        }

        // 底部手势条 / 导航栏让位
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val density = act.resources.displayMetrics.density
            (panel.layoutParams as FrameLayout.LayoutParams).bottomMargin =
                (12 * density).toInt() + navBottom
            insets
        }

        renderScore()
        dialog.show()

        // 弹簧入场
        overlay.alpha = 0f
        panel.post {
            panel.translationY = panel.height + 48f
            panel.animate()
                .translationY(0f)
                .setInterpolator(OvershootInterpolator(1.12f))
                .setDuration(460)
                .start()
            overlay.animate().alpha(1f).setDuration(260).start()
        }
    }

    /** 在可选档位间循环：+ 走正数升序循环，− 走扣分加深循环 */
    private fun bump(positive: Boolean) {
        val opts = if (positive) positives else negatives
        if (opts.isEmpty()) return
        selected = when {
            positive && selected <= 0 -> opts.first()
            positive -> opts[(opts.indexOf(selected) + 1).coerceAtLeast(0) % opts.size]
            selected >= 0 -> opts.first()
            else -> opts[(opts.indexOf(selected) + 1).coerceAtLeast(0) % opts.size]
        }
        renderScore()
        floatDelta(if (positive) btnPlus else btnMinus, selected)
        Haptic.selection(act)
    }

    /**
     * 提交输入框里的分值（细节轮2）。
     *
     * 为什么这样校验：Discuz 的可选档位由服务端表单决定（[positives]/[negatives]），
     * 但用户想直接敲数字——所以**接受任意整数**，只拦两件事：
     * ① 空/非法输入回落到 0；② 超出当前用户组权限方向（只有加分的组不许填负数，
     * 反之亦然）时按方向提示，不静默改写用户输入。
     */
    private fun commitInput() {
        if (submitting) return
        val raw = inputValue.text?.toString()?.trim().orEmpty()
        val v = raw.removePrefix("+").toIntOrNull()
        val resolved = when {
            raw.isBlank() -> 0
            v == null -> {
                S1Notify.show(act, "请输入整数分值", S1Notify.Type.INFO)
                inputValue.setText(selected.toString())
                selected
            }
            v > 0 && positives.isEmpty() -> {
                S1Notify.show(act, "当前用户组没有加分权限", S1Notify.Type.INFO)
                inputValue.setText(selected.toString())
                selected
            }
            v < 0 && negatives.isEmpty() -> {
                S1Notify.show(act, "当前用户组没有扣分权限", S1Notify.Type.INFO)
                inputValue.setText(selected.toString())
                selected
            }
            else -> v
        }
        selected = resolved
        renderScore()
        inputValue.setText(if (resolved == 0) "" else resolved.toString())
        if (resolved != 0) Haptic.selection(act)
    }

    private fun renderScore() {
        scoreValue.text = when {
            selected == 0 -> "0"
            selected > 0 -> "+$selected"
            else -> selected.toString()
        }
        // 细节轮4（2026-10-16）：抽屉统一黑白两色，取消绿升红降与辉光 ——
        // 方向由数字自带的 +/− 号表达，SCORE 大字不再靠颜色喊话。
        when {
            selected != 0 -> {
                scoreValue.setTextColor(inkOnLightFace())
                scoreValue.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
            }
            else -> {
                // 细节轮2：黑白反色体系 —— 数字在浅色（暖白）屏上用近黑，中性 0 值不加辉光
                val neutral = 0xFF1A1A1A.toInt()
                scoreValue.setTextColor(neutral)
                scoreValue.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
            }
        }
        // 输入框与分数同步（±键切换时输入框也跟着变；提交时反过来）
        if (::inputValue.isInitialized && inputValue.hasFocus().not()) {
            inputValue.setText(if (selected == 0) "" else selected.toString())
        }
    }

    /** 键帽上方浮起 +1 / −1，1 秒后消失 */
    private fun floatDelta(anchor: View, value: Int) {
        val anchorRect = Rect()
        val layerRect = Rect()
        anchor.getGlobalVisibleRect(anchorRect)
        floatLayer.getGlobalVisibleRect(layerRect)
        val density = act.resources.displayMetrics.density
        val tv = TextView(act).apply {
            text = (if (value > 0) "+" else "") + value
            textSize = 18f
            setTypeCaseBoldMono()
            // 细节轮4：浮字同样统一黑白（随机身明暗取反色）
            setTextColor(inkOnLightFace())
            setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        }
        val spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        tv.measure(spec, spec)
        val x = anchorRect.centerX() - layerRect.left - tv.measuredWidth / 2f
        val y = anchorRect.top - layerRect.top - tv.measuredHeight / 2f
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = x.toInt()
            topMargin = y.toInt()
        }
        floatLayer.addView(tv, lp)
        tv.alpha = 0f
        tv.scaleX = 0.4f
        tv.scaleY = 0.4f
        tv.animate()
            .alpha(1f)
            .translationY(-16f * density)
            .scaleX(1.25f)
            .scaleY(1.25f)
            .setDuration(190)
            .withEndAction {
                tv.animate()
                    .alpha(0f)
                    .translationY(-54f * density)
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .setDuration(720)
                    .withEndAction { floatLayer.removeView(tv) }
                    .start()
            }
            .start()
    }

    private fun confirm(reason: String, notifyAuthor: Boolean) {
        if (submitting) return
        if (selected == 0) {
            Haptic.light(act)
            S1Notify.show(act, "请先选择加分或扣分", S1Notify.Type.INFO)
            return
        }
        val pid = post.pid
        if (pid.isNullOrBlank()) {
            S1Notify.show(act, "楼层信息缺失，无法评分", S1Notify.Type.ERROR)
            return
        }
        submitting = true
        btnDone.isEnabled = false
        btnDone.text = "提交中…"
        act.lifecycleScope.launch {
            val ok = onSubmit(pid, selected, reason, notifyAuthor, form)
            if (ok) {
                Haptic.success(act)
                close()
            } else {
                submitting = false
                btnDone.isEnabled = true
                btnDone.text = "确定"
            }
        }
    }

    private fun close() {
        if (closing || !dialog.isShowing) return
        closing = true
        overlay.animate().alpha(0f).setDuration(200).start()
        panel.animate()
            .translationY(panel.height + 48f)
            .setInterpolator(AccelerateInterpolator(1.2f))
            .setDuration(220)
            .withEndAction { runCatching { dialog.dismiss() } }
            .start()
    }


    /**
     * 抽屉黑白取色（细节轮4，2026-10-16）。
     *
     * 抽屉键帽是「深面纯黑 / 浅面纯白」，文字必须与之反色才能看清；
     * 但抽屉机身本身跟随主题明暗（[Ps1Skin.isLight]），所以**文字色要跟着主题走**：
     * 浅色皮肤 → 黑字；深色皮肤 → 白字（原先写死黑，深色主题下整块黑字不可见）。
     */
    private fun inkOnLightFace(): Int =
        if (Ps1Skin.isLight()) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()

    private fun inkOnDarkFace(): Int =
        if (Ps1Skin.isLight()) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()

    private fun attachPressScale(v: View) {
        v.setOnTouchListener { view, e ->
            when (e.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN ->
                    view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(110).start()
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL ->
                    view.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }
            false
        }
    }

    private fun TextView.setTypeCaseBoldMono() {
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD
        )
    }

    companion object {
        fun show(
            act: ThreadDetailActivity,
            post: Post,
            form: RateFormOptions,
            onSubmit: suspend (
                pid: String,
                amount: Int,
                reason: String,
                notifyAuthor: Boolean,
                form: RateFormOptions
            ) -> Boolean
        ) {
            RateDrawer(act, post, form, onSubmit).show()
        }
    }
}
