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

class DotIndicator : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var currentPage = 0
    private var previousPage = -1

    private var dotSize = 30 // This is a px value
    private var lineWidth = 4f

    private val startAngle: Float
        get() = floatEvaluator.evaluate(progressAnimator.animatedFraction, START_ANGLE, END_ANGLE)

    private val sweepAngle: Float
        get() = (SWEEP_ANGLE - startAngle + START_ANGLE)

    private var dotReadColor = Color.parseColor("#6177E5")
    private var dotPathColor = Color.parseColor("#4D6177E5")
    private var dotUnReadColor = Color.parseColor("#22262F")
    private var dotReadInnerArcColor = Color.parseColor("#B36177E5")

    private val referenceRect by lazy { RectF() }
    private val argbEvaluator by lazy { ArgbEvaluator() }
    private val floatEvaluator by lazy { FloatEvaluator() }

    private val arcReadPaint by lazy {
        Paint().also {
            it.isAntiAlias = true
            it.color = dotReadColor
            it.strokeWidth = lineWidth
            it.style = Paint.Style.STROKE
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
            it.color = dotReadColor
            it.strokeWidth = lineWidth
            it.style = Paint.Style.STROKE
        }
    }

    private val progressAnimator by lazy {
        ValueAnimator().also {
            it.doOnEnd { next() }
            it.duration = PROGRESS_DURATION
            it.setFloatValues(0f, 1f)
            it.addUpdateListener { invalidate() }
            it.interpolator = LinearInterpolator()
        }
    }

    private val transitionAnimator by lazy {
        ValueAnimator().also {
            it.doOnEnd { animateProgress() }
            it.setFloatValues(0f, 1f)
            it.duration = TRANSITION_DURATION
            it.addUpdateListener { invalidate() }
            it.interpolator = LinearInterpolator()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(dotSize * 5, dotSize)
    }

    fun start() {
        animateDot()
    }

    private fun animateDot() {
        if (transitionAnimator.isRunning) transitionAnimator.cancel()
        transitionAnimator.start()
    }

    private fun animateProgress() {
        if (progressAnimator.isRunning) progressAnimator.cancel()
        progressAnimator.start()
    }

    private fun next() {
        previousPage = currentPage
        currentPage = currentPage.inc() % INDICATOR_COUNT
        animateDot()
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

    private fun drawProgress(canvas: Canvas, index: Int, left: Int) {
        if (index != currentPage && index != previousPage) return

        val padding = lineWidth / 2f

        referenceRect.set(
            left + padding, padding,
            left + dotSize - padding, dotSize - padding
        )

        val forward = currentPage == index

        canvas.drawArc(
            referenceRect,
            START_ANGLE,
            SWEEP_ANGLE,
            false,
            preparePaint(pathFillPaint, forward, dotPathColor)
        )

        var start = startAngle
        var sweep = sweepAngle

        if (forward && transitionAnimator.isRunning) {
            start = START_ANGLE
            sweep = SWEEP_ANGLE
        }

        canvas.drawArc(
            referenceRect,
            start,
            sweep,
            true,
            preparePaint(arcReadInnerPaint, forward, dotReadInnerArcColor)
        )
        canvas.drawArc(
            referenceRect,
            start,
            sweep,
            false,
            preparePaint(arcReadPaint, forward, dotReadColor)
        )
    }

    private fun preparePaint(paint: Paint, forward: Boolean, color: Int): Paint {
        val animatedColor = if (forward) {
            argbEvaluator.evaluate(transitionAnimator.animatedFraction, Color.TRANSPARENT, color)
        } else {
            argbEvaluator.evaluate(transitionAnimator.animatedFraction, color, Color.TRANSPARENT)
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
        const val TRANSITION_DURATION = 300L
    }
}

fun Float.toDegree(): Float {
    return ((this % 360) + 360) % 360
}

fun Int.toDegree(): Float {
    return this.toFloat().toDegree()
}