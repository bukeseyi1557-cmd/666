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

        root.addView(TextView(this).apply {
            text = "小伙伴桌宠 2.0 ✨"
            textSize = 28f
            setTextColor(Color.rgb(68, 53, 86))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }, matchWrap())

        root.addView(TextView(this).apply {
            text = "更清晰的角色素材、更丰富的表情动作，以及支持直接在桌面悬浮窗聊天的升级版。"
            textSize = 15f
            setTextColor(Color.rgb(105, 94, 115))
            setPadding(0, dp(12), 0, dp(20))
        }, matchWrap())

        root.addView(makeButton("开启桌面宠物") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), REQUEST_OVERLAY)
            } else {
                startPetService()
            }
        })
        root.addView(makeButton("关闭桌面宠物") {
            stopService(Intent(this, PetService::class.java))
            Toast.makeText(this, "桌宠已关闭", Toast.LENGTH_SHORT).show()
        })
        root.addView(makeButton("打开完整聊天与 AI 设置") {
            startActivity(Intent(this, ChatActivity::class.java))
        })
        root.addView(makeButton("演示 15 种动作") {
            val started = PetAnimator.playDemoSequence()
            Toast.makeText(this, if (started) "动作演示开始啦～" else "请先开启桌面宠物，再播放动作演示。", Toast.LENGTH_LONG).show()
        })

        root.addView(TextView(this).apply {
            text = "升级内容\n1. 角色素材更清晰，减少手机上发糊的问题。\n2. 单击桌宠：随机表情互动。\n3. 双击桌宠：打开桌面对话悬浮窗，可直接聊天。\n4. 长按桌宠：打开功能菜单，可切换模式、休息、隐藏、演示动作。\n5. 完整聊天页保留，适合更长对话和配置独立 AI。\n\n提示：如果桌面对话无法输入，请检查系统是否允许悬浮窗获取焦点，或在某些系统里关闭“游戏加速/悬浮窗限制”。"
            textSize = 14f
            setTextColor(Color.rgb(99, 89, 108))
            setPadding(0, dp(24), 0, 0)
            setLineSpacing(dp(4).toFloat(), 1.05f)
        }, matchWrap())

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
