package com.example.climbingproject.mapper;

import com.example.climbingproject.dto.WallResponse;
import com.example.climbingproject.model.Wall;
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
