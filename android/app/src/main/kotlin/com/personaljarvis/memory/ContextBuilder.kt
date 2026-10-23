package com.personaljarvis.memory

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import com.personaljarvis.observe.AccessibilityObserver
import com.personaljarvis.wechat.WechatConversationReader
import com.personaljarvis.wechat.WechatMessageDirection
import java.time.Instant
import java.time.ZoneId

class ContextBuilder(
    private val context: Context,
    private val memoryStore: MemoryStore,
    private val wechatReader: WechatConversationReader = WechatConversationReader()
) : AutoCloseable {
    fun build(): TaskContextPayload {
        val now = System.currentTimeMillis()
        val observation = AccessibilityObserver.instance?.snapshot()
        val recentTasks = memoryStore.recentEvents(
            sinceMillis = now - 24 * 60 * 60 * 1_000,
            kinds = setOf("task"),
            limit = 20
        )
        val relevantMemory = memoryStore.recentEvents(
            sinceMillis = now - 7 * 24 * 60 * 60 * 1_000,
            kinds = setOf("commitment", "message_summary", "page_summary"),
            limit = 10
        )
        val currentConversation = if (
            observation?.foregroundPackage == WECHAT_PACKAGE
        ) {
            runCatching { wechatReader.read() }
                .getOrNull()
                ?.let { state ->
                    CurrentConversationPayload(
                        title = state.title,
                        appPackage = WECHAT_PACKAGE,
                        recentMessages = state.recentMessages.map { message ->
                            WechatMessagePayload(
                                direction = when (message.direction) {
                                    WechatMessageDirection.INCOMING ->
                                        "incoming"
                                    WechatMessageDirection.OUTGOING ->
                                        "outgoing"
                                    WechatMessageDirection.UNKNOWN ->
                                        "unknown"
                                },
                                text = message.text
                            )
                        },
                        capturedAt = Instant.ofEpochMilli(now).toString()
                    )
                }
        } else {
            null
        }

        return TaskContextPayload(
            currentTime = Instant.ofEpochMilli(now).toString(),
            timeZone = ZoneId.systemDefault().id,
            currentPackage = observation?.foregroundPackage,
            screenSummary = observation?.pageTitle,
            currentConversation = currentConversation,
            upcomingEvents = upcomingEvents(now),
            recentTasks = recentTasks.map {
                RecentTaskContext(
                    summary = it.summary,
                    occurredAt = Instant.ofEpochMilli(it.occurredAt).toString()
                )
            },
            relevantMemory = relevantMemory.map {
                RelevantMemoryContext(
                    kind = it.kind,
                    summary = it.summary,
                    occurredAt = Instant.ofEpochMilli(it.occurredAt).toString()
                )
            },
            contacts = memoryStore.contacts(),
            preferences = memoryStore.preferences()
        )
    }

    private fun upcomingEvents(now: Long): List<CalendarContextEvent> {
        if (
            context.checkSelfPermission(Manifest.permission.READ_CALENDAR) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }
        val calendarIds = writableCalendars()
        if (calendarIds.isEmpty()) {
            return emptyList()
        }
        val rangeEnd = now + 14 * 24 * 60 * 60 * 1_000
        val selection =
            "${CalendarContract.Events.DELETED} = 0 AND " +
                "${CalendarContract.Events.CALENDAR_ID} IN " +
                calendarIds.joinToString(
                    prefix = "(",
                    postfix = ")",
                    separator = ","
                ) { "?" } +
                " AND ${CalendarContract.Events.DTSTART} < ? AND " +
                "(${CalendarContract.Events.DTEND} > ? OR " +
                "${CalendarContract.Events.DTEND} IS NULL)"
        val args = buildList {
            addAll(calendarIds.map(Long::toString))
            add(rangeEnd.toString())
            add(now.toString())
        }.toTypedArray()
        val events = runCatching {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.DTEND,
                    CalendarContract.Events.EVENT_TIMEZONE
                ),
                selection,
                args,
                "${CalendarContract.Events.DTSTART} ASC"
            )?.use { cursor ->
                val titleIndex = cursor.getColumnIndexOrThrow(
                    CalendarContract.Events.TITLE
                )
                val startIndex = cursor.getColumnIndexOrThrow(
                    CalendarContract.Events.DTSTART
                )
                val endIndex = cursor.getColumnIndexOrThrow(
                    CalendarContract.Events.DTEND
                )
                val zoneIndex = cursor.getColumnIndexOrThrow(
                    CalendarContract.Events.EVENT_TIMEZONE
                )
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            CalendarContextEvent(
                                title = cursor.getString(titleIndex).orEmpty(),
                                startTime = Instant.ofEpochMilli(
                                    cursor.getLong(startIndex)
                                ).toString(),
                                endTime = Instant.ofEpochMilli(
                                    cursor.getLong(endIndex)
                                ).toString(),
                                timeZone = cursor.getString(zoneIndex)
                                    .orEmpty()
                                    .ifBlank {
                                        ZoneId.systemDefault().id
                                    }
                            )
                        )
                    }
                }
            }
        }.getOrNull().orEmpty()
        return events
            .distinctBy {
                "${it.title}|${it.startTime}|${it.endTime}"
            }
            .take(20)
    }

    override fun close() {
        wechatReader.close()
    }

    private fun writableCalendars(): List<Long> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        return runCatching {
            val calendars = mutableListOf<Pair<Long, Int>>()
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.VISIBLE} = 1",
                null,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(
                    CalendarContract.Calendars._ID
                )
                val accessIndex = cursor.getColumnIndexOrThrow(
                    CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
                )
                while (cursor.moveToNext()) {
                    val access = cursor.getInt(accessIndex)
                    if (
                        access >=
                        CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR
                    ) {
                        calendars.add(cursor.getLong(idIndex) to access)
                    }
                }
            }
            calendars.sortedByDescending { it.second }.map { it.first }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val WECHAT_PACKAGE = "com.tencent.mm"
    }
}
