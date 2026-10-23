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
            RiskDecision.Allow -> {
                val driverResult = runCatching {
                    driver.execute(action)
                }.getOrElse {
                    DriverResult.Failure("driver_exception")
                }
                when (driverResult) {
                    DriverResult.Success -> ActionResult.Completed()
                    is DriverResult.SuccessWithMessage ->
                        ActionResult.Completed(driverResult.message)
                    is DriverResult.Failure ->
                        ActionResult.Failed(driverResult.reason)
                    is DriverResult.Unsupported ->
                        ActionResult.Failed(driverResult.reason)
                }
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
