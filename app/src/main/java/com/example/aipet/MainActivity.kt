package com.example.aipet

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    companion object {
        private const val REQUEST_OVERLAY = 4101
        private const val REQUEST_NOTIFICATIONS = 4102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        askNotificationPermissionIfNeeded()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(30), dp(24), dp(24))
            setBackgroundColor(Color.rgb(255, 248, 252))
            gravity = Gravity.TOP
        }

        val title = TextView(this).apply {
            text = "小伙伴桌宠 ✨"
            textSize = 28f
            setTextColor(Color.rgb(68, 53, 86))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        root.addView(title, matchWrap())

        val subtitle = TextView(this).apply {
            text = "一个住在手机桌面上的二次元小伙伴。先免费运行基础版，再按需接入聊天服务。"
            textSize = 15f
            setTextColor(Color.rgb(105, 94, 115))
            setPadding(0, dp(12), 0, dp(20))
        }
        root.addView(subtitle, matchWrap())

        val start = makeButton("开启桌面宠物") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_OVERLAY)
            } else {
                startPetService()
            }
        }
        root.addView(start)

        val stop = makeButton("关闭桌面宠物") {
            stopService(Intent(this, PetService::class.java))
            Toast.makeText(this, "桌宠已关闭", Toast.LENGTH_SHORT).show()
        }
        root.addView(stop)

        val chat = makeButton("打开聊天与 AI 设置") {
            startActivity(Intent(this, ChatActivity::class.java))
        }
        root.addView(chat)

        val demo = makeButton("演示她的 15 种动作 ✨") {
            val started = PetAnimator.playDemoSequence()
            Toast.makeText(
                this,
                if (started) "动作演示开始啦！看看她的表情变化～" else "请先开启桌面宠物，再播放动作演示。",
                Toast.LENGTH_LONG
            ).show()
        }
        root.addView(demo)

        val note = TextView(this).apply {
            text = "使用说明\n1. 首次开启时，请允许“显示在其他应用上层”。\n2. 单击她会有表情反应；双击打开聊天；长按进入休息/唤醒；拖动可以移动位置。\n3. 点击“演示她的 15 种动作”可播放动作图鉴。\n4. 在聊天中试试输入“害羞”“跳一下”“吃东西”“喝水”“走两步”“晚安”等词，她会做出对应动作。\n5. 免费本地模式不调用网络；独立 AI 模式需要自行配置 API。\n\n提示：部分手机需要在系统设置中允许后台运行或关闭电池优化，才能更稳定地保留悬浮宠物。"
            textSize = 14f
            setTextColor(Color.rgb(99, 89, 108))
            setPadding(0, dp(24), 0, 0)
            setLineSpacing(dp(4).toFloat(), 1.05f)
        }
        root.addView(note, matchWrap())

        setContentView(root)
    }

    private fun startPetService() {
        val intent = Intent(this, PetService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        Toast.makeText(this, "正在召唤小伙伴…", Toast.LENGTH_SHORT).show()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                startPetService()
            } else {
                Toast.makeText(this, "需要悬浮窗权限才能显示桌宠", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }

    private fun makeButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 15f
        isAllCaps = false
        setTextColor(Color.rgb(67, 49, 87))
        setOnClickListener { action() }
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52))
        lp.bottomMargin = dp(10)
        layoutParams = lp
    }

    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
