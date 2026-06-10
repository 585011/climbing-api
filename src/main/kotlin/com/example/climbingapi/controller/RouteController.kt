package com.example.climbingapi.controller

import com.example.climbingapi.dto.CreateRouteRequest
import com.example.climbingapi.dto.PagedResponse
import com.example.climbingapi.dto.RouteResponse
import com.example.climbingapi.dto.UpdateRouteRequest
import com.example.climbingapi.exception.ErrorResponse
import com.example.climbingapi.mapper.RouteMapper
import com.example.climbingapi.service.RouteService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder

@Tag(name = "Routes", description = "Manage climbing routes")
@Validated
@RestController
@RequestMapping("/api/routes")
class RouteController(
    private val routeService: RouteService,
    private val routeMapper: RouteMapper
) {

    @Operation(summary = "List all routes")
    @GetMapping
    fun getAll(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): PagedResponse<RouteResponse> {
        val paged = routeService.getAll(page, size)
        return PagedResponse(paged.data.map { routeMapper.toResponse(it) }, paged.page, paged.pageSize, paged.total)
    }

    @Operation(summary = "Get a route by ID")
    @ApiResponse(responseCode = "404", description = "Route not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Int): RouteResponse = routeMapper.toResponse(routeService.getById(id))

    @Operation(summary = "Create a route")
    @ApiResponse(responseCode = "400", description = "Validation error",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @PostMapping
    fun create(@Valid @RequestBody request: CreateRouteRequest, ucb: UriComponentsBuilder): ResponseEntity<RouteResponse> {
        val created = routeMapper.toResponse(routeService.create(request))
        val location = ucb.path("/api/routes/{id}").buildAndExpand(created.id).toUri()
        return ResponseEntity.created(location).body(created)
    }

    @Operation(summary = "Update a route")
    @ApiResponse(responseCode = "404", description = "Route not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "400", description = "Validation error",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @PutMapping("/{id}")
    fun update(@PathVariable id: Int, @Valid @RequestBody request: UpdateRouteRequest): RouteResponse =
        routeMapper.toResponse(routeService.update(id, request))

    @Operation(summary = "Delete a route")
    @ApiResponse(responseCode = "204", description = "Route deleted")
    @ApiResponse(responseCode = "404", description = "Route not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Int) = routeService.delete(id)
}
