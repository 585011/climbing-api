package com.example.climbingapi.dto;

import com.example.climbingapi.model.Route;

import java.time.OffsetDateTime;

public record RouteResponse(
        Integer id,
        Integer wallId,
        String name,
        String grade,
        Integer length,
        String style,
        Integer bolts,
        Integer ropeLengths,
        String firstAscendant,
        String description,
        OffsetDateTime createdAt
) {

    public static RouteResponse from(Route route) {
        return new RouteResponse(
                route.getId(),
                route.getWallId(),
                route.getName(),
                route.getGrade(),
                route.getLength(),
                route.getStyle(),
                route.getBolts(),
                route.getRopeLengths(),
                route.getFirstAscendant(),
                route.getDescription(),
                route.getCreatedAt()
        );
    }
}
