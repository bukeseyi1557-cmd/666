package com.example.aipet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import kotlin.math.abs

class PetService : Service() {
    private lateinit var windowManager: WindowManager
    private var petView: PetView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var initialX = 0
    private var initialY = 0
    private var touchX = 0f
    private var touchY = 0f
    private var dragging = false
    private val touchSlop by lazy { (8 * resources.displayMetrics.density).toInt() }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(2026, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showPet()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (petView == null) showPet()
        return START_STICKY
    }

    private fun showPet() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请先允许悬浮窗权限", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }
        if (petView != null) return

        val view = PetView(this)
        @Suppress("DEPRECATION")
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            dp(128),
            dp(164),
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(180)
        }
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) dragging = true
                    if (dragging) {
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try { windowManager.updateViewLayout(v, params) } catch (_: Exception) { }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) {
                        v.performClick()
                        openChat()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
        try {
            windowManager.addView(view, params)
            petView = view
            layoutParams = params
        } catch (e: Exception) {
            Toast.makeText(this, "桌宠显示失败：${e.localizedMessage ?: "未知错误"}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun openChat() {
        val intent = Intent(this, ChatActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "pet_service")
                .setContentTitle("小伙伴桌宠正在运行")
                .setContentText("点击此通知管理桌宠")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(openIntent)
                .setOngoing(true)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("小伙伴桌宠正在运行")
                .setContentText("点击此通知管理桌宠")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(openIntent)
                .setOngoing(true)
                .build()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel("pet_service", "桌宠运行状态", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "桌面宠物运行时显示的通知"
                }
            )
        }
    }

    override fun onDestroy() {
        petView?.let { view ->
            try { windowManager.removeView(view) } catch (_: Exception) { }
        }
        petView = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE) else stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
