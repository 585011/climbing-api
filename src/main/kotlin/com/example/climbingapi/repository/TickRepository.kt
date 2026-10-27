package com.example.climbingapi.repository

import com.example.climbingapi.model.UserRoute
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class TickRepository(
    private val jdbcTemplate: JdbcTemplate
) {

    private val tickRowMapper = RowMapper { rs, _ ->
        UserRoute(
            id = rs.getInt("id"),
            userId = rs.getInt("user_id"),
            routeId = rs.getInt("route_id"),
            tickedAt = rs.getObject("ticked_at", OffsetDateTime::class.java),
            style = rs.getString("style"),
            rating = rs.getObject("rating") as Int?,
            personalNote = rs.getString("personal_note")
        )
    }

    fun getById(id: Int): UserRoute? {
        val sql = """
            SELECT id, user_id, route_id, ticked_at, style, rating, personal_note
            FROM user_route_ticks
            WHERE id = ?
        """.trimIndent()
        return jdbcTemplate.query(sql, tickRowMapper, id).firstOrNull()
    }

    // Optional filter on the ticked route's area type ('crag' | 'gym'); a NULL parameter matches all
    private val areaTypeFilter = """
        (?::text IS NULL OR EXISTS (
            SELECT 1 FROM routes r
            JOIN walls w ON w.id = r.wall_id
            JOIN climbing_areas a ON a.id = w.area_id
            WHERE r.id = t.route_id AND a.type = ?::text
        ))
    """.trimIndent()

    fun findByUserId(userId: Int, page: Int, size: Int, areaType: String? = null): List<UserRoute> {
        val sql = """
            SELECT t.id, t.user_id, t.route_id, t.ticked_at, t.style, t.rating, t.personal_note
            FROM user_route_ticks t
            WHERE t.user_id = ?
              AND $areaTypeFilter
            ORDER BY t.ticked_at DESC
            LIMIT ? OFFSET ?
        """.trimIndent()
        return jdbcTemplate.query(sql, tickRowMapper, userId, areaType, areaType, size, page * size)
    }

    fun countByUserId(userId: Int, areaType: String? = null): Int {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_route_ticks t WHERE t.user_id = ? AND $areaTypeFilter",
            Int::class.java, userId, areaType, areaType
        ) ?: 0
    }

    fun create(tick: UserRoute): UserRoute {
        val sql = """
            INSERT INTO user_route_ticks (user_id, route_id, style, rating, personal_note)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id, user_id, route_id, ticked_at, style, rating, personal_note
        """.trimIndent()
        return jdbcTemplate.query(
            sql, tickRowMapper,
            tick.userId, tick.routeId, tick.style, tick.rating, tick.personalNote
        ).firstOrNull() ?: error("INSERT RETURNING returned no row")
    }

    fun update(id: Int, tick: UserRoute): UserRoute? {
        val sql = """
            UPDATE user_route_ticks
            SET style = ?, rating = ?, personal_note = ?
            WHERE id = ?
            RETURNING id, user_id, route_id, ticked_at, style, rating, personal_note
        """.trimIndent()
        return jdbcTemplate.query(sql, tickRowMapper, tick.style, tick.rating, tick.personalNote, id).firstOrNull()
    }

    fun deleteById(id: Int): Boolean {
        return jdbcTemplate.update("DELETE FROM user_route_ticks WHERE id = ?", id) == 1
    }
}