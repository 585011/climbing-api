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

    fun remove(activityId: Int, userId: Int) {
        jdbcTemplate.update("DELETE FROM kudos WHERE activity_id = ? AND user_id = ?", activityId, userId)
    }

    /** Removes [giverId]'s Kudos on every Session of [ownerId] (used when [ownerId] blocks [giverId]). */
    fun deleteByGiverOnSessionsOf(giverId: Int, ownerId: Int) {
        jdbcTemplate.update(
            "DELETE FROM kudos k USING activities a WHERE k.activity_id = a.id AND k.user_id = ? AND a.user_id = ?",
            giverId, ownerId
        )
    }

    /** The climbers who gave Kudos to [activityId], most recent first, leaving out anyone who blocked [viewerId]. */
    fun findGivers(activityId: Int, viewerId: Int, page: Int, size: Int): List<User> {
        val sql = """
            SELECT u.id, u.email, u.display_name, u.created_at, u.auth0_id
            FROM kudos k
            JOIN users u ON u.id = k.user_id
            WHERE k.activity_id = ? AND ${notBlockingViewer("u.id")}
            ORDER BY k.created_at DESC, u.id DESC
            LIMIT ? OFFSET ?
        """.trimIndent()
        return jdbcTemplate.query(sql, userRowMapper, activityId, viewerId, size, page * size)
    }

    fun countGivers(activityId: Int, viewerId: Int): Int =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM kudos k WHERE k.activity_id = ? AND ${notBlockingViewer("k.user_id")}",
            Int::class.java, activityId, viewerId
        ) ?: 0
}
