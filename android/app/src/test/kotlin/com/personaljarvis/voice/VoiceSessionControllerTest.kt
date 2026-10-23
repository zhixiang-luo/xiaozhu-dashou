package com.personaljarvis.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceSessionControllerTest {
    @Test
    fun startMovesFromIdleToListening() {
        val controller = VoiceSessionController()

        controller.startPressToTalk()

        assertEquals(VoiceState.Listening, controller.state.value)
    }

    @Test
    fun stopSubmitsTrimmedText() {
        val controller = VoiceSessionController()
        controller.startPressToTalk()

        controller.stopPressToTalk("  给小张说我在路上  ")

        assertEquals(
            VoiceState.Submitted("给小张说我在路上"),
            controller.state.value
        )
    }

    @Test
    fun stopWithoutTextReturnsToIdle() {
        val controller = VoiceSessionController()
        controller.startPressToTalk()

        controller.stopPressToTalk("   ")

        assertEquals(VoiceState.Idle, controller.state.value)
    }

    @Test
    fun cancelReturnsToIdle() {
        val controller = VoiceSessionController()
        controller.startPressToTalk()

        controller.cancel()

        assertEquals(VoiceState.Idle, controller.state.value)
    }
}
