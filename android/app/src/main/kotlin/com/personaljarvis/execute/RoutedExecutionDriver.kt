package com.personaljarvis.execute

import com.personaljarvis.policy.DriverResult
import com.personaljarvis.policy.RuntimeAction
import com.personaljarvis.policy.RuntimeActionType

class RoutedExecutionDriver(
    private val calendar: ExecutionDriver,
    private val general: ExecutionDriver,
    private val wechat: ExecutionDriver
) : ExecutionDriver {
    override val isAvailable: Boolean
        get() = calendar.isAvailable || general.isAvailable || wechat.isAvailable

    override fun execute(action: RuntimeAction): DriverResult {
        return when {
            action.type.name.startsWith("CALENDAR_") ->
                calendar.execute(action)
            action.type == RuntimeActionType.WECHAT_REPLY_CURRENT ->
                wechat.execute(action)
            else -> general.execute(action)
        }
    }

    override fun cancel() {
        calendar.cancel()
        general.cancel()
        wechat.cancel()
    }
}
