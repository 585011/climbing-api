package com.example.climbingapi.service

import com.example.climbingapi.dto.CreateRouteRequest
import com.example.climbingapi.dto.PagedResponse
import com.example.climbingapi.dto.UpdateRouteRequest
import com.example.climbingapi.exception.ForbiddenException
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.model.Route
import com.example.climbingapi.repository.ClimbingAreaRepository
import com.example.climbingapi.repository.RouteRepository
import com.example.climbingapi.repository.WallRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Locale

@Service
class RouteService(
    private val routeRepository: RouteRepository,
    private val wallRepository: WallRepository,
    private val climbingAreaRepository: ClimbingAreaRepository
) {

    fun getAll(page: Int, size: Int, includeRetired: Boolean = false): PagedResponse<Route> {
        val effectiveSize = size.coerceIn(1, 100)
        val data = routeRepository.getAll(page, effectiveSize, includeRetired)
        val total = routeRepository.count(includeRetired)
        return PagedResponse(data, page, effectiveSize, total)
    }

    fun getById(id: Int): Route {
        return routeRepository.getById(id)
            ?: throw NotFoundException("Route not found: $id")
    }

    fun getByWallId(wallId: Int, includeRetired: Boolean = false): List<Route> {
        return routeRepository.findByWallId(wallId, includeRetired)
    }

    /**
     * Any user may retire a route in a gym (gyms rotate routes weekly); un-retiring and
     * retiring outdoor routes are admin-only.
     */
    @Transactional
    fun setRetired(id: Int, retired: Boolean, isAdmin: Boolean): Route {
        val route = getById(id)
        if (!isAdmin) {
            if (!retired) throw ForbiddenException("Only admins can un-retire a route")
            val wall = wallRepository.getById(route.wallId!!)
            val area = wall?.areaId?.let { climbingAreaRepository.getById(it) }
            if (area?.type != "gym") throw ForbiddenException("Only routes in climbing gyms can be retired by users")
        }
        return routeRepository.setRetired(id, retired) ?: throw NotFoundException("Route not found: $id")
    }

    fun delete(id: Int) {
        if (!routeRepository.deleteById(id)) throw NotFoundException("Route not found: $id")
    }

    @Transactional
    fun update(id: Int, request: UpdateRouteRequest): Route {
        wallRepository.getById(request.wallId)
            ?: throw NotFoundException("Wall not found: ${request.wallId}")
        return routeRepository.update(id, Route(
            id = null,
            wallId = request.wallId,
            name = request.name?.trim(),
            grade = request.grade?.trim(),
            length = request.length,
            style = request.style?.lowercase(Locale.ROOT),
            bolts = request.bolts,
            ropeLengths = request.ropeLengths,
            firstAscendant = request.firstAscendant,
            description = request.description,
            createdAt = null,
            holdColor = request.holdColor?.trim()
        )) ?: throw NotFoundException("Route not found: $id")
    }

    @Transactional
    fun create(request: CreateRouteRequest): Route {
        wallRepository.getById(request.wallId)
            ?: throw NotFoundException("Wall not found: ${request.wallId}")

        val route = Route(
            id = null,
            wallId = request.wallId,
            name = request.name?.trim(),
            grade = request.grade?.trim(),
            length = request.length,
            style = request.style?.lowercase(Locale.ROOT),
            bolts = request.bolts,
            ropeLengths = request.ropeLengths,
            firstAscendant = request.firstAscendant,
            description = request.description,
            createdAt = null,
            holdColor = request.holdColor?.trim()
        )

        return routeRepository.create(route)
    }
}
