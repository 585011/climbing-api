package com.example.climbingapi.repository

import com.example.climbingapi.model.User
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class KudosRepository(
    private val jdbcTemplate: JdbcTemplate
) {

    private val userRowMapper = RowMapper { rs, _ ->
        User(
            id = rs.getInt("id"),
            email = rs.getString("email"),
            displayName = rs.getString("display_name"),
            createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
            auth0Id = rs.getString("auth0_id")
        )
    }

    /** Idempotent: giving Kudos twice keeps the first. */
    fun give(activityId: Int, userId: Int) {
        jdbcTemplate.update(
            "INSERT INTO kudos (activity_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
            activityId, userId
        )
    }

    fun remove(activityId: Int, userId: Int) {
        jdbcTemplate.update("DELETE FROM kudos WHERE activity_id = ? AND user_id = ?", activityId, userId)
    }

    /** The climbers who gave Kudos to [activityId], most recent first. */
    fun findGivers(activityId: Int, page: Int, size: Int): List<User> {
        val sql = """
            SELECT u.id, u.email, u.display_name, u.created_at, u.auth0_id
            FROM kudos k
            JOIN users u ON u.id = k.user_id
            WHERE k.activity_id = ?
            ORDER BY k.created_at DESC, u.id DESC
            LIMIT ? OFFSET ?
        """.trimIndent()
        return jdbcTemplate.query(sql, userRowMapper, activityId, size, page * size)
    }

    fun countGivers(activityId: Int): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kudos WHERE activity_id = ?", Int::class.java, activityId) ?: 0
}
