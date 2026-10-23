package com.personaljarvis.execute

import com.personaljarvis.policy.RuntimeAction
import com.personaljarvis.policy.RuntimeActionType

sealed interface CalendarEventRequest {
    data class Create(
        val title: String,
        val startTimeMillis: Long,
        val endTimeMillis: Long,
        val timeZone: String
    ) : CalendarEventRequest

    data class ListEvents(
        val rangeStartMillis: Long,
        val rangeEndMillis: Long,
        val titleQuery: String?,
        val timeZone: String
    ) : CalendarEventRequest

    data class Update(
        val rangeStartMillis: Long,
        val rangeEndMillis: Long,
        val titleQuery: String,
        val newTitle: String?,
        val newStartTimeMillis: Long?,
        val newDurationMinutes: Int?,
        val timeZone: String
    ) : CalendarEventRequest

    data class Delete(
        val rangeStartMillis: Long,
        val rangeEndMillis: Long,
        val titleQuery: String,
        val timeZone: String
    ) : CalendarEventRequest

    companion object {
        private const val MAX_DURATION_MS = 24 * 60 * 60 * 1_000L
        private const val MAX_RANGE_MS = 366 * 24 * 60 * 60 * 1_000L

        fun from(
            action: RuntimeAction,
            nowMillis: Long = System.currentTimeMillis()
        ): CalendarEventRequest? {
            return when (action.type) {
                RuntimeActionType.CALENDAR_CREATE_EVENT ->
                    createRequest(action, nowMillis)
                RuntimeActionType.CALENDAR_LIST_EVENTS ->
                    listRequest(action)
                RuntimeActionType.CALENDAR_UPDATE_EVENT ->
                    updateRequest(action, nowMillis)
                RuntimeActionType.CALENDAR_DELETE_EVENT ->
                    deleteRequest(action)
                else -> null
            }
        }

        private fun createRequest(
            action: RuntimeAction,
            nowMillis: Long
        ): Create? {
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
            return Create(title, start, end, timeZone)
        }

        private fun listRequest(action: RuntimeAction): ListEvents? {
            val range = validRange(
                action.rangeStartMillis,
                action.rangeEndMillis,
                action.timeZone
            ) ?: return null
            val titleQuery = action.titleQuery?.trim().orEmpty()
            if (titleQuery.length > 200) {
                return null
            }
            return ListEvents(
                rangeStartMillis = range.start,
                rangeEndMillis = range.end,
                titleQuery = titleQuery.ifBlank { null },
                timeZone = range.timeZone
            )
        }

        private fun updateRequest(
            action: RuntimeAction,
            nowMillis: Long
        ): Update? {
            val range = validRange(
                action.rangeStartMillis,
                action.rangeEndMillis,
                action.timeZone
            ) ?: return null
            val titleQuery = action.titleQuery?.trim().orEmpty()
            val newTitle = action.newTitle?.trim().orEmpty()
            val newStart = action.newStartTimeMillis
            val newDuration = action.newDurationMinutes
            if (titleQuery.isEmpty() || titleQuery.length > 200) {
                return null
            }
            if (newTitle.length > 200) {
                return null
            }
            if (newTitle.isEmpty() && newStart == null && newDuration == null) {
                return null
            }
            if (newStart != null && newStart <= nowMillis) {
                return null
            }
            if (
                newDuration != null &&
                (newDuration < 1 || newDuration > 1_440)
            ) {
                return null
            }
            return Update(
                rangeStartMillis = range.start,
                rangeEndMillis = range.end,
                titleQuery = titleQuery,
                newTitle = newTitle.ifBlank { null },
                newStartTimeMillis = newStart,
                newDurationMinutes = newDuration,
                timeZone = range.timeZone
            )
        }

        private fun deleteRequest(action: RuntimeAction): Delete? {
            val range = validRange(
                action.rangeStartMillis,
                action.rangeEndMillis,
                action.timeZone
            ) ?: return null
            val titleQuery = action.titleQuery?.trim().orEmpty()
            if (titleQuery.isEmpty() || titleQuery.length > 200) {
                return null
            }
            return Delete(
                rangeStartMillis = range.start,
                rangeEndMillis = range.end,
                titleQuery = titleQuery,
                timeZone = range.timeZone
            )
        }

        private fun validRange(
            start: Long?,
            end: Long?,
            timeZone: String?
        ): Range? {
            if (start == null || end == null || end <= start) {
                return null
            }
            if (end - start > MAX_RANGE_MS) {
                return null
            }
            val normalizedTimeZone = timeZone?.trim().orEmpty()
            if (normalizedTimeZone.isEmpty()) {
                return null
            }
            return Range(start, end, normalizedTimeZone)
        }
    }

    private data class Range(
        val start: Long,
        val end: Long,
        val timeZone: String
    )
}
