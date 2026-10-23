package com.personaljarvis.policy

import com.personaljarvis.execute.ExecutionDriver

class SafeExecutionRunner(
    private val driver: ExecutionDriver,
    private val policy: PolicyGate = PolicyGate(),
    initialGeneration: Int = 1
) {
    private var generation = initialGeneration

    fun execute(
        action: RuntimeAction,
        observation: ObservationSnapshot
    ): ActionResult {
        if (action.taskGeneration != generation) {
            return ActionResult.Rejected("stale_action")
        }

        return when (val decision = policy.evaluate(action, observation)) {
            RiskDecision.Allow -> when (val result = driver.execute(action)) {
                DriverResult.Success -> ActionResult.Completed
                is DriverResult.Failure -> ActionResult.Failed(result.reason)
                is DriverResult.Unsupported -> ActionResult.Failed(result.reason)
            }
            is RiskDecision.Confirm ->
                ActionResult.NeedsConfirmation(decision.reason)
            is RiskDecision.Block -> ActionResult.Blocked(decision.reason)
        }
    }

    fun cancel() {
        generation += 1
        driver.cancel()
    }
}
