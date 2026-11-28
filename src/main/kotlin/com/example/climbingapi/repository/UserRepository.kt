package com.example.climbingapi.repository

import com.example.climbingapi.model.User
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class UserRepository(
    private val jdbcTemplate: JdbcTemplate
) {

    private val userRowMapper = RowMapper { rs, _ ->
        User(
            id = rs.getInt("id"),
            email = rs.getString("email"),
            displayName = rs.getString("display_name"),
            createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
            auth0Id = rs.getString("auth0_id"),
            shareActivity = rs.getBoolean("share_activity")
        )
    }

    // Optional case-insensitive substring match on display_name; the pattern is bound twice (null check + ILIKE).
    private val displayNameFilter = "(?::text IS NULL OR display_name ILIKE ? ESCAPE '\\')"

    /** `%`, `_` and `\` in [query] are matched literally. */
    private fun likePattern(query: String?): String? =
        query?.let { "%" + it.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%" }

    fun getAll(page: Int, size: Int, query: String? = null): List<User> {
        val sql = """
            SELECT id, email, display_name, created_at, auth0_id, share_activity
            FROM users
            WHERE $displayNameFilter
            ORDER BY id
            LIMIT ? OFFSET ?
        """.trimIndent()
        val pattern = likePattern(query)
        return jdbcTemplate.query(sql, userRowMapper, pattern, pattern, size, page * size)
    }

    fun count(query: String? = null): Int {
        val pattern = likePattern(query)
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE $displayNameFilter", Int::class.java, pattern, pattern
        ) ?: 0
    }

    fun getById(id: Int): User? {
        val sql = """
            SELECT id, email, display_name, created_at, auth0_id, share_activity
            FROM users
            WHERE id = ?
        """.trimIndent()
        return jdbcTemplate.query(sql, userRowMapper, id).firstOrNull()
    }

    fun findByAuth0Id(auth0Id: String): User? {
        val sql = """
            SELECT id, email, display_name, created_at, auth0_id, share_activity
            FROM users
            WHERE auth0_id = ?
        """.trimIndent()
        return jdbcTemplate.query(sql, userRowMapper, auth0Id).firstOrNull()
    }

    fun create(user: User): User {
        val sql = """
            INSERT INTO users (email, display_name, auth0_id)
            VALUES (?, ?, ?)
            RETURNING id, email, display_name, created_at, auth0_id, share_activity
        """.trimIndent()
        return jdbcTemplate.query(sql, userRowMapper, user.email, user.displayName, user.auth0Id).firstOrNull()
            ?: error("INSERT RETURNING returned no row")
    }

    fun update(id: Int, user: User): User? {
        val sql = """
            UPDATE users
            SET email = ?, display_name = ?
            WHERE id = ?
            RETURNING id, email, display_name, created_at, auth0_id, share_activity
        """.trimIndent()
        return jdbcTemplate.query(sql, userRowMapper, user.email, user.displayName, id).firstOrNull()
    }

    /** Changes only the account setting; existing Sessions keep their own `shared` value (ADR-0002). */
    fun updateShareActivity(id: Int, shareActivity: Boolean): User? {
        val sql = """
            UPDATE users
            SET share_activity = ?
            WHERE id = ?
            RETURNING id, email, display_name, created_at, auth0_id, share_activity
        """.trimIndent()
        return jdbcTemplate.query(sql, userRowMapper, shareActivity, id).firstOrNull()
    }

    fun deleteById(id: Int): Boolean {
        return jdbcTemplate.update("DELETE FROM users WHERE id = ?", id) == 1
    }
}
