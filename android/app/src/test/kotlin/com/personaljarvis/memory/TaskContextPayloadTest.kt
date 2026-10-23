package com.personaljarvis.memory

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskContextPayloadTest {
    @Test
    fun serializesCalendarAndRecentTaskContext() {
        val payload = TaskContextPayload(
            currentTime = "2026-10-23T05:00:00.000Z",
            timeZone = "Asia/Shanghai",
            currentPackage = "com.tencent.mm",
            screenSummary = "文件传输助手",
            upcomingEvents = listOf(
                CalendarContextEvent(
                    title = "和猪哥开会",
                    startTime = "2026-10-24T15:00:00+08:00",
                    endTime = "2026-10-24T16:00:00+08:00",
                    timeZone = "Asia/Shanghai"
                )
            ),
            recentTasks = listOf(
                RecentTaskContext(
                    summary = "创建和猪哥开会的日程",
                    occurredAt = "2026-10-23T04:50:00.000Z"
                )
            ),
            relevantMemory = emptyList(),
            contacts = emptyList(),
            preferences = emptyList()
        )

        val json = JSONObject(payload.toJson())

        assertEquals(
            "和猪哥开会",
            json.getJSONArray("upcomingEvents")
                .getJSONObject(0)
                .getString("title")
        )
        assertEquals(
            "创建和猪哥开会的日程",
            json.getJSONArray("recentTasks")
                .getJSONObject(0)
                .getString("summary")
        )
    }
}
