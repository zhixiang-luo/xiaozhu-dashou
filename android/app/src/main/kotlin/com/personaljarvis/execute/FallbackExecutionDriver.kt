package com.personaljarvis.execute

import com.personaljarvis.policy.DriverResult
import com.personaljarvis.policy.RuntimeAction

class FallbackExecutionDriver(
    private val primary: ExecutionDriver,
    private val fallback: ExecutionDriver
) : ExecutionDriver {
    override val isAvailable: Boolean
        get() = primary.isAvailable || fallback.isAvailable

    override fun execute(action: RuntimeAction): DriverResult {
        return when {
            primary.isAvailable -> primary.execute(action)
            fallback.isAvailable -> fallback.execute(action)
            else -> DriverResult.Unsupported("execution_driver_unavailable")
        }
    }

    override fun cancel() {
        primary.cancel()
        fallback.cancel()
    }
}
