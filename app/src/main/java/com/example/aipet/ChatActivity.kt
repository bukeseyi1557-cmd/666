package com.example.aipet

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class ChatActivity : Activity() {
    private lateinit var messageColumn: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var input: EditText
    private lateinit var modeLabel: TextView
    private var mode = Mode.LOCAL
    private var openedSettingsForApi = false
    private val executor = Executors.newSingleThreadExecutor()
    private val history = mutableListOf<JSONObject>()

    private enum class Mode { LOCAL, API }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        addMessage("小伙伴", "嗨！我住在你的手机桌面上。你可以拖动我，也可以在这里跟我说说话。现在是免费本地模式，不会把聊天发送到网络。", false)
    }

    override fun onResume() {
        super.onResume()
        if (openedSettingsForApi) {
            openedSettingsForApi = false
            if (!ApiKeyStore.load(this).isNullOrBlank()) {
                mode = Mode.API
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
            setOnClickListener { mode = Mode.LOCAL; updateModeLabel() }
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

        scrollView = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, dp(8), 0, dp(8))
        }
        messageColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(2), dp(2), dp(10))
        }
        
        scrollView.addView(messageColumn, android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(scrollView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        input = EditText(this).apply {
            hint = "和小伙伴说点什么…"
            textSize = 15f
            minHeight = dp(48)
            maxLines = 4
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
        modeLabel.text = if (mode == Mode.LOCAL) {
            "当前模式：免费本地陪聊（固定规则回复，不调用 AI）"
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
            mode = Mode.API
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
        if (mode == Mode.LOCAL) {
            addMessage("小伙伴", localReply(text), false)
        } else {
            sendToApi(text)
        }
    }

    private fun localReply(text: String): String {
        val s = text.lowercase()
        return when {
            listOf("你好", "嗨", "哈喽", "hello", "hi").any { s.contains(it) } -> "嗨嗨！今天也辛苦啦。想聊点什么都可以，我在这里陪你一会儿。"
            listOf("难过", "伤心", "烦", "压力", "累", "孤独", "不开心").any { s.contains(it) } -> "听起来你现在不太轻松。你不用急着把事情想明白，也不用马上振作起来。要不要先从最让你在意的那一点说起？"
            listOf("开心", "高兴", "好消息", "成功").any { s.contains(it) } -> "这听起来是件值得开心的事！虽然我现在只是基础版小伙伴，但我愿意认真听你分享。"
            listOf("睡觉", "晚安", "困").any { s.contains(it) } -> "那就别太勉强自己啦。希望你今晚能休息好，晚安！"
            listOf("你是谁", "介绍一下", "你叫什么").any { s.contains(it) } -> "我是住在你手机桌面上的小伙伴。现在我用的是免费本地回复，还没有真正的 AI 思考能力；配置独立 AI 接口后，就能进行更自然的对话。"
            text.endsWith("？") || text.endsWith("?") -> "我现在的免费模式还比较简单，没办法真正理解所有问题。你可以切换到独立 AI 模式，或者先换个方式告诉我你最想解决的部分。"
            else -> "我看到你说：“$text”。免费本地模式还不会像真正的 AI 那样深入理解，不过我在听。你愿意再多告诉我一点吗？"
        }
    }

    private fun sendToApi(userText: String) {
        val prefs = getSharedPreferences("ai_settings", MODE_PRIVATE)
        val endpoint = prefs.getString("endpoint", AiSettingsActivity.DEFAULT_ENDPOINT) ?: AiSettingsActivity.DEFAULT_ENDPOINT
        val model = prefs.getString("model", AiSettingsActivity.DEFAULT_MODEL) ?: AiSettingsActivity.DEFAULT_MODEL
        val apiKey = ApiKeyStore.load(this)
        if (apiKey.isNullOrBlank()) {
            addMessage("小伙伴", "还没有配置 API Key。请点击“AI 设置”添加接口信息。", false)
            mode = Mode.LOCAL
            updateModeLabel()
            return
        }

        history.add(JSONObject().put("role", "user").put("content", userText))
        addMessage("小伙伴", "正在思考…", false)
        val systemPrompt = JSONObject()
            .put("role", "system")
            .put("content", "你是一个温柔、自然、不过度打扰用户的二次元桌面宠物。请用简体中文回复，语气友善、简洁。不要声称自己有真实情感或能在后台主动观察用户。")
        val messages = JSONArray().put(systemPrompt)
        history.takeLast(12).forEach { messages.put(it) }
        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.7)

        executor.execute {
            var reply = "这次没有成功连接到 AI 服务，请检查网络、接口地址、模型名称和 API Key。"
            try {
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 60000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                }
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                if (status !in 200..299) {
                    val detail = try { JSONObject(responseText).optString("error", responseText) } catch (_: Exception) { responseText }
                    reply = "AI 服务返回 HTTP $status：${detail.take(240)}"
                } else {
                    val json = JSONObject(responseText)
                    reply = json.getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").optString("content", "（服务没有返回文字）")
                    history.add(JSONObject().put("role", "assistant").put("content", reply))
                }
                connection.disconnect()
            } catch (e: Exception) {
                reply = "连接失败：${e.localizedMessage ?: "请检查网络与接口配置"}"
            }
            runOnUiThread { addMessage("小伙伴", reply, false) }
        }
    }

    private fun addMessage(sender: String, message: String, fromUser: Boolean) {
        val bubble = TextView(this).apply {
            text = "$sender\n$message"
            textSize = 15f
            setTextColor(if (fromUser) Color.rgb(255, 255, 255) else Color.rgb(69, 55, 82))
            setPadding(dp(13), dp(10), dp(13), dp(10))
            background = GradientDrawable().apply {
                setColor(if (fromUser) Color.rgb(139, 116, 206) else Color.rgb(244, 237, 250))
                cornerRadius = dp(16).toFloat()
            }
            maxWidth = (resources.displayMetrics.widthPixels * 0.78f).toInt()
        }
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.gravity = if (fromUser) Gravity.END else Gravity.START
        params.bottomMargin = dp(9)
        messageColumn.addView(bubble, params)
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
