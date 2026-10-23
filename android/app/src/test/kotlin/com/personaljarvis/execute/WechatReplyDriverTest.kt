package com.personaljarvis.execute

import com.personaljarvis.policy.DriverResult
import com.personaljarvis.policy.RuntimeAction
import com.personaljarvis.policy.RuntimeActionType
import com.personaljarvis.wechat.WechatMessage
import com.personaljarvis.wechat.WechatMessageDirection
import com.personaljarvis.wechat.WechatOcrLine
import com.personaljarvis.wechat.WechatScreenState
import org.junit.Assert.assertEquals
import org.junit.Test

class WechatReplyDriverTest {
    @Test
    fun refusesInputWhenConversationTitleChanged() {
        val ui = FakeWechatReplyUi(
            screens = ArrayDeque(listOf(chat("别的会话")))
        )

        val result = WechatReplyDriver(ui).execute(
            replyAction(
                conversationTitle = "文件传输助手",
                message = "收到"
            )
        )

        assertEquals(
            DriverResult.Failure("wechat_conversation_mismatch_before_input"),
            result
        )
        assertEquals(0, ui.focusCalls)
    }

    @Test
    fun stopsWhenTitleChangesAfterInputFocus() {
        val ui = FakeWechatReplyUi(
            screens = ArrayDeque(
                listOf(
                    chat("文件传输助手"),
                    chat("别的会话")
                )
            )
        )

        val result = WechatReplyDriver(ui).execute(
            replyAction(
                conversationTitle = "文件传输助手",
                message = "收到"
            )
        )

        assertEquals(
            DriverResult.Failure("wechat_conversation_mismatch_after_focus"),
            result
        )
        assertEquals(1, ui.focusCalls)
        assertEquals(0, ui.pasteCalls)
    }

    @Test
    fun doesNotSendWhenDraftIsNotVisible() {
        val ui = FakeWechatReplyUi(
            screens = ArrayDeque(
                listOf(
                    chat("文件传输助手"),
                    chat("文件传输助手"),
                    chat("文件传输助手")
                )
            )
        )

        val result = WechatReplyDriver(ui).execute(
            replyAction(
                conversationTitle = "文件传输助手",
                message = "收到"
            )
        )

        assertEquals(
            DriverResult.Failure("wechat_draft_not_verified"),
            result
        )
        assertEquals(0, ui.sendCalls)
    }

    @Test
    fun reportsFailureWhenSentMessageCannotBeVerified() {
        val ui = FakeWechatReplyUi(
            screens = ArrayDeque(
                listOf(
                    chat("文件传输助手"),
                    chat("文件传输助手"),
                    chat("文件传输助手", draft = "收到"),
                    chat("文件传输助手")
                )
            )
        )

        val result = WechatReplyDriver(ui).execute(
            replyAction(
                conversationTitle = "文件传输助手",
                message = "收到"
            )
        )

        assertEquals(
            DriverResult.Failure("wechat_sent_message_not_verified"),
            result
        )
        assertEquals(1, ui.sendCalls)
    }

    @Test
    fun sendsAfterVerifyingTitleDraftAndSentMessage() {
        val ui = FakeWechatReplyUi(
            screens = ArrayDeque(
                listOf(
                    chat("文件传输助手"),
                    chat("文件传输助手"),
                    chat("文件传输助手", draft = "收到"),
                    chat("文件传输助手", outgoing = "收到")
                )
            )
        )

        val result = WechatReplyDriver(ui).execute(
            replyAction(
                conversationTitle = "文件传输助手",
                message = "收到"
            )
        )

        assertEquals(
            DriverResult.SuccessWithMessage("消息已发送：文件传输助手"),
            result
        )
        assertEquals(1, ui.focusCalls)
        assertEquals(1, ui.pasteCalls)
        assertEquals(1, ui.sendCalls)
    }

    @Test
    fun toleratesOneOcrCharacterInLongConversationTitle() {
        val ui = FakeWechatReplyUi(
            screens = ArrayDeque(
                listOf(
                    chat("文件传輸助手"),
                    chat("文件传輸助手"),
                    chat("文件传輸助手", draft = "收到"),
                    chat("文件传輸助手", outgoing = "收到")
                )
            )
        )

        val result = WechatReplyDriver(ui).execute(
            replyAction(
                conversationTitle = "文件传输助手",
                message = "收到"
            )
        )

        assertEquals(
            DriverResult.SuccessWithMessage("消息已发送：文件传输助手"),
            result
        )
    }

    @Test
    fun verifiesDraftAcrossOcrLineBreaks() {
        val message = "收到，明天一定记得发你"
        val ui = FakeWechatReplyUi(
            screens = ArrayDeque(
                listOf(
                    chat("文件传輸助手"),
                    chat("文件传輸助手"),
                    chatWithDraftLines(
                        title = "文件传輸助手",
                        draftLines = listOf("收到，明天", "一定记得", "发你")
                    ),
                    chat("文件传輸助手", outgoing = message)
                )
            )
        )

        val result = WechatReplyDriver(ui).execute(
            replyAction(
                conversationTitle = "文件传输助手",
                message = message
            )
        )

        assertEquals(
            DriverResult.SuccessWithMessage("消息已发送：文件传输助手"),
            result
        )
    }

    @Test
    fun toleratesMinorOcrErrorsInLongDraft() {
        val message = "收到，明天一定记得发你"
        val ui = FakeWechatReplyUi(
            screens = ArrayDeque(
                listOf(
                    chat("文件传输助手"),
                    chat("文件传输助手"),
                    chatWithDraftLines(
                        title = "文件传输助手",
                        draftLines = listOf(
                            "收到,明天一定的记得",
                            "發你"
                        )
                    ),
                    chat("文件传输助手", outgoing = message)
                )
            )
        )

        val result = WechatReplyDriver(ui).execute(
            replyAction(
                conversationTitle = "文件传输助手",
                message = message
            )
        )

        assertEquals(
            DriverResult.SuccessWithMessage("消息已发送：文件传输助手"),
            result
        )
    }

    @Test
    fun verifiesSentMessageInChatRegionRegardlessOfTextAlignment() {
        val message = "收到，明天一定记得发你"
        val ui = FakeWechatReplyUi(
            screens = ArrayDeque(
                listOf(
                    chat("文件传输助手"),
                    chat("文件传输助手"),
                    chatWithDraftLines(
                        title = "文件传输助手",
                        draftLines = listOf(
                            "收到，明天一定记得",
                            "发你"
                        )
                    ),
                    chatWithSentLine(
                        title = "文件传输助手",
                        message = message,
                        direction = WechatMessageDirection.UNKNOWN
                    )
                )
            )
        )

        val result = WechatReplyDriver(ui).execute(
            replyAction(
                conversationTitle = "文件传输助手",
                message = message
            )
        )

        assertEquals(
            DriverResult.SuccessWithMessage("消息已发送：文件传输助手"),
            result
        )
    }

    @Test
    fun doesNotTreatPreviousSentMessageAsDraft() {
        val message = "不客气，有需要随时找我"
        val ui = FakeWechatReplyUi(
            screens = ArrayDeque(
                listOf(
                    chat("文件传输助手"),
                    chat("文件传输助手"),
                    WechatScreenState(
                        title = "文件传输助手",
                        recentMessages = emptyList(),
                        lines = listOf(
                            line("文件传输助手", 390, 150, 680, 200),
                            line(message, 180, 1_220, 900, 1_270)
                        )
                    )
                )
            )
        )

        val result = WechatReplyDriver(ui).execute(
            replyAction(
                conversationTitle = "文件传输助手",
                message = message
            )
        )

        assertEquals(
            DriverResult.Failure("wechat_draft_not_verified"),
            result
        )
        assertEquals(0, ui.sendCalls)
    }

    @Test
    fun toleratesOneOcrCharacterInFourCharacterDraft() {
        val message = "收到谢谢"
        val ui = FakeWechatReplyUi(
            screens = ArrayDeque(
                listOf(
                    chat("文件传输助手"),
                    chat("文件传输助手"),
                    chatWithDraftLines(
                        title = "文件传输助手",
                        draftLines = listOf("收到谢射")
                    ),
                    chat("文件传输助手", outgoing = message)
                )
            )
        )

        val result = WechatReplyDriver(ui).execute(
            replyAction(
                conversationTitle = "文件传输助手",
                message = message
            )
        )

        assertEquals(
            DriverResult.SuccessWithMessage("消息已发送：文件传输助手"),
            result
        )
    }

    @Test
    fun acceptsShortDraftWhenLeadingCharactersAreStable() {
        val message = "收到谢谢"
        val ui = FakeWechatReplyUi(
            screens = ArrayDeque(
                listOf(
                    chat("文件传输助手"),
                    chat("文件传输助手"),
                    chatWithDraftLines(
                        title = "文件传输助手",
                        draftLines = listOf("收到您好")
                    ),
                    chat("文件传输助手", outgoing = message)
                )
            )
        )

        val result = WechatReplyDriver(ui).execute(
            replyAction(
                conversationTitle = "文件传输助手",
                message = message
            )
        )

        assertEquals(
            DriverResult.SuccessWithMessage("消息已发送：文件传输助手"),
            result
        )
    }

    private fun replyAction(
        conversationTitle: String,
        message: String
    ) = RuntimeAction(
        actionId = "wechat-1",
        taskId = "task-1",
        taskGeneration = 1,
        type = RuntimeActionType.WECHAT_REPLY_CURRENT,
        text = message,
        expectedConversationTitle = conversationTitle
    )

    private fun chat(
        title: String,
        draft: String? = null,
        outgoing: String? = null
    ): WechatScreenState {
        val lines = buildList {
            add(line(title, 390, 150, 680, 200))
            draft?.let { add(line(it, 180, 1390, 850, 1440)) }
            outgoing?.let { add(line(it, 180, 1_150, 900, 1_200)) }
        }
        val messages = outgoing?.let {
            listOf(
                WechatMessage(
                    direction = WechatMessageDirection.OUTGOING,
                    text = it
                )
            )
        }.orEmpty()
        return WechatScreenState(
            title = title,
            recentMessages = messages,
            lines = lines
        )
    }

    private fun chatWithDraftLines(
        title: String,
        draftLines: List<String>
    ): WechatScreenState {
        return WechatScreenState(
            title = title,
            recentMessages = emptyList(),
            lines = buildList {
                add(line(title, 390, 150, 680, 200))
                draftLines.forEachIndexed { index, text ->
                    add(
                        line(
                            text,
                            180,
                            1_300 + index * 55,
                            900,
                            1_350 + index * 55
                        )
                    )
                }
                add(line("发送", 900, 1_370, 1_020, 1_425))
            }
        )
    }

    private fun chatWithSentLine(
        title: String,
        message: String,
        direction: WechatMessageDirection
    ): WechatScreenState {
        return WechatScreenState(
            title = title,
            recentMessages = listOf(
                WechatMessage(
                    direction = direction,
                    text = message
                )
            ),
            lines = listOf(
                line(title, 390, 150, 680, 200),
                line(message, 180, 1_150, 900, 1_200)
            )
        )
    }

    private fun line(
        text: String,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) = WechatOcrLine(text, left, top, right, bottom)

    private class FakeWechatReplyUi(
        private val screens: ArrayDeque<WechatScreenState?>
    ) : WechatReplyUi {
        var focusCalls = 0
        var pasteCalls = 0
        var sendCalls = 0

        override fun read(): WechatScreenState? {
            return if (screens.size > 1) {
                screens.removeFirst()
            } else {
                screens.firstOrNull()
            }
        }

        override fun focusInput(): Boolean {
            focusCalls += 1
            return true
        }

        override fun paste(text: String): Boolean {
            pasteCalls += 1
            return true
        }

        override fun tapSend(): Boolean {
            sendCalls += 1
            return true
        }
    }
}
