package com.personaljarvis.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceStreamProtocolTest {
    @Test
    fun buildsAnAuthenticatedLoopbackStreamUrl() {
        assertEquals(
            "ws://127.0.0.1:4100/v1/voice/stream?deviceId=xiaozhu%20k60",
            buildVoiceStreamUrl(
                "http://127.0.0.1:4100/",
                "xiaozhu k60"
            )
        )
    }

    @Test
    fun parsesCumulativeAndFinalServerEvents() {
        assertEquals(
            VoiceStreamEvent.Partial("打开微信并发送消息"),
            parseVoiceStreamEvent(
                """{"type":"partial","text":"打开微信并发送消息"}"""
            )
        )
        assertEquals(
            VoiceStreamEvent.Final("完整文本"),
            parseVoiceStreamEvent("""{"type":"final","text":"完整文本"}""")
        )
    }
}
