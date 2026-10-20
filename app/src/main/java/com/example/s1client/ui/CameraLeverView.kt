package com.example.s1client.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.example.s1client.R

/**
 * 相机过片扳手刷新（实验功能，PULL_IMPL = 1）。
 *
 * **2026-10-19 L4 左手版重做**（用户在位置/方向预览页拍板 L4+D6）：
 * 相机贴**左缘**常驻（HomeFeed TopStart），feed 左侧让出过片通道；
 * 行程改为 **D6 压杆**：静息 **-30°**（斜指右上）→ 拉满 **+60°**（斜指右下），
 * 左拇指贴缘即握、行程顺拇指下压弧线。转轴 (58,50) 与球头半径 44 不变——
 * 扳手始终在机身**右侧**的通道内摆动，几何不再镜像。
 *
 * ### 沿革
 * - 2026-10-18 22:30 重做：几何取自设计者《相机下拉.html》，修掉「不连机身」「下拉反升」。
 * - 2026-10-18 23:15：球头半径 32→44、画布加宽 124。
 * - 2026-10-18 22:58：阈值 52→45、热区放宽。
 * - 2026-10-19：L4+D6（本次）+ 五项手感增强（见下）。
 * - 2026-10-20 **方案 B（外壳重做）**：旧画布 124×132 左边空 13vb、下边空 32vb，
 *   外框左缘与机身左缘差 11dp，挂载点怎么调都对不齐卡片边距（30dp 时机身落 41.7dp）。
 *   画布收紧到 93×82 = 图形外框，VIEW_W 104→78dp（缩放比不变，机身大小不变）；
 *   start 30→10dp 贴卡片边距；静止态不再画轨迹弧（改按行程角 6° 渐显）；
 *   深色主题机身加 25% 白微光边。**行程 / 阈值 / 半径 / 热区全部未动。**
 *
 * ### 触摸模型：球头跟手
 * 角度由球头坐标反算（atan2），手指到哪球头到哪；起手热区跟球头 [HIT_BALL] 或转轴 [HIT_AXIS]。
 * 松手两条：过 [THRESHOLD_ANGLE]（绝对角），或甩动速度够快（[FLICK_DEG_PER_MS]，按行程角判定）。
 *
 * ### 手感增强（2026-10-19 用户逐项确认）
 * - **P1 甩动残影**：拖动中最近采样沿弧画渐隐 accent 光点（速度感可视化）。
 * - **P2 到位磁吸**：显示角超过阈值后向拉满方向轻吸 12%，跨越阈值加一次 light 震——
 *   把「该松手了」从记阈值变成手感。跟手仍是 1:1，磁吸只作用于显示角。
 * - **P3 拉满光圈**：行程末 15° 镜头内圈微开 + 高光增强（「光圈全开，准备曝光」）。
 * - **P5 浅底环境光晕**：页面底亮度 > 0.5 时机身背后加一圈 16% 白晕，
 *   拟物黑机身在浅色主题上不再像「黑色贴纸」（深色主题不变）。
 * （P4「下拉过片」首次提示在 HomeFeed 侧实现，触发一次后本地记住并淡出。）
 *
 * ### 配色纪律（宪法 18）
 * 就绪弧、球头到位色、残影取 s1Accent（onDraw 自愈跟色）；
 * 机身与扳手为**拟物固有色**（黑机身 + 金属杆），等同桌面图标第 20 条例外逻辑。
 *
 * 状态权威（宪法 23.A.3）：revealed（滚动隐现）与 refreshing（曝光中）由 HomeFeed 回写。
 */
class CameraLeverView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        // ————————— 画布基准（CameraLeverViewTest 锚点） —————————
        /**
         * viewBox 宽（vb 单位），几何常量都以它为参照。
         *
         * **2026-10-20 方案 B**：124 → 93。旧画布左边空 13vb、下边空 32vb，
         * 于是「外框左缘」和用户眼睛看到的「机身左缘」差 11dp，挂载点 start 怎么调
         * 都对不齐卡片边距（旧值 30dp 时机身实际落在 41.7dp）。现在容器 = 图形。
         * 缩放比保持不变（78/93 = 104/124），机身视觉大小与旧版一致。
         */
        const val VB_W = 93f
        /** viewBox 高（vb 单位），同步收紧（132 → 82） */
        const val VB_H = 82f
        /** 图形原点在旧 vb 坐标系里的偏移：onDraw translate、触摸 toVb 回加 */
        const val ORIGIN_X = 13f
        const val ORIGIN_Y = 19f
        /** View 建议宽度 dp；高度按比例推出 */
        const val VIEW_W_DP = 78f

        /** 转轴（机身内） */
        const val PIVOT_X = 58f
        const val PIVOT_Y = 50f
        /** 球头回转半径（2026-10-18 23:15 由 32 加长到 44：用户实测「拨杆加长」） */
        const val BALL_R = 44f
        /** 起始角：扳手斜指右上（L4 左手版 D6，2026-10-19） */
        const val BASE_ANGLE = -30f
        /** 到底角：扳手斜指右下 */
        const val MAX_ANGLE = 60f
        /** 松手触发阈值：绝对角。行程 -30→60 共 90°，半程 = -30+45 = 15°（2026-10-19 随 D6 重定） */
        const val THRESHOLD_ANGLE = 15f
        /** 甩动触发：角速度（度/ms）超过此值且已离开起点即到位 */
        const val FLICK_DEG_PER_MS = 1.2f
        /** 甩动判定的最小起角（防止原地轻抖也算甩） */
        const val FLICK_MIN_ANGLE = 18f
        /** 分档咔哒步长（度），跨档给一次 selection 震感。按**行程角**计（静息 = 0 档） */
        const val TICK_STEP_DEG = 15f
        /** 起手热区：球头半径 vb（22:58 由 28 放宽到 34，配合控件放大，手指更好抓） */
        const val HIT_BALL = 34f
        /** 起手热区：转轴附近半径 vb（46 → 54：拇指不必精确命中） */
        const val HIT_AXIS = 54f
        /** 浮现/隐藏位移 dp */
        const val REVEAL_SHIFT_DP = -56f

        /** 建议 View 高度 dp（按 viewBox 比例） */
        val VIEW_H_DP: Float = VB_H * (VIEW_W_DP / VB_W)

        /** 角度钳制 */
        fun clampAngle(a: Float): Float = a.coerceIn(BASE_ANGLE, MAX_ANGLE)

        /**
         * 球头坐标 → 扳手角度（atan2 反算，1:1 跟手）。
         * 指针落在转轴左侧时按上下半区给端点值，避免 atan2 在 ±180° 抖动。
         */
        fun angleForPoint(px: Float, py: Float): Float {
            val dx = px - PIVOT_X
            val dy = py - PIVOT_Y
            if (dx < 0f) return if (dy >= 0f) MAX_ANGLE else BASE_ANGLE
            return clampAngle(Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat())
        }

        /** 角度 → 球头坐标（与 angleForPoint 互逆） */
        fun ballPosForAngle(angle: Float): Pair<Float, Float> {
            val r = Math.toRadians(clampAngle(angle).toDouble())
            return Pair(
                PIVOT_X + BALL_R * Math.cos(r).toFloat(),
                PIVOT_Y + BALL_R * Math.sin(r).toFloat()
            )
        }

        /** 球头是否在起手热区内 */
        fun hitTest(px: Float, py: Float, currentAngle: Float): Boolean {
            val b = ballPosForAngle(currentAngle)
            val dBall = Math.hypot((px - b.first).toDouble(), (py - b.second).toDouble())
            if (dBall <= HIT_BALL) return true
            val dAxis = Math.hypot((px - PIVOT_X).toDouble(), (py - PIVOT_Y).toDouble())
            return dAxis <= HIT_AXIS
        }

        /** 松手是否触发（角度维度；速度维度在 onTouchEvent 里判） */
        fun shouldFire(angle: Float): Boolean = angle >= THRESHOLD_ANGLE

        /** 当前咔哒档位：按行程角计（静息 = 0 档），跨 15° 一档 */
        fun tickIndex(angle: Float, stepDeg: Float = TICK_STEP_DEG): Int =
            ((clampAngle(angle) - BASE_ANGLE) / stepDeg).toInt()

        /**
         * View 内像素坐标 → 旧 vb 坐标（几何常量的参照系）。
         * 2026-10-20 方案 B：画布左上角裁掉了 [ORIGIN_X, ORIGIN_Y]，
         * 触摸坐标必须回加偏移才能与 PIVOT / BALL_R 对得上。
         */
        fun toVb(x: Float, y: Float, s: Float): Pair<Float, Float> =
            Pair(x / s + ORIGIN_X, y / s + ORIGIN_Y)
    }

    // ————————— 拟物固有色（宪法 20 例外） —————————
    private val bodyTop = 0xFF3F3F44.toInt()
    private val bodyMid = 0xFF232327.toInt()
    private val bodyBottom = 0xFF131315.toInt()
    private val bodyEdge = 0xFF4A4A4E.toInt()
    private val decoDark = 0xFF2C2C30.toInt()
    private val decoLight = 0xFF3A3A3F.toInt()
    private val lensRingOuter = 0xFF55555A.toInt()
    private val lensRingInner = 0xFF2E2E33.toInt()
    private val lensInner = 0xFF0A0A0C.toInt()
    private val lensRingDeep = 0xFF38383D.toInt()
    private val leverTop = 0xFFF4F4F4.toInt()
    private val leverMid = 0xFFC4C4C4.toInt()
    private val leverLow = 0xFF8D8D8D.toInt()
    private val leverBottom = 0xFF484848.toInt()
    private val knurl = 0xFF707070.toInt()
    private val ballHi = 0xFFFFFFFF.toInt()
    private val ballMid = 0xFFD4D4D4.toInt()
    private val ballLo = 0xFF6F6F6F.toInt()
    private val ballEdge = 0xFF5A5A5A.toInt()
    private val axisOuter = 0xFF2A2A2E.toInt()
    private val axisRing = 0xFF55555A.toInt()
    private val axisDot = 0xFF77777C.toInt()
    private val trackBase = 0xFF3A3A3F.toInt()

    // ————————— 状态 —————————
    private var angle = BASE_ANGLE
    private var dragging = false
    private var busy = false
    private var revealed = false
    private var exposing = false
    private var resetAnimator: ValueAnimator? = null
    private var lastTick = 0
    private var samples = ArrayList<FloatArray>()   // [timeMs, angle]
    private var armed = false                       // P2：已越过阈值（到位态）
    private val trail = ArrayList<FloatArray>()     // P1：[timeMs, angle] 甩动残影采样

    private val density get() = resources.displayMetrics.density
    /** vb 单位 → px */
    private var s = 1f

    // ————————— 画笔 —————————
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rect = RectF()
    private val arcRect = RectF()

    private var accentColor = 0
    private var lightBg = false
    private fun refreshThemeColors(): Boolean {
        val tv = android.util.TypedValue()
        var changed = false
        context.theme.resolveAttribute(R.attr.s1Accent, tv, true)
        val a = tv.data
        if (a != accentColor) { accentColor = a; changed = true }
        // P5：页面底亮度 > 0.5 视为浅底 —— 拟物黑机身加环境光晕（深色主题不加）
        if (context.theme.resolveAttribute(R.attr.s1BgPage, tv, true)) {
            val c = tv.data
            val lum = (0.2126f * ((c shr 16) and 0xFF) +
                    0.7152f * ((c shr 8) and 0xFF) +
                    0.0722f * (c and 0xFF)) / 255f
            val lb = lum > 0.5f
            if (lb != lightBg) { lightBg = lb; changed = true }
        }
        return changed
    }

    var onRefresh: (() -> Unit)? = null

    init {
        alpha = 0f
        translationY = REVEAL_SHIFT_DP * density
        refreshThemeColors()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0) s = w / VB_W
    }

    private fun mix(c1: Int, c2: Int, t: Float): Int {
        fun ch(shift: Int) = (((c1 shr shift) and 0xFF) * (1 - t) + (((c2 shr shift) and 0xFF) * t)).toInt()
        return (ch(24) shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (refreshThemeColors()) invalidate()
        if (width == 0) return

        canvas.save()
        canvas.scale(s, s)
        // 方案 B：画布已收紧到图形外框，把几何整体挪回左上（触摸侧由 toVb 回加偏移）
        canvas.translate(-ORIGIN_X, -ORIGIN_Y)
        if (accentColor == 0) refreshThemeColors()

        val ready = angle >= THRESHOLD_ANGLE

        // ---- P5 浅底环境光晕：黑机身在浅色页面上的「坐进感」（深色主题 lightBg=false 不画） ----
        if (lightBg) {
            fill.shader = RadialGradient(58f, 70f, 62f,
                intArrayOf(0x29FFFFFF, 0x00FFFFFF),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(58f, 70f, 62f, fill)
            fill.shader = null
        }

        // ---- 机身：竖长圆角矩形 + 顶部装饰 ----
        rect.set(14f, 38f, 70f, 100f)
        fill.shader = LinearGradient(
            14f, 38f, 50f, 100f,
            intArrayOf(bodyTop, bodyMid, bodyBottom), null, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, 13f, 13f, fill)
        fill.shader = null
        stroke.strokeWidth = 1f
        // 方案 B：深色主题下黑机身沉进卡片，描一道 25% 白微光边把它托出来
        // （浅色主题保持原拟物深色描边；与 P5 浅底光晕互补）
        stroke.color = if (lightBg) bodyEdge else 0x40FFFFFF.toInt()
        canvas.drawRoundRect(rect, 13f, 13f, stroke)

        fill.color = decoDark; canvas.drawRoundRect(21f, 43f, 41f, 46.5f, 1.75f, 1.75f, fill)
        fill.color = decoLight; canvas.drawRoundRect(45f, 43f, 54f, 46.5f, 1.75f, 1.75f, fill)

        // ---- 镜头：同心圆组 + 斜向高光 ----
        stroke.strokeWidth = 2f; stroke.color = lensRingOuter
        canvas.drawCircle(42f, 72f, 19f, stroke)
        fill.shader = RadialGradient(36f, 66f, 30f,
            intArrayOf(0xFF4D4D55.toInt(), 0xFF1B1B1F.toInt(), 0xFF050506.toInt()),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(42f, 72f, 17f, fill)
        fill.shader = null
        stroke.strokeWidth = 1.5f; stroke.color = lensRingInner
        canvas.drawCircle(42f, 72f, 11f, stroke)
        // P3 拉满光圈：行程末 15° 内圈微开 + 高光增强（「光圈全开，准备曝光」）
        val ap = ((angle - (MAX_ANGLE - 15f)) / 15f).coerceIn(0f, 1f)
        val irisR = 7f + ap * 1.6f
        fill.color = lensInner
        canvas.drawCircle(42f, 72f, irisR, fill)
        stroke.strokeWidth = 1f; stroke.color = lensRingDeep
        canvas.drawCircle(42f, 72f, irisR, stroke)
        fill.color = 0x24FFFFFF
        fill.alpha = (0x24 + (0x38 * ap).toInt()).coerceIn(0, 255)
        canvas.save()
        canvas.rotate(-30f, 37f, 66f)
        rect.set(32f, 63.2f, 42f, 68.8f)
        canvas.drawOval(rect, fill)
        canvas.restore()
        fill.alpha = 255

        // ---- 轨迹弧：底弧 + 就绪进度（accent）。弧段从静息角 BASE 摆到 MAX ----
        // 方案 B：静止态不留弧。旧版 #3A3A3F 底弧常亮，在卡片上像一道刮痕。
        // 现在按行程角 6° 渐显渐隐：起手有引导，回位自动收干净。
        val travel = ((angle - BASE_ANGLE) / 6f).coerceIn(0f, 1f)
        if (travel > 0.01f) {
            arcRect.set(PIVOT_X - BALL_R, PIVOT_Y - BALL_R, PIVOT_X + BALL_R, PIVOT_Y + BALL_R)
            stroke.strokeWidth = 1.8f
            stroke.color = trackBase
            stroke.alpha = (255f * travel).toInt().coerceIn(0, 255)
            stroke.strokeCap = Paint.Cap.ROUND
            canvas.drawArc(arcRect, BASE_ANGLE, MAX_ANGLE - BASE_ANGLE, false, stroke)
            stroke.alpha = 255
        }
        if (angle - BASE_ANGLE > 0.5f) {
            stroke.strokeWidth = 3f
            stroke.color = accentColor
            canvas.drawArc(arcRect, BASE_ANGLE, angle - BASE_ANGLE, false, stroke)
        }

        // ---- P1 甩动残影：最近采样沿弧渐隐（仅拖动中，采样窗 ~6 帧复用触摸采样） ----
        if (dragging && trail.size > 1) {
            val now = System.currentTimeMillis()
            for (i in 0 until trail.size - 1) {
                val t = trail[i]
                val age = (now - t[0]) / 380f
                if (age >= 1f) continue
                val b = ballPosForAngle(t[1])
                fill.color = accentColor
                fill.alpha = ((1f - age) * 130).toInt().coerceIn(0, 255)
                canvas.drawCircle(b.first, b.second, 3.2f * (1f - age * 0.6f), fill)
                fill.alpha = 255
            }
        }

        // ---- 扳手组：绕轴 (58,50) 旋转 ----
        canvas.save()
        canvas.rotate(angle, PIVOT_X, PIVOT_Y)

        rect.set(54f, 45.5f, 100f, 54.5f)
        fill.shader = LinearGradient(0f, 45.5f, 0f, 54.5f,
            intArrayOf(leverTop, leverMid, leverLow, leverBottom),
            floatArrayOf(0f, 0.42f, 0.58f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, 4.5f, 4.5f, fill)
        fill.shader = null
        stroke.strokeWidth = 0.9f; stroke.color = knurl; stroke.strokeCap = Paint.Cap.BUTT
        for (x in floatArrayOf(76f, 81f, 86f)) canvas.drawLine(x, 46.6f, x, 53.4f, stroke)

        // 球头：过阈值时向 accent 靠一档，给「到位」视觉
        val bMid = if (ready) mix(ballMid, accentColor, 0.55f) else ballMid
        val bLo = if (ready) mix(ballLo, accentColor, 0.35f) else ballLo
        fill.shader = RadialGradient(98.8f, 46.8f, 11f,
            intArrayOf(ballHi, bMid, bLo), floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(102f, 50f, 8f, fill)
        fill.shader = null
        stroke.strokeWidth = 1f; stroke.color = ballEdge
        canvas.drawCircle(102f, 50f, 8f, stroke)

        fill.color = axisOuter
        canvas.drawCircle(PIVOT_X, PIVOT_Y, 6f, fill)
        stroke.strokeWidth = 1.2f; stroke.color = axisRing
        canvas.drawCircle(PIVOT_X, PIVOT_Y, 6f, stroke)
        fill.color = axisDot
        canvas.drawCircle(PIVOT_X, PIVOT_Y, 2f, fill)

        canvas.restore()
        canvas.restore()
    }

    // ————————— 手势：球头跟手 —————————
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!revealed) return false
        val (vx, vy) = toVb(event.x, event.y, s)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (busy) return false
                if (!hitTest(vx, vy, angle)) return false      // 热区外穿透给下方卡片
                dragging = true
                armed = false
                trail.clear()
                lastTick = 0
                samples = ArrayList()
                samples.add(floatArrayOf(System.currentTimeMillis().toFloat(), angle))
                resetAnimator?.cancel()
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return false
                val raw = angleForPoint(vx, vy)
                // P2 到位磁吸：显示角过阈值后向拉满方向轻吸 12%（跟手仍 1:1，只加趋势）
                angle = if (raw > THRESHOLD_ANGLE)
                    (raw + (raw - THRESHOLD_ANGLE) * 0.12f).coerceAtMost(MAX_ANGLE)
                else raw
                // P2 到位震：跨越阈值那一次给 light（比 selection 强一档）
                if (!armed && angle >= THRESHOLD_ANGLE) {
                    armed = true
                    com.example.s1client.util.Haptic.light(context)
                }
                val now = System.currentTimeMillis()
                samples.add(floatArrayOf(now.toFloat(), angle))
                if (samples.size > 6) samples.removeAt(0)
                // P1 残影采样：与触摸采样同源，onDraw 里按年龄渐隐
                trail.add(floatArrayOf(now.toFloat(), angle))
                if (trail.size > 8) trail.removeAt(0)
                val tick = tickIndex(raw)
                if (tick != lastTick) {
                    lastTick = tick
                    com.example.s1client.util.Haptic.selection(context)
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!dragging) return false
                dragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
                // 甩动判定：取窗口内首尾角速度；「已离开起点」按行程角计（静息 = 0）
                var flick = false
                if (samples.size >= 2) {
                    val f = samples[0]; val l = samples[samples.size - 1]
                    val dt = (l[0] - f[0]).coerceAtLeast(1f)
                    val v = Math.abs((l[1] - f[1]).toDouble() / dt.toDouble())
                    flick = v >= FLICK_DEG_PER_MS && (angle - BASE_ANGLE) >= FLICK_MIN_ANGLE
                }
                val cancelled = event.actionMasked == MotionEvent.ACTION_CANCEL
                if (!cancelled && (flick || shouldFire(angle))) fire(flick) else springBack()
                return true
            }
        }
        return false
    }

    /** 触发：快门组合震 → 扳手 200ms 到底 → 回位 → 刷新回调 */
    private fun fire(flick: Boolean) {
        busy = true
        com.example.s1client.util.Haptic.success(context)
        resetAnimator?.cancel()
        val snapshot = if (flick) MAX_ANGLE else angle
        resetAnimator = ValueAnimator.ofFloat(snapshot, MAX_ANGLE).apply {
            duration = 200L
            interpolator = DecelerateInterpolator()
            addUpdateListener { angle = it.animatedValue as Float; invalidate() }
            start()
        }
        onRefresh?.invoke()
        // 到头后回位（不等刷新结束：回位是手感，刷新是业务）
        postDelayed({
            resetAnimator?.cancel()
            resetAnimator = ValueAnimator.ofFloat(MAX_ANGLE, BASE_ANGLE).apply {
                duration = 260L
                interpolator = DecelerateInterpolator()
                addUpdateListener { angle = it.animatedValue as Float; invalidate() }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) { busy = false }
                })
                start()
            }
        }, 220L)
    }

    /** 未达标回弹：420ms，末尾解锁 */
    private fun springBack() {
        busy = true
        resetAnimator?.cancel()
        resetAnimator = ValueAnimator.ofFloat(angle, BASE_ANGLE).apply {
            duration = 420L
            interpolator = DecelerateInterpolator()
            addUpdateListener { angle = it.animatedValue as Float; invalidate() }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) { busy = false }
            })
            start()
        }
    }

    // ————————— 权威回写入口（HomeFeed 调用） —————————
    fun setRevealed(value: Boolean) {
        if (revealed == value) return
        revealed = value
        animate().cancel()
        if (value) {
            animate().translationY(0f).alpha(1f)
                .setDuration(260L)
                .setInterpolator(android.view.animation.OvershootInterpolator(1.1f))
                .start()
        } else {
            animate().translationY(REVEAL_SHIFT_DP * density).alpha(0f)
                .setDuration(200L)
                .start()
        }
    }

    fun setRefreshing(active: Boolean) {
        if (active) {
            exposing = true
            busy = true
            invalidate()
        } else if (exposing) {
            exposing = false
            postDelayed({
                busy = false
                com.example.s1client.util.Haptic.light(context)
                invalidate()
            }, 240)
            invalidate()
        }
    }

    override fun onDetachedFromWindow() {
        resetAnimator?.cancel()
        animate().cancel()
        super.onDetachedFromWindow()
    }
}