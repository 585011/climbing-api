package com.example.climbingapi.service

import com.example.climbingapi.dto.CreateWallRequest
import com.example.climbingapi.dto.UpdateWallRequest
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.model.Wall
import com.example.climbingapi.model.Route
import com.example.climbingapi.repository.WallRepository
import org.springframework.stereotype.Service

@Service
class WallService(
    private val wallRepository: WallRepository,
    private val routeService: RouteService
) {

    fun getAll(): List<Wall> {
        return wallRepository.getAll()
    }

    fun getById(id: Int): Wall {
        return wallRepository.getById(id)
            ?: throw NotFoundException("Wall not found: $id")
    }

    fun getRoutes(wallId: Int): List<Route> {
        getById(wallId)
        return routeService.getByWallId(wallId)
    }

    fun delete(id: Int) {
        if (!wallRepository.deleteById(id)) throw NotFoundException("Wall not found: $id")
    }

    fun update(id: Int, request: UpdateWallRequest): Wall {
        return wallRepository.update(id, Wall(
            id = null,
            areaId = request.areaId,
            name = request.name?.trim(),
            description = request.description,
            latitude = request.latitude,
            longitude = request.longitude,
            approachInfo = request.approachInfo,
            createdAt = null
        )) ?: throw NotFoundException("Wall not found: $id")
    }

    fun create(request: CreateWallRequest): Wall {
        val wall = Wall(
            id = null,
            areaId = request.areaId,
            name = request.name?.trim(),
            description = request.description,
            latitude = request.latitude,
            longitude = request.longitude,
            approachInfo = request.approachInfo,
            createdAt = null
        )

        return wallRepository.create(wall)
    }
}
