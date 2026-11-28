package com.example.climbingapi.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime

/** Sessions created from Ticks (ADR-0001) and the Sharing snapshot (ADR-0002). */
class SessionIT : IntegrationTestBase() {

    private var userId = 0
    private var areaA = 0
    private var routeA1 = 0
    private var routeA2 = 0
    private var routeB1 = 0

    @BeforeEach
    fun setup() {
        userId = extractId(postJson("/api/users/me", """{"displayName":"Alex","email":"test@example.com"}"""))
        areaA = extractId(postJson("/api/climbing-areas", """{"name":"Area A"}"""))
        val wallA = extractId(postJson("/api/walls", """{"areaId":$areaA,"name":"Wall A"}"""))
        routeA1 = extractId(postJson("/api/routes", """{"wallId":$wallA,"name":"A1"}"""))
        routeA2 = extractId(postJson("/api/routes", """{"wallId":$wallA,"name":"A2"}"""))
        val areaB = extractId(postJson("/api/climbing-areas", """{"name":"Area B","type":"gym"}"""))
        val wallB = extractId(postJson("/api/walls", """{"areaId":$areaB,"name":"Wall B"}"""))
        routeB1 = extractId(postJson("/api/routes", """{"wallId":$wallB,"name":"B1"}"""))
    }

    private fun tick(routeId: Int): Int =
        extractId(postJson("/api/users/$userId/ticks", """{"routeId":$routeId}""", testJwt()))

    private fun sessionOf(tickId: Int): Int? =
        jdbcTemplate.queryForObject("SELECT activity_id FROM user_route_ticks WHERE id = ?", Int::class.javaObjectType, tickId)

    private fun sessionCount(): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM activities", Int::class.java)!!

    private fun shared(sessionId: Int): Boolean =
        jdbcTemplate.queryForObject("SELECT shared FROM activities WHERE id = ?", Boolean::class.java, sessionId)!!

    private fun lastTickAt(sessionId: Int): OffsetDateTime =
        jdbcTemplate.queryForObject("SELECT last_tick_at FROM activities WHERE id = ?", OffsetDateTime::class.java, sessionId)!!

    private fun setSharing(on: Boolean) {
        mockMvc.perform(
            put("/api/users/me/sharing").with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"shareActivity":$on}""")
        ).andExpect(status().isOk)
    }

    @Test
    fun `two Ticks at the same area on the same day share one Session`() {
        val first = tick(routeA1)
        val second = tick(routeA2)

        assertEquals(1, sessionCount())
        assertEquals(sessionOf(first), sessionOf(second))
        val session = sessionOf(first)!!
        val (type, areaId) = jdbcTemplate.queryForObject(
            "SELECT type, area_id FROM activities WHERE id = ?",
            { rs, _ -> rs.getString("type") to rs.getInt("area_id") }, session
        )!!
        assertEquals("session", type)
        assertEquals(areaA, areaId)
        val secondTickedAt = jdbcTemplate.queryForObject(
            "SELECT ticked_at FROM user_route_ticks WHERE id = ?", OffsetDateTime::class.java, second
        )
        assertEquals(secondTickedAt, lastTickAt(session))
    }

    @Test
    fun `a Tick at another area starts a separate Session`() {
        val crag = tick(routeA1)
        val gym = tick(routeB1)

        assertEquals(2, sessionCount())
        assertNotEquals(sessionOf(crag), sessionOf(gym))
    }

    @Test
    fun `a Tick on another day starts a separate Session`() {
        val yesterday = tick(routeA1)
        // Move the first Tick and its Session back one day.
        jdbcTemplate.update("UPDATE user_route_ticks SET ticked_at = ticked_at - INTERVAL '1 day' WHERE id = ?", yesterday)
        jdbcTemplate.update(
            "UPDATE activities SET activity_date = activity_date - 1, last_tick_at = last_tick_at - INTERVAL '1 day' WHERE id = ?",
            sessionOf(yesterday)
        )

        val today = tick(routeA2)

        assertEquals(2, sessionCount())
        assertNotEquals(sessionOf(yesterday), sessionOf(today))
    }

    @Test
    fun `a new Session copies the Sharing setting and keeps it when the setting changes`() {
        val sharedSession = sessionOf(tick(routeA1))!!
        assertEquals(true, shared(sharedSession))

        setSharing(false)
        assertEquals(true, shared(sharedSession), "turning Sharing off must not rewrite existing Sessions")

        val hiddenSession = sessionOf(tick(routeB1))!!
        assertEquals(false, shared(hiddenSession))

        setSharing(true)
        assertEquals(false, shared(hiddenSession), "a Session created while Sharing was off stays hidden")

        // A later Tick in the same Session doesn't re-snapshot the setting either.
        tick(routeA2)
        assertEquals(true, shared(sharedSession))
    }

    @Test
    fun `deleting the last Tick of a Session deletes the Session`() {
        val only = tick(routeA1)

        mockMvc.perform(delete("/api/users/$userId/ticks/$only").with(testJwt())).andExpect(status().isNoContent)

        assertEquals(0, sessionCount())
    }

    @Test
    fun `deleting one of two Ticks keeps the Session and recomputes last_tick_at`() {
        val first = tick(routeA1)
        val second = tick(routeA2)
        val session = sessionOf(first)!!
        // Make the order unambiguous: the first Tick was an hour earlier.
        jdbcTemplate.update("UPDATE user_route_ticks SET ticked_at = ticked_at - INTERVAL '1 hour' WHERE id = ?", first)
        val firstTickedAt = jdbcTemplate.queryForObject(
            "SELECT ticked_at FROM user_route_ticks WHERE id = ?", OffsetDateTime::class.java, first
        )

        mockMvc.perform(delete("/api/users/$userId/ticks/$second").with(testJwt())).andExpect(status().isNoContent)

        assertEquals(1, sessionCount())
        assertEquals(firstTickedAt, lastTickAt(session))
    }

    @Test
    fun `deleting a climbing area deletes its Sessions`() {
        tick(routeA1)
        val gymTick = tick(routeB1)

        mockMvc.perform(delete("/api/climbing-areas/$areaA").with(adminJwt())).andExpect(status().isNoContent)

        assertEquals(1, sessionCount())
        assertEquals(sessionOf(gymTick), jdbcTemplate.queryForObject("SELECT id FROM activities", Int::class.java))
    }

    @Test
    fun `deleting a route deletes Sessions left empty and refreshes the rest`() {
        // Session at A: Ticks on A1 (an hour earlier) and A2. Session at B: only B1.
        val a1 = tick(routeA1)
        val a2 = tick(routeA2)
        val b1 = tick(routeB1)
        val sessionA = sessionOf(a1)!!
        jdbcTemplate.update("UPDATE user_route_ticks SET ticked_at = ticked_at - INTERVAL '1 hour' WHERE id = ?", a1)
        val a1TickedAt = jdbcTemplate.queryForObject(
            "SELECT ticked_at FROM user_route_ticks WHERE id = ?", OffsetDateTime::class.java, a1
        )

        mockMvc.perform(delete("/api/routes/$routeA2").with(adminJwt())).andExpect(status().isNoContent)
        assertEquals(a1TickedAt, lastTickAt(sessionA))

        mockMvc.perform(delete("/api/routes/$routeB1").with(adminJwt())).andExpect(status().isNoContent)
        assertEquals(1, sessionCount())
        assertEquals(sessionA, sessionOf(a1))
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_route_ticks WHERE id IN (?, ?)", Int::class.java, a2, b1))
    }

    @Test
    fun `deleting a wall deletes the Sessions left empty`() {
        tick(routeA1)
        val gymTick = tick(routeB1)
        val wallA = jdbcTemplate.queryForObject("SELECT wall_id FROM routes WHERE id = ?", Int::class.java, routeA1)

        mockMvc.perform(delete("/api/walls/$wallA").with(adminJwt())).andExpect(status().isNoContent)

        assertEquals(1, sessionCount())
        assertEquals(sessionOf(gymTick), jdbcTemplate.queryForObject("SELECT id FROM activities", Int::class.java))
    }

    @Test
    fun `Ticks from before the Feed keep no Session and can still be deleted`() {
        val old = tick(routeA1)
        jdbcTemplate.update("UPDATE user_route_ticks SET activity_id = NULL WHERE id = ?", old)
        jdbcTemplate.update("DELETE FROM activities")

        mockMvc.perform(delete("/api/users/$userId/ticks/$old").with(testJwt())).andExpect(status().isNoContent)
        assertNull(jdbcTemplate.queryForObject("SELECT MAX(id) FROM activities", Int::class.javaObjectType))
    }
}
