package com.example.aipet

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.random.Random

object PetChatEngine {
    enum class Mode { LOCAL, API }
    data class ReplyResult(val reply: String, val mode: Mode, val ok: Boolean = true)

    private val executor = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun localReply(text: String): String {
        val s = text.lowercase()
        return when {
            listOf("你好", "嗨", "哈喽", "hello", "hi").any { s.contains(it) } -> listOf(
                "你好呀～今天想让我怎么陪你？",
                "嗨嗨，我在桌面上陪着你呢。",
                "见到你啦，要先摸摸头还是先聊聊天？"
            ).random()
            listOf("你是谁", "介绍一下", "你叫什么").any { s.contains(it) } -> "我是住在你手机桌面上的二次元小伙伴，可以陪你聊天、卖萌、摸鱼提醒和做小动作。"
            listOf("夸夸我", "表扬我", "鼓励我", "安慰我").any { s.contains(it) } -> listOf(
                "你已经很努力啦，不要因为一时的卡住就否定自己。",
                "你比自己想象得更可靠，慢一点也没关系。",
                "给你一个大大的夸奖！能坚持到现在就已经很棒啦。"
            ).random()
            listOf("笑话", "段子").any { s.contains(it) } -> listOf(
                "小伙伴问手机：你怎么总发热？手机说：因为主人一靠近我就心动。",
                "我问闹钟为什么天天吵你。闹钟说：不吵不行，他总想再睡五分钟。",
                "程序员的恋爱观：不是没有回应，只是还在加载。"
            ).random()
            listOf("难过", "伤心", "烦", "压力", "累", "孤独", "不开心").any { s.contains(it) } -> "抱抱你。现在先不用急着变好，我们先把情绪安顿一下。要不要告诉我，今天最让你心累的是哪件事？"
            listOf("开心", "高兴", "好消息", "成功").any { s.contains(it) } -> "太好了！快和我多分享一点，我想陪你把这份开心再放大一点。"
            listOf("晚安", "睡觉", "困", "休息").any { s.contains(it) } -> "那就把今天先交给夜晚吧。晚安呀，愿你睡个好觉。"
            listOf("摸摸头", "抱抱", "亲亲").any { s.contains(it) } -> "好呀～给你摸摸头，也给你一个软乎乎的拥抱。"
            listOf("提醒", "待办", "学习", "工作").any { s.contains(it) } -> "你可以把现在最重要的一件事告诉我，我们把它缩小成一个马上能开始的小步骤。"
            text.endsWith("？") || text.endsWith("?") -> "我先认真想想：如果你希望更聪明的回答，也可以切到独立 AI 模式。现在你也可以再具体一点告诉我你的问题。"
            else -> {
                val followUps = listOf(
                    "我听到你说：“$text”。你愿意再多说一点吗？",
                    "收到～关于“$text”，你现在最在意的部分是什么？",
                    "嗯嗯，我在听。和“$text”有关的，是心情问题还是实际问题呢？"
                )
                followUps.random()
            }
        }
    }

    fun requestReply(
        context: Context,
        userText: String,
        mode: Mode,
        history: List<JSONObject> = emptyList(),
        callback: (ReplyResult) -> Unit
    ) {
        if (mode == Mode.LOCAL) {
            mainHandler.postDelayed({ callback(ReplyResult(localReply(userText), Mode.LOCAL)) }, 450L)
            return
        }

        val prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
        val endpoint = prefs.getString("endpoint", AiSettingsActivity.DEFAULT_ENDPOINT) ?: AiSettingsActivity.DEFAULT_ENDPOINT
        val model = prefs.getString("model", AiSettingsActivity.DEFAULT_MODEL) ?: AiSettingsActivity.DEFAULT_MODEL
        val apiKey = ApiKeyStore.load(context)
        if (apiKey.isNullOrBlank()) {
            mainHandler.post {
                callback(
                    ReplyResult(
                        "还没有配置 API Key，我先用本地模式陪你聊。",
                        Mode.LOCAL,
                        ok = false
                    )
                )
            }
            return
        }

        executor.execute {
            var reply = "这次没有成功连接到 AI 服务，请检查网络、接口地址、模型名称和 API Key。"
            var ok = false
            try {
                val messages = JSONArray().put(
                    JSONObject()
                        .put("role", "system")
                        .put(
                            "content",
                            "你是一个温柔、自然、不过度打扰用户的二次元桌面宠物。请使用简体中文、简洁自然的语气回复。可以可爱，但不要过度幼稚。"
                        )
                )
                history.takeLast(10).forEach { messages.put(it) }
                messages.put(JSONObject().put("role", "user").put("content", userText))

                val body = JSONObject()
                    .put("model", model)
                    .put("messages", messages)
                    .put("temperature", 0.8)

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
                    val detail = try {
                        JSONObject(responseText).optString("error", responseText)
                    } catch (_: Exception) {
                        responseText
                    }
                    reply = "AI 服务返回 HTTP $status：${detail.take(180)}"
                } else {
                    val json = JSONObject(responseText)
                    reply = json.getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").optString("content", "（服务没有返回文字）")
                    ok = true
                }
                connection.disconnect()
            } catch (e: Exception) {
                reply = "连接失败：${e.localizedMessage ?: "请检查网络与接口配置"}"
            }
            mainHandler.post { callback(ReplyResult(reply, if (ok) Mode.API else Mode.LOCAL, ok)) }
        }
    }

    fun randomIdleLine(): String = listOf(
        "今天也一起加油吧～",
        "记得喝水哦。",
        "需要我陪你摸会儿鱼吗？",
        "别太累啦，眨眨眼休息一下。",
        "我在这里，不打扰也陪着你。"
    ).random()

    fun suggestions(): List<String> = listOf("你好", "夸夸我", "安慰我", "讲个笑话", "晚安", "摸摸头")
}
