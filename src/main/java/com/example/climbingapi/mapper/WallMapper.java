package com.example.climbingapi.mapper;

import com.example.climbingapi.dto.WallResponse;
import com.example.climbingapi.model.Wall;
import org.springframework.stereotype.Component;

@Component
public class WallMapper {

    public WallResponse toResponse(Wall wall) {
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
