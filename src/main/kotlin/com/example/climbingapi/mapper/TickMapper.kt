package com.example.climbingapi.mapper

import com.example.climbingapi.dto.TickResponse
import com.example.climbingapi.model.UserRoute
import com.example.climbingapi.service.StorageService
import org.springframework.stereotype.Component

@Component
class TickMapper(
    private val storageService: StorageService
) {

    fun toResponse(tick: UserRoute): TickResponse {
        return TickResponse(
            id = tick.id!!,
            userId = tick.userId!!,
            routeId = tick.routeId!!,
            tickedAt = tick.tickedAt!!,
            style = tick.style,
            rating = tick.rating,
            personalNote = tick.personalNote,
            photoUrl = tick.photoKey?.let { storageService.presignGet(it) }
        )
    }
}
