package com.example.climbingapi.repository;

import com.example.climbingapi.model.Route;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class RouteRepository {

    private static final RowMapper<Route> ROUTE_ROW_MAPPER = (rs, rowNum) -> {
        OffsetDateTime createdTime = rs.getObject("created_at", OffsetDateTime.class);

        return new Route(
                rs.getInt("id"),
                rs.getInt("wall_id"),
                rs.getString("name"),
                rs.getString("grade"),
                rs.getObject("length", Integer.class),
                rs.getString("style"),
                rs.getObject("bolts", Integer.class),
                rs.getObject("rope_lengths", Integer.class),
                rs.getString("first_ascendant"),
                rs.getString("description"),
                createdTime
        );
    };

    private final JdbcTemplate jdbcTemplate;

    public RouteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Route> getAll() {
        String sql = """
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
                """;
        return jdbcTemplate.query(sql, ROUTE_ROW_MAPPER);
    }

    public Optional<Route> getById(Integer id) {
        String sql = """
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
                """;
        return jdbcTemplate.query(sql, ROUTE_ROW_MAPPER, id).stream().findFirst();
    }

    public List<Route> findByWallId(Integer wallId) {
        String sql = """
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
                """;
        return jdbcTemplate.query(sql, ROUTE_ROW_MAPPER, wallId);
    }

    public Route create(Route route) {
        String sql = """
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
                """;

        return jdbcTemplate.queryForObject(
                sql,
                ROUTE_ROW_MAPPER,
                route.getWallId(),
                route.getName(),
                route.getGrade(),
                route.getLength(),
                route.getStyle(),
                route.getBolts(),
                route.getRopeLengths(),
                route.getFirstAscendant(),
                route.getDescription()
        );
    }
}
