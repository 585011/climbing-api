package com.example.climbingapi.mapper

import com.example.climbingapi.dto.RouteResponse
import com.example.climbingapi.model.Route
import org.springframework.stereotype.Component

@Component
class RouteMapper {

    fun toResponse(route: Route): RouteResponse {
        return RouteResponse(
            id = route.id,
            wallId = route.wallId,
            name = route.name,
            grade = route.grade,
            length = route.length,
            style = route.style,
            bolts = route.bolts,
            ropeLengths = route.ropeLengths,
            firstAscendant = route.firstAscendant,
            description = route.description,
            createdAt = route.createdAt
        )
    }
}
