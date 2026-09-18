package com.riyaz.rssclipboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class FloatingBubbleView(context: Context, private val onClickAction: () -> Unit) : View(context) {
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var downX = 0f
    private var downY = 0f
    private var moved = false

    init {
        isClickable = true
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        circlePaint.color = 0xFF202124.toInt()
        circlePaint.setShadowLayer(12f, 0f, 5f, 0x55000000)
        iconPaint.color = 0xFFFFFFFF.toInt()
        iconPaint.style = Paint.Style.STROKE
        iconPaint.strokeWidth = 5f
        iconPaint.strokeCap = Paint.Cap.ROUND
        iconPaint.strokeJoin = Paint.Join.ROUND
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(dp(58), dp(58))
    }

    override fun onDraw(canvas: Canvas) {
        val c = width / 2f
        canvas.drawCircle(c, c, dp(27).toFloat(), circlePaint)
        val left = c - dp(12)
        val top = c - dp(15)
        val right = c + dp(12)
        val bottom = c + dp(15)
        canvas.drawRoundRect(RectF(left, top, right, bottom), dp(4).toFloat(), dp(4).toFloat(), iconPaint)
        canvas.drawLine(c - dp(6), top - dp(3), c + dp(6), top - dp(3), iconPaint)
        canvas.drawLine(c - dp(6), top + dp(8), c + dp(6), top + dp(8), iconPaint)
        canvas.drawLine(c - dp(6), top + dp(16), c + dp(5), top + dp(16), iconPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; moved = false; return true }
            MotionEvent.ACTION_MOVE -> {
                if (abs(event.rawX - downX) > dp(8) || abs(event.rawY - downY) > dp(8)) moved = true
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!moved) onClickAction()
                performClick()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
