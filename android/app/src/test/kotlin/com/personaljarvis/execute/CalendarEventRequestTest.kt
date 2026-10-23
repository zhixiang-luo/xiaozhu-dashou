package com.personaljarvis.execute

import com.personaljarvis.policy.RuntimeAction
import com.personaljarvis.policy.RuntimeActionType
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

        assertNotNull(request)
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

    private fun action(
        title: String,
        start: Long,
        end: Long
    ) = RuntimeAction(
        actionId = "calendar-1",
        taskId = "task-1",
        taskGeneration = 1,
        type = RuntimeActionType.CALENDAR_CREATE_EVENT,
        text = null,
        eventTitle = title,
        startTimeMillis = start,
        endTimeMillis = end,
        timeZone = "Asia/Shanghai"
    )
}
