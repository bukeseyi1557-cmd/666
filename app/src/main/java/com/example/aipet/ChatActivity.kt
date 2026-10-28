package com.example.aipet

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import org.json.JSONObject

class ChatActivity : Activity() {
    private lateinit var messageColumn: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var input: EditText
    private lateinit var modeLabel: TextView
    private var mode = PetChatEngine.Mode.LOCAL
    private var openedSettingsForApi = false
    private val history = mutableListOf<JSONObject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        addMessage("小伙伴", "欢迎回来～现在支持更丰富的互动了：单击表情互动、双击桌面对话、长按桌宠打开菜单。你也可以在这里进行完整聊天和 AI 设置。", false)
        PetAnimator.react(PetMood.HAPPY, "欢迎回来～", 1300L)
    }

    override fun onResume() {
        super.onResume()
        if (openedSettingsForApi) {
            openedSettingsForApi = false
            if (!ApiKeyStore.load(this).isNullOrBlank()) {
                mode = PetChatEngine.Mode.API
                updateModeLabel()
                Toast.makeText(this, "已切换到独立 AI 模式", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(10))
            setBackgroundColor(Color.rgb(255, 248, 252))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "和小伙伴聊天 ✨"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(68, 53, 86))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(Button(this).apply {
            text = "AI 设置"
            isAllCaps = false
            setOnClickListener { openSettings() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(44)))
        root.addView(header, matchWrap())

        modeLabel = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.rgb(115, 96, 130))
            setPadding(dp(2), dp(5), dp(2), dp(5))
        }
        root.addView(modeLabel, matchWrap())

        val modeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        modeRow.addView(Button(this).apply {
            text = "免费本地"
            isAllCaps = false
            setOnClickListener { mode = PetChatEngine.Mode.LOCAL; updateModeLabel() }
        }, LinearLayout.LayoutParams(0, dp(44), 1f))
        modeRow.addView(Button(this).apply {
            text = "ChatGPT 网页"
            isAllCaps = false
            setOnClickListener { openChatGptWeb() }
        }, LinearLayout.LayoutParams(0, dp(44), 1f))
        modeRow.addView(Button(this).apply {
            text = "独立 AI"
            isAllCaps = false
            setOnClickListener { selectApiMode() }
        }, LinearLayout.LayoutParams(0, dp(44), 1f))
        root.addView(modeRow, matchWrap())

        val tips = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            val tipsContainer = LinearLayout(this@ChatActivity).apply { orientation = LinearLayout.HORIZONTAL }
            PetChatEngine.suggestions().forEach { suggestion ->
                tipsContainer.addView(TextView(this@ChatActivity).apply {
                    text = suggestion
                    textSize = 13f
                    setTextColor(Color.rgb(72, 79, 117))
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    background = chipBackground()
                    setOnClickListener { input.setText(suggestion); input.setSelection(input.text.length) }
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        rightMargin = dp(8)
                    }
                })
            }
            addView(tipsContainer)
        }
        root.addView(tips, matchWrap())

        scrollView = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, dp(8), 0, dp(8))
        }
        messageColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(2), dp(2), dp(10))
        }
        scrollView.addView(messageColumn, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(scrollView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        input = EditText(this).apply {
            hint = "试试说：夸夸我、安慰我、跳一下、晚安…"
            textSize = 15f
            minHeight = dp(48)
            maxLines = 4
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            imeOptions = EditorInfo.IME_ACTION_SEND
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
            }
        }
        composer.addView(input, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        composer.addView(Button(this).apply {
            text = "发送"
            isAllCaps = false
            setOnClickListener { sendMessage() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(50)))
        root.addView(composer, matchWrap())

        setContentView(root)
        updateModeLabel()
    }

    private fun updateModeLabel() {
        modeLabel.text = if (mode == PetChatEngine.Mode.LOCAL) {
            "当前模式：免费本地陪聊（固定规则回复，不调用网络）"
        } else {
            "当前模式：独立 AI 接口（消息将发送给你配置的服务商）"
        }
    }

    private fun openChatGptWeb() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com/")))
        } catch (_: Exception) {
            Toast.makeText(this, "无法打开浏览器，请检查系统应用", Toast.LENGTH_LONG).show()
        }
    }

    private fun selectApiMode() {
        if (ApiKeyStore.load(this).isNullOrBlank()) {
            openedSettingsForApi = true
            startActivity(Intent(this, AiSettingsActivity::class.java))
            Toast.makeText(this, "先配置接口地址、模型和 API Key", Toast.LENGTH_LONG).show()
        } else {
            mode = PetChatEngine.Mode.API
            updateModeLabel()
        }
    }

    private fun openSettings() {
        openedSettingsForApi = true
        startActivity(Intent(this, AiSettingsActivity::class.java))
    }

    private fun sendMessage() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return
        input.setText("")
        addMessage("你", text, true)
        history.add(JSONObject().put("role", "user").put("content", text))
        PetAnimator.reactToText(text)
        addMessage("小伙伴", "正在思考…", false, typing = true)
        PetChatEngine.requestReply(this, text, mode, history.takeLast(10)) { result ->
            removeLastTypingIndicator()
            addMessage("小伙伴", result.reply, false)
            if (result.ok) history.add(JSONObject().put("role", "assistant").put("content", result.reply))
            if (!result.ok && mode == PetChatEngine.Mode.API) {
                mode = PetChatEngine.Mode.LOCAL
                updateModeLabel()
            }
            PetAnimator.reactToText(result.reply, isReply = true)
        }
    }

    private fun removeLastTypingIndicator() {
        if (messageColumn.childCount <= 0) return
        val last = messageColumn.getChildAt(messageColumn.childCount - 1)
        if (last.tag == "typing") messageColumn.removeView(last)
    }

    private fun addMessage(name: String, text: String, fromUser: Boolean, typing: Boolean = false) {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (fromUser) Gravity.END else Gravity.START
            tag = if (typing) "typing" else null
        }
        val label = TextView(this).apply {
            this.text = name
            textSize = 11f
            setTextColor(Color.rgb(129, 135, 168))
            setPadding(dp(4), dp(2), dp(4), dp(2))
        }
        val bubble = TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(if (fromUser) Color.WHITE else Color.rgb(67, 73, 107))
            setPadding(dp(12), dp(9), dp(12), dp(9))
            background = if (fromUser) {
                bubbleBackground(Color.rgb(108, 157, 255), Color.rgb(108, 157, 255))
            } else {
                bubbleBackground(Color.rgb(244, 247, 255), Color.rgb(222, 231, 255))
            }
            maxWidth = dp(260)
        }
        wrapper.addView(label)
        wrapper.addView(bubble)
        messageColumn.addView(wrapper, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(8)
        })
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun bubbleBackground(fill: Int, stroke: Int) = android.graphics.drawable.GradientDrawable().apply {
        cornerRadius = dp(16).toFloat()
        setColor(fill)
        setStroke(dp(1), stroke)
    }

    private fun chipBackground() = android.graphics.drawable.GradientDrawable().apply {
        cornerRadius = dp(18).toFloat()
        setColor(Color.rgb(244, 247, 255))
        setStroke(dp(1), Color.rgb(221, 230, 255))
    }

    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
