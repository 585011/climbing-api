package com.example.climbingapi.controller

import com.example.climbingapi.dto.CreateWallRequest
import com.example.climbingapi.dto.RouteResponse
import com.example.climbingapi.dto.WallResponse
import com.example.climbingapi.mapper.RouteMapper
import com.example.climbingapi.mapper.WallMapper
import com.example.climbingapi.service.WallService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/walls")
class WallController(
    private val wallService: WallService,
    private val wallMapper: WallMapper,
    private val routeMapper: RouteMapper
) {

    @GetMapping
    fun getAll(): List<WallResponse> {
        return wallService.getAll().map { wallMapper.toResponse(it) }
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Int): WallResponse {
        return wallMapper.toResponse(wallService.getById(id))
    }

    @GetMapping("/{wallId}/routes")
    fun getRoutes(@PathVariable wallId: Int): List<RouteResponse> {
        return wallService.getRoutes(wallId).map { routeMapper.toResponse(it) }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateWallRequest): WallResponse {
        return wallMapper.toResponse(wallService.create(request))
    }
}
