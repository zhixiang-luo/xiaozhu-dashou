package com.personaljarvis.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface VoiceState {
    data object Idle : VoiceState
    data object Listening : VoiceState
    data class Submitted(val text: String) : VoiceState
}

class VoiceSessionController {
    private val mutableState = MutableStateFlow<VoiceState>(VoiceState.Idle)

    val state: StateFlow<VoiceState> = mutableState.asStateFlow()

    fun startPressToTalk() {
        mutableState.value = VoiceState.Listening
    }

    fun stopPressToTalk(text: String) {
        val trimmed = text.trim()
        mutableState.value = if (trimmed.isEmpty()) {
            VoiceState.Idle
        } else {
            VoiceState.Submitted(trimmed)
        }
    }

    fun consumeSubmitted(): String? {
        val text = (mutableState.value as? VoiceState.Submitted)?.text
        if (text != null) {
            mutableState.value = VoiceState.Idle
        }
        return text
    }

    fun cancel() {
        mutableState.value = VoiceState.Idle
    }
}
