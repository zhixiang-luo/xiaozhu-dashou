package com.personaljarvis.voice

import org.json.JSONObject
import java.net.URLEncoder

sealed interface VoiceStreamEvent {
    data object Ready : VoiceStreamEvent
    data class Partial(val text: String) : VoiceStreamEvent
    data class Final(val text: String) : VoiceStreamEvent
    data class Error(val message: String) : VoiceStreamEvent
}

fun buildVoiceStreamUrl(baseUrl: String, deviceId: String): String {
    val websocketBase = when {
        baseUrl.startsWith("https://") ->
            "wss://" + baseUrl.removePrefix("https://")
        baseUrl.startsWith("http://") ->
            "ws://" + baseUrl.removePrefix("http://")
        else -> baseUrl
    }.trimEnd('/')
    return "$websocketBase/v1/voice/stream?deviceId=" +
        URLEncoder.encode(deviceId, "UTF-8").replace("+", "%20")
}

fun parseVoiceStreamEvent(raw: String): VoiceStreamEvent? {
    return runCatching {
        val json = JSONObject(raw)
        when (json.optString("type")) {
            "ready" -> VoiceStreamEvent.Ready
            "partial" -> VoiceStreamEvent.Partial(json.getString("text"))
            "final" -> VoiceStreamEvent.Final(json.getString("text"))
            "error" -> VoiceStreamEvent.Error(
                json.optString("message").ifBlank {
                    "实时语音识别失败，请重试。"
                }
            )
            else -> null
        }
    }.getOrNull()
}
