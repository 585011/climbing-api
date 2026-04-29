package com.example.climbingapi.dto;

import com.example.climbingapi.model.Wall;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record WallResponse(
        Integer id,
        Integer areaId,
        String name,
        String description,
        BigDecimal latitude,
        BigDecimal longitude,
        String approachInfo,
        OffsetDateTime createdAt
) {

    public static WallResponse from(Wall wall) {
        return new WallResponse(
                wall.getId(),
                wall.getAreaId(),
                wall.getName(),
                wall.getDescription(),
                wall.getLatitude(),
                wall.getLongitude(),
                wall.getApproachInfo(),
                wall.getCreatedAt()
        );
    }
}
