package com.personaljarvis.execute

import com.personaljarvis.policy.DriverResult
import com.personaljarvis.policy.RuntimeAction
import com.personaljarvis.policy.RuntimeActionType
import com.personaljarvis.wechat.WechatScreenState

interface WechatReplyUi {
    fun read(): WechatScreenState?
    fun focusInput(): Boolean
    fun paste(text: String): Boolean
    fun tapSend(): Boolean
}

class WechatReplyDriver(
    private val ui: WechatReplyUi,
    private val debug: (String) -> Unit = {}
) : ExecutionDriver {
    override val isAvailable: Boolean
        get() = true

    override fun execute(action: RuntimeAction): DriverResult {
        if (action.type != RuntimeActionType.WECHAT_REPLY_CURRENT) {
            return DriverResult.Unsupported("wechat_reply_action_required")
        }
        val expectedTitle = action.expectedConversationTitle
            ?.trim()
            .orEmpty()
        val message = action.text?.trim().orEmpty()
        if (expectedTitle.isEmpty() || message.isEmpty()) {
            return DriverResult.Failure("invalid_wechat_reply")
        }

        val initial = ui.read()
            ?: return DriverResult.Failure("wechat_screen_unavailable")
        if (!matchesTitle(initial, expectedTitle)) {
            return DriverResult.Failure(
                "wechat_conversation_mismatch_before_input"
            )
        }
        if (!ui.focusInput()) {
            return DriverResult.Failure("wechat_input_focus_failed")
        }
        val focused = ui.read()
            ?: return DriverResult.Failure("wechat_screen_unavailable")
        if (!matchesTitle(focused, expectedTitle)) {
            return DriverResult.Failure(
                "wechat_conversation_mismatch_after_focus"
            )
        }
        if (!ui.paste(message)) {
            return DriverResult.Failure("wechat_paste_failed")
        }
        val drafted = ui.read()
            ?: return DriverResult.Failure("wechat_screen_unavailable")
        if (!matchesTitle(drafted, expectedTitle)) {
            return DriverResult.Failure(
                "wechat_conversation_mismatch_after_paste"
            )
        }
        val draftLines = drafted.lines
            .filter { line ->
                line.centerY / drafted.screenHeight in INPUT_TOP..INPUT_BOTTOM &&
                    line.centerX / drafted.screenWidth < INPUT_TEXT_MAX_X
            }
        val draftText = draftLines
            .joinToString("") { it.text }
        debug(
            "wechat_draft_lines=${draftLines.size} " +
                "draft_length=${normalize(draftText).length} " +
                "expected_length=${normalize(message).length} " +
                "similarity=${fuzzySimilarity(
                    normalize(draftText),
                    normalize(message)
                )}"
        )
        if (!draftMatches(draftText, message)) {
            return DriverResult.Failure("wechat_draft_not_verified")
        }
        if (!ui.tapSend()) {
            return DriverResult.Failure("wechat_send_tap_failed")
        }
        val sent = ui.read()
            ?: return DriverResult.Failure("wechat_screen_unavailable")
        if (!matchesTitle(sent, expectedTitle)) {
            return DriverResult.Failure(
                "wechat_conversation_mismatch_after_send"
            )
        }
        val sentChatText = sent.lines
            .filter { line ->
                line.centerY / sent.screenHeight in SENT_CHAT_TOP..SENT_CHAT_BOTTOM
            }
            .joinToString("") { it.text }
        val remainingInputText = sent.lines
            .filter { line ->
                line.centerY / sent.screenHeight in
                    POST_SEND_INPUT_TOP..POST_SEND_INPUT_BOTTOM &&
                    line.centerX / sent.screenWidth < INPUT_TEXT_MAX_X
            }
            .joinToString("") { it.text }
        val outgoingVerified = similarText(sentChatText, message) &&
            !similarText(remainingInputText, message)
        return if (outgoingVerified) {
            DriverResult.SuccessWithMessage("消息已发送：$expectedTitle")
        } else {
            DriverResult.Failure("wechat_sent_message_not_verified")
        }
    }

    override fun cancel() = Unit

    private fun matchesTitle(
        screen: WechatScreenState,
        expectedTitle: String
    ): Boolean {
        val actual = normalize(screen.title)
        val expected = normalize(expectedTitle)
        if (actual == expected) {
            return true
        }
        if (
            actual.length < MIN_FUZZY_TITLE_LENGTH ||
            expected.length < MIN_FUZZY_TITLE_LENGTH
        ) {
            return false
        }
        val longest = maxOf(actual.length, expected.length)
        val distance = editDistance(actual, expected)
        return distance <= 1 &&
            1.0 - distance.toDouble() / longest >= MIN_TITLE_SIMILARITY
    }

    private fun similarText(actual: String, expected: String): Boolean {
        val normalizedActual = normalize(actual)
        val normalizedExpected = normalize(expected)
        val similarityThreshold =
            if (minOf(normalizedActual.length, normalizedExpected.length) <= 4) {
                MIN_SHORT_MESSAGE_SIMILARITY
            } else {
                MIN_MESSAGE_SIMILARITY
            }
        return normalizedActual == normalizedExpected ||
            normalizedActual.contains(normalizedExpected) ||
            fuzzySimilarity(normalizedActual, normalizedExpected) >=
            similarityThreshold
    }

    private fun draftMatches(actual: String, expected: String): Boolean {
        if (similarText(actual, expected)) {
            return true
        }
        val normalizedActual = normalize(actual)
        val normalizedExpected = normalize(expected)
        return (
            normalizedExpected.length in 4..5 &&
                normalizedActual.length in 4..8 &&
                normalizedActual.take(2) == normalizedExpected.take(2)
            )
    }

    private fun normalize(value: String): String {
        return value.replace(WHITESPACE_OR_PUNCTUATION, "")
    }

    private fun editDistance(left: String, right: String): Int {
        var previous = IntArray(right.length + 1) { it }
        for (leftIndex in left.indices) {
            val current = IntArray(right.length + 1)
            current[0] = leftIndex + 1
            for (rightIndex in right.indices) {
                val substitution = previous[rightIndex] +
                    if (left[leftIndex] == right[rightIndex]) 0 else 1
                current[rightIndex + 1] = minOf(
                    current[rightIndex] + 1,
                    previous[rightIndex + 1] + 1,
                    substitution
                )
            }
            previous = current
        }
        return previous[right.length]
    }

    private fun fuzzySimilarity(left: String, right: String): Double {
        val longest = maxOf(left.length, right.length)
        if (longest < MIN_FUZZY_MESSAGE_LENGTH) {
            return 0.0
        }
        return 1.0 - editDistance(left, right).toDouble() / longest
    }

    private companion object {
        const val MIN_FUZZY_TITLE_LENGTH = 5
        const val MIN_TITLE_SIMILARITY = 0.8
        const val MIN_FUZZY_MESSAGE_LENGTH = 4
        const val MIN_MESSAGE_SIMILARITY = 0.8
        const val MIN_SHORT_MESSAGE_SIMILARITY = 0.75
        const val INPUT_TOP = 0.53f
        const val INPUT_BOTTOM = 0.61f
        const val INPUT_TEXT_MAX_X = 0.82f
        const val SENT_CHAT_TOP = 0.45f
        const val SENT_CHAT_BOTTOM = 0.53f
        const val POST_SEND_INPUT_TOP = 0.53f
        const val POST_SEND_INPUT_BOTTOM = 0.61f
        val WHITESPACE_OR_PUNCTUATION =
            Regex("""[\s，。,.、"'“”‘’：:；;！!？?]""")
    }
}
