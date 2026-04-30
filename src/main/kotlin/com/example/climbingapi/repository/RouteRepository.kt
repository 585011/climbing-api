package com.example.climbingapi.repository

import com.example.climbingapi.model.Route
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class RouteRepository(
    private val jdbcTemplate: JdbcTemplate
) {

    private val routeRowMapper = RowMapper { rs, _ ->
        val createdTime = rs.getObject("created_at", OffsetDateTime::class.java)

        Route(
            id = rs.getInt("id"),
            wallId = rs.getInt("wall_id"),
            name = rs.getString("name"),
            grade = rs.getString("grade"),
            length = rs.getInt("length"),
            style = rs.getString("style"),
            bolts = rs.getInt("bolts"),
            ropeLengths = rs.getInt("rope_lengths"),
            firstAscendant = rs.getString("first_ascendant"),
            description = rs.getString("description"),
            createdAt = createdTime
        )
    }

    fun getAll(): List<Route> {
        val sql = """
            SELECT id,
                   wall_id,
                   name,
                   grade,
                   length,
                   style,
                   bolts,
                   rope_lengths,
                   created_at,
                   first_ascendant,
                   description
            FROM routes
        """.trimIndent()
        return jdbcTemplate.query(sql, routeRowMapper)
    }

    fun getById(id: Int): Route? {
        val sql = """
            SELECT id,
                   wall_id,
                   name,
                   grade,
                   length,
                   style,
                   bolts,
                   rope_lengths,
                   created_at,
                   first_ascendant,
                   description
            FROM routes
            WHERE id = ?
        """.trimIndent()
        return jdbcTemplate.query(sql, routeRowMapper, id).firstOrNull()
    }

    fun findByWallId(wallId: Int): List<Route> {
        val sql = """
            SELECT id,
                   wall_id,
                   name,
                   grade,
                   length,
                   style,
                   bolts,
                   rope_lengths,
                   created_at,
                   first_ascendant,
                   description
            FROM routes
            WHERE wall_id = ?
            ORDER BY id
        """.trimIndent()
        return jdbcTemplate.query(sql, routeRowMapper, wallId)
    }

    fun deleteById(id: Int): Boolean {
        return jdbcTemplate.update("DELETE FROM routes WHERE id = ?", id) == 1
    }

    fun create(route: Route): Route {
        val sql = """
            INSERT INTO routes (
                wall_id,
                name,
                grade,
                length,
                style,
                bolts,
                rope_lengths,
                first_ascendant,
                description
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id,
                      wall_id,
                      name,
                      grade,
                      length,
                      style,
                      bolts,
                      rope_lengths,
                      created_at,
                      first_ascendant,
                      description
        """.trimIndent()

        return jdbcTemplate.queryForObject(
            sql,
            routeRowMapper,
            route.wallId,
            route.name,
            route.grade,
            route.length,
            route.style,
            route.bolts,
            route.ropeLengths,
            route.firstAscendant,
            route.description
        )!!
    }
}
