package com.booking.availability

import com.booking.generated.model.AvailabilityConfig
import com.booking.generated.model.DayOfWeek
import com.booking.storage.InMemoryStore
import kotlinx.datetime.*

class StoreBackedAvailabilityProvider(
    private val store: InMemoryStore
) : AvailabilityProvider {

    override fun getAvailabilityWindows(date: LocalDate, timezone: TimeZone): List<AvailabilityWindow> {
        val config = store.availabilityConfig

        val dateStr = date.toString()
        val override = config.overrides.firstOrNull { it.date == dateStr }
        if (override != null) {
            val windows = override.windows
            if (windows.isNullOrEmpty()) return emptyList()
            return windows.map { toWindow(date, it.start, it.end, timezone) }
        }

        val dayOfWeek = mapDayOfWeek(date.dayOfWeek)
        val weekly = config.weekly.firstOrNull { it.dayOfWeek == dayOfWeek }
        if (weekly != null && weekly.windows.isNotEmpty()) {
            return weekly.windows.map { toWindow(date, it.start, it.end, timezone) }
        }

        val startOfDay = date.atStartOfDayIn(timezone)
        val endOfDay = LocalDateTime(date, LocalTime(23, 59)).toInstant(timezone)
        return listOf(AvailabilityWindow(start = startOfDay, end = endOfDay))
    }

    private fun toWindow(date: LocalDate, start: String, end: String, timezone: TimeZone): AvailabilityWindow {
        val parts = start.split(":")
        val startTime = LocalTime(parts[0].toInt(), parts[1].toInt())
        val endParts = end.split(":")
        val endTime = LocalTime(endParts[0].toInt(), endParts[1].toInt())
        return AvailabilityWindow(
            start = LocalDateTime(date, startTime).toInstant(timezone),
            end = LocalDateTime(date, endTime).toInstant(timezone)
        )
    }

    private fun mapDayOfWeek(kotlinxDay: kotlinx.datetime.DayOfWeek): DayOfWeek = when (kotlinxDay) {
        kotlinx.datetime.DayOfWeek.MONDAY -> DayOfWeek.monday
        kotlinx.datetime.DayOfWeek.TUESDAY -> DayOfWeek.tuesday
        kotlinx.datetime.DayOfWeek.WEDNESDAY -> DayOfWeek.wednesday
        kotlinx.datetime.DayOfWeek.THURSDAY -> DayOfWeek.thursday
        kotlinx.datetime.DayOfWeek.FRIDAY -> DayOfWeek.friday
        kotlinx.datetime.DayOfWeek.SATURDAY -> DayOfWeek.saturday
        kotlinx.datetime.DayOfWeek.SUNDAY -> DayOfWeek.sunday
    }
}
