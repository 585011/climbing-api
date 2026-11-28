package com.example.climbingapi.repository

import com.example.climbingapi.model.Session
import com.example.climbingapi.model.SessionTick
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime

/** Activities shown in the Feed. Today every Activity is a Session (ADR-0001). */
@Repository
class ActivityRepository(
    private val jdbcTemplate: JdbcTemplate
) {

    private val sessionRowMapper = RowMapper { rs, _ ->
        Session(
            id = rs.getInt("id"),
            userId = rs.getInt("user_id"),
            userDisplayName = rs.getString("display_name"),
            areaId = rs.getInt("area_id"),
            areaName = rs.getString("area_name"),
            areaType = rs.getString("area_type"),
            date = rs.getObject("activity_date", LocalDate::class.java),
            lastTickAt = rs.getObject("last_tick_at", OffsetDateTime::class.java),
            shared = rs.getBoolean("shared"),
            kudosCount = rs.getInt("kudos_count"),
            hasGivenKudos = rs.getBoolean("has_given_kudos")
        )
    }

    private val sessionTickRowMapper = RowMapper { rs, _ ->
        SessionTick(
            id = rs.getInt("id"),
            sessionId = rs.getInt("activity_id"),
            tickedAt = rs.getObject("ticked_at", OffsetDateTime::class.java),
            style = rs.getString("style"),
            rating = rs.getObject("rating") as Int?,
            photoKey = rs.getString("photo_key"),
            routeId = rs.getInt("route_id"),
            routeName = rs.getString("route_name"),
            grade = rs.getString("grade"),
            gradeBand = rs.getString("grade_band"),
            holdColor = rs.getString("hold_color"),
            routeStyle = rs.getString("route_style"),
            wallId = rs.getInt("wall_id"),
            wallName = rs.getString("wall_name"),
            retired = rs.getBoolean("retired")
        )
    }

    /**
     * The visibility rule (ADR-0002), the only place it is written down. A Session is visible to the viewer if the
     * viewer owns it, or follows its climber while the climber's Sharing setting is on and the Session is shared.
     * Takes the viewer's user id twice.
     */
    private val visibleToViewer = """
        (a.user_id = ? OR (
            a.shared AND u.share_activity
            AND a.user_id IN (SELECT f.followee_id FROM follows f WHERE f.follower_id = ?)
        ))
    """.trimIndent()

    /** Takes the viewer's user id once, for `has_given_kudos`. */
    private val sessionSelect = """
        SELECT a.id, a.user_id, u.display_name, a.area_id, ca.name AS area_name, ca.type AS area_type,
               a.activity_date, a.last_tick_at, a.shared,
               (SELECT COUNT(*) FROM kudos k WHERE k.activity_id = a.id) AS kudos_count,
               EXISTS (SELECT 1 FROM kudos k WHERE k.activity_id = a.id AND k.user_id = ?) AS has_given_kudos
        FROM activities a
        JOIN users u ON u.id = a.user_id
        JOIN climbing_areas ca ON ca.id = a.area_id
    """.trimIndent()

    /** The Following Feed of [viewerId]: most recently active first. Ticks are not loaded. */
    fun findFeed(viewerId: Int, page: Int, size: Int): List<Session> {
        val sql = """
            $sessionSelect
            WHERE a.type = 'session' AND $visibleToViewer
            ORDER BY a.last_tick_at DESC, a.id DESC
            LIMIT ? OFFSET ?
        """.trimIndent()
        return jdbcTemplate.query(sql, sessionRowMapper, viewerId, viewerId, viewerId, size, page * size)
    }

    fun countFeed(viewerId: Int): Int {
        val sql = """
            SELECT COUNT(*)
            FROM activities a
            JOIN users u ON u.id = a.user_id
            WHERE a.type = 'session' AND $visibleToViewer
        """.trimIndent()
        return jdbcTemplate.queryForObject(sql, Int::class.java, viewerId, viewerId) ?: 0
    }

    /** The Session [id] if [viewerId] may see it, else null (same as a Session that doesn't exist). */
    fun findVisibleSession(id: Int, viewerId: Int): Session? {
        val sql = """
            $sessionSelect
            WHERE a.id = ? AND a.type = 'session' AND $visibleToViewer
        """.trimIndent()
        return jdbcTemplate.query(sql, sessionRowMapper, viewerId, id, viewerId, viewerId).firstOrNull()
    }

    /** The Ticks of all [sessionIds] in one query, each Session's Ticks in the order they were logged. */
    fun findTicksBySessionIds(sessionIds: List<Int>): List<SessionTick> {
        if (sessionIds.isEmpty()) return emptyList()
        val placeholders = sessionIds.joinToString(", ") { "?" }
        val sql = """
            SELECT t.id, t.activity_id, t.ticked_at, t.style, t.rating, t.photo_key,
                   r.id AS route_id, r.name AS route_name, r.grade, r.grade_band, r.hold_color, r.style AS route_style,
                   (r.retired_at IS NOT NULL) AS retired,
                   w.id AS wall_id, w.name AS wall_name
            FROM user_route_ticks t
            JOIN routes r ON r.id = t.route_id
            JOIN walls w ON w.id = r.wall_id
            WHERE t.activity_id IN ($placeholders)
            ORDER BY t.ticked_at, t.id
        """.trimIndent()
        return jdbcTemplate.query(sql, sessionTickRowMapper, *sessionIds.toTypedArray())
    }

    fun updateShared(id: Int, shared: Boolean): Boolean =
        jdbcTemplate.update("UPDATE activities SET shared = ? WHERE id = ?", shared, id) == 1

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
