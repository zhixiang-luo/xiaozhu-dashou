package com.personaljarvis.execute

import com.personaljarvis.policy.RuntimeAction
import com.personaljarvis.policy.RuntimeActionType

data class CalendarEventRequest(
    val title: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val timeZone: String
) {
    companion object {
        private const val MAX_DURATION_MS = 24 * 60 * 60 * 1_000L

        fun from(
            action: RuntimeAction,
            nowMillis: Long = System.currentTimeMillis()
        ): CalendarEventRequest? {
            if (action.type != RuntimeActionType.CALENDAR_CREATE_EVENT) {
                return null
            }
            val title = action.eventTitle?.trim().orEmpty()
            val start = action.startTimeMillis ?: return null
            val end = action.endTimeMillis ?: return null
            val timeZone = action.timeZone?.trim().orEmpty()
            if (title.isEmpty() || title.length > 200) {
                return null
            }
            if (start <= nowMillis || end <= start) {
                return null
            }
            if (end - start > MAX_DURATION_MS) {
                return null
            }
            if (timeZone.isEmpty()) {
                return null
            }
            return CalendarEventRequest(title, start, end, timeZone)
        }
    }
}
