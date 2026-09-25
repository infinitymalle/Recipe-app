package dev.malkolm.recipeapp.data.calendar

import java.time.ZonedDateTime

/** A calendar on the phone that events can be added to, e.g. a Google account's calendar. */
data class DeviceCalendar(val id: Long, val displayName: String, val accountName: String)

/** A plain event: no reminders or alerts, it just shows up in the calendar. */
data class CalendarEvent(val title: String, val description: String, val start: ZonedDateTime, val end: ZonedDateTime)

/**
 * The phone's calendars (Android's calendar provider), behind an interface so the sync logic can
 * be tested without one. Every call needs the calendar permissions and may throw
 * [SecurityException] if they were taken away.
 */
interface CalendarGateway {
    /** Calendars the user may add events to. */
    fun writableCalendars(): List<DeviceCalendar>

    /**
     * Writes [event] into [calendarId]: updates [eventId] if it still exists there, otherwise
     * creates a new event (e.g. the user deleted ours in the calendar app). Returns the event's id.
     */
    fun upsertEvent(calendarId: Long, eventId: Long?, event: CalendarEvent): Long

    /** Deletes an event this app created; does nothing if it is already gone. */
    fun deleteEvent(eventId: Long)
}
