package dev.malkolm.recipeapp.testutil

import dev.malkolm.recipeapp.data.calendar.CalendarEvent
import dev.malkolm.recipeapp.data.calendar.CalendarGateway
import dev.malkolm.recipeapp.data.calendar.DeviceCalendar

/** An in-memory phone calendar: events by id, with the calendar each one is in. */
class FakeCalendarGateway(private val calendars: List<DeviceCalendar> = emptyList()) : CalendarGateway {
    data class StoredEvent(val calendarId: Long, val event: CalendarEvent)

    val events = mutableMapOf<Long, StoredEvent>()
    private var nextId = 1L

    override fun writableCalendars(): List<DeviceCalendar> = calendars

    override fun upsertEvent(calendarId: Long, eventId: Long?, event: CalendarEvent): Long {
        // Like the real gateway: update only an event that still exists in that calendar.
        val id = eventId?.takeIf { events[it]?.calendarId == calendarId } ?: nextId++
        events[id] = StoredEvent(calendarId, event)
        return id
    }

    override fun deleteEvent(eventId: Long) {
        events.remove(eventId)
    }

    fun eventsIn(calendarId: Long): List<CalendarEvent> = events.values.filter {
        it.calendarId == calendarId
    }.map { it.event }
}
