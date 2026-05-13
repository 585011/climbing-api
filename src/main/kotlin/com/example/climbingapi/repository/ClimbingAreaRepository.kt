package com.example.climbingapi.repository

import com.example.climbingapi.model.ClimbingArea
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class ClimbingAreaRepository (
    private val jdbcTemplate: JdbcTemplate
) {

    private val climbingAreaMapper = RowMapper { rs, _ ->
        val createdTime = rs.getObject("created_at", OffsetDateTime::class.java)

        ClimbingArea(
            id = rs.getInt("id"),
            name = rs.getString("name"),
            description = rs.getString("description"),
            latitude = rs.getBigDecimal("latitude"),
            longitude = rs.getBigDecimal("longitude"),
            region = rs.getString("region"),
            createdAt = createdTime
        )
    }

    fun getAll(): List<ClimbingArea> {
        val sql = """
            SELECT id,
                   name,
                   description,
                   latitude,
                   longitude,
                   region,
                   created_at
            FROM climbing_areas
        """.trimIndent()
        return jdbcTemplate.query(sql, climbingAreaMapper)
    }

    fun getById(id: Int): ClimbingArea? {
        val sql = """
            SELECT id,
                   name,
                   description,
                   latitude,
                   longitude,
                   region,
                   created_at
            from climbing_areas
            WHERE id = ?
        """.trimIndent()
        return jdbcTemplate.query(sql, climbingAreaMapper, id).firstOrNull()
    }

    fun deleteById(id: Int): Boolean {
        return jdbcTemplate.update("DELETE FROM climbing_areas WHERE id = ?", id) == 1
    }

    fun update(id: Int, climbingArea: ClimbingArea): ClimbingArea? {
        val sql = """
            UPDATE climbing_areas
            SET name = ?,
                description = ?,
                latitude = ?,
                longitude = ?,
                region = ?
            WHERE id = ?
            RETURNING id,
                      name,
                      description,
                      latitude,
                      longitude,
                      region,
                      created_at
        """.trimIndent()
        return jdbcTemplate.query(
            sql,
            climbingAreaMapper,
            climbingArea.name,
            climbingArea.description,
            climbingArea.latitude,
            climbingArea.longitude,
            climbingArea.region,
            id
        ).firstOrNull()
    }

    fun create(climbingArea: ClimbingArea): ClimbingArea {
        val sql = """
            INSERT INTO climbing_areas (
                name,
                description,
                latitude,
                longitude,
                region
            ) VALUES (?, ?, ?, ?, ?)
            RETURNING id,
            name,
            description,
            latitude,
            longitude,
            region,
            created_at
        """.trimIndent()

        return jdbcTemplate.queryForObject(
            sql,
            climbingAreaMapper,
            climbingArea.name,
            climbingArea.description,
            climbingArea.latitude,
            climbingArea.longitude,
            climbingArea.region
        )!!
    }
}