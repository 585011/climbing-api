package com.example.climbingapi.repository

import com.example.climbingapi.model.Wall
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
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
            latitude = rs.getBigDecimal("latitude"),
            longitude = rs.getBigDecimal("longitude"),
            approachInfo = rs.getString("approach_info"),
            imageKey = rs.getString("image_key"),
            createdAt = createdTime,
            optimizedKey = rs.getString("optimized_key"),
            thumbnailKey = rs.getString("thumbnail_key")
        )
    }

    fun getAll(page: Int, size: Int): List<Wall> {
        val sql = """
            SELECT
                id,
                area_id,
                name,
                description,
                latitude,
                longitude,
                approach_info,
                image_key,
                created_at,
                optimized_key,
                thumbnail_key
            FROM walls
            ORDER BY id
            LIMIT ? OFFSET ?
        """.trimIndent()
        return jdbcTemplate.query(sql, wallRowMapper, size, page * size)
    }

    fun count(): Int {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM walls", Int::class.java) ?: 0
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
                image_key,
                created_at,
                optimized_key,
                thumbnail_key
            FROM walls
            WHERE id = ?
        """.trimIndent()
        return jdbcTemplate.query(sql, wallRowMapper, id).firstOrNull()
    }

    fun deleteById(id: Int): Boolean {
        return jdbcTemplate.update("DELETE FROM walls WHERE id = ?", id) == 1
    }

    fun update(id: Int, wall: Wall): Wall? {
        // image_key is intentionally not touched here — images are managed via PUT /{id}/image.
        val sql = """
            UPDATE walls
            SET area_id = ?,
                name = ?,
                description = ?,
                latitude = ?,
                longitude = ?,
                approach_info = ?
            WHERE id = ?
            RETURNING id,
                      area_id,
                      name,
                      description,
                      latitude,
                      longitude,
                      approach_info,
                      image_key,
                      created_at,
                      optimized_key,
                      thumbnail_key
        """.trimIndent()
        return jdbcTemplate.query(
            sql,
            wallRowMapper,
            wall.areaId,
            wall.name,
            wall.description,
            wall.latitude,
            wall.longitude,
            wall.approachInfo,
            id
        ).firstOrNull()
    }

    fun updateImageKeys(id: Int, imageKey: String, optimizedKey: String, thumbnailKey: String): Wall? {
        val sql = """
            UPDATE walls
            SET image_key = ?, optimized_key = ?, thumbnail_key = ?
            WHERE id = ?
            RETURNING id, area_id, name, description, latitude, longitude,
                      approach_info, image_key, created_at, optimized_key, thumbnail_key
        """.trimIndent()
        return jdbcTemplate.query(sql, wallRowMapper, imageKey, optimizedKey, thumbnailKey, id).firstOrNull()
    }

    fun findByAreaId(areaId: Int): List<Wall> {
        val sql = """
            SELECT
                id,
                area_id,
                name,
                description,
                latitude,
                longitude,
                approach_info,
                image_key,
                created_at,
                optimized_key,
                thumbnail_key
            FROM walls
            WHERE area_id = ?
            ORDER BY id
        """.trimIndent()
        return jdbcTemplate.query(sql, wallRowMapper, areaId)
    }

    fun create(wall: Wall): Wall {
        val sql = """
            INSERT INTO walls (
                area_id, name, description, latitude, longitude,
                approach_info, image_key, optimized_key, thumbnail_key
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, area_id, name, description, latitude, longitude,
                      approach_info, image_key, created_at, optimized_key, thumbnail_key
        """.trimIndent()

        return jdbcTemplate.query(
            sql, wallRowMapper,
            wall.areaId, wall.name, wall.description, wall.latitude, wall.longitude,
            wall.approachInfo, wall.imageKey, wall.optimizedKey, wall.thumbnailKey
        ).firstOrNull() ?: error("INSERT RETURNING returned no row")
    }

    fun findWallsNeedingBackfill(): List<Wall> {
        val sql = """
            SELECT
                id, area_id, name, description, latitude, longitude,
                approach_info, image_key, created_at, optimized_key, thumbnail_key
            FROM walls
            WHERE image_key IS NOT NULL
              AND (optimized_key IS NULL OR thumbnail_key IS NULL)
            ORDER BY id
        """.trimIndent()
        return jdbcTemplate.query(sql, wallRowMapper)
    }
}
