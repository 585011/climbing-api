package com.example.climbingapi.service

import com.example.climbingapi.dto.CreateTickRequest
import com.example.climbingapi.dto.PagedResponse
import com.example.climbingapi.dto.UpdateTickRequest
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.model.UserRoute
import com.example.climbingapi.repository.RouteRepository
import com.example.climbingapi.repository.TickRepository
import com.example.climbingapi.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TickService(
    private val tickRepository: TickRepository,
    private val userRepository: UserRepository,
    private val routeRepository: RouteRepository
) {

    fun getByUserId(userId: Int, page: Int, size: Int): PagedResponse<UserRoute> {
        userRepository.getById(userId) ?: throw NotFoundException("User not found: $userId")
        val effectiveSize = size.coerceIn(1, 100)
        val data = tickRepository.findByUserId(userId, page, effectiveSize)
        val total = tickRepository.countByUserId(userId)
        return PagedResponse(data, page, effectiveSize, total)
    }

    fun getById(userId: Int, tickId: Int): UserRoute {
        userRepository.getById(userId) ?: throw NotFoundException("User not found: $userId")
        val tick = tickRepository.getById(tickId) ?: throw NotFoundException("Tick not found: $tickId")
        if (tick.userId != userId) throw NotFoundException("Tick not found: $tickId")
        return tick
    }

    @Transactional
    fun create(userId: Int, request: CreateTickRequest): UserRoute {
        userRepository.getById(userId) ?: throw NotFoundException("User not found: $userId")
        routeRepository.getById(request.routeId) ?: throw NotFoundException("Route not found: ${request.routeId}")
        return tickRepository.create(UserRoute(
            id = null,
            userId = userId,
            routeId = request.routeId,
            tickedAt = null,
            style = request.style,
            rating = request.rating,
            personalNote = request.personalNote
        ))
    }

    fun update(userId: Int, tickId: Int, request: UpdateTickRequest): UserRoute {
        val tick = getById(userId, tickId)
        return tickRepository.update(tickId, tick.copy(
            style = request.style,
            rating = request.rating,
            personalNote = request.personalNote
        )) ?: throw NotFoundException("Tick not found: $tickId")
    }

    fun delete(userId: Int, tickId: Int) {
        getById(userId, tickId)
        tickRepository.deleteById(tickId)
    }
}