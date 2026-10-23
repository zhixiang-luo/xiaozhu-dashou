package com.personaljarvis.execute

import com.personaljarvis.policy.RuntimeAction
import com.personaljarvis.policy.RuntimeActionType
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarEventRequestTest {
    @Test
    fun acceptsAValidMeeting() {
        val request = CalendarEventRequest.from(
            action(
                title = "和朱哥开会",
                start = 1_800_000_000_000,
                end = 1_800_003_600_000
            )
        )

        assertTrue(request is CalendarEventRequest.Create)
    }

    @Test
    fun rejectsEventsLongerThanOneDay() {
        val request = CalendarEventRequest.from(
            action(
                title = "超长会议",
                start = 1_800_000_000_000,
                end = 1_800_000_000_000 + 25 * 60 * 60 * 1_000L
            )
        )

        assertNull(request)
    }

    @Test
    fun buildsAUniqueDeleteRequest() {
        val request = CalendarEventRequest.from(
            action(
                title = "unused",
                start = 1_800_000_000_000,
                end = 1_800_003_600_000,
                type = RuntimeActionType.CALENDAR_DELETE_EVENT,
                queryTitle = "朱哥开会",
                rangeStart = 1_800_000_000_000,
                rangeEnd = 1_800_086_400_000
            )
        )

        assertTrue(request is CalendarEventRequest.Delete)
        require(request is CalendarEventRequest.Delete)
        assertTrue(request.titleQuery == "朱哥开会")
    }

    @Test
    fun rejectsAnUpdateWithoutChanges() {
        val request = CalendarEventRequest.from(
            action(
                title = "unused",
                start = 1_800_000_000_000,
                end = 1_800_003_600_000,
                type = RuntimeActionType.CALENDAR_UPDATE_EVENT,
                queryTitle = "朱哥开会",
                rangeStart = 1_800_000_000_000,
                rangeEnd = 1_800_086_400_000
            )
        )

        assertNull(request)
    }

    private fun action(
        title: String,
        start: Long,
        end: Long,
        type: RuntimeActionType = RuntimeActionType.CALENDAR_CREATE_EVENT,
        queryTitle: String? = null,
        rangeStart: Long? = null,
        rangeEnd: Long? = null
    ) = RuntimeAction(
        actionId = "calendar-1",
        taskId = "task-1",
        taskGeneration = 1,
        type = type,
        text = null,
        eventTitle = title,
        startTimeMillis = start,
        endTimeMillis = end,
        timeZone = "Asia/Shanghai",
        titleQuery = queryTitle,
        rangeStartMillis = rangeStart,
        rangeEndMillis = rangeEnd
    )
}
