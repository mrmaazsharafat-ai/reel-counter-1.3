package com.reelcounter.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/** Pichle 7 din ka simple bar chart. data = (label, value), purana pehle. */
class BarChartView(context: Context) : View(context) {

    var data: List<Pair<String, Int>> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private val d = resources.displayMetrics.density
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 11f * d
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            (170 * d).toInt()
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (data.isEmpty()) return
        val max = (data.maxOf { it.second }).coerceAtLeast(1)
        val labelH = 22 * d
        val valueH = 18 * d
        val chartH = height - labelH - valueH
        val slot = width / data.size.toFloat()
        val barW = slot * 0.5f

        data.forEachIndexed { i, (label, value) ->
            val cx = slot * i + slot / 2
            val h = if (value == 0) 3 * d else chartH * value / max
            val top = valueH + (chartH - h)
            val isToday = i == data.lastIndex

            barPaint.color = if (isToday) 0xFFFF3B6B.toInt() else 0xFF3A3A4A.toInt()
            canvas.drawRoundRect(
                RectF(cx - barW / 2, top, cx + barW / 2, valueH + chartH),
                6 * d, 6 * d, barPaint
            )

            textPaint.color = Color.WHITE
            canvas.drawText(value.toString(), cx, top - 5 * d, textPaint)
            textPaint.color = 0xFF9A9AA8.toInt()
            canvas.drawText(label, cx, height - 5 * d, textPaint)
        }
    }
}
