package com.personaljarvis.voice

interface VoiceCapture {
    fun start(consume: (ByteArray) -> Unit)
    fun stop()
}

interface VoiceStreamTransport {
    fun connect(listener: (VoiceStreamEvent) -> Unit)
    fun sendAudio(bytes: ByteArray)
    fun sendStop()
    fun close()
}

interface VoiceSessionScheduler {
    fun postDelayed(delayMs: Long, action: () -> Unit)
}

class RealtimeVoiceSession(
    private val capture: VoiceCapture,
    private val transport: VoiceStreamTransport,
    private val scheduler: VoiceSessionScheduler,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val lock = Any()
    private val bufferedAudio = mutableListOf<ByteArray>()
    private var streamReady = false
    private var streaming = false
    private var stopping = false
    private var stopSent = false
    private var finished = false

    val latestText: String
        get() = synchronized(lock) { currentText }
    private var currentText = ""

    fun startCapture() {
        if (streaming || stopped()) {
            return
        }
        capture.start { bytes ->
            synchronized(lock) {
                if (finished) {
                    return@synchronized
                }
                if (streamReady) {
                    transport.sendAudio(bytes)
                } else {
                    bufferedAudio.add(bytes)
                }
            }
        }
    }

    fun startStreaming() {
        if (streaming || stopped()) {
            return
        }
        streaming = true
        transport.connect(::accept)
    }

    fun accept(event: VoiceStreamEvent) {
        synchronized(lock) {
            if (finished) {
                return
            }
            when (event) {
                VoiceStreamEvent.Ready -> {
                    streamReady = true
                    if (bufferedAudio.isNotEmpty()) {
                        transport.sendAudio(
                            bufferedAudio
                                .flatMap { it.asIterable() }
                                .toByteArray()
                        )
                        bufferedAudio.clear()
                    }
                    if (stopping) {
                        sendStopIfReady()
                    }
                }
                is VoiceStreamEvent.Partial -> {
                    currentText = event.text.trim()
                    if (currentText.isNotEmpty()) {
                        onPartial(currentText)
                    }
                }
                is VoiceStreamEvent.Final -> {
                    finishWith(event.text.ifBlank { currentText })
                }
                is VoiceStreamEvent.Error -> {
                    if (stopping && currentText.isNotBlank()) {
                        finishWith(currentText)
                    } else {
                        finished = true
                        transport.close()
                        onError(event.message)
                    }
                }
            }
        }
    }

    fun stopAndFinish() {
        synchronized(lock) {
            if (stopping || finished) {
                return
            }
            stopping = true
        }
        capture.stop()
        synchronized(lock) {
            sendStopIfReady()
        }
        scheduler.postDelayed(FINAL_TIMEOUT_MS) {
            synchronized(lock) {
                if (finished) {
                    return@synchronized
                }
                if (currentText.isNotBlank()) {
                    finishWith(currentText)
                } else {
                    finished = true
                    transport.close()
                    onError("没有听清，请再试一次。")
                }
            }
        }
    }

    fun cancel() {
        synchronized(lock) {
            if (finished) {
                return
            }
            finished = true
        }
        capture.stop()
        transport.close()
    }

    private fun finishWith(text: String) {
        if (finished) {
            return
        }
        finished = true
        currentText = text.trim()
        transport.close()
        onFinal(currentText)
    }

    private fun sendStopIfReady() {
        if (!streamReady || stopSent || finished) {
            return
        }
        stopSent = true
        transport.sendStop()
    }

    private fun stopped(): Boolean = synchronized(lock) { stopping || finished }

    private companion object {
        const val FINAL_TIMEOUT_MS = 6_000L
    }
}
