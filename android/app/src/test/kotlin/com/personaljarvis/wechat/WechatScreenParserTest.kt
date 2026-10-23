package com.personaljarvis.wechat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WechatScreenParserTest {
    @Test
    fun extractsChatTitleAndMessageDirections() {
        val state = WechatScreenParser.parse(
            width = 1080,
            height = 2400,
            lines = listOf(
                line("14:23", 36, 30, 250, 80),
                line("文件传输助手", 390, 146, 680, 195),
                line("对方：你好", 190, 366, 760, 420),
                line("13:42", 490, 720, 570, 768),
                line("我：收到", 520, 610, 900, 664)
            )
        )

        assertEquals("文件传输助手", state?.title)
        assertEquals(
            listOf(
                WechatMessage(
                    direction = WechatMessageDirection.INCOMING,
                    text = "对方：你好"
                ),
                WechatMessage(
                    direction = WechatMessageDirection.OUTGOING,
                    text = "我：收到"
                )
            ),
            state?.recentMessages
        )
    }

    @Test
    fun rejectsWechatConversationList() {
        val state = WechatScreenParser.parse(
            width = 1080,
            height = 2400,
            lines = listOf(
                line("微信（1）", 430, 146, 650, 195),
                line("通讯录", 100, 2300, 200, 2350)
            )
        )

        assertNull(state)
    }

    @Test
    fun ignoresStatusBarAndLimitsRecentMessages() {
        val messages = (1..25).map { index ->
            line(
                text = "消息$index",
                100,
                300 + index * 70,
                800,
                350 + index * 70
            )
        }

        val state = WechatScreenParser.parse(
            width = 1080,
            height = 2400,
            lines = listOf(
                line("14:23", 36, 30, 250, 80),
                line("文件传输助手", 390, 146, 680, 195)
            ) + messages
        )

        assertEquals(20, state?.recentMessages?.size)
        assertEquals("消息6", state?.recentMessages?.first()?.text)
        assertEquals("消息25", state?.recentMessages?.last()?.text)
    }

    private fun line(
        text: String,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) = WechatOcrLine(text, left, top, right, bottom)
}
