package com.example.climbingapi.repository

import com.example.climbingapi.model.Wall
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.OffsetDateTime

@Repository
class WallRepository(
    private val jdbcTemplate: JdbcTemplate
) {

    private val wallRowMapper = RowMapper { rs, _ ->
        val createdTime = rs.getObject("created_at", OffsetDateTime::class.java)

        Wall(
            id = rs.getInt("id"),
            areaId = rs.getInt("area_id"),
            name = rs.getString("name"),
            description = rs.getString("description"),
            latitude = rs.getObject("latitude", BigDecimal::class.java),
            longitude = rs.getObject("longitude", BigDecimal::class.java),
            approachInfo = rs.getString("approach_info"),
            createdAt = createdTime
        )
    }

    fun getAll(): List<Wall> {
        val sql = """
            SELECT
                id,
                area_id,
                name,
                description,
                latitude,
                longitude,
                approach_info,
                created_at
            FROM walls
        """.trimIndent()
        return jdbcTemplate.query(sql, wallRowMapper)
    }

    fun getById(id: Int): Wall? {
        val sql = """
            SELECT
                id,
                area_id,
                name,
                description,
                latitude,
                longitude,
                approach_info,
                created_at
            FROM walls
            WHERE id = ?
        """.trimIndent()
        return jdbcTemplate.query(sql, wallRowMapper, id).firstOrNull()
    }

    fun create(wall: Wall): Wall {
        val sql = """
            INSERT INTO walls (
                area_id,
                name,
                description,
                latitude,
                longitude,
                approach_info
            )
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id,
                      area_id,
                      name,
                      description,
                      latitude,
                      longitude,
                      approach_info,
                      created_at
        """.trimIndent()

        return jdbcTemplate.queryForObject(
            sql,
            wallRowMapper,
            wall.areaId,
            wall.name,
            wall.description,
            wall.latitude,
            wall.longitude,
            wall.approachInfo
        )!!
    }
}
