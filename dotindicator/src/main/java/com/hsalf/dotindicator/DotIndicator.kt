package com.hsalf.dotindicator

import android.animation.ArgbEvaluator
import android.animation.FloatEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE

class DotIndicator : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var focusedPage = 0
    private var animatingDot = -1
    private var manualDrag = false
    private var viewPager: ViewPager2? = null
    private var focusedPageVisibilityFraction = 1f
    private var pageScrollState = SCROLL_STATE_IDLE

    private val dotSize: Float
        get() = context.resources.displayMetrics.density * 12

    private val totalDotSpace: Float
        get() = ((dotCount * 2) - 1) * dotSize

    private val verticalPadding: Float
        get() = context.resources.displayMetrics.density * 2

    private var lineWidth = dotSize / 8f

    private val startAngle: Float
        get() {
            return if (progressAnimator.isRunning) {
                floatEvaluator.evaluate(progressAnimator.animatedFraction, START_ANGLE, END_ANGLE)
            } else {
                progressReverseAnimator.animatedValue as Float
            }
        }

    private val dotCount: Int
        get() = viewPager?.adapter?.itemCount ?: INITIAL_COUNT

    private val sweepAngle: Float
        get() = (SWEEP_ANGLE - startAngle + START_ANGLE)

    private val isAnimating: Boolean
        get() = progressAnimator.isRunning || progressReverseAnimator.isRunning

    private var dotBorderColor = Color.parseColor("#6177E5")
    private var dotUnReadColor = Color.parseColor("#22262F")
    private var dotReadInnerArcColor = Color.parseColor("#B36177E5")

    private val referenceRect by lazy { RectF() }
    private val argbEvaluator by lazy { ArgbEvaluator() }
    private val floatEvaluator by lazy { FloatEvaluator() }

    private var allowTimeoutCallback = true

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrollStateChanged(state: Int) {
            pageScrollState = state
            if (SCROLL_STATE_DRAGGING == state) {
                manualDrag = true
                reverseTimer()
            } else if (SCROLL_STATE_IDLE == state) {
                manualDrag = false
                startTimer()
            }
        }

        override fun onPageScrolled(position: Int, offset: Float, pixels: Int) {
            focusedPage = position
            focusedPageVisibilityFraction = offset
            invalidate()
        }
    }

    private val arcReadInnerPaint by lazy {
        Paint().also {
            it.isAntiAlias = true
            it.strokeWidth = lineWidth
            it.style = Paint.Style.FILL
            it.color = dotReadInnerArcColor
        }
    }

    private val dotPaint by lazy {
        Paint().also {
            it.isAntiAlias = true
            it.color = dotUnReadColor
            it.style = Paint.Style.FILL
        }
    }

    private val pathFillPaint by lazy {
        Paint().also {
            it.isAntiAlias = true
            it.color = dotBorderColor
            it.strokeWidth = lineWidth
            it.style = Paint.Style.STROKE
        }
    }

    private val progressAnimator by lazy {
        ValueAnimator().also {
            it.duration = PROGRESS_DURATION
            it.setFloatValues(0f, 1f)
            it.addUpdateListener { invalidate() }
            it.interpolator = LinearInterpolator()
            it.doOnEnd { if (allowTimeoutCallback) viewPager?.next() }
        }
    }

    private val progressReverseAnimator by lazy {
        ValueAnimator().also {
            it.duration = PROGRESS_REVERSE_DURATION
            it.setFloatValues(0f, 0f)
            it.addUpdateListener { invalidate() }
            it.interpolator = AccelerateDecelerateInterpolator()
        }
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(((verticalPadding * 2) + totalDotSpace).toInt(), dotSize.toInt())
    }

    private fun startTimer() {
        cancelTimer()
        cancelReverseTimer()
        animatingDot = focusedPage
        progressAnimator.start()
    }

    private fun reverseTimer() {
        allowTimeoutCallback = false
        val startAngle = this.startAngle
        cancelTimer()
        cancelReverseTimer()
        animatingDot = focusedPage
        progressReverseAnimator.let {
            it.setFloatValues(startAngle, START_ANGLE)
            it.start()
        }
        allowTimeoutCallback = true
    }

    private fun cancelTimer() = progressAnimator.let {
        allowTimeoutCallback = false
        if (it.isRunning) it.cancel()
        allowTimeoutCallback = true
    }

    private fun cancelReverseTimer() = progressReverseAnimator.let { if (it.isRunning) it.cancel() }

    fun bindViewPager(viewPager: ViewPager2) {
        this.viewPager = viewPager
        this.viewPager!!.registerOnPageChangeCallback(pageChangeCallback)
        startTimer()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewPager?.unregisterOnPageChangeCallback(pageChangeCallback)
        viewPager = null
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val radius = dotSize / 2f
        for (i in 0 until dotCount) {
            val start = (dotSize * 2 * i) + verticalPadding
            val x = radius + start
            val y = radius
            canvas.drawCircle(x, y, radius, dotPaint)
            drawProgress(canvas, i, start)
        }
    }

    private fun drawProgress(canvas: Canvas, index: Int, left: Float) {
        if (index == focusedPage || index == focusedPage + 1) {

            referenceRect.set(left, 0f, left + dotSize, dotSize)
            referenceRect.addPadding(lineWidth / 2f)

            val focused = focusedPage == index

            canvas.drawArc(
                referenceRect, START_ANGLE, SWEEP_ANGLE, false,
                preparePaint(pathFillPaint, focused, dotBorderColor)
            )

            var start = startAngle
            val sweep: Float

            if (animatingDot != index) {
                start = START_ANGLE
                sweep = SWEEP_ANGLE
            } else {
                sweep = when {
                    isAnimating -> sweepAngle
                    manualDrag -> SWEEP_ANGLE
                    else -> 0f
                }
            }

            canvas.drawArc(
                referenceRect, start, sweep, true,
                preparePaint(arcReadInnerPaint, focused, dotReadInnerArcColor)
            )
        }
    }

    private fun preparePaint(paint: Paint, focused: Boolean, color: Int): Paint {
        val animatedColor = if (focused) {
            argbEvaluator.evaluate(focusedPageVisibilityFraction, color, Color.TRANSPARENT)
        } else {
            argbEvaluator.evaluate(focusedPageVisibilityFraction, Color.TRANSPARENT, color)
        }
        paint.color = animatedColor as Int
        return paint
    }

    companion object {
        const val END_ANGLE = 270f
        const val START_ANGLE = -90f
        const val SWEEP_ANGLE = 360f
        const val INITIAL_COUNT = 1
        const val PROGRESS_DURATION = 4000L
        const val PROGRESS_REVERSE_DURATION = 200L
    }
}

fun Float.toDegree(): Float {
    return ((this % 360) + 360) % 360
}

fun Int.toDegree(): Float {
    return this.toFloat().toDegree()
}

fun ViewPager2.next() {
    this.currentItem = this.currentItem.inc() % this.adapter!!.itemCount
}

fun RectF.addPadding(padding: Float) {
    set(left + padding, top + padding, right - padding, bottom - padding)
}