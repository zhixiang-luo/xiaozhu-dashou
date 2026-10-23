package com.personaljarvis.execute

import com.personaljarvis.policy.DriverResult
import com.personaljarvis.policy.RuntimeAction
import com.personaljarvis.policy.RuntimeActionType
import org.junit.Assert.assertEquals
import org.junit.Test

class FallbackExecutionDriverTest {
    @Test
    fun fallsBackWhenShizukuIsUnavailable() {
        val primary = FakeDriver(isAvailable = false, result = DriverResult.Success)
        val fallback = FakeDriver(
            isAvailable = true,
            result = DriverResult.Failure("fallback_used")
        )
        val driver = FallbackExecutionDriver(primary, fallback)

        val result = driver.execute(action())

        assertEquals(DriverResult.Failure("fallback_used"), result)
        assertEquals(1, fallback.calls)
        assertEquals(0, primary.calls)
    }

    @Test
    fun usesShizukuWhenAvailable() {
        val primary = FakeDriver(isAvailable = true, result = DriverResult.Success)
        val fallback = FakeDriver(
            isAvailable = true,
            result = DriverResult.Failure("fallback_used")
        )
        val driver = FallbackExecutionDriver(primary, fallback)

        val result = driver.execute(action())

        assertEquals(DriverResult.Success, result)
        assertEquals(1, primary.calls)
        assertEquals(0, fallback.calls)
    }

    private fun action() = RuntimeAction(
        actionId = "action-1",
        taskId = "task-1",
        taskGeneration = 1,
        type = RuntimeActionType.TAP,
        text = null
    )

    private class FakeDriver(
        override val isAvailable: Boolean,
        private val result: DriverResult
    ) : ExecutionDriver {
        var calls = 0

        override fun execute(action: RuntimeAction): DriverResult {
            calls += 1
            return result
        }

        override fun cancel() = Unit
    }
}
