package com.example.climbingapi.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime

/** Activities shown in the Feed. Today every Activity is a Session (ADR-0001). */
@Repository
class ActivityRepository(
    private val jdbcTemplate: JdbcTemplate
) {

    /**
     * Finds or creates [userId]'s Session at the area of [routeId] on [activityDate] and returns its id.
     * A new Session copies the user's Sharing setting; an existing one keeps its own `shared` value.
     */
    fun upsertSession(userId: Int, routeId: Int, activityDate: LocalDate, tickedAt: OffsetDateTime): Int {
        val sql = """
            INSERT INTO activities (user_id, type, area_id, activity_date, shared, last_tick_at)
            SELECT u.id, 'session', w.area_id, ?, u.share_activity, ?
            FROM users u
            JOIN routes r ON r.id = ?
            JOIN walls w ON w.id = r.wall_id
            WHERE u.id = ?
            ON CONFLICT (user_id, area_id, activity_date) WHERE type = 'session'
            DO UPDATE SET last_tick_at = GREATEST(activities.last_tick_at, EXCLUDED.last_tick_at)
            RETURNING id
        """.trimIndent()
        return jdbcTemplate.queryForObject(sql, Int::class.java, activityDate, tickedAt, routeId, userId)
            ?: error("Session upsert returned no row")
    }

    /** The Sessions holding Ticks on [routeId]; collect them before the route's Ticks are removed. */
    fun findSessionIdsByRouteId(routeId: Int): List<Int> =
        jdbcTemplate.queryForList(
            "SELECT DISTINCT activity_id FROM user_route_ticks WHERE route_id = ? AND activity_id IS NOT NULL",
            Int::class.java, routeId
        )

    /** The Sessions holding Ticks on any route of [wallId]; collect them before the wall's Ticks are removed. */
    fun findSessionIdsByWallId(wallId: Int): List<Int> {
        val sql = """
            SELECT DISTINCT t.activity_id
            FROM user_route_ticks t
            JOIN routes r ON r.id = t.route_id
            WHERE r.wall_id = ? AND t.activity_id IS NOT NULL
        """.trimIndent()
        return jdbcTemplate.queryForList(sql, Int::class.java, wallId)
    }

    /**
     * After Ticks are removed: deletes each of [sessionIds] that has no Ticks left and recomputes
     * `last_tick_at` for the rest.
     */
    fun deleteOrRefreshSessions(sessionIds: Collection<Int>) {
        if (sessionIds.isEmpty()) return
        val ids = sessionIds.distinct().toTypedArray()
        val placeholders = ids.joinToString(", ") { "?" }
        jdbcTemplate.update(
            """
            DELETE FROM activities a
            WHERE a.id IN ($placeholders)
              AND NOT EXISTS (SELECT 1 FROM user_route_ticks t WHERE t.activity_id = a.id)
            """.trimIndent(),
            *ids
        )
        // Every Session still left has at least one Tick, so MAX is never NULL.
        jdbcTemplate.update(
            """
            UPDATE activities a
            SET last_tick_at = (SELECT MAX(t.ticked_at) FROM user_route_ticks t WHERE t.activity_id = a.id)
            WHERE a.id IN ($placeholders)
            """.trimIndent(),
            *ids
        )
    }
}
