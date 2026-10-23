package com.personaljarvis.execute

import com.personaljarvis.policy.DriverResult
import com.personaljarvis.policy.RuntimeAction

interface ExecutionDriver {
    val isAvailable: Boolean
        get() = true

    fun execute(action: RuntimeAction): DriverResult
    fun cancel()
}
