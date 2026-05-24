package com.example.climbingapi.mapper

import com.example.climbingapi.dto.TickResponse
import com.example.climbingapi.model.UserRoute
import org.springframework.stereotype.Component

@Component
class TickMapper {

    fun toResponse(tick: UserRoute): TickResponse {
        return TickResponse(
            id = tick.id,
            userId = tick.userId,
            routeId = tick.routeId,
            tickedAt = tick.tickedAt,
            style = tick.style,
            rating = tick.rating,
            personalNote = tick.personalNote
        )
    }
}