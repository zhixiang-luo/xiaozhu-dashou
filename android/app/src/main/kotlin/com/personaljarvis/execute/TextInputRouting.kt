package com.personaljarvis.execute

object TextInputRouting {
    fun requiresClipboard(text: String): Boolean {
        return text.any { it.code > 0x7f }
    }
}
