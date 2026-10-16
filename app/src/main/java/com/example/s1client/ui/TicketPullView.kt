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
        /**
         * 待命态透明度（2026-10-16，最后定稿）。
         *
         * 演进：最早(32aa21a)是 0.22（太淡，用户说"看不见"）→ 临时改实底刮刮乐(1f)偏离体系
         * → 回归宪法20「戏票档浓票纸」：票夹用主题accent实色、票纸用浓票纸深底（本身吸睛），
         * 待命态常驻可见且自然融入票据语言，不再靠"实底+高饱和描边"硬撑。
         * 另由 HomeFeed 在列表滚动时把整体 alpha 淡出（FAB 行为），停下再淡入，
         * 解决"固定在右上压住卡片/与内容混叠"。
         */
        const val IDLE_ALPHA = 1f
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
    // pull / scanDeg 必须是 public（2026-10-16 根因修复）：回弹、抽出、插回、条码走位
    // 全部走 ObjectAnimator(this, "pull"/"scanDeg", ...) 反射找 setPull/getPull，
    // private 属性不生成访问器 → 动画【静默失效】（不崩溃但完全不动）。
    // 实机症状完全对上："拉到一半松手不回位"、松手触发后的抽出/插回动画没影。
    var pull = 0f
    private var dragging = false
    private var busy = false
    private var triggerArmed = false
    private var reachedLimit = false
    private var startY = 0f
    private var downTime = 0L
    var scanDeg = 0f
    private var sweep = -1f// -1 = 无扫光；0..1 = 扫光位置
    // ===== 戏票档浓票纸调色板（从 Compose 侧桥接注入，宪法20「浮层用戏票」）=====
    // 默认 0 表示"未注入"，onDraw 回退到 attr 派生（兼容旧路径 / 自测）。
    var palettePaper: Int = 0
    var paletteGlow: IntArray = IntArray(4)
    var paletteOnText: Int = Color.WHITE
    var paletteAccentLine: Int = Color.WHITE
    fun applyPalette(paper: Int, glows: IntArray, onText: Int, accentLine: Int) {
        palettePaper = paper
        paletteGlow = glows.copyOf()
        paletteAccentLine = accentLine
        paletteOnText = onText
        invalidate()
    }

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
        // Levi's® 厂标字：粗无衬线（品牌补丁语汇），居中
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD
        )
        textAlign = Paint.Align.CENTER
        textSize = 10.5f * density
        setLetterSpacing(0.02f)
    }

    /** 票夹距视图顶部的偏移：留出 Tab 栏高度，否则票夹被 Tab 盖住看不见 */
    private val clipTopOffset = 34f * density

    /** 票夹右缘距视图右缘的间隙：HomeFeed 用 end padding 把视图右缘对到
     *  屏幕内 2dp 处，2 + 8 = 10dp —— 与瀑布流 contentPadding end 严格对齐 */
    private val clipRightGap = 8f * density

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
        // 2026-10-16 二改：票夹**右对齐**（旧版居中，而票纸 116dp 比宿主视图宽，
        // 左右各溢出 10dp——alpha<1 时 Android 离屏合成按视图边界裁切，
        // 溢出段被切掉 = 用户看到的"淡入先部分出现、回 1 才整体弹出"）。
        // 右缘贴视图内侧 clipRightGap，配合 HomeFeed 的 end padding 与瀑布流外框对齐。
        val right = w - clipRightGap
        clipRect.set(right - ticketW, clipTopOffset, right, clipTopOffset + clipH)
        buildTicketPath()
    }

    /**
     * 票据外形：圆角矩形 + 底部锯齿（撕票口）。
     *
     * 2026-10-16 几何重修（实机截图实证）：
     * - 旧版静止时票纸大半悬在票夹**上方**（top = clipH - peek，方向算反），
     *   灰票纸片与橙票夹条断裂成两截 —— 用户截图里的"先部分出现"残影；
     * - 右缘锯齿收笔落在 bottom+r（越界）+ 退化 quadTo（起点=终点）→ 右下角多出一个尖角；
     * - 右缘锯齿只有 5dp 深，观感不明显 → 移到底边（撕票口语汇，露出段正好可见）。
     *
     * 新几何（对齐 SwipeRefreshLayout 的"内容藏起来、拉动才露出来"机制）：
     * 票纸恒 40dp 高，静止时整体藏在票夹后面、只露底部 18dp 锯齿段；
     * 下拉时整张票向下抽出（slide = pull * 0.9，与手指同向），松手弹簧回位。
     */
    private fun buildTicketPath() {
        val slide = pull * 0.9f
        val top = clipRect.bottom - 22f * density + slide
        val bottom = top + 40f * density
        ticketRect.set(clipRect.left, top, clipRect.right, bottom)
        ticketPath.reset()
        val r = 5f * density
        val teeth = 5
        val depth = 4f * density
        val toothW = ticketRect.width() / teeth
        // 顺时针一笔：上边 → 右缘圆角 → 下缘锯齿（右→左）→ 左缘圆角
        ticketPath.moveTo(ticketRect.left + r, ticketRect.top)
        ticketPath.lineTo(ticketRect.right - r, ticketRect.top)
        ticketPath.quadTo(ticketRect.right, ticketRect.top, ticketRect.right, ticketRect.top + r)
        ticketPath.lineTo(ticketRect.right, ticketRect.bottom)
        var i = 0
        var x = ticketRect.right
        while (i < teeth) {
            ticketPath.lineTo(x - toothW / 2f, ticketRect.bottom + depth)
            x -= toothW
            ticketPath.lineTo(x, ticketRect.bottom)
            i++
        }
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

        // ---- 戏票档（浮层浓票纸，宪法20）：票夹=主题accent实色吸睛，票纸=浓票纸深底+四色荧光 ----
        val paper = if (palettePaper != 0) palettePaper else paperColor
        val accentLine = if (paletteAccentLine != 0) paletteAccentLine else withAlpha(Color.WHITE, 200)
        val onText = paletteOnText

        // 画序：票纸先画 → 票夹后画盖在票纸上，形成"票插在夹里"的层叠。
        // （旧版票夹先画、票纸后画，静止时票纸反而整段浮在票夹上方，截图实证断裂成两截）
        // 票纸：浓票纸深底（若注入 palette 则用它，否则 attr 派生）
        ticketPaint.color = if (triggerArmed) accentLine else paper
        canvas.drawPath(ticketPath, ticketPaint)
        // 四色荧光轻叠加（浓票纸质感，宪法20）：半透明叠加在深底上，避免盖掉纸面
        if (paletteGlow[0] != 0) {
            gradPaint.alpha = 95
            gradPaint.shader = android.graphics.LinearGradient(
                ticketRect.left, ticketRect.top, ticketRect.right, ticketRect.bottom,
                intArrayOf(paletteGlow[0], paletteGlow[1], paletteGlow[2], paletteGlow[3]),
                null, android.graphics.Shader.TileMode.CLAMP
            )
            canvas.drawPath(ticketPath, gradPaint)
            gradPaint.shader = null
            gradPaint.alpha = 255
        }
        // 亮白细描边（B：替代刮刮乐粗描边）
        edgePaint.color = accentLine
        edgePaint.strokeWidth = 1.4f * density
        canvas.drawPath(ticketPath, edgePaint)
        // 虚线内框（C：票据语汇）
        edgePaint.color = withAlpha(onText, 90)
        edgePaint.pathEffect = DashPathEffect(floatArrayOf(3f * density, 2.5f * density), 0f)
        val inner = RectF(
            ticketRect.left + 5f * density, ticketRect.top + 5f * density,
            ticketRect.right - 5f * density, ticketRect.bottom - 5f * density
        )
        canvas.drawRoundRect(inner, 2.5f * density, 2.5f * density, edgePaint)
        edgePaint.pathEffect = null

        // 票夹（顶部固定）：主题 accent 实色块（一眼可辨"可下拉"），亮白细描边，
        // 压住票纸"藏在夹里"的上半段 —— 静止时只露底部 18dp 锯齿撕票口
        clipPaint.color = accentColor
        canvas.drawRoundRect(clipRect, 2.5f * density, 2.5f * density, clipPaint)
        edgePaint.color = accentLine
        edgePaint.strokeWidth = 1.4f * density
        canvas.drawRoundRect(clipRect, 2.5f * density, 2.5f * density, edgePaint)

        // Levi's® 厂标：白字常驻印在票夹上（用户实机反馈"票夹像李维斯皮牌"，
        // 顺势做成品牌补丁——下拉=撕牌子的隐喻）。任何状态恒在，不参与闪烁。
        textPaint.color = withAlpha(Color.WHITE, 235)
        canvas.drawText(
            "Levi's\u00AE",
            clipRect.centerX(),
            clipRect.centerY() + textPaint.textSize / 3f,
            textPaint
        )

        // ---- 票面文字 ----
        // 待发（过阈值）：「松手」**常驻**票面正中（2026-10-16 用户要求：提示类文字一律
        // 常驻，不做"只显示一次就隐藏"）；空闲/刷新中票面留给撕票口与条码，保持干净。
        if (triggerArmed && !refreshing) {
            textPaint.color = paper
            canvas.drawText(
                "松手",
                ticketRect.centerX(),
                ticketRect.centerY() + textPaint.textSize / 3f,
                textPaint
            )
        }

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
                y >= 0f && y <= clipRect.bottom + 180f * density
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