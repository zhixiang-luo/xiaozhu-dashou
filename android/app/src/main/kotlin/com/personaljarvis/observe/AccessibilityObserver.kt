package com.personaljarvis.observe

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.personaljarvis.BuildConfig
import com.personaljarvis.policy.DriverResult
import com.personaljarvis.policy.ObservationSnapshot
import com.personaljarvis.policy.PageClassifier
import com.personaljarvis.policy.RecipientResolution
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AccessibilityObserver : AccessibilityService() {
    private val compactor = UiTreeCompactor()
    private val nodesById = mutableMapOf<String, AccessibilityNodeInfo>()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        nodesById.clear()
        super.onDestroy()
    }

    fun snapshot(): ObservationSnapshot? {
        val root = applicationRoot() ?: return null
        val packageName = root.packageName?.toString() ?: return null
        val collected = mutableListOf<Pair<RawUiNode, AccessibilityNodeInfo>>()
        collect(root, collected)

        val eligible = collected.filter { (raw, _) ->
            raw.visible &&
                (raw.clickable || raw.editable || raw.actions.isNotEmpty())
        }
        val snapshots = compactor.compact(eligible.map { it.first })

        nodesById.clear()
        snapshots.forEachIndexed { index, snapshot ->
            nodesById[snapshot.nodeId] = eligible[index].second
        }

        val texts = snapshots.mapNotNull { it.text ?: it.contentDescription }
        val hasSensitiveInput = eligible.any { it.first.sensitive }
        val pageKind = PageClassifier.classify(
            packageName = packageName,
            texts = texts,
            hasSensitiveInput = hasSensitiveInput
        )

        return ObservationSnapshot(
            foregroundPackage = packageName,
            pageTitle = texts.firstOrNull(),
            pageKind = pageKind,
            recipientResolution = RecipientResolution.NONE,
            nodes = snapshots,
            screenHash = hashSnapshot(packageName, snapshots)
        )
    }

    fun performTap(nodeId: String): DriverResult {
        val node = nodesById[nodeId]
            ?: return DriverResult.Failure("node_not_found")
        return if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            DriverResult.Success
        } else {
            DriverResult.Failure("click_action_failed")
        }
    }

    fun performText(nodeId: String, text: String): DriverResult {
        val node = nodesById[nodeId]
            ?: return DriverResult.Failure("node_not_found")
        val arguments = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
        }
        return if (
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        ) {
            DriverResult.Success
        } else {
            DriverResult.Failure("set_text_action_failed")
        }
    }

    fun performCoordinateTap(normalizedX: Float, normalizedY: Float): DriverResult {
        return performCoordinateGesture(
            normalizedX = normalizedX,
            normalizedY = normalizedY,
            durationMs = TAP_DURATION_MS
        )
    }

    fun performCoordinateLongPress(
        normalizedX: Float,
        normalizedY: Float
    ): DriverResult {
        return performCoordinateGesture(
            normalizedX = normalizedX,
            normalizedY = normalizedY,
            durationMs = LONG_PRESS_DURATION_MS
        )
    }

    private fun performCoordinateGesture(
        normalizedX: Float,
        normalizedY: Float,
        durationMs: Long
    ): DriverResult {
        val metrics = resources.displayMetrics
        val x = normalizedX.coerceIn(0f, 1f) * metrics.widthPixels
        val y = normalizedY.coerceIn(0f, 1f) * metrics.heightPixels
        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(path, 0L, durationMs)
            )
            .build()
        val latch = CountDownLatch(1)
        var completed = false
        mainHandler.post {
            dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        completed = true
                        latch.countDown()
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        latch.countDown()
                    }
                },
                null
            )
        }
        return if (latch.await(GESTURE_TIMEOUT_MS, TimeUnit.MILLISECONDS) && completed) {
            DriverResult.Success
        } else {
            DriverResult.Failure("accessibility_gesture_failed")
        }
    }

    fun captureScreenshot(): Bitmap? {
        val latch = CountDownLatch(1)
        var bitmap: Bitmap? = null
        val posted = mainHandler.post {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(result: ScreenshotResult) {
                        val buffer = result.hardwareBuffer
                        bitmap = runCatching {
                            Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                                ?.copy(Bitmap.Config.ARGB_8888, false)
                        }.getOrNull()
                        buffer.close()
                        if (BuildConfig.DEBUG) {
                            Log.d(
                                DEBUG_LOG_TAG,
                                "wechat_screenshot_ready=${bitmap != null}"
                            )
                        }
                        latch.countDown()
                    }

                    override fun onFailure(errorCode: Int) {
                        if (BuildConfig.DEBUG) {
                            Log.d(
                                DEBUG_LOG_TAG,
                                "wechat_screenshot_failed=$errorCode"
                            )
                        }
                        latch.countDown()
                    }
                }
            )
        }
        if (!posted || !latch.await(SCREENSHOT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            return null
        }
        return bitmap
    }

    private fun collect(
        node: AccessibilityNodeInfo,
        output: MutableList<Pair<RawUiNode, AccessibilityNodeInfo>>
    ) {
        if (output.size >= UiTreeCompactor.MAX_NODES) {
            return
        }

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val actions = buildSet {
            if (node.isClickable) add("click")
            if (node.isEditable) add("set_text")
        }
        val sensitive = node.isPassword
        output.add(
            RawUiNode(
                role = node.className?.toString()?.substringAfterLast('.') ?: "node",
                text = if (sensitive) null else node.text?.toString(),
                contentDescription = if (sensitive) {
                    null
                } else {
                    node.contentDescription?.toString()
                },
                bounds = UiBounds(
                    left = bounds.left,
                    top = bounds.top,
                    right = bounds.right,
                    bottom = bounds.bottom
                ),
                visible = node.isVisibleToUser,
                clickable = node.isClickable,
                editable = node.isEditable,
                actions = actions,
                sensitive = sensitive
            ) to node
        )

        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { child ->
                collect(child, output)
            }
        }
    }

    private fun applicationRoot(): AccessibilityNodeInfo? {
        val applicationWindow = windows.firstOrNull {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION
        }
        return applicationWindow?.root ?: rootInActiveWindow
    }

    private fun hashSnapshot(
        packageName: String,
        nodes: List<UiNodeSnapshot>
    ): String {
        val source = buildString {
            append(packageName)
            nodes.forEach { node ->
                append(node.role)
                append(node.text)
                append(node.contentDescription)
                append(node.bounds)
            }
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(source.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
    }

    companion object {
        private const val TAP_DURATION_MS = 80L
        private const val LONG_PRESS_DURATION_MS = 900L
        private const val GESTURE_TIMEOUT_MS = 2_000L
        private const val SCREENSHOT_TIMEOUT_MS = 5_000L
        private const val DEBUG_LOG_TAG = "XiaozhuDashou"

        @Volatile
        var instance: AccessibilityObserver? = null
            private set
    }
}
