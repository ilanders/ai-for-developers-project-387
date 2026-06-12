package com.booking.availability

import com.booking.generated.model.*
import com.booking.storage.InMemoryStore
import kotlinx.datetime.*
import kotlin.test.*

class StoreBackedAvailabilityProviderTest {
    private val store = InMemoryStore()
    private val provider = StoreBackedAvailabilityProvider(store)
    private val timezone = TimeZone.UTC

    @Test
    fun `uses weekly windows for configured day`() {
        val monday = LocalDate(2026, 6, 15)
        val windows = provider.getAvailabilityWindows(monday, timezone)
        assertEquals(1, windows.size)
        assertEquals(LocalDateTime(monday, LocalTime(9, 0)).toInstant(timezone), windows[0].start)
        assertEquals(LocalDateTime(monday, LocalTime(17, 0)).toInstant(timezone), windows[0].end)
    }

    @Test
    fun `falls back to all-day for unconfigured day`() {
        val sunday = LocalDate(2026, 6, 14)
        val windows = provider.getAvailabilityWindows(sunday, timezone)
        assertEquals(1, windows.size)
        assertEquals(sunday.atStartOfDayIn(timezone), windows[0].start)
        assertEquals(LocalDateTime(sunday, LocalTime(23, 59)).toInstant(timezone), windows[0].end)
    }

    @Test
    fun `uses date override when present`() {
        store.availabilityConfig = store.availabilityConfig.copy(
            overrides = listOf(
                DateOverride(date = "2026-06-15", windows = listOf(TimeRange("10:00", "12:00")))
            )
        )
        val monday = LocalDate(2026, 6, 15)
        val windows = provider.getAvailabilityWindows(monday, timezone)
        assertEquals(1, windows.size)
        assertEquals(LocalDateTime(monday, LocalTime(10, 0)).toInstant(timezone), windows[0].start)
        assertEquals(LocalDateTime(monday, LocalTime(12, 0)).toInstant(timezone), windows[0].end)
    }

    @Test
    fun `returns empty for holiday override`() {
        store.availabilityConfig = store.availabilityConfig.copy(
            overrides = listOf(
                DateOverride(date = "2026-06-15", windows = null)
            )
        )
        val monday = LocalDate(2026, 6, 15)
        val windows = provider.getAvailabilityWindows(monday, timezone)
        assertTrue(windows.isEmpty())
    }

    @Test
    fun `supports multiple windows per day`() {
        store.availabilityConfig = store.availabilityConfig.copy(
            weekly = listOf(
                WeeklyAvailability(
                    dayOfWeek = DayOfWeek.monday,
                    windows = listOf(TimeRange("09:00", "12:00"), TimeRange("13:00", "17:00"))
                )
            )
        )
        val monday = LocalDate(2026, 6, 15)
        val windows = provider.getAvailabilityWindows(monday, timezone)
        assertEquals(2, windows.size)
        assertEquals(LocalDateTime(monday, LocalTime(9, 0)).toInstant(timezone), windows[0].start)
        assertEquals(LocalDateTime(monday, LocalTime(12, 0)).toInstant(timezone), windows[0].end)
        assertEquals(LocalDateTime(monday, LocalTime(13, 0)).toInstant(timezone), windows[1].start)
        assertEquals(LocalDateTime(monday, LocalTime(17, 0)).toInstant(timezone), windows[1].end)
    }
}
