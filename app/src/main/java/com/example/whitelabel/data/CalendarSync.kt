package com.example.whitelabel.data

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
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
            Log.d("CalendarSync", "Attempting to insert event: $title")
            Log.d("CalendarSync", "Start time: $startMillis, End time: $endMillis")
            Log.d("CalendarSync", "Calendar ID: $calendarId")
            
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.HAS_ALARM, 1)
                put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                put(CalendarContract.Events.VISIBLE, 1)
                put(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_DEFAULT)
                put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_FREE)
            }
            
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            if (uri != null) {
                val eventId = uri.lastPathSegment?.toLong()
                Log.d("CalendarSync", "Event created successfully with ID: $eventId")
                eventId
            } else {
                Log.e("CalendarSync", "Failed to create event - URI is null")
                null
            }
        } catch (e: Exception) {
            Log.e("CalendarSync", "Error creating calendar event: ${e.message}", e)
            null
        }
    }
    
    fun getAvailableCalendars(context: Context): List<CalendarInfo> {
        return try {
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.IS_PRIMARY
            )
            
            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )
            
            val calendars = mutableListOf<CalendarInfo>()
            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    val name = it.getString(1) ?: "Unknown"
                    val account = it.getString(2) ?: "Unknown"
                    val isPrimary = it.getInt(3) == 1
                    
                    calendars.add(CalendarInfo(id, name, account, isPrimary))
                    Log.d("CalendarSync", "Found calendar: $name (ID: $id, Primary: $isPrimary)")
                }
            }
            
            Log.d("CalendarSync", "Total calendars found: ${calendars.size}")
            calendars
        } catch (e: Exception) {
            Log.e("CalendarSync", "Error getting calendars: ${e.message}", e)
            emptyList()
        }
    }
    
    fun verifyEventExists(context: Context, eventId: Long): Boolean {
        return try {
            val projection = arrayOf(CalendarContract.Events._ID, CalendarContract.Events.TITLE)
            val selection = "${CalendarContract.Events._ID} = ?"
            val selectionArgs = arrayOf(eventId.toString())
            
            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )
            
            val exists = cursor?.count ?: 0 > 0
            cursor?.close()
            
            Log.d("CalendarSync", "Event $eventId exists: $exists")
            exists
        } catch (e: Exception) {
            Log.e("CalendarSync", "Error verifying event: ${e.message}", e)
            false
        }
    }
    
    fun getEventsInCalendar(context: Context, calendarId: Long): List<EventInfo> {
        return try {
            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND
            )
            val selection = "${CalendarContract.Events.CALENDAR_ID} = ?"
            val selectionArgs = arrayOf(calendarId.toString())
            
            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} DESC"
            )
            
            val events = mutableListOf<EventInfo>()
            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    val title = it.getString(1) ?: "Unknown"
                    val start = it.getLong(2)
                    val end = it.getLong(3)
                    
                    events.add(EventInfo(id, title, start, end))
                }
            }
            
            Log.d("CalendarSync", "Found ${events.size} events in calendar $calendarId")
            events
        } catch (e: Exception) {
            Log.e("CalendarSync", "Error getting events: ${e.message}", e)
            emptyList()
        }
    }
}

data class EventInfo(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long
)

data class CalendarInfo(
    val id: Long,
    val name: String,
    val account: String,
    val isPrimary: Boolean
)
