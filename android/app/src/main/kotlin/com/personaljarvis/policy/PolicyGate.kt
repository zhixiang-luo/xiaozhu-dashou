package com.personaljarvis.policy

class PolicyGate {
    fun evaluate(
        action: RuntimeAction,
        observation: ObservationSnapshot
    ): RiskDecision {
        if (
            observation.pageKind == PageKind.LOGIN ||
            observation.pageKind == PageKind.PAYMENT ||
            observation.pageKind == PageKind.OTP ||
            observation.pageKind == PageKind.SECURE ||
            observation.pageKind == PageKind.PERMISSION
        ) {
            return RiskDecision.Block("account_or_payment_screen")
        }

        if (action.type == RuntimeActionType.SEND_MESSAGE) {
            return when (observation.recipientResolution) {
                RecipientResolution.UNIQUE -> RiskDecision.Allow
                RecipientResolution.AMBIGUOUS ->
                    RiskDecision.Confirm("ambiguous_message_recipient")
                RecipientResolution.MISSING ->
                    RiskDecision.Block("message_recipient_missing")
                RecipientResolution.NONE ->
                    RiskDecision.Block("message_recipient_unresolved")
            }
        }

        return RiskDecision.Allow
    }
}
