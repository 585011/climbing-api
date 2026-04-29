package com.example.climbingapi.repository;

import com.example.climbingapi.model.Wall;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class WallRepository {

    private static final RowMapper<Wall> WALL_ROW_MAPPER = (rs, rowNum) -> {
            OffsetDateTime createdTime = rs.getObject("created_at", OffsetDateTime.class);

            return new Wall(
                    rs.getInt("id"),
                    rs.getObject("area_id", Integer.class),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getObject("latitude", BigDecimal.class),
                    rs.getObject("longitude", BigDecimal.class),
                    rs.getString("approach_info"),
                    createdTime
            );
    };

    private final JdbcTemplate jdbcTemplate;

    public WallRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Wall> getAll() {
        String sql = """
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
        """;
        return jdbcTemplate.query(sql, WALL_ROW_MAPPER);
    }

    public Optional<Wall> getById(Integer id) {
        String sql = """
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
        """;
        return jdbcTemplate.query(sql, WALL_ROW_MAPPER, id).stream().findFirst();
    }

    public Wall create(Wall wall) {
        String sql = """
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
                """;

        return jdbcTemplate.queryForObject(
                sql,
                WALL_ROW_MAPPER,
                wall.getAreaId(),
                wall.getName(),
                wall.getDescription(),
                wall.getLatitude(),
                wall.getLongitude(),
                wall.getApproachInfo()
        );
    }
}
