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

    /** After a Tick is removed: deletes the Session if it has no Ticks left, else recomputes `last_tick_at`. */
    fun deleteOrRefreshSession(sessionId: Int) {
        val deleted = jdbcTemplate.update(
            """
            DELETE FROM activities a
            WHERE a.id = ? AND NOT EXISTS (SELECT 1 FROM user_route_ticks t WHERE t.activity_id = a.id)
            """.trimIndent(),
            sessionId
        )
        if (deleted == 0) {
            jdbcTemplate.update(
                """
                UPDATE activities
                SET last_tick_at = (SELECT MAX(ticked_at) FROM user_route_ticks WHERE activity_id = ?)
                WHERE id = ?
                """.trimIndent(),
                sessionId, sessionId
            )
        }
    }
}
