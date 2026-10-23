package com.personaljarvis.execute

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import com.personaljarvis.policy.DriverResult
import com.personaljarvis.policy.RuntimeAction

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
            ?: return DriverResult.Failure("invalid_calendar_event")
        if (!isAvailable) {
            return DriverResult.Failure("calendar_permission_missing")
        }
        val calendarId = findWritableCalendar()
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
        val eventId = ContentUris.parseId(uri)
        return if (eventMatches(eventId, request)) {
            DriverResult.Success
        } else {
            DriverResult.Failure("calendar_verification_failed")
        }
    }

    override fun cancel() = Unit

    private fun findWritableCalendar(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        return runCatching {
            var bestId: Long? = null
            var bestLevel = -1
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
                        access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR &&
                        access > bestLevel
                    ) {
                        bestId = cursor.getLong(idIndex)
                        bestLevel = access
                    }
                }
            }
            bestId
        }.getOrNull()
    }

    private fun eventMatches(
        eventId: Long,
        request: CalendarEventRequest
    ): Boolean {
        val uri = ContentUris.withAppendedId(
            CalendarContract.Events.CONTENT_URI,
            eventId
        )
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_TIMEZONE
        )
        return runCatching {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use false
                }
                cursor.getString(
                    cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                ) == request.title &&
                    cursor.getLong(
                        cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                    ) == request.startTimeMillis &&
                    cursor.getLong(
                        cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
                    ) == request.endTimeMillis &&
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            CalendarContract.Events.EVENT_TIMEZONE
                        )
                    ) == request.timeZone
            } ?: false
        }.getOrDefault(false)
    }
}
