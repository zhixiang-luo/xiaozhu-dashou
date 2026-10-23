package com.personaljarvis.execute

import android.content.Context
import android.content.Intent
import com.personaljarvis.observe.AccessibilityObserver
import com.personaljarvis.policy.DriverResult
import com.personaljarvis.policy.RuntimeAction
import com.personaljarvis.policy.RuntimeActionType

class AccessibilityDriver(
    private val context: Context,
    private val observerProvider: () -> AccessibilityObserver? = {
        AccessibilityObserver.instance
    }
) : ExecutionDriver {
    override val isAvailable: Boolean
        get() = observerProvider() != null

    override fun execute(action: RuntimeAction): DriverResult {
        val observer = observerProvider()
            ?: return DriverResult.Unsupported("accessibility_unavailable")

        return when (action.type) {
            RuntimeActionType.OPEN_APP -> openApp(action)
            RuntimeActionType.RESTART_APP ->
                DriverResult.Unsupported("shizuku_required")
            RuntimeActionType.TAP -> {
                val nodeId = action.targetNodeId
                    ?: return DriverResult.Failure("tap_target_missing")
                observer.performTap(nodeId)
            }
            RuntimeActionType.INPUT_TEXT -> {
                val nodeId = action.targetNodeId
                    ?: return DriverResult.Failure("input_target_missing")
                val text = action.text
                    ?: return DriverResult.Failure("input_text_missing")
                observer.performText(nodeId, text)
            }
            RuntimeActionType.SEND_MESSAGE,
            RuntimeActionType.MEDIA_CONTROL ->
                DriverResult.Unsupported("action_requires_specialized_adapter")
        }
    }

    override fun cancel() = Unit

    private fun openApp(action: RuntimeAction): DriverResult {
        val packageName = action.targetPackage
            ?: return DriverResult.Failure("target_package_missing")
        val intent = context.packageManager
            .getLaunchIntentForPackage(packageName)
            ?: return DriverResult.Failure("app_not_installed")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return DriverResult.Success
    }
}
