package com.example.s1client.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.example.s1client.util.Haptic

/**
 * 抽票式下拉刷新（2026-10-16，替换 RopeRefreshView 的默认实现）。
 *
 * 为什么不是绳子：原来的实现是一根从屏幕顶垂下的直线 + 悬空圆球，物理上不成立
 * （绳子一头连着看不见的来源），而且下拉刷新是**平面**动作，用一维垂线表达必然别扭。
 * 换成像「从票夹里抽出一张票」——平面动作配平面物，物理关系完整。
 *
 * 与 RopeRefreshView **接口完全一致**（alpha / orbitColors / onPullRatio / onRefresh /
 * onKnobClick / refreshing），所以切换只需把 HomeFeed 里的 `RopeRefreshView(ctx)` 换成
 * `TicketPullView(ctx)`，其余代码不动；相机过片扳手方案另存为备选实现。
 *
 * 时序：
 * - 待命：票夹贴在顶部，只露出约 18dp 的票据边（不干扰看帖）
 * - 下拉：票据从票夹里被抽出，露出越多越长；过阈值时票面亮起（提示松手）
 * - 松手触发：整张票抽到位→ 高光扫过 → "咔哒"插回（震动）→ 走刷新
 * - 刷新中：票据底部**虚线走位**（像条码在跑），比原来的三色珠子更票据
 */
class TicketPullView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        /** 待命态透明度：不用全0 —— 全透明则用户看不到"这里能拉票"。
         *  给一点极低透明度（票夹淡显）既不抢戏，也给出可拉提示。 */
        const val IDLE_ALPHA = 0.22f
        /** 静止时票据露出高度（dp） */
        private const val IDLE_PEEK = 18f
    }

    // ===== 对外接口（与 RopeRefreshView 对齐）=====
    var onRefresh: (() -> Unit)? = null
    var onPullRatio: ((Float) -> Unit)? = null
    var onKnobClick: (() -> Unit)? = null
    var orbitColors: List<Int> = emptyList()
    var refreshing: Boolean = false
        set(v) {
            if (field == v) return
            field = v
            if (v) startScan() else stopScan()
            invalidate()
        }

    // ===== 尺寸 =====
    private val density = resources.displayMetrics.density
    private val maxPull get() = 168f * density
    private val clipH get() = 26f * density
    private val ticketW get() = 116f * density

    // ===== 状态 =====
    private var pull = 0f
    private var dragging = false
    private var busy = false
    private var triggerArmed = false
    private var reachedLimit = false
    private var startY = 0f
    private var downTime = 0L
    private var scanDeg = 0f
    private var sweep = -1f// -1 = 无扫光；0..1 = 扫光位置
    private var scanAnim: ObjectAnimator? = null

    // ===== 主题色（自愈取色，切主题即时跟随）=====
    private var pageColor = Color.WHITE
    private var accentColor = Color.GRAY
    private var paperColor = Color.WHITE
    private var lineColor = Color.LTGRAY

    // ===== Paint =====
    private val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ticketPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.4f * density
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f * density
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD
        )
        textAlign = Paint.Align.CENTER
        textSize = 10f * density
    }

    /** 票夹距视图顶部的偏移：留出 Tab 栏高度，否则票夹被 Tab 盖住看不见 */
    private val clipTopOffset = 8f * density

    private val clipRect = RectF()
    private val ticketRect = RectF()
    private val ticketPath = Path()
    private val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        // 刷新中：票据底部虚线走位（条码感），比三色珠子更贴票据语言
        scanAnim = ObjectAnimator.ofFloat(this, "scanDeg", 0f, 360f).apply {
            duration = 1400
            repeatCount = ValueAnimator_INFINITE
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener { invalidate() }
        }
    }

    private val ValueAnimator_INFINITE = android.animation.ValueAnimator.INFINITE

    fun refreshThemeColors() {
        val p = resolveColor(com.example.s1client.R.attr.s1BgPage, Color.WHITE)
        val a = resolveColor(com.example.s1client.R.attr.s1Accent, Color.GRAY)
        val paper = resolveColor(com.example.s1client.R.attr.s1BgCard, Color.WHITE)
        val line = resolveColor(com.example.s1client.R.attr.s1BorderLight, Color.LTGRAY)
        pageColor = p
        accentColor = a
        paperColor = paper
        lineColor = line
    }

    private fun resolveColor(attr: Int, fallback: Int): Int {
        val tv = TypedValue()
        return if (context.theme.resolveAttribute(attr, tv, true)) tv.data else fallback
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val cx = w / 2f
        clipRect.set(cx - ticketW / 2f, clipTopOffset, cx + ticketW / 2f, clipTopOffset + clipH)
        buildTicketPath()
    }

    /** 票据外形：圆角矩形 + 底部齿口（票据语汇，与便签同一套） */
    private fun buildTicketPath() {
        val shown = IDLE_PEEK * density + pull
        ticketRect.set(
            clipRect.left, clipH - shown + pull * 0.35f,
            clipRect.right, clipH - shown + pull * 0.35f + 40f * density
        )
        ticketPath.reset()
        val r = 5f * density
        val teeth = 6
        val depth = 5f * density
        val toothH = ticketRect.height() / teeth
        ticketPath.moveTo(ticketRect.left + r, ticketRect.top)
        ticketPath.lineTo(ticketRect.right - r, ticketRect.top)
        ticketPath.quadTo(ticketRect.right, ticketRect.top, ticketRect.right, ticketRect.top + r)
        var i = 0
        while (i < teeth) {
            val y = ticketRect.top + r + i * toothH
            ticketPath.lineTo(ticketRect.right, y + toothH / 2f)
            ticketPath.lineTo(ticketRect.right - depth, y + toothH)
            i++
        }
        ticketPath.lineTo(ticketRect.right - r, ticketRect.bottom)
        ticketPath.quadTo(ticketRect.right, ticketRect.bottom, ticketRect.right - r, ticketRect.bottom)
        ticketPath.lineTo(ticketRect.left + r, ticketRect.bottom)
        ticketPath.quadTo(ticketRect.left, ticketRect.bottom, ticketRect.left, ticketRect.bottom - r)
        ticketPath.lineTo(ticketRect.left, ticketRect.top + r)
        ticketPath.quadTo(ticketRect.left, ticketRect.top, ticketRect.left + r, ticketRect.top)
        ticketPath.close()
    }

    override fun onDraw(canvas: Canvas) {
        refreshThemeColors()
        val ratio = (pull / maxPull).coerceIn(0f, 1f)
        buildTicketPath()

        // ---- 票夹（顶部固定，票据从它里面被抽出）----
        clipPaint.color = withAlpha(lineColor, 150)
        canvas.drawRoundRect(clipRect, 4f * density, 4f * density, clipPaint)
        edgePaint.color = withAlpha(accentColor, 120)
        edgePaint.pathEffect = null
        canvas.drawRoundRect(clipRect, 4f * density, 4f * density, edgePaint)

        // ---- 票纸 ----
        ticketPaint.color = withAlpha(paperColor, 235)
        canvas.drawPath(ticketPath, ticketPaint)
        // 亮白描边（票据语汇）
        edgePaint.color = withAlpha(Color.WHITE, 200)
        canvas.drawPath(ticketPath, edgePaint)
        // 虚线内框
        edgePaint.color = withAlpha(accentColor, 110)
        edgePaint.pathEffect = DashPathEffect(floatArrayOf(3f * density, 2.5f * density), 0f)
        val inner = RectF(
            ticketRect.left + 5f * density, ticketRect.top + 5f * density,
            ticketRect.right - 5f * density, ticketRect.bottom - 5f * density
        )
        canvas.drawRoundRect(inner, 3f * density, 3f * density, edgePaint)
        edgePaint.pathEffect = null

        // ---- 票面内容：过阈值前画向下箭头，过阈值后亮起 + 翻成向上 ----
        val cx = ticketRect.centerX()
        val cy = ticketRect.centerY()
        barPaint.color = if (triggerArmed) withAlpha(accentColor, 235) else withAlpha(accentColor, 170)
        val ar = 5.5f * density
        val dir = if (triggerArmed) -1f else 1f
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(1f, dir)
        canvas.drawLine(0f, -ar, 0f, ar * 0.5f, barPaint)
        canvas.drawLine(-ar * 0.72f, 0f, 0f, ar * 0.5f, barPaint)
        canvas.drawLine(ar * 0.72f, 0f, 0f, ar * 0.5f, barPaint)
        canvas.restore()

        // ---- 刷新中：底部虚线走位（条码）----
        if (refreshing) {
            val baseY = ticketRect.bottom - 7f * density
            val half = ticketRect.width() / 2f - 7f * density
            val n = 7
            val step = (half * 2) / n
            for (i in 0 until n) {
                // 让其中 3 格按scanDeg 顺序点亮，形成"跑动"感
                val phase = ((scanDeg / 360f) * n).toInt()
                val dist = ((phase - i) + n) % n
                val a = when {
                    dist == 0 -> 235
                    dist == 1 -> 150
                    else -> 60
                }
                barPaint.color = withAlpha(accentColor, a)
                val x = ticketRect.left + 7f * density + i * step
                canvas.drawLine(x, baseY, x + step * 0.55f, baseY, barPaint)
            }
        } else {
            // 待命/待发：文字提示
            textPaint.color = withAlpha(accentColor, if (triggerArmed) 235 else 150)
            canvas.drawText(
                if (triggerArmed) "松手" else "拉票",
                cx,
                ticketRect.bottom - 7f * density,
                textPaint
            )
        }

        // ---- 触发瞬间的高光扫过 ----
        if (sweep in 0f..1f) {
            val bandW = 30f * density
            val x = ticketRect.left - bandW + (ticketRect.width() + bandW * 2) * sweep
            gradPaint.shader = android.graphics.LinearGradient(
                x - bandW / 2f, 0f, x + bandW / 2f, 0f,
                intArrayOf(0x00FFFFFF, 0x99FFFFFF.toInt(), 0x00FFFFFF),
                null, android.graphics.Shader.TileMode.CLAMP
            )
            canvas.drawPath(ticketPath, gradPaint)
            gradPaint.shader = null
        }
    }

    private fun withAlpha(c: Int, a: Int): Int =
        Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))

    /** 触摸热区扩到票夹左右各 24dp、下方延伸到整条可拉带，
     *  否则 60dp 窄条太难点中（实机反馈：拉不动）。*/
    private fun hitTest(x: Float, y: Float): Boolean {
        val slop = 26f * density
        return x >= clipRect.left - slop && x <= clipRect.right + slop &&
                y >= 0f && y <= clipRect.bottom + 150f * density
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN && !hitTest(event.x, event.y)) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (busy) return false
                dragging = true
                reachedLimit = false
                triggerArmed = false
                downTime = android.os.SystemClock.elapsedRealtime()
                startY = event.y
                onPullRatio?.invoke(0f)
                parent?.requestDisallowInterceptTouchEvent(true)
                animate().alpha(1f).setDuration(80).start()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return false
                val raw = (event.y - startY).coerceAtLeast(0f)
                pull = (maxPull * (1f - kotlin.math.exp(-raw / maxPull))).coerceIn(0f, maxPull)
                // 两段式张力震感（与原实现同语义）
                if (!triggerArmed && pull >= maxPull * 0.62f) {
                    triggerArmed = true
                    runCatching { Haptic.selection(context) }
                }
                if (!reachedLimit && pull >= maxPull * 0.96f) {
                    reachedLimit = true
                    runCatching { Haptic.light(context) }
                }
                onPullRatio?.invoke(pull / maxPull)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!dragging) return false
                dragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
                val isTap = pull < maxPull * 0.08f &&
                        android.os.SystemClock.elapsedRealtime() - downTime < 260L
                if (isTap && !busy && !refreshing) onKnobClick?.invoke()
                releaseTicket(fire = pull >= maxPull * 0.62f)
                return true
            }
        }
        return false
    }

    /** 回弹：过冲弹性；触发刷新时先"插票"再走 onRefresh */
    private fun releaseTicket(fire: Boolean) {
        busy = true
        if (!fire) {
            refreshing = false
            triggerArmed = false
            val back = ObjectAnimator.ofFloat(this, "pull", pull, 0f).apply {
                duration = 300
                interpolator = OvershootInterpolator(1.4f)
                addUpdateListener { invalidate() }
            }
            back.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    busy = false
                    animate().alpha(IDLE_ALPHA).setDuration(260).start()
                }
            })
            back.start()
            return
        }

        // 触发：抽到位 → 扫光 → 插回 → 刷新
        triggerArmed = false
        val out = ObjectAnimator.ofFloat(this, "pull", pull, maxPull).apply {
            duration = 110
            interpolator = DecelerateInterpolator()
            addUpdateListener { invalidate() }
        }
        out.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // 扫光
                val sw = ObjectAnimator.ofFloat(this@TicketPullView, "sweep", 0f, 1f).apply {
                    duration = 240
                    interpolator = DecelerateInterpolator()
                    addUpdateListener { invalidate() }
                }
                sw.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(a: Animator) {
                        // 插回（票缩回夹里）
                        Haptic.medium(context)
                        val back = ObjectAnimator.ofFloat(this@TicketPullView, "pull", maxPull, 0f).apply {
                            duration = 220
                            interpolator = AccelerateInterpolator()
                            addUpdateListener { invalidate() }
                        }
                        back.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(a2: Animator) {
                                sweep = -1f
                                refreshing = true
                                busy = false
                                onRefresh?.invoke()
                            }
                        })
                        back.start()
                    }
                })
                sw.start()
            }
        })
        out.start()
    }

    private fun startScan() {
        scanAnim?.start()
    }

    private fun stopScan() {
        scanAnim?.cancel()
        scanDeg = 0f
    }

    override fun onDetachedFromWindow() {
        stopScan()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) startScan() else stopScan()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (visibility == VISIBLE) startScan()
    }
}