package com.personaljarvis.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeVoiceSessionTest {
    @Test
    fun buffersAudioUntilTheStreamIsReady() {
        val capture = FakeCapture()
        val transport = FakeTransport()
        val session = RealtimeVoiceSession(
            capture = capture,
            transport = transport,
            scheduler = FakeScheduler(),
            onPartial = {},
            onFinal = {},
            onError = {}
        )

        session.startCapture()
        capture.emit(byteArrayOf(1, 2))
        capture.emit(byteArrayOf(3, 4))
        assertEquals(0, transport.audio.size)

        session.startStreaming()
        session.accept(VoiceStreamEvent.Ready)

        assertEquals(1, transport.audio.size)
        assertTrue(transport.audio.single().contentEquals(byteArrayOf(1, 2, 3, 4)))
    }

    @Test
    fun usesCumulativePartialTextAndStopsOnce() {
        val capture = FakeCapture()
        val transport = FakeTransport()
        val partials = mutableListOf<String>()
        var finalText = ""
        val session = RealtimeVoiceSession(
            capture = capture,
            transport = transport,
            scheduler = FakeScheduler(),
            onPartial = partials::add,
            onFinal = { finalText = it },
            onError = {}
        )

        session.startCapture()
        session.startStreaming()
        session.accept(VoiceStreamEvent.Ready)
        session.accept(VoiceStreamEvent.Partial("第一段"))
        session.accept(VoiceStreamEvent.Partial("第一段第二句"))
        session.stopAndFinish()
        session.stopAndFinish()
        session.accept(VoiceStreamEvent.Final("第一段第二句完整版"))

        assertEquals(listOf("第一段", "第一段第二句"), partials)
        assertEquals(1, transport.stopCount)
        assertEquals("第一段第二句完整版", finalText)
    }

    @Test
    fun fallsBackToTheLatestCumulativeTextAfterFinalTimeout() {
        val scheduler = FakeScheduler()
        var finalText = ""
        val session = RealtimeVoiceSession(
            capture = FakeCapture(),
            transport = FakeTransport(),
            scheduler = scheduler,
            onPartial = {},
            onFinal = { finalText = it },
            onError = {}
        )

        session.startCapture()
        session.startStreaming()
        session.accept(VoiceStreamEvent.Ready)
        session.accept(VoiceStreamEvent.Partial("已经识别到的完整前半段"))
        session.stopAndFinish()
        scheduler.runPending()

        assertEquals("已经识别到的完整前半段", finalText)
    }

    @Test
    fun defersStopUntilTheStreamBecomesReady() {
        val transport = FakeTransport()
        val session = RealtimeVoiceSession(
            capture = FakeCapture(),
            transport = transport,
            scheduler = FakeScheduler(),
            onPartial = {},
            onFinal = {},
            onError = {}
        )

        session.startCapture()
        session.startStreaming()
        session.stopAndFinish()
        assertEquals(0, transport.stopCount)

        session.accept(VoiceStreamEvent.Ready)
        assertEquals(1, transport.stopCount)
    }

    private class FakeCapture : VoiceCapture {
        private var onAudio: ((ByteArray) -> Unit)? = null

        override fun start(consume: (ByteArray) -> Unit) {
            onAudio = consume
        }

        override fun stop() = Unit

        fun emit(bytes: ByteArray) {
            onAudio?.invoke(bytes)
        }
    }

    private class FakeTransport : VoiceStreamTransport {
        val audio = mutableListOf<ByteArray>()
        var stopCount = 0

        override fun connect(listener: (VoiceStreamEvent) -> Unit) = Unit

        override fun sendAudio(bytes: ByteArray) {
            audio.add(bytes)
        }

        override fun sendStop() {
            stopCount++
        }

        override fun close() = Unit
    }

    private class FakeScheduler : VoiceSessionScheduler {
        private var pending: (() -> Unit)? = null

        override fun postDelayed(delayMs: Long, action: () -> Unit) {
            pending = action
        }

        fun runPending() {
            pending?.invoke()
        }
    }
}
