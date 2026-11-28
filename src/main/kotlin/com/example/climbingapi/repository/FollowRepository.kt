package com.example.climbingapi.repository

import com.example.climbingapi.model.User
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class FollowRepository(
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

    /** Idempotent: following someone you already follow keeps the original row. A block row is never overwritten. */
    fun follow(followerId: Int, followeeId: Int) {
        jdbcTemplate.update(
            "INSERT INTO follows (follower_id, followee_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
            followerId, followeeId
        )
    }

    /** Leaves a block row in place, so a blocked climber can't clear their own block by unfollowing. */
    fun unfollow(followerId: Int, followeeId: Int) {
        jdbcTemplate.update(
            "DELETE FROM follows WHERE follower_id = ? AND followee_id = ? AND status = 'active'",
            followerId, followeeId
        )
    }

    /** Turns [followerId]'s active Follow of [followeeId] into a block. Returns false if there was no active Follow. */
    fun block(followerId: Int, followeeId: Int): Boolean =
        jdbcTemplate.update(
            "UPDATE follows SET status = 'blocked' WHERE follower_id = ? AND followee_id = ? AND status = 'active'",
            followerId, followeeId
        ) == 1

    /** The climbers [followerId] follows, most recently followed first. */
    fun getFollowing(followerId: Int, page: Int, size: Int): List<User> {
        val sql = """
            SELECT u.id, u.email, u.display_name, u.created_at, u.auth0_id
            FROM follows f
            JOIN users u ON u.id = f.followee_id
            WHERE f.follower_id = ? AND f.status = 'active'
            ORDER BY f.created_at DESC, u.id DESC
            LIMIT ? OFFSET ?
        """.trimIndent()
        return jdbcTemplate.query(sql, userRowMapper, followerId, size, page * size)
    }

    fun countFollowing(followerId: Int): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM follows WHERE follower_id = ? AND status = 'active'", Int::class.java, followerId) ?: 0

    /** The Followers of [followeeId], most recent first. */
    fun getFollowers(followeeId: Int, page: Int, size: Int): List<User> {
        val sql = """
            SELECT u.id, u.email, u.display_name, u.created_at, u.auth0_id
            FROM follows f
            JOIN users u ON u.id = f.follower_id
            WHERE f.followee_id = ? AND f.status = 'active'
            ORDER BY f.created_at DESC, u.id DESC
            LIMIT ? OFFSET ?
        """.trimIndent()
        return jdbcTemplate.query(sql, userRowMapper, followeeId, size, page * size)
    }

    fun countFollowers(followeeId: Int): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM follows WHERE followee_id = ? AND status = 'active'", Int::class.java, followeeId) ?: 0
}
