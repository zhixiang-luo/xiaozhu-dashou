package com.personaljarvis.execute

import com.personaljarvis.policy.DriverResult
import com.personaljarvis.policy.RuntimeAction
import com.personaljarvis.policy.RuntimeActionType
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutedExecutionDriverTest {
    @Test
    fun routesCurrentConversationRepliesToWechatDriver() {
        val wechat = RecordingDriver()
        val routed = RoutedExecutionDriver(
            calendar = RecordingDriver(),
            general = RecordingDriver(),
            wechat = wechat
        )

        val result = routed.execute(
            RuntimeAction(
                actionId = "wechat-1",
                taskId = "task-1",
                taskGeneration = 1,
                type = RuntimeActionType.WECHAT_REPLY_CURRENT,
                text = "收到",
                expectedConversationTitle = "文件传输助手"
            )
        )

        assertEquals(DriverResult.Success, result)
        assertEquals(1, wechat.calls)
    }

    private class RecordingDriver(
        override val isAvailable: Boolean = true
    ) : ExecutionDriver {
        var calls = 0

        override fun execute(action: RuntimeAction): DriverResult {
            calls += 1
            return DriverResult.Success
        }

        override fun cancel() = Unit
    }
}
