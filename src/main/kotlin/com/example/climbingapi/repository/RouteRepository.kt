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
            createdAt = createdTime,
            holdColor = rs.getString("hold_color"),
            retiredAt = rs.getObject("retired_at", OffsetDateTime::class.java),
            createdBy = rs.getObject("created_by") as Int?
        )
    }

    private val columns = """
        id,
        wall_id,
        name,
        grade,
        length,
        style,
        bolts,
        rope_lengths,
        created_at,
        first_ascendant,
        description,
        hold_color,
        retired_at,
        created_by
    """.trimIndent()

    private fun activeFilter(includeRetired: Boolean) = if (includeRetired) "TRUE" else "retired_at IS NULL"

    fun getAll(page: Int, size: Int, includeRetired: Boolean = false): List<Route> {
        val sql = """
            SELECT $columns
            FROM routes
            WHERE ${activeFilter(includeRetired)}
            ORDER BY id
            LIMIT ? OFFSET ?
        """.trimIndent()
        return jdbcTemplate.query(sql, routeRowMapper, size, page * size)
    }

    fun count(includeRetired: Boolean = false): Int {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM routes WHERE ${activeFilter(includeRetired)}", Int::class.java
        ) ?: 0
    }

    fun getById(id: Int): Route? {
        val sql = """
            SELECT $columns
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
              AND r.retired_at IS NULL
            GROUP BY w.area_id
        """.trimIndent()
        return jdbcTemplate.query(
            sql,
            { rs, _ -> rs.getInt("area_id") to rs.getInt("route_count") },
            *areaIds.toTypedArray()
        ).toMap()
    }

    fun findByWallId(wallId: Int, includeRetired: Boolean = false): List<Route> {
        val sql = """
            SELECT $columns
            FROM routes
            WHERE wall_id = ?
              AND ${activeFilter(includeRetired)}
            ORDER BY id
        """.trimIndent()
        return jdbcTemplate.query(sql, routeRowMapper, wallId)
    }

    fun deleteById(id: Int): Boolean {
        return jdbcTemplate.update("DELETE FROM routes WHERE id = ?", id) == 1
    }

    fun setRetired(id: Int, retired: Boolean): Route? {
        val sql = """
            UPDATE routes
            SET retired_at = CASE WHEN ? THEN COALESCE(retired_at, CURRENT_TIMESTAMP) ELSE NULL END
            WHERE id = ?
            RETURNING $columns
        """.trimIndent()
        return jdbcTemplate.query(sql, routeRowMapper, retired, id).firstOrNull()
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
                description = ?,
                hold_color = ?
            WHERE id = ?
            RETURNING $columns
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
            route.holdColor,
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
                description,
                hold_color,
                created_by
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING $columns
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
            route.holdColor,
            route.createdBy
        ).firstOrNull() ?: error("INSERT RETURNING returned no row")
    }
}
