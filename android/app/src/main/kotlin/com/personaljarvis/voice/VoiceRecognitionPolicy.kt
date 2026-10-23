package com.personaljarvis.voice

sealed interface VoiceErrorAction {
    data object Retry : VoiceErrorAction
    data object SubmitText : VoiceErrorAction
    data object ShowEmpty : VoiceErrorAction
}

class VoiceRecognitionPolicy {
    fun onError(isHolding: Boolean, hasText: Boolean): VoiceErrorAction {
        if (isHolding) {
            return VoiceErrorAction.Retry
        }
        return if (hasText) {
            VoiceErrorAction.SubmitText
        } else {
            VoiceErrorAction.ShowEmpty
        }
    }
}
