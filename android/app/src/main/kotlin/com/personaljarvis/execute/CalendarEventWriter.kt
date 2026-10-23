package com.personaljarvis.execute

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import com.personaljarvis.policy.DriverResult
import com.personaljarvis.policy.RuntimeAction
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CalendarEventWriter(
    private val context: Context
) : ExecutionDriver {
    override val isAvailable: Boolean
        get() =
            context.checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    override fun execute(action: RuntimeAction): DriverResult {
        val request = CalendarEventRequest.from(action)
            ?: return DriverResult.Failure("invalid_calendar_request")
        if (!isAvailable) {
            return DriverResult.Failure("calendar_permission_missing")
        }
        return when (request) {
            is CalendarEventRequest.Create -> create(request)
            is CalendarEventRequest.ListEvents -> list(request)
            is CalendarEventRequest.Update -> update(request)
            is CalendarEventRequest.Delete -> delete(request)
        }
    }

    override fun cancel() = Unit

    private fun create(request: CalendarEventRequest.Create): DriverResult {
        val calendarId = writableCalendars().firstOrNull()
            ?: return DriverResult.Failure("writable_calendar_missing")
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, request.title)
            put(CalendarContract.Events.DTSTART, request.startTimeMillis)
            put(CalendarContract.Events.DTEND, request.endTimeMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, request.timeZone)
            put(CalendarContract.Events.ALL_DAY, 0)
        }
        val uri = runCatching {
            context.contentResolver.insert(
                CalendarContract.Events.CONTENT_URI,
                values
            )
        }.getOrNull() ?: return DriverResult.Failure("calendar_insert_failed")
        val event = eventById(ContentUris.parseId(uri))
            ?: return DriverResult.Failure("calendar_verification_failed")
        return if (
            event.title == request.title &&
            event.startTimeMillis == request.startTimeMillis &&
            event.endTimeMillis == request.endTimeMillis
        ) {
            DriverResult.Success
        } else {
            DriverResult.Failure("calendar_verification_failed")
        }
    }

    private fun list(request: CalendarEventRequest.ListEvents): DriverResult {
        val events = findEvents(
            rangeStartMillis = request.rangeStartMillis,
            rangeEndMillis = request.rangeEndMillis,
            titleQuery = request.titleQuery
        ) ?: return DriverResult.Failure("calendar_query_failed")
        val message = if (events.isEmpty()) {
            "这个时间范围内没有找到日程。"
        } else {
            val preview = events
                .take(MAX_RESULT_COUNT)
                .joinToString("；") {
                    "${formatTime(it.startTimeMillis, request.timeZone)} ${it.title}"
                }
            "找到 ${events.size} 个日程：$preview"
        }
        return DriverResult.SuccessWithMessage(message)
    }

    private fun update(request: CalendarEventRequest.Update): DriverResult {
        val event = uniqueEvent(
            rangeStartMillis = request.rangeStartMillis,
            rangeEndMillis = request.rangeEndMillis,
            titleQuery = request.titleQuery
        )
        if (event is UniqueEventResult.Failure) {
            return event.result
        }
        val target = (event as UniqueEventResult.Found).event
        if (target.recurring) {
            return DriverResult.Failure(
                "calendar_recurring_event_requires_manual_review"
            )
        }

        val newTitle = request.newTitle ?: target.title
        val newStart = request.newStartTimeMillis ?: target.startTimeMillis
        val durationMinutes = request.newDurationMinutes
            ?: ((target.endTimeMillis - target.startTimeMillis) / 60_000)
                .coerceAtLeast(1L)
                .toInt()
        val newEnd = newStart + durationMinutes * 60_000L
        if (
            newTitle.isBlank() ||
            newTitle.length > 200 ||
            newEnd <= newStart ||
            newEnd - newStart > MAX_DURATION_MS
        ) {
            return DriverResult.Failure("invalid_calendar_update")
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, newTitle)
            put(CalendarContract.Events.DTSTART, newStart)
            put(CalendarContract.Events.DTEND, newEnd)
            put(CalendarContract.Events.EVENT_TIMEZONE, request.timeZone)
        }
        val uri = ContentUris.withAppendedId(
            CalendarContract.Events.CONTENT_URI,
            target.id
        )
        val updated = runCatching {
            context.contentResolver.update(uri, values, null, null)
        }.getOrDefault(0)
        if (updated <= 0) {
            return DriverResult.Failure("calendar_update_failed")
        }
        val verified = eventById(target.id)
            ?: return DriverResult.Failure("calendar_verification_failed")
        return if (
            verified.title == newTitle &&
            verified.startTimeMillis == newStart &&
            verified.endTimeMillis == newEnd &&
            verified.timeZone == request.timeZone
        ) {
            DriverResult.SuccessWithMessage(
                "日程已修改：${formatTime(newStart, request.timeZone)} $newTitle"
            )
        } else {
            DriverResult.Failure("calendar_verification_failed")
        }
    }

    private fun delete(request: CalendarEventRequest.Delete): DriverResult {
        val event = uniqueEvent(
            rangeStartMillis = request.rangeStartMillis,
            rangeEndMillis = request.rangeEndMillis,
            titleQuery = request.titleQuery
        )
        if (event is UniqueEventResult.Failure) {
            return event.result
        }
        val target = (event as UniqueEventResult.Found).event
        if (target.recurring) {
            return DriverResult.Failure(
                "calendar_recurring_event_requires_manual_review"
            )
        }
        val uri = ContentUris.withAppendedId(
            CalendarContract.Events.CONTENT_URI,
            target.id
        )
        val deleted = runCatching {
            context.contentResolver.delete(uri, null, null)
        }.getOrDefault(0)
        if (deleted <= 0 || eventById(target.id) != null) {
            return DriverResult.Failure("calendar_delete_failed")
        }
        return DriverResult.SuccessWithMessage("日程已删除：${target.title}")
    }

    private fun uniqueEvent(
        rangeStartMillis: Long,
        rangeEndMillis: Long,
        titleQuery: String
    ): UniqueEventResult {
        val events = findEvents(
            rangeStartMillis = rangeStartMillis,
            rangeEndMillis = rangeEndMillis,
            titleQuery = titleQuery
        ) ?: return UniqueEventResult.Failure(
            DriverResult.Failure("calendar_query_failed")
        )
        return when (events.size) {
            0 -> UniqueEventResult.Failure(
                DriverResult.Failure("calendar_event_not_found")
            )
            1 -> UniqueEventResult.Found(events.single())
            else -> UniqueEventResult.Failure(
                DriverResult.Failure("calendar_event_ambiguous")
            )
        }
    }

    private fun findEvents(
        rangeStartMillis: Long,
        rangeEndMillis: Long,
        titleQuery: String?
    ): List<EventRecord>? {
        val calendarIds = writableCalendars()
        if (calendarIds.isEmpty()) {
            return null
        }
        val selectionParts = mutableListOf(
            "${CalendarContract.Events.DELETED} = 0",
            "${CalendarContract.Events.CALENDAR_ID} IN " +
                calendarIds.joinToString(
                    prefix = "(",
                    postfix = ")",
                    separator = ","
                ) { "?" },
            "${CalendarContract.Events.DTSTART} < ?",
            "(${CalendarContract.Events.DTEND} > ? OR " +
                "${CalendarContract.Events.DTEND} IS NULL)"
        )
        val selectionArgs = mutableListOf<String>()
        selectionArgs.addAll(calendarIds.map(Long::toString))
        selectionArgs.add(rangeEndMillis.toString())
        selectionArgs.add(rangeStartMillis.toString())
        if (!titleQuery.isNullOrBlank()) {
            selectionParts.add(
                "instr(lower(${CalendarContract.Events.TITLE}), lower(?)) > 0"
            )
            selectionArgs.add(titleQuery)
        }
        return runCatching {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                EVENT_PROJECTION,
                selectionParts.joinToString(" AND "),
                selectionArgs.toTypedArray(),
                "${CalendarContract.Events.DTSTART} ASC"
            )?.use { cursor -> readEvents(cursor) }
        }.getOrNull()
    }

    private fun eventById(eventId: Long): EventRecord? {
        val uri = ContentUris.withAppendedId(
            CalendarContract.Events.CONTENT_URI,
            eventId
        )
        return runCatching {
            context.contentResolver.query(
                uri,
                EVENT_PROJECTION,
                "${CalendarContract.Events.DELETED} = 0",
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    readEvent(cursor)
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun readEvents(cursor: android.database.Cursor): List<EventRecord> {
        val events = mutableListOf<EventRecord>()
        while (cursor.moveToNext()) {
            events.add(readEvent(cursor))
        }
        return events
    }

    private fun readEvent(cursor: android.database.Cursor): EventRecord {
        return EventRecord(
            id = cursor.getLong(
                cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
            ),
            title = cursor.getString(
                cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            ).orEmpty(),
            startTimeMillis = cursor.getLong(
                cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            ),
            endTimeMillis = cursor.getLong(
                cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
            ),
            timeZone = cursor.getString(
                cursor.getColumnIndexOrThrow(
                    CalendarContract.Events.EVENT_TIMEZONE
                )
            ).orEmpty(),
            recurring =
                !cursor.getString(
                    cursor.getColumnIndexOrThrow(CalendarContract.Events.RRULE)
                ).isNullOrBlank() ||
                    !cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            CalendarContract.Events.ORIGINAL_ID
                        )
                    ).isNullOrBlank()
        )
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

    private fun formatTime(epochMillis: Long, timeZone: String): String {
        val zone = runCatching { ZoneId.of(timeZone) }
            .getOrDefault(ZoneId.systemDefault())
        return DateTimeFormatter.ofPattern("MM月dd日 HH:mm")
            .withZone(zone)
            .format(Instant.ofEpochMilli(epochMillis))
    }

    private data class EventRecord(
        val id: Long,
        val title: String,
        val startTimeMillis: Long,
        val endTimeMillis: Long,
        val timeZone: String,
        val recurring: Boolean
    )

    private sealed interface UniqueEventResult {
        data class Found(val event: EventRecord) : UniqueEventResult
        data class Failure(val result: DriverResult) : UniqueEventResult
    }

    private companion object {
        const val MAX_DURATION_MS = 24 * 60 * 60 * 1_000L
        const val MAX_RESULT_COUNT = 20

        val EVENT_PROJECTION = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_TIMEZONE,
            CalendarContract.Events.RRULE,
            CalendarContract.Events.ORIGINAL_ID
        )
    }
}
