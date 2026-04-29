package com.example.climbingapi.controller

import com.example.climbingapi.dto.CreateRouteRequest
import com.example.climbingapi.dto.RouteResponse
import com.example.climbingapi.mapper.RouteMapper
import com.example.climbingapi.service.RouteService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.ResponseStatus

@RestController
@RequestMapping("/api/routes")
class RouteController(
    private val routeService: RouteService,
    private val routeMapper: RouteMapper
) {

    @GetMapping
    fun getAll(): List<RouteResponse> {
        return routeService.getAll().map { routeMapper.toResponse(it) }
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Int): RouteResponse {
        return routeMapper.toResponse(routeService.getById(id))
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateRouteRequest): RouteResponse {
        return routeMapper.toResponse(routeService.create(request))
    }
}
