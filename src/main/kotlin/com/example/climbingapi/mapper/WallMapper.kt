package com.example.climbingapi.mapper

import com.example.climbingapi.dto.WallResponse
import com.example.climbingapi.model.Wall
import com.example.climbingapi.service.StorageService
import org.springframework.stereotype.Component

@Component
class WallMapper(
    private val storageService: StorageService
) {

    fun toResponse(wall: Wall): WallResponse {
        return WallResponse(
            id = wall.id!!,
            areaId = wall.areaId!!,
            name = wall.name!!,
            description = wall.description,
            latitude = wall.latitude,
            longitude = wall.longitude,
            approachInfo = wall.approachInfo,
            imageUrl = (wall.optimizedKey ?: wall.imageKey)?.let { storageService.presignGet(it) },
            thumbnailUrl = (wall.thumbnailKey ?: wall.imageKey)?.let { storageService.presignGet(it) },
            createdAt = wall.createdAt!!
        )
    }
}
