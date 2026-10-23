package com.personaljarvis.policy

import com.personaljarvis.observe.UiNodeSnapshot

enum class RuntimeActionType {
    OPEN_APP,
    RESTART_APP,
    TAP,
    INPUT_TEXT,
    CALENDAR_CREATE_EVENT,
    CALENDAR_LIST_EVENTS,
    CALENDAR_UPDATE_EVENT,
    CALENDAR_DELETE_EVENT,
    WECHAT_REPLY_CURRENT,
    SEND_MESSAGE,
    MEDIA_CONTROL
}

enum class PageKind {
    NORMAL,
    LOGIN,
    PAYMENT,
    OTP,
    SECURE,
    PERMISSION
}

enum class RecipientResolution {
    NONE,
    UNIQUE,
    AMBIGUOUS,
    MISSING
}

data class RuntimeAction(
    val actionId: String,
    val taskId: String,
    val taskGeneration: Int,
    val type: RuntimeActionType,
    val text: String?,
    val targetPackage: String? = null,
    val requiredPackage: String? = null,
    val targetNodeId: String? = null,
    val targetX: Float? = null,
    val targetY: Float? = null,
    val keyCode: Int? = null,
    val eventTitle: String? = null,
    val startTimeMillis: Long? = null,
    val endTimeMillis: Long? = null,
    val timeZone: String? = null,
    val titleQuery: String? = null,
    val rangeStartMillis: Long? = null,
    val rangeEndMillis: Long? = null,
    val newTitle: String? = null,
    val newStartTimeMillis: Long? = null,
    val newDurationMinutes: Int? = null,
    val expectedConversationTitle: String? = null
)

data class ObservationSnapshot(
    val foregroundPackage: String,
    val pageTitle: String?,
    val pageKind: PageKind,
    val recipientResolution: RecipientResolution,
    val nodes: List<UiNodeSnapshot>,
    val screenHash: String = ""
)

sealed interface RiskDecision {
    data object Allow : RiskDecision
    data class Confirm(val reason: String) : RiskDecision
    data class Block(val reason: String) : RiskDecision
}

sealed interface DriverResult {
    data object Success : DriverResult
    data class SuccessWithMessage(val message: String) : DriverResult
    data class Failure(val reason: String) : DriverResult
    data class Unsupported(val reason: String) : DriverResult
}

sealed interface ActionResult {
    data class Completed(val message: String? = null) : ActionResult
    data class Rejected(val reason: String) : ActionResult
    data class NeedsConfirmation(val reason: String) : ActionResult
    data class Blocked(val reason: String) : ActionResult
    data class Failed(val reason: String) : ActionResult
}
