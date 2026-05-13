package com.example.climbingapi.mapper

import com.example.climbingapi.dto.ClimbingAreaResponse
import com.example.climbingapi.model.ClimbingArea
import org.springframework.stereotype.Component

@Component
class ClimbingAreaMapper {

    fun toResponse(area: ClimbingArea): ClimbingAreaResponse {
        return ClimbingAreaResponse(
            id = area.id,
            name = area.name,
            description = area.description,
            latitude = area.latitude,
            longitude = area.longitude,
            region = area.region,
            createdAt = area.createdAt
        )
    }
}
