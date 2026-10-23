package com.personaljarvis.execute

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.personaljarvis.BuildConfig
import com.personaljarvis.observe.AccessibilityObserver
import com.personaljarvis.policy.DriverResult
import com.personaljarvis.wechat.WechatConversationReader
import com.personaljarvis.wechat.WechatScreenState

class WechatAccessibilityUi(
    private val context: Context,
    private val reader: WechatConversationReader,
    private val observerProvider: () -> AccessibilityObserver? = {
        AccessibilityObserver.instance
    },
    private val pause: (Long) -> Unit = Thread::sleep
) : WechatReplyUi {
    override fun read(): WechatScreenState? {
        val observer = observerProvider() ?: return null
        if (observer.snapshot()?.foregroundPackage != WECHAT_PACKAGE) {
            return null
        }
        val state = reader.read()
        debugLog("wechat_ocr_complete=${state != null}")
        return state
    }

    override fun focusInput(): Boolean {
        val observer = observerProvider() ?: return false
        val tapped = observer.performCoordinateTap(
            INPUT_X,
            INPUT_Y
        ) is DriverResult.Success
        if (tapped) {
            pause(INPUT_FOCUS_DELAY_MS)
        }
        return tapped
    }

    override fun paste(text: String): Boolean {
        val observer = observerProvider() ?: return false
        val clipboard = context.getSystemService(ClipboardManager::class.java)
            ?: return false
        val previous = runCatching { clipboard.primaryClip }.getOrNull()
        if (
            runCatching {
                clipboard.setPrimaryClip(
                    ClipData.newPlainText("小猪搭手回复", text)
                )
            }.isFailure
        ) {
            return false
        }
        val longPressed = observer.performCoordinateLongPress(
            FOCUSED_INPUT_X,
            FOCUSED_INPUT_Y
        ) is DriverResult.Success
        debugLog("wechat_paste_long_pressed=$longPressed")
        if (!longPressed) {
            restoreClipboard(clipboard, previous)
            return false
        }
        pause(PASTE_MENU_DELAY_MS)
        val menu = reader.read()
        debugLog("wechat_paste_menu_visible=${menu != null}")
        val pasteLine = menu?.lines?.firstOrNull(::isPasteMenuLine)
        debugLog("wechat_paste_menu_found=${pasteLine != null}")
        if (pasteLine == null) {
            restoreClipboard(clipboard, previous)
            return false
        }
        val pasted = observer.performCoordinateTap(
            pasteTapX(pasteLine) / menuWidth(menu),
            pasteLine.centerY / menuHeight(menu)
        ) is DriverResult.Success
        debugLog("wechat_paste_tapped=$pasted")
        if (pasted) {
            pause(DRAFT_DELAY_MS)
        }
        restoreClipboard(clipboard, previous, CLIPBOARD_RESTORE_DELAY_MS)
        return pasted
    }

    override fun tapSend(): Boolean {
        val observer = observerProvider() ?: return false
        val tapped = observer.performCoordinateTap(
            SEND_X,
            SEND_Y
        ) is DriverResult.Success
        if (tapped) {
            pause(SEND_DELAY_MS)
        }
        return tapped
    }

    private companion object {
        const val INPUT_X = 0.50f
        const val INPUT_Y = 0.93f
        const val FOCUSED_INPUT_X = 0.45f
        const val FOCUSED_INPUT_Y = 0.59f
        const val SEND_X = 0.91f
        const val SEND_Y = 0.59f
        const val WECHAT_PACKAGE = "com.tencent.mm"
        const val DEBUG_LOG_TAG = "XiaozhuDashou"
        const val INPUT_FOCUS_DELAY_MS = 700L
        const val PASTE_MENU_DELAY_MS = 1_200L
        const val DRAFT_DELAY_MS = 800L
        const val SEND_DELAY_MS = 1_200L
        const val CLIPBOARD_RESTORE_DELAY_MS = 8_000L
        const val PASTE_LABEL_X_FRACTION = 0.15f
        val PASTE_MENU_LABELS = setOf("粘贴", "粘貼", "粘帖", "Paste")
    }

    private fun menuWidth(menu: WechatScreenState): Float {
        return context.resources.displayMetrics.widthPixels.toFloat()
    }

    private fun menuHeight(menu: WechatScreenState): Float {
        return context.resources.displayMetrics.heightPixels.toFloat()
    }

    private fun isPasteMenuLine(
        line: com.personaljarvis.wechat.WechatOcrLine
    ): Boolean {
        return PASTE_MENU_LABELS.any { label ->
            line.text.trim().contains(label, ignoreCase = true)
        }
    }

    private fun pasteTapX(
        line: com.personaljarvis.wechat.WechatOcrLine
    ): Float {
        return line.left + (line.right - line.left) * PASTE_LABEL_X_FRACTION
    }

    private fun restoreClipboard(
        clipboard: ClipboardManager,
        previous: ClipData?,
        delayMs: Long = 0L
    ) {
        if (previous == null) {
            return
        }
        Handler(Looper.getMainLooper()).postDelayed(
            {
                runCatching { clipboard.setPrimaryClip(previous) }
            },
            delayMs
        )
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(DEBUG_LOG_TAG, message)
        }
    }
}
