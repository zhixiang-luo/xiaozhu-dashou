package com.personaljarvis.wechat

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.personaljarvis.observe.AccessibilityObserver
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WechatConversationReader : AutoCloseable {
    private val recognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )

    fun read(): WechatScreenState? {
        val screenshot = AccessibilityObserver.instance
            ?.captureScreenshot()
            ?: return null
        return try {
            val latch = CountDownLatch(1)
            var recognized: com.google.mlkit.vision.text.Text? = null
            recognizer.process(
                InputImage.fromBitmap(screenshot, 0)
            ).addOnSuccessListener {
                recognized = it
                latch.countDown()
            }.addOnFailureListener {
                latch.countDown()
            }
            if (!latch.await(OCR_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                return null
            }
            val text = recognized ?: return null
            val lines = text.textBlocks.flatMap { block ->
                block.lines.mapNotNull { line ->
                    val bounds = line.boundingBox ?: return@mapNotNull null
                    WechatOcrLine(
                        text = line.text,
                        left = bounds.left,
                        top = bounds.top,
                        right = bounds.right,
                        bottom = bounds.bottom
                    )
                }
            }
            WechatScreenParser.parse(
                width = screenshot.width,
                height = screenshot.height,
                lines = lines
            )
        } finally {
            screenshot.recycle()
        }
    }

    override fun close() {
        recognizer.close()
    }

    private companion object {
        const val OCR_TIMEOUT_SECONDS = 10L
    }
}
