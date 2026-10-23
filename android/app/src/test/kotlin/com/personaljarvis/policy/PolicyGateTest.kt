package com.personaljarvis.policy

import com.personaljarvis.execute.ExecutionDriver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyGateTest {
    private val gate = PolicyGate()

    @Test
    fun blocksPaymentScreenEvenForTap() {
        val decision = gate.evaluate(
            action = action(RuntimeActionType.TAP),
            observation = observation(pageKind = PageKind.PAYMENT)
        )

        assertEquals(RiskDecision.Block("account_or_payment_screen"), decision)
    }

    @Test
    fun allowsNormalTapOnANormalScreen() {
        val decision = gate.evaluate(
            action = action(RuntimeActionType.TAP),
            observation = observation()
        )

        assertEquals(RiskDecision.Allow, decision)
    }

    @Test
    fun blocksTapWhenTheExpectedAppIsNotForeground() {
        val decision = gate.evaluate(
            action = action(
                RuntimeActionType.TAP,
                requiredPackage = "com.tencent.mm"
            ),
            observation = observation(foregroundPackage = "com.example")
        )

        assertEquals(
            RiskDecision.Block("unexpected_foreground_app"),
            decision
        )
    }

    @Test
    fun asksUserWhenMessageRecipientIsAmbiguous() {
        val decision = gate.evaluate(
            action = action(RuntimeActionType.SEND_MESSAGE),
            observation = observation(recipient = RecipientResolution.AMBIGUOUS)
        )

        assertEquals(
            RiskDecision.Confirm("ambiguous_message_recipient"),
            decision
        )
    }

    @Test
    fun rejectsAStaleActionBeforeCallingTheDriver() {
        val driver = RecordingDriver()
        val runner = SafeExecutionRunner(driver)
        val action = action(RuntimeActionType.TAP, taskGeneration = 0)

        runner.cancel()
        val result = runner.execute(action, observation())

        assertEquals(ActionResult.Rejected("stale_action"), result)
        assertTrue(driver.executed.isEmpty())
    }

    private fun action(
        type: RuntimeActionType,
        taskGeneration: Int = 1,
        requiredPackage: String? = null
    ) = RuntimeAction(
        actionId = "action-1",
        taskId = "task-1",
        taskGeneration = taskGeneration,
        type = type,
        text = null,
        requiredPackage = requiredPackage
    )

    private fun observation(
        pageKind: PageKind = PageKind.NORMAL,
        recipient: RecipientResolution = RecipientResolution.NONE,
        foregroundPackage: String = "com.example"
    ) = ObservationSnapshot(
        foregroundPackage = foregroundPackage,
        pageTitle = "Example",
        pageKind = pageKind,
        recipientResolution = recipient,
        nodes = emptyList()
    )

    private class RecordingDriver : ExecutionDriver {
        val executed = mutableListOf<RuntimeAction>()

        override fun execute(action: RuntimeAction): DriverResult {
            executed += action
            return DriverResult.Success
        }

        override fun cancel() = Unit
    }
}
