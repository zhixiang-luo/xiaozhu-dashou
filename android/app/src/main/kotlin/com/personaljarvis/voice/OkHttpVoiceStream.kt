package com.personaljarvis.voice

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString

class OkHttpVoiceStream(
    baseUrl: String,
    private val deviceToken: String,
    deviceId: String,
    private val client: OkHttpClient = OkHttpClient()
) : VoiceStreamTransport {
    private val request = Request.Builder()
        .url(buildVoiceStreamUrl(baseUrl, deviceId))
        .header("Authorization", "Bearer $deviceToken")
        .header("X-Device-ID", deviceId)
        .build()
    private var socket: WebSocket? = null
    private var listener: ((VoiceStreamEvent) -> Unit)? = null

    override fun connect(listener: (VoiceStreamEvent) -> Unit) {
        this.listener = listener
        socket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    parseVoiceStreamEvent(text)?.let(listener)
                }

                override fun onFailure(
                    webSocket: WebSocket,
                    t: Throwable,
                    response: Response?
                ) {
                    listener(
                        VoiceStreamEvent.Error(
                            "实时语音连接失败，请检查电脑上的小猪搭手服务。"
                        )
                    )
                }
            }
        )
    }

    override fun sendAudio(bytes: ByteArray) {
        socket?.send(bytes.toByteString())
    }

    override fun sendStop() {
        socket?.send("""{"type":"stop"}""")
    }

    override fun close() {
        socket?.close(1000, "finished")
        socket = null
        listener = null
    }
}
