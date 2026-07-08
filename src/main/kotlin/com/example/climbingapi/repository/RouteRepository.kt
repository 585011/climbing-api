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

    fun getAll(page: Int, size: Int): List<Route> {
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
            ORDER BY id
            LIMIT ? OFFSET ?
        """.trimIndent()
        return jdbcTemplate.query(sql, routeRowMapper, size, page * size)
    }

    fun count(): Int {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM routes", Int::class.java) ?: 0
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

    fun countByAreaIds(areaIds: List<Int>): Map<Int, Int> {
        if (areaIds.isEmpty()) return emptyMap()
        val placeholders = areaIds.joinToString(", ") { "?" }
        val sql = """
            SELECT w.area_id, COUNT(*) AS route_count
            FROM routes r
            JOIN walls w ON w.id = r.wall_id
            WHERE w.area_id IN ($placeholders)
            GROUP BY w.area_id
        """.trimIndent()
        return jdbcTemplate.query(
            sql,
            { rs, _ -> rs.getInt("area_id") to rs.getInt("route_count") },
            *areaIds.toTypedArray()
        ).toMap()
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

    fun update(id: Int, route: Route): Route? {
        val sql = """
            UPDATE routes
            SET wall_id = ?,
                name = ?,
                grade = ?,
                length = ?,
                style = ?,
                bolts = ?,
                rope_lengths = ?,
                first_ascendant = ?,
                description = ?
            WHERE id = ?
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
        return jdbcTemplate.query(
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
            route.description,
            id
        ).firstOrNull()
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

        return jdbcTemplate.query(
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
        ).firstOrNull() ?: error("INSERT RETURNING returned no row")
    }
}
