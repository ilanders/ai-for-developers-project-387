package com.booking.api

import com.booking.generated.model.*
import com.booking.plugins.configureRouting
import com.booking.plugins.configureSerialization
import com.booking.plugins.configureStatusPages
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import kotlin.test.*

fun Application.testModule() {
    configureSerialization()
    configureStatusPages()
    configureRouting()
}

class AdminRoutesTest {

    @Test
    fun `GET admin owner returns owner`() = testApplication {
        application { testModule() }
        val response = client.get("/api/admin/owner")
        assertEquals(HttpStatusCode.OK, response.status)
        val owner = body<Owner>(response)
        assertEquals("owner-1", owner.id)
        assertEquals("UTC", owner.timezone)
    }

    @Test
    fun `POST event-type creates event type`() = testApplication {
        application { testModule() }
        val request = CreateEventTypeRequest("consultation", "Consultation", "30 min consultation", 30)
        val response = client.post("/api/admin/event-types") {
            contentType(ContentType.Application.Json)
            setBody(testJson.encodeToString(request))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val eventType = body<EventType>(response)
        assertEquals("consultation", eventType.id)
        assertEquals(30, eventType.durationMinutes)
    }

    @Test
    fun `POST event-type duplicate id returns 409`() = testApplication {
        application { testModule() }
        val request = CreateEventTypeRequest("dup-1", "Duplicate", "Test duplicate", 30)
        client.post("/api/admin/event-types") {
            contentType(ContentType.Application.Json)
            setBody(testJson.encodeToString(request))
        }
        val response = client.post("/api/admin/event-types") {
            contentType(ContentType.Application.Json)
            setBody(testJson.encodeToString(request))
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
        val error = body<ConflictError>(response)
        assertEquals(ConflictError.Code.CONFLICT, error.code)
    }

    @Test
    fun `POST event-type invalid data returns 422`() = testApplication {
        application { testModule() }
        val request = CreateEventTypeRequest("", "", "", 0)
        val response = client.post("/api/admin/event-types") {
            contentType(ContentType.Application.Json)
            setBody(testJson.encodeToString(request))
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        val error = body<ValidationError>(response)
        assertEquals(ValidationError.Code.VALIDATION_ERROR, error.code)
    }

    @Test
    fun `GET event-types returns list`() = testApplication {
        application { testModule() }
        client.post("/api/admin/event-types") {
            contentType(ContentType.Application.Json)
            setBody(testJson.encodeToString(CreateEventTypeRequest("a", "A", "A desc", 30)))
        }
        client.post("/api/admin/event-types") {
            contentType(ContentType.Application.Json)
            setBody(testJson.encodeToString(CreateEventTypeRequest("b", "B", "B desc", 60)))
        }
        val response = client.get("/api/admin/event-types")
        assertEquals(HttpStatusCode.OK, response.status)
        val list = body<List<EventType>>(response)
        assertEquals(2, list.size)
    }

    @Test
    fun `GET admin bookings returns list`() = testApplication {
        application { testModule() }
        val response = client.get("/api/admin/bookings")
        assertEquals(HttpStatusCode.OK, response.status)
        val list = body<List<Booking>>(response)
        assertTrue(list.isEmpty())
    }

    @Test
    fun `GET availability returns default config`() = testApplication {
        application { testModule() }
        val response = client.get("/api/admin/availability")
        assertEquals(HttpStatusCode.OK, response.status)
        val config = body<AvailabilityConfig>(response)
        assertFalse(config.weekly.isEmpty())
        assertEquals(5, config.weekly.size)
        assertTrue(config.overrides.isEmpty())
    }

    @Test
    fun `PUT availability updates and returns new config`() = testApplication {
        application { testModule() }
        val newConfig = AvailabilityConfig(
            weekly = listOf(
                WeeklyAvailability(
                    dayOfWeek = DayOfWeek.monday,
                    windows = listOf(TimeRange("09:00", "13:00"))
                )
            ),
            overrides = listOf(
                DateOverride(date = "2026-12-25", windows = null)
            )
        )
        val response = client.put("/api/admin/availability") {
            contentType(ContentType.Application.Json)
            setBody(testJson.encodeToString(newConfig))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val config = body<AvailabilityConfig>(response)
        assertEquals(1, config.weekly.size)
        assertEquals(DayOfWeek.monday, config.weekly[0].dayOfWeek)
        assertEquals(1, config.weekly[0].windows.size)
        assertEquals("09:00", config.weekly[0].windows[0].start)
        assertEquals(1, config.overrides.size)
        assertEquals("2026-12-25", config.overrides[0].date)
        assertNull(config.overrides[0].windows)
    }

    @Test
    fun `GET availability returns updated config after PUT`() = testApplication {
        application { testModule() }
        val newConfig = AvailabilityConfig(
            weekly = emptyList(),
            overrides = listOf(
                DateOverride(date = "2026-07-04", windows = listOf(TimeRange("10:00", "14:00")))
            )
        )
        client.put("/api/admin/availability") {
            contentType(ContentType.Application.Json)
            setBody(testJson.encodeToString(newConfig))
        }
        val response = client.get("/api/admin/availability")
        assertEquals(HttpStatusCode.OK, response.status)
        val config = body<AvailabilityConfig>(response)
        assertTrue(config.weekly.isEmpty())
        assertEquals(1, config.overrides.size)
        assertEquals("2026-07-04", config.overrides[0].date)
    }
}
