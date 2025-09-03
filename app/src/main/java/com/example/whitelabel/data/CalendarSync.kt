package com.example.whitelabel.data

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import java.util.TimeZone

object CalendarSync {
    fun insertEvent(
        context: Context,
        title: String,
        description: String,
        startMillis: Long,
        endMillis: Long,
        calendarId: Long = 1L
    ): Long? {
        return try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLong()
        } catch (_: Throwable) {
            null
        }
    }
}
