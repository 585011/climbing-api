package com.example.climbingapi.service

import com.example.climbingapi.dto.CreateRouteRequest
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.model.Route
import com.example.climbingapi.repository.RouteRepository
import com.example.climbingapi.repository.WallRepository
import org.springframework.stereotype.Service

@Service
class RouteService(
    private val routeRepository: RouteRepository,
    private val wallRepository: WallRepository
) {

    fun getAll(): List<Route> {
        return routeRepository.getAll()
    }

    fun getById(id: Int): Route {
        return routeRepository.getById(id)
            ?: throw NotFoundException("Route not found: $id")
    }

    fun getByWallId(wallId: Int): List<Route> {
        return routeRepository.findByWallId(wallId)
    }

    fun create(request: CreateRouteRequest): Route {
        wallRepository.getById(request.wallId!!)
            ?: throw NotFoundException("Wall not found: ${request.wallId}")

        val route = Route(
            id = null,
            wallId = request.wallId,
            name = request.name?.trim(),
            grade = request.grade?.trim(),
            length = request.length,
            style = request.style,
            bolts = request.bolts,
            ropeLengths = request.ropeLengths,
            firstAscendant = request.firstAscendant,
            description = request.description,
            createdAt = null
        )

        return routeRepository.create(route)
    }
}
