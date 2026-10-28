package com.example.aipet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import java.lang.ref.WeakReference
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

enum class PetMood(val frameIndex: Int) {
    IDLE(0), BLINK(1), HAPPY(2), SHY(3), SURPRISED(4), ANGRY(5), EMBARRASSED(6),
    SLEEP(7), EAT(8), DRINK(9), WALK(10), RUN(11), JUMP(12), SIT(13), LIE(14),
    THINKING(0), SAD(6)
}

/** A tiny bridge so chat and the floating service can share the pet's current reaction. */
object PetAnimator {
    private var activeView: WeakReference<PetView>? = null

    internal fun attach(view: PetView) {
        activeView = WeakReference(view)
    }

    internal fun detach(view: PetView) {
        if (activeView?.get() === view) activeView = null
    }

    fun react(mood: PetMood, bubble: String = "", durationMs: Long = 1800L) {
        activeView?.get()?.showMood(mood, bubble, durationMs)
    }

    fun reactToTap() {
        activeView?.get()?.reactToTap()
    }

    fun toggleSleep(): Boolean? = activeView?.get()?.toggleSleep()

    fun playDemoSequence(): Boolean = activeView?.get()?.playDemoSequence() ?: false

    fun reactToText(text: String, isReply: Boolean = false) {
        val s = text.lowercase()
        val reaction = when {
            listOf("晚安", "睡觉", "睡了", "困了", "好困").any { s.contains(it) } -> PetMood.SLEEP to "那就休息一下吧～"
            listOf("走两步", "走路", "散步").any { s.contains(it) } -> PetMood.WALK to "哒哒哒～"
            listOf("跑步", "跑起来", "冲呀").any { s.contains(it) } -> PetMood.RUN to "冲呀！"
            listOf("跳一下", "跳舞", "蹦蹦跳跳").any { s.contains(it) } -> PetMood.JUMP to "看我的！"
            listOf("坐下", "坐一会", "坐好").any { s.contains(it) } -> PetMood.SIT to "乖乖坐好～"
            listOf("躺下", "躺一会", "趴下").any { s.contains(it) } -> PetMood.LIE to "躺平一下～"
            listOf("生气", "气死", "烦死", "讨厌").any { s.contains(it) } -> PetMood.ANGRY to "哼！我听着呢。"
            listOf("害羞", "夸我", "可爱", "喜欢你").any { s.contains(it) } -> PetMood.SHY to "才、才没有脸红呢！"
            listOf("惊讶", "吓一跳", "真的假的").any { s.contains(it) } -> PetMood.SURPRISED to "欸？！真的嘛？"
            listOf("难过", "伤心", "孤独", "委屈", "不开心", "压力").any { s.contains(it) } -> PetMood.SAD to "我在这里陪你。"
            listOf("开心", "高兴", "成功", "好消息", "谢谢").any { s.contains(it) } -> PetMood.HAPPY to "耶！真好～"
            listOf("吃饭", "吃东西", "饿了", "零食").any { s.contains(it) } -> PetMood.EAT to "一起补充能量！"
            listOf("喝水", "喝饮料", "奶茶").any { s.contains(it) } -> PetMood.DRINK to "咕噜咕噜～"
            isReply -> PetMood.HAPPY to "嗯嗯，我在听～"
            else -> PetMood.THINKING to "让我想想…"
        }
        react(reaction.first, reaction.second, if (reaction.first == PetMood.SLEEP) 2600L else 1900L)
    }
}

/** Animated anime-style floating companion. The 5x3 sprite atlas is bundled locally. */
class PetView(context: Context) : View(context) {
    private val atlas: Bitmap? = BitmapFactory.decodeResource(resources, R.drawable.pet_atlas)
    private val spritePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 255, 255) }
    private val bubbleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(202, 219, 255)
        style = Paint.Style.STROKE
        strokeWidth = dp(1.2f)
    }
    private val bubbleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(62, 68, 105)
        textSize = dp(11.5f)
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(34, 55, 52, 83) }
    private var frameCount = 0
    private var mood = PetMood.IDLE
    private var moodUntil = 0L
    private var bubbleText = ""
    private var bubbleUntil = 0L
    private var sleeping = false
    private var nextIdleActionAt = SystemClock.uptimeMillis() + 2800L
    private val demoHandler = Handler(Looper.getMainLooper())
    private val demoActions = listOf(
        PetMood.BLINK to "眨眨眼～", PetMood.HAPPY to "开心！", PetMood.SHY to "有点害羞…",
        PetMood.SURPRISED to "欸？！", PetMood.ANGRY to "哼！", PetMood.EMBARRASSED to "抱抱…",
        PetMood.THINKING to "让我想想…", PetMood.EAT to "吃点东西～", PetMood.DRINK to "喝口水～",
        PetMood.WALK to "散步时间", PetMood.RUN to "冲呀！", PetMood.JUMP to "跳一下！",
        PetMood.SIT to "坐下休息", PetMood.LIE to "躺平一下", PetMood.SLEEP to "晚安～"
    )
    private var demoIndex = 0
    private var demoRunning = false
    private val demoRunnable = object : Runnable {
        override fun run() {
            if (!demoRunning) return
            if (demoIndex >= demoActions.size) {
                demoRunning = false
                showMood(PetMood.IDLE, "演示完成～", 1500L)
                return
            }
            val (demoMood, text) = demoActions[demoIndex++]
            showMood(demoMood, text, 900L)
            demoHandler.postDelayed(this, 1050L)
        }
    }

    init {
        contentDescription = "二次元小伙伴桌宠：单击互动，双击聊天，长按休息或唤醒，拖动移动"
        isClickable = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        PetAnimator.attach(this)
        postInvalidateDelayed(120L)
    }

    override fun onDetachedFromWindow() {
        demoRunning = false
        demoHandler.removeCallbacks(demoRunnable)
        PetAnimator.detach(this)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        frameCount++
        val now = SystemClock.uptimeMillis()
        updateMood(now)

        val size = min(width - dp(6f), height - dp(42f)).coerceAtLeast(dp(64f))
        val left = (width - size) / 2f
        val top = height - size - dp(15f)
        val bob = if (!sleeping && mood in listOf(PetMood.IDLE, PetMood.BLINK, PetMood.HAPPY, PetMood.SHY)) {
            (sin(frameCount / 8.0) * dp(1.5f)).toFloat()
        } else 0f

        canvas.drawOval(
            RectF(width * 0.25f, height - dp(15f), width * 0.75f, height - dp(5f)),
            shadowPaint
        )
        val activeMood = if (sleeping) PetMood.SLEEP else mood
        drawSprite(canvas, activeMood.frameIndex, RectF(left, top + bob, left + size, top + size + bob))
        if (bubbleText.isNotBlank() && now < bubbleUntil) drawBubble(canvas, bubbleText)
        postInvalidateDelayed(120L)
    }

    private fun updateMood(now: Long) {
        if (!sleeping && mood != PetMood.IDLE && moodUntil > 0L && now >= moodUntil) {
            mood = PetMood.IDLE
            moodUntil = 0L
        }
        if (now >= bubbleUntil) bubbleText = ""
        if (!sleeping && mood == PetMood.IDLE && now >= nextIdleActionAt) {
            val choice = listOf(PetMood.BLINK, PetMood.BLINK, PetMood.THINKING, PetMood.HAPPY).random()
            mood = choice
            moodUntil = now + when (choice) {
                PetMood.BLINK -> 420L
                PetMood.THINKING -> 1250L
                else -> 900L
            }
            bubbleText = when (choice) {
                PetMood.THINKING -> "嗯…让我想想"
                PetMood.HAPPY -> "今天也加油～"
                else -> ""
            }
            bubbleUntil = if (bubbleText.isBlank()) 0L else now + 1450L
            nextIdleActionAt = now + Random.nextLong(4200L, 8200L)
        }
    }

    private fun drawSprite(canvas: Canvas, frame: Int, destination: RectF) {
        val bitmap = atlas ?: return
        val safeFrame = frame.coerceIn(0, 14)
        val source = Rect((safeFrame % 5) * 256, (safeFrame / 5) * 256, (safeFrame % 5 + 1) * 256, (safeFrame / 5 + 1) * 256)
        canvas.drawBitmap(bitmap, source, destination, spritePaint)
    }

    private fun drawBubble(canvas: Canvas, rawText: String) {
        val text = if (rawText.length > 10) rawText.take(9) + "…" else rawText
        val maxWidth = width - dp(8f)
        val bubbleWidth = (bubbleTextPaint.measureText(text) + dp(20f)).coerceAtMost(maxWidth)
        val bubbleHeight = dp(28f)
        val left = (width - bubbleWidth) / 2f
        val top = dp(2f)
        val rect = RectF(left, top, left + bubbleWidth, top + bubbleHeight)
        canvas.drawRoundRect(rect, dp(12f), dp(12f), bubblePaint)
        canvas.drawRoundRect(rect, dp(12f), dp(12f), bubbleBorderPaint)
        val tail = Path().apply {
            moveTo(width / 2f - dp(4f), top + bubbleHeight - dp(1f))
            lineTo(width / 2f, top + bubbleHeight + dp(5f))
            lineTo(width / 2f + dp(4f), top + bubbleHeight - dp(1f))
            close()
        }
        canvas.drawPath(tail, bubblePaint)
        canvas.drawPath(tail, bubbleBorderPaint)
        val baseline = top + (bubbleHeight - bubbleTextPaint.ascent() - bubbleTextPaint.descent()) / 2f
        canvas.drawText(text, width / 2f, baseline, bubbleTextPaint)
    }

    internal fun showMood(newMood: PetMood, bubble: String, durationMs: Long) {
        if (sleeping && newMood != PetMood.HAPPY && newMood != PetMood.SURPRISED) return
        mood = newMood
        moodUntil = SystemClock.uptimeMillis() + durationMs.coerceAtLeast(250L)
        bubbleText = bubble
        bubbleUntil = if (bubble.isBlank()) 0L else SystemClock.uptimeMillis() + durationMs.coerceAtLeast(700L)
        nextIdleActionAt = moodUntil + Random.nextLong(1800L, 4200L)
        invalidate()
    }

    internal fun reactToTap() {
        if (sleeping) {
            toggleSleep()
            return
        }
        val reaction = listOf(
            PetMood.HAPPY to "嘿嘿！",
            PetMood.SHY to "被你发现啦～",
            PetMood.SURPRISED to "欸？你戳我啦！",
            PetMood.BLINK to "眨眨眼～"
        ).random()
        showMood(reaction.first, reaction.second, 1700L)
    }

    internal fun playDemoSequence(): Boolean {
        if (sleeping) toggleSleep()
        demoRunning = true
        demoIndex = 0
        demoHandler.removeCallbacks(demoRunnable)
        demoHandler.post(demoRunnable)
        return true
    }

    internal fun toggleSleep(): Boolean {
        sleeping = !sleeping
        val now = SystemClock.uptimeMillis()
        if (sleeping) {
            mood = PetMood.SLEEP
            moodUntil = 0L
            bubbleText = "Zzz…"
            bubbleUntil = now + 2400L
        } else {
            mood = PetMood.HAPPY
            moodUntil = now + 1800L
            bubbleText = "我醒啦～"
            bubbleUntil = now + 2100L
            nextIdleActionAt = now + 4000L
        }
        invalidate()
        return sleeping
    }

    override fun performClick(): Boolean {
        super.performClick()
        reactToTap()
        return true
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
