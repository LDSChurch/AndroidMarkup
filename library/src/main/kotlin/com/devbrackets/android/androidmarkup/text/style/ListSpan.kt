package com.devbrackets.android.androidmarkup.text.style

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.text.Layout
import android.text.style.LeadingMarginSpan

class ListSpan @JvmOverloads constructor(val type: ListSpan.Type = ListSpan.Type.BULLET, protected val gapWidth: Int = ListSpan.DEFAULT_GAP_WIDTH, protected val bulletRadius: Int = ListSpan.DEFAULT_BULLET_RADIUS) : LeadingMarginSpan {

    enum class Type {
        BULLET,
        NUMERICAL
    }

    //Used for the Numerical list
    private var number = 0
    private var lastBaseline = -1

    constructor(gapWidth: Int) : this(Type.BULLET, gapWidth, DEFAULT_BULLET_RADIUS) { }

    override fun getLeadingMargin(first: Boolean): Int {
        val numberOffset = number.toString().length;
        return 2 * bulletRadius + gapWidth + (if (numberOffset == 1) numberOffset * 20 else numberOffset * 10)
    }

    override fun drawLeadingMargin(canvas: Canvas, paint: Paint, marginPosition: Int, direction: Int, top: Int, baseline: Int, bottom: Int, text: CharSequence, start: Int, end: Int, first: Boolean, layout: Layout) {
        val newItem = start == 0 || text[start -1].equals('\n');
        if (!newItem) {
            return;
        }

        //Cache the style so our changes don't affect others
        val style = paint.style

        if (type == Type.BULLET) {
            drawBulletMargin(canvas, paint, marginPosition, direction, top, baseline, bottom)
        } else {
            drawNumericalMargin(canvas, paint, marginPosition, direction, top, baseline, bottom)
        }

        paint.style = style
    }

    protected fun drawBulletMargin(canvas: Canvas, paint: Paint, marginPosition: Int, direction: Int, top: Int, baseline: Int, bottom: Int) {
        paint.style = Paint.Style.FILL
        val verticalCenter = (top + bottom) / 2

        //If we don't support hardware acceleration just draw the circle
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB || !canvas.isHardwareAccelerated) {
            canvas.drawCircle((marginPosition + direction * bulletRadius).toFloat(), verticalCenter.toFloat(), bulletRadius.toFloat(), paint)
            return
        }

        //Since we support hardware acceleration make sure we have a vector to draw
        if (bulletPath == null) {
            bulletPath = Path()
            // Bullet is slightly better to avoid aliasing artifacts on mdpi devices.
            bulletPath?.addCircle(0.0f, 0.0f, bulletRadius.toFloat(), Path.Direction.CW)
        }

        canvas.save()
        canvas.translate((marginPosition + direction * bulletRadius).toFloat(), verticalCenter.toFloat())
        canvas.drawPath(bulletPath, paint)
        canvas.restore()
    }

    protected fun drawNumericalMargin(canvas: Canvas, paint: Paint, marginPosition: Int, direction: Int, top: Int, baseline: Int, bottom: Int) {
        if (baseline > lastBaseline) {
            lastBaseline = baseline
            number++
        } else if (baseline < lastBaseline) {
            lastBaseline = baseline
            number = 1
        }

        canvas.drawText("$number.", (marginPosition + direction * bulletRadius).toFloat(), baseline.toFloat(), paint)
    }

    companion object {
        val DEFAULT_GAP_WIDTH = 40 //px
        val DEFAULT_BULLET_RADIUS = 4 //px

        private var bulletPath: Path? = null
    }
}