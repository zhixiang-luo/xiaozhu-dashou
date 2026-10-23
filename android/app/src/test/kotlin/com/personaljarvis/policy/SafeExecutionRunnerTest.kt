package com.personaljarvis.policy

import com.personaljarvis.execute.ExecutionDriver
import org.junit.Assert.assertEquals
import org.junit.Test

class SafeExecutionRunnerTest {
    @Test
    fun convertsDriverExceptionToFailure() {
        val runner = SafeExecutionRunner(
            driver = object : ExecutionDriver {
                override val isAvailable: Boolean = true

                override fun execute(action: RuntimeAction): DriverResult {
                    error("driver exploded")
                }

                override fun cancel() = Unit
            },
            initialGeneration = 1
        )

        val result = runner.execute(
            action = RuntimeAction(
                actionId = "a1",
                taskId = "t1",
                taskGeneration = 1,
                type = RuntimeActionType.TAP,
                text = null
            ),
            observation = ObservationSnapshot(
                foregroundPackage = "com.tencent.mm",
                pageTitle = "文件传输助手",
                pageKind = PageKind.NORMAL,
                recipientResolution = RecipientResolution.NONE,
                nodes = emptyList()
            )
        )

        assertEquals(
            ActionResult.Failed("driver_exception"),
            result
        )
    }
}
