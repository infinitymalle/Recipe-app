package dev.malkolm.recipeapp.data.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * [CalendarGateway] on Android's calendar provider. It only ever touches events whose ids it
 * stored itself (see MealPlanCalendarSync), never the user's other events.
 */
class AndroidCalendarGateway
@Inject
constructor(@ApplicationContext private val context: Context) : CalendarGateway {
    private val resolver get() = context.contentResolver

    override fun writableCalendars(): List<DeviceCalendar> {
        val projection = arrayOf(Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME, Calendars.ACCOUNT_NAME)
        val selection = "${Calendars.CALENDAR_ACCESS_LEVEL} >= ? AND ${Calendars.VISIBLE} = 1"
        val args = arrayOf(Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
        return resolver.query(Calendars.CONTENT_URI, projection, selection, args, null)?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        DeviceCalendar(
                            id = cursor.getLong(0),
                            displayName = cursor.getString(1).orEmpty(),
                            accountName = cursor.getString(2).orEmpty()
                        )
                    )
                }
            }
        }.orEmpty()
    }

    override fun upsertEvent(calendarId: Long, eventId: Long?, event: CalendarEvent): Long {
        val values =
            ContentValues().apply {
                put(Events.CALENDAR_ID, calendarId)
                put(Events.TITLE, event.title)
                put(Events.DESCRIPTION, event.description)
                put(Events.DTSTART, event.start.toInstant().toEpochMilli())
                put(Events.DTEND, event.end.toInstant().toEpochMilli())
                put(Events.EVENT_TIMEZONE, event.start.zone.id)
                put(Events.HAS_ALARM, 0)
            }
        if (eventId != null && isLiveEventIn(calendarId, eventId)) {
            resolver.update(ContentUris.withAppendedId(Events.CONTENT_URI, eventId), values, null, null)
            return eventId
        }
        val uri = resolver.insert(Events.CONTENT_URI, values) ?: error("The calendar did not accept the event")
        return ContentUris.parseId(uri)
    }

    override fun deleteEvent(eventId: Long) {
        resolver.delete(ContentUris.withAppendedId(Events.CONTENT_URI, eventId), null, null)
    }

    /**
     * Whether [eventId] still exists, undeleted, in [calendarId]. Synced calendars keep deleted
     * events as rows marked DELETED until their next sync, and updating those would do nothing.
     */
    private fun isLiveEventIn(calendarId: Long, eventId: Long): Boolean {
        val uri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId)
        return resolver.query(uri, arrayOf(Events.CALENDAR_ID, Events.DELETED), null, null, null)?.use { cursor ->
            cursor.moveToFirst() && cursor.getLong(0) == calendarId && cursor.getInt(1) == 0
        } ?: false
    }
}
