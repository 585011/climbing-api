package com.example.climbingapi.mapper

import com.example.climbingapi.dto.WallResponse
import com.example.climbingapi.model.Wall
import org.springframework.stereotype.Component

@Component
class WallMapper {

    fun toResponse(wall: Wall): WallResponse {
        return WallResponse(
            id = wall.id,
            areaId = wall.areaId,
            name = wall.name,
            description = wall.description,
            latitude = wall.latitude,
            longitude = wall.longitude,
            approachInfo = wall.approachInfo,
            createdAt = wall.createdAt
        )
    }
}
