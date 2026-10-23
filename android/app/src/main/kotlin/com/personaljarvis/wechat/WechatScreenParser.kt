package com.personaljarvis.wechat

import kotlin.math.abs

data class WechatOcrLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val centerX: Float
        get() = (left + right) / 2f

    val centerY: Float
        get() = (top + bottom) / 2f
}

enum class WechatMessageDirection {
    INCOMING,
    OUTGOING,
    UNKNOWN
}

data class WechatMessage(
    val direction: WechatMessageDirection,
    val text: String
)

data class WechatScreenState(
    val title: String,
    val recentMessages: List<WechatMessage>,
    val lines: List<WechatOcrLine>,
    val screenWidth: Int = 1080,
    val screenHeight: Int = 2400
)

object WechatScreenParser {
    private val bottomNavigationLabels = setOf("微信", "通讯录", "发现", "我")
    private val timePattern = Regex(
        """^(?:\d{1,2}:\d{2}|昨天|前天|星期[一二三四五六日天]|\d{4}年.*)$"""
    )

    fun parse(
        width: Int,
        height: Int,
        lines: List<WechatOcrLine>
    ): WechatScreenState? {
        if (width <= 0 || height <= 0) {
            return null
        }
        val usable = lines
            .map { it.copy(text = it.text.trim()) }
            .filter { it.text.isNotEmpty() }
        if (isConversationList(usable, width, height)) {
            return null
        }

        val titleLine = usable
            .asSequence()
            .filter { line ->
                val centerX = line.centerX / width
                val centerY = line.centerY / height
                centerX in TITLE_LEFT..TITLE_RIGHT &&
                    centerY in TITLE_TOP..TITLE_BOTTOM &&
                    line.text.length <= MAX_TITLE_LENGTH &&
                    !timePattern.matches(line.text)
            }
            .minByOrNull { line ->
                abs(line.centerY / height - TITLE_CENTER_Y)
            }
            ?: return null

        val recentMessages = usable
            .asSequence()
            .filter { line ->
                val centerY = line.centerY / height
                centerY in CHAT_TOP..CHAT_BOTTOM &&
                    line.text.length >= MIN_MESSAGE_LENGTH &&
                    !timePattern.matches(line.text)
            }
            .sortedWith(
                compareBy<WechatOcrLine> { it.centerY }
                    .thenBy { it.centerX }
            )
            .map { line ->
                WechatMessage(
                    direction = directionOf(line, width),
                    text = line.text
                )
            }
            .toList()

        return WechatScreenState(
            title = titleLine.text,
            recentMessages = recentMessages.takeLast(MAX_MESSAGES),
            lines = usable,
            screenWidth = width,
            screenHeight = height
        )
    }

    private fun isConversationList(
        lines: List<WechatOcrLine>,
        width: Int,
        height: Int
    ): Boolean {
        if (width <= 0 || height <= 0) {
            return true
        }
        return lines.any { line ->
            line.text in bottomNavigationLabels &&
                line.centerY / height >= BOTTOM_NAV_TOP
        }
    }

    private fun directionOf(
        line: WechatOcrLine,
        width: Int
    ): WechatMessageDirection {
        val centerX = line.centerX / width
        return when {
            centerX <= INCOMING_MAX_X -> WechatMessageDirection.INCOMING
            centerX >= OUTGOING_MIN_X -> WechatMessageDirection.OUTGOING
            else -> WechatMessageDirection.UNKNOWN
        }
    }

    private const val TITLE_LEFT = 0.24f
    private const val TITLE_RIGHT = 0.76f
    private const val TITLE_TOP = 0.045f
    private const val TITLE_BOTTOM = 0.105f
    private const val TITLE_CENTER_Y = 0.068f
    private const val CHAT_TOP = 0.11f
    private const val CHAT_BOTTOM = 0.90f
    private const val BOTTOM_NAV_TOP = 0.94f
    private const val INCOMING_MAX_X = 0.48f
    private const val OUTGOING_MIN_X = 0.52f
    private const val MIN_MESSAGE_LENGTH = 2
    private const val MAX_MESSAGES = 20
    private const val MAX_TITLE_LENGTH = 100
}
