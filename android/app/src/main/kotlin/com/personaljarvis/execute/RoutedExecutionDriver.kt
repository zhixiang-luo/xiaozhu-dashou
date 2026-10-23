package com.personaljarvis.execute

import com.personaljarvis.policy.DriverResult
import com.personaljarvis.policy.RuntimeAction
import com.personaljarvis.policy.RuntimeActionType

class RoutedExecutionDriver(
    private val calendar: ExecutionDriver,
    private val general: ExecutionDriver
) : ExecutionDriver {
    override val isAvailable: Boolean
        get() = calendar.isAvailable || general.isAvailable

    override fun execute(action: RuntimeAction): DriverResult {
        return if (action.type == RuntimeActionType.CALENDAR_CREATE_EVENT) {
            calendar.execute(action)
        } else {
            general.execute(action)
        }
    }

    override fun cancel() {
        calendar.cancel()
        general.cancel()
    }
}
