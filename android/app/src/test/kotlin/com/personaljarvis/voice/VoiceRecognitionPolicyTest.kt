package com.personaljarvis.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceRecognitionPolicyTest {
    private val policy = VoiceRecognitionPolicy()

    @Test
    fun retriesSilentRecognitionErrorsWhileFingerIsHeld() {
        assertEquals(
            VoiceErrorAction.Retry,
            policy.onError(isHolding = true, hasText = false)
        )
    }

    @Test
    fun submitsPartialTextAfterRelease() {
        assertEquals(
            VoiceErrorAction.SubmitText,
            policy.onError(isHolding = false, hasText = true)
        )
    }

    @Test
    fun reportsEmptyOnlyAfterRelease() {
        assertEquals(
            VoiceErrorAction.ShowEmpty,
            policy.onError(isHolding = false, hasText = false)
        )
    }
}
