package com.example.climbingapi.controller

import com.example.climbingapi.dto.CreateRouteRequest
import com.example.climbingapi.dto.RouteResponse
import com.example.climbingapi.exception.ErrorResponse
import com.example.climbingapi.mapper.RouteMapper
import com.example.climbingapi.service.RouteService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.ResponseStatus

@Tag(name = "Routes", description = "Manage climbing routes")
@RestController
@RequestMapping("/api/routes")
class RouteController(
    private val routeService: RouteService,
    private val routeMapper: RouteMapper
) {

    @Operation(summary = "List all routes")
    @GetMapping
    fun getAll(): List<RouteResponse> {
        return routeService.getAll().map { routeMapper.toResponse(it) }
    }

    @Operation(summary = "Get a route by ID")
    @ApiResponse(responseCode = "404", description = "Route not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Int): RouteResponse {
        return routeMapper.toResponse(routeService.getById(id))
    }

    @Operation(summary = "Create a route")
    @ApiResponse(responseCode = "400", description = "Validation error",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateRouteRequest): RouteResponse {
        return routeMapper.toResponse(routeService.create(request))
    }
}
