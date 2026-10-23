package com.personaljarvis.voice

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread
import kotlin.math.max

class PcmAudioCapture : VoiceCapture {
    @Volatile
    private var recording = false
    private var audioRecord: AudioRecord? = null
    private var worker: Thread? = null

    override fun start(consume: (ByteArray) -> Unit) {
        if (recording) {
            return
        }
        val minimumBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        require(minimumBuffer > 0) { "audio_record_unavailable" }
        val record = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build()
            )
            .setBufferSizeInBytes(max(minimumBuffer * 2, CHUNK_BYTES * 2))
            .build()
        check(record.state == AudioRecord.STATE_INITIALIZED) {
            "audio_record_initialization_failed"
        }
        audioRecord = record
        recording = true
        record.startRecording()
        worker = thread(name = "xiaozhu-pcm", isDaemon = true) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val shorts = ShortArray(CHUNK_SAMPLES)
            val bytes = ByteBuffer.allocate(CHUNK_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
            while (recording) {
                val count = record.read(
                    shorts,
                    0,
                    shorts.size,
                    AudioRecord.READ_BLOCKING
                )
                if (count <= 0) {
                    continue
                }
                bytes.clear()
                bytes.asShortBuffer().put(shorts, 0, count)
                consume(bytes.array().copyOf(count * 2))
            }
        }
    }

    override fun stop() {
        if (!recording) {
            return
        }
        recording = false
        runCatching { audioRecord?.stop() }
        runCatching { worker?.join(500) }
        runCatching { audioRecord?.release() }
        audioRecord = null
        worker = null
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val CHUNK_SAMPLES = 320
        const val CHUNK_BYTES = CHUNK_SAMPLES * 2
    }
}
