package com.example.climbingapi.mapper

import com.example.climbingapi.dto.PublicUserResponse
import com.example.climbingapi.dto.SessionAreaResponse
import com.example.climbingapi.dto.SessionResponse
import com.example.climbingapi.dto.SessionRouteResponse
import com.example.climbingapi.dto.SessionTickResponse
import com.example.climbingapi.model.Session
import com.example.climbingapi.service.StorageService
import org.springframework.stereotype.Component

@Component
class SessionMapper(
    private val storageService: StorageService
) {

    fun toResponse(session: Session): SessionResponse {
        return SessionResponse(
            id = session.id,
            date = session.date,
            lastTickAt = session.lastTickAt,
            shared = session.shared,
            user = PublicUserResponse(id = session.userId, displayName = session.userDisplayName),
            area = SessionAreaResponse(id = session.areaId, name = session.areaName, type = session.areaType),
            ticks = session.ticks.map { tick ->
                SessionTickResponse(
                    id = tick.id,
                    tickedAt = tick.tickedAt,
                    style = tick.style,
                    rating = tick.rating,
                    photoUrl = tick.photoKey?.let { storageService.presignGet(it) },
                    route = SessionRouteResponse(
                        id = tick.routeId,
                        name = tick.routeName,
                        grade = tick.grade,
                        gradeBand = tick.gradeBand,
                        holdColor = tick.holdColor,
                        style = tick.routeStyle,
                        wallId = tick.wallId,
                        wallName = tick.wallName,
                        retired = tick.retired
                    )
                )
            }
        )
    }
}
