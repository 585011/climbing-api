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
            auth0Id = rs.getString("auth0_id")
        )
    }

    fun getAll(page: Int, size: Int): List<User> {
        val sql = """
            SELECT id, email, display_name, created_at, auth0_id
            FROM users
            ORDER BY id
            LIMIT ? OFFSET ?
        """.trimIndent()
        return jdbcTemplate.query(sql, userRowMapper, size, page * size)
    }

    fun count(): Int {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Int::class.java) ?: 0
    }

    fun getById(id: Int): User? {
        val sql = """
            SELECT id, email, display_name, created_at, auth0_id
            FROM users
            WHERE id = ?
        """.trimIndent()
        return jdbcTemplate.query(sql, userRowMapper, id).firstOrNull()
    }

    fun findByAuth0Id(auth0Id: String): User? {
        val sql = """
            SELECT id, email, display_name, created_at, auth0_id
            FROM users
            WHERE auth0_id = ?
        """.trimIndent()
        return jdbcTemplate.query(sql, userRowMapper, auth0Id).firstOrNull()
    }

    fun create(user: User): User {
        val sql = """
            INSERT INTO users (email, display_name, auth0_id)
            VALUES (?, ?, ?)
            RETURNING id, email, display_name, created_at, auth0_id
        """.trimIndent()
        return jdbcTemplate.query(sql, userRowMapper, user.email, user.displayName, user.auth0Id).firstOrNull()
            ?: error("INSERT RETURNING returned no row")
    }

    fun update(id: Int, user: User): User? {
        val sql = """
            UPDATE users
            SET email = ?, display_name = ?
            WHERE id = ?
            RETURNING id, email, display_name, created_at, auth0_id
        """.trimIndent()
        return jdbcTemplate.query(sql, userRowMapper, user.email, user.displayName, id).firstOrNull()
    }

    fun deleteById(id: Int): Boolean {
        return jdbcTemplate.update("DELETE FROM users WHERE id = ?", id) == 1
    }
}
