package com.example.climbingapi.service

import com.example.climbingapi.dto.CreateClimbingAreaRequest
import com.example.climbingapi.dto.UpdateClimbingAreaRequest
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.model.ClimbingArea
import com.example.climbingapi.model.Wall
import com.example.climbingapi.repository.ClimbingAreaRepository
import org.springframework.stereotype.Service

@Service
class ClimbingAreaService(
    private val climbingAreaRepository: ClimbingAreaRepository,
    private val wallService: WallService
) {

    fun getAll(): List<ClimbingArea> {
        return climbingAreaRepository.getAll()
    }

    fun getById(id: Int): ClimbingArea {
        return climbingAreaRepository.getById(id)
            ?: throw NotFoundException("Climbing area not found: $id")
    }

    fun getWalls(climbingAreaId: Int): List<Wall> {
        getById(climbingAreaId)
        return wallService.getByAreaId(climbingAreaId)
    }

    fun delete(id: Int) {
        if (!climbingAreaRepository.deleteById(id)) throw NotFoundException("Climbing area not found: $id")
    }

    fun update(id: Int, request: UpdateClimbingAreaRequest): ClimbingArea {
        return climbingAreaRepository.update(id, ClimbingArea(
            id = null,
            name = request.name?.trim(),
            description = request.description,
            latitude = request.latitude,
            longitude = request.longitude,
            region = request.region,
            createdAt = null
        )) ?: throw NotFoundException("Climbing area not found: $id")
    }

    fun create(request: CreateClimbingAreaRequest): ClimbingArea {
        return climbingAreaRepository.create(ClimbingArea(
            id = null,
            name = request.name?.trim(),
            description = request.description,
            latitude = request.latitude,
            longitude = request.longitude,
            region = request.region,
            createdAt = null
        ))
    }
}
