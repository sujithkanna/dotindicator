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

    private var currentPage = 0
    private lateinit var viewPager: ViewPager2
    private var currentPageVisibleFraction = 1f
    private var pageScrollState = SCROLL_STATE_IDLE

    private var dotSize = 30 // This is a px value
    private var lineWidth = dotSize / 8f

    private val startAngle: Float
        get() = floatEvaluator.evaluate(progressAnimator.animatedFraction, START_ANGLE, END_ANGLE)

    private val sweepAngle: Float
        get() = (SWEEP_ANGLE - startAngle + START_ANGLE)

    private var dotBorderColor = Color.parseColor("#6177E5")
    private var dotUnReadColor = Color.parseColor("#22262F")
    private var dotReadInnerArcColor = Color.parseColor("#B36177E5")

    private val referenceRect by lazy { RectF() }
    private val argbEvaluator by lazy { ArgbEvaluator() }
    private val floatEvaluator by lazy { FloatEvaluator() }

    private var allowTimeoutCallback = true

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
            it.doOnEnd {
                if (allowTimeoutCallback) viewPager.next()
            }
            it.duration = PROGRESS_DURATION
            it.setFloatValues(0f, 1f)
            it.addUpdateListener { invalidate() }
            it.interpolator = LinearInterpolator()
        }
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(((INDICATOR_COUNT * 2) - 1) * dotSize, dotSize)
    }

    private fun startTimer() {
        cancelTimer()
        progressAnimator.start()
    }

    private fun cancelTimer() {
        allowTimeoutCallback = false
        if (progressAnimator.isRunning) progressAnimator.cancel()
        allowTimeoutCallback = true
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val radius = dotSize / 2f
        for (i in 0 until INDICATOR_COUNT) {
            val start = dotSize * 2 * i
            val x = radius + start
            val y = radius
            canvas.drawCircle(x, y, radius, dotPaint)
            drawProgress(canvas, i, start)
        }
    }

    fun bindViewPager(viewPager: ViewPager2) {
        this.viewPager = viewPager
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                pageScrollState = state
                when (state) {
                    SCROLL_STATE_DRAGGING -> cancelTimer()
                    SCROLL_STATE_IDLE -> startTimer()
                }
            }

            override fun onPageScrolled(position: Int, offset: Float, pixels: Int) {
                super.onPageScrolled(position, offset, pixels)
                currentPage = position
                currentPageVisibleFraction = offset
                invalidate()
            }
        })
        requestLayout()
        startTimer()
    }

    private fun drawProgress(canvas: Canvas, index: Int, left: Int) {
        if (index == currentPage || index == currentPage + 1) {
            val padding = lineWidth / 2f

            referenceRect.set(
                left + padding, padding,
                left + dotSize - padding, dotSize - padding
            )

            val first = currentPage == index

            canvas.drawArc(
                referenceRect,
                START_ANGLE,
                SWEEP_ANGLE,
                false,
                preparePaint(pathFillPaint, first, dotBorderColor)
            )

            var start = startAngle
            var sweep = sweepAngle
            if (pageScrollState != SCROLL_STATE_IDLE) {
                start = START_ANGLE
                sweep = SWEEP_ANGLE
            }

            canvas.drawArc(
                referenceRect,
                start,
                sweep,
                true,
                preparePaint(arcReadInnerPaint, first, dotReadInnerArcColor)
            )
        }
    }

    private fun preparePaint(paint: Paint, first: Boolean, color: Int): Paint {
        val animatedColor = if (first) {
            argbEvaluator.evaluate(currentPageVisibleFraction, color, Color.TRANSPARENT)
        } else {
            argbEvaluator.evaluate(currentPageVisibleFraction, Color.TRANSPARENT, color)
        }
        paint.color = animatedColor as Int
        return paint
    }

    companion object {
        const val END_ANGLE = 270f
        const val START_ANGLE = -90f
        const val SWEEP_ANGLE = 360f
        const val INDICATOR_COUNT = 3
        const val PROGRESS_DURATION = 5000L
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