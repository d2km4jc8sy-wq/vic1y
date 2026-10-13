package com.example.s1client.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.min

/**
 * 图片查看用 ImageView。
 * 特性：
 * - 初始 fitCenter（大图缩小到屏内，小图居中不放大）
 * - 双指缩放 1x ~ 5x
 * - 双击：fitCenter <-> 2.5x（以双击点为中心）
 * - 放大后可拖动，边界限制，拖动到边界外交给父容器（ViewPager2 翻页）
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    private val matrix = Matrix()
    private val baseMatrix = Matrix()
    private var baseScale = 1f
    private var scaleFactor = 1f
    private val minScale = 1f
    private val maxScale = 5f
    private val doubleTapScale = 2.5f

    private val lastTouch = PointF()
    private var isDragging = false

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val newScale = (scaleFactor * detector.scaleFactor)
                    .coerceIn(minScale, maxScale)
                val realFactor = newScale / scaleFactor
                scaleFactor = newScale
                matrix.postScale(realFactor, realFactor,
                    detector.focusX, detector.focusY)
                constrainMatrix()
                imageMatrix = matrix
                return true
            }
        })

    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (scaleFactor > 1.01f) {
                    matrix.set(baseMatrix)
                    scaleFactor = 1f
                    // 细节 20：两段缩放落定 SELECTION（回原位）
                    runCatching { com.example.s1client.util.Haptic.selection(context) }
                } else {
                    val factor = doubleTapScale / scaleFactor
                    scaleFactor = doubleTapScale
                    matrix.postScale(factor, factor, e.x, e.y)
                    constrainMatrix()
                    // 细节 20：两段缩放落定 SELECTION（放大到位）
                    runCatching { com.example.s1client.util.Haptic.selection(context) }
                }
                imageMatrix = matrix
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                performClick()
                return true
            }
        })

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        resetToFitCenter()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && drawable != null) resetToFitCenter()
    }

    private fun resetToFitCenter() {
        val d = drawable ?: return
        val vw = width.toFloat()
        val vh = height.toFloat()
        val dw = d.intrinsicWidth.toFloat()
        val dh = d.intrinsicHeight.toFloat()
        if (vw <= 0f || vh <= 0f || dw <= 0f || dh <= 0f) return

        baseScale = min(vw / dw, vh / dh)
        val dx = (vw - dw * baseScale) / 2f
        val dy = (vh - dh * baseScale) / 2f

        baseMatrix.reset()
        baseMatrix.postScale(baseScale, baseScale)
        baseMatrix.postTranslate(dx, dy)

        matrix.set(baseMatrix)
        imageMatrix = matrix
        scaleFactor = 1f
    }

    private fun constrainMatrix() {
        val d = drawable ?: return
        val v = FloatArray(9)
        matrix.getValues(v)
        val scaleX = v[Matrix.MSCALE_X]
        val scaleY = v[Matrix.MSCALE_Y]
        val transX = v[Matrix.MTRANS_X]
        val transY = v[Matrix.MTRANS_Y]

        val dw = d.intrinsicWidth * scaleX
        val dh = d.intrinsicHeight * scaleY
        val vw = width.toFloat()
        val vh = height.toFloat()

        var dx = 0f
        var dy = 0f

        if (dw <= vw) {
            dx = (vw - dw) / 2f - transX
        } else {
            if (transX > 0f) dx = -transX
            else if (transX + dw < vw) dx = vw - transX - dw
        }

        if (dh <= vh) {
            dy = (vh - dh) / 2f - transY
        } else {
            if (transY > 0f) dy = -transY
            else if (transY + dh < vh) dy = vh - transY - dh
        }

        if (dx != 0f || dy != 0f) matrix.postTranslate(dx, dy)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouch.set(event.x, event.y)
                isDragging = true
                if (scaleFactor > 1.01f) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && scaleFactor > 1.01f
                    && !scaleDetector.isInProgress
                    && event.pointerCount == 1) {
                    val dx = event.x - lastTouch.x
                    val dy = event.y - lastTouch.y

                    val v = FloatArray(9)
                    matrix.getValues(v)
                    val transX = v[Matrix.MTRANS_X]
                    val transY = v[Matrix.MTRANS_Y]
                    val d = drawable ?: return true
                    val dw = d.intrinsicWidth * v[Matrix.MSCALE_X]
                    val dh = d.intrinsicHeight * v[Matrix.MSCALE_Y]
                    val vw = width.toFloat()
                    val vh = height.toFloat()

                    val atLeftEdge = transX >= -0.5f && dx > 0
                    val atRightEdge = transX + dw <= vw + 0.5f && dx < 0
                    val atTopEdge = transY >= -0.5f && dy > 0
                    val atBottomEdge = transY + dh <= vh + 0.5f && dy < 0

                    if (atLeftEdge || atRightEdge || atTopEdge || atBottomEdge) {
                        parent?.requestDisallowInterceptTouchEvent(false)
                    } else {
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }

                    matrix.postTranslate(dx, dy)
                    constrainMatrix()
                    imageMatrix = matrix
                    lastTouch.set(event.x, event.y)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()
}
