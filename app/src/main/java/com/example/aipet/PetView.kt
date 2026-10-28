package com.example.aipet

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import kotlin.math.sin

/** A tiny anime-style character drawn entirely with Canvas; no image assets required. */
class PetView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var frame = 0

    init {
        contentDescription = "小伙伴桌宠，点击聊天，拖动移动"
        isClickable = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val sx = width / 160f
        val sy = height / 190f
        canvas.save()
        canvas.scale(sx, sy)
        frame++
        val bob = (sin(frame / 12.0) * 2.0).toFloat()
        canvas.translate(0f, bob)
        drawCharacter(canvas)
        canvas.restore()
        postInvalidateDelayed(90)
    }

    private fun drawCharacter(c: Canvas) {
        // soft ground shadow
        fill(Color.argb(45, 72, 52, 85))
        c.drawOval(RectF(43f, 176f, 117f, 188f), paint)

        // legs and shoes
        fill(Color.rgb(255, 222, 205))
        roundRect(c, 61f, 137f, 76f, 166f, 7f)
        roundRect(c, 85f, 137f, 100f, 166f, 7f)
        fill(Color.rgb(100, 81, 123))
        roundRect(c, 55f, 158f, 79f, 170f, 6f)
        roundRect(c, 82f, 158f, 106f, 170f, 6f)

        // hair behind head
        fill(Color.rgb(75, 53, 91))
        val backHair = Path().apply {
            moveTo(42f, 75f)
            cubicTo(31f, 42f, 47f, 17f, 79f, 17f)
            cubicTo(112f, 16f, 129f, 42f, 119f, 77f)
            lineTo(111f, 105f)
            lineTo(48f, 105f)
            close()
        }
        c.drawPath(backHair, paint)
        // little side locks
        c.drawOval(RectF(35f, 58f, 53f, 104f), paint)
        c.drawOval(RectF(107f, 56f, 125f, 103f), paint)

        // body / sweater
        fill(Color.rgb(172, 151, 229))
        roundRect(c, 49f, 103f, 111f, 151f, 18f)
        // collar
        fill(Color.rgb(255, 236, 246))
        val collar = Path().apply {
            moveTo(67f, 105f); lineTo(80f, 119f); lineTo(94f, 105f)
            lineTo(89f, 101f); lineTo(80f, 110f); lineTo(72f, 101f); close()
        }
        c.drawPath(collar, paint)
        // sleeves
        fill(Color.rgb(172, 151, 229))
        roundRect(c, 42f, 111f, 58f, 137f, 8f)
        roundRect(c, 102f, 111f, 118f, 137f, 8f)
        fill(Color.rgb(255, 222, 205))
        c.drawCircle(50f, 138f, 7f, paint)
        c.drawCircle(110f, 138f, 7f, paint)

        // face and ears
        fill(Color.rgb(255, 222, 205))
        c.drawCircle(48f, 69f, 8f, paint)
        c.drawCircle(112f, 69f, 8f, paint)
        c.drawOval(RectF(47f, 35f, 113f, 108f), paint)

        // fringe
        fill(Color.rgb(75, 53, 91))
        val fringe = Path().apply {
            moveTo(44f, 63f)
            cubicTo(39f, 33f, 56f, 20f, 79f, 21f)
            cubicTo(103f, 20f, 119f, 38f, 115f, 63f)
            lineTo(105f, 52f)
            cubicTo(97f, 62f, 91f, 61f, 82f, 49f)
            cubicTo(72f, 63f, 62f, 61f, 54f, 52f)
            close()
        }
        c.drawPath(fringe, paint)
        // hair shine
        paint.color = Color.rgb(133, 105, 151)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.2f
        c.drawArc(RectF(55f, 28f, 91f, 52f), 205f, 105f, false, paint)
        paint.style = Paint.Style.FILL

        // eyes
        fill(Color.rgb(64, 48, 76))
        c.drawOval(RectF(60f, 66f, 70f, 80f), paint)
        c.drawOval(RectF(90f, 66f, 100f, 80f), paint)
        fill(Color.WHITE)
        c.drawCircle(67f, 69f, 2.5f, paint)
        c.drawCircle(97f, 69f, 2.5f, paint)
        // blush
        fill(Color.rgb(247, 157, 177))
        paint.alpha = 150
        c.drawOval(RectF(52f, 82f, 66f, 89f), paint)
        c.drawOval(RectF(94f, 82f, 108f, 89f), paint)
        paint.alpha = 255
        // smile
        paint.color = Color.rgb(150, 79, 103)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.strokeCap = Paint.Cap.ROUND
        c.drawArc(RectF(73f, 79f, 88f, 91f), 15f, 150f, false, paint)
        paint.style = Paint.Style.FILL

        // tiny hair bow
        fill(Color.rgb(255, 183, 204))
        val bowLeft = Path().apply { moveTo(102f, 35f); lineTo(116f, 26f); lineTo(113f, 42f); close() }
        val bowRight = Path().apply { moveTo(112f, 35f); lineTo(126f, 39f); lineTo(114f, 47f); close() }
        c.drawPath(bowLeft, paint)
        c.drawPath(bowRight, paint)
        fill(Color.rgb(255, 230, 239))
        c.drawCircle(113f, 37f, 3f, paint)

        // small heart on sweater
        fill(Color.rgb(255, 230, 239))
        val heart = Path().apply {
            moveTo(80f, 132f)
            cubicTo(70f, 124f, 68f, 136f, 80f, 142f)
            cubicTo(92f, 136f, 90f, 124f, 80f, 132f)
        }
        c.drawPath(heart, paint)
    }

    private fun fill(color: Int) {
        paint.color = color
        paint.alpha = Color.alpha(color)
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 1f
    }

    private fun roundRect(c: Canvas, left: Float, top: Float, right: Float, bottom: Float, radius: Float) {
        c.drawRoundRect(RectF(left, top, right, bottom), radius, radius, paint)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
