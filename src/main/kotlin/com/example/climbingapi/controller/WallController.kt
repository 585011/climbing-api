package com.example.climbingapi.controller

import com.example.climbingapi.dto.CreateWallRequest
import com.example.climbingapi.dto.PagedResponse
import com.example.climbingapi.dto.RouteResponse
import com.example.climbingapi.dto.UpdateWallRequest
import com.example.climbingapi.dto.WallResponse
import com.example.climbingapi.exception.ErrorResponse
import com.example.climbingapi.mapper.RouteMapper
import com.example.climbingapi.mapper.WallMapper
import com.example.climbingapi.service.WallService
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

@Tag(name = "Walls", description = "Manage climbing walls")
@Validated
@RestController
@RequestMapping("/api/walls")
class WallController(
    private val wallService: WallService,
    private val wallMapper: WallMapper,
    private val routeMapper: RouteMapper
) {

    @Operation(summary = "List all walls")
    @GetMapping
    fun getAll(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): PagedResponse<WallResponse> {
        val paged = wallService.getAll(page, size)
        return PagedResponse(paged.data.map { wallMapper.toResponse(it) }, paged.page, paged.pageSize, paged.total)
    }

    @Operation(summary = "Get a wall by ID")
    @ApiResponse(responseCode = "404", description = "Wall not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Int): WallResponse = wallMapper.toResponse(wallService.getById(id))

    @Operation(summary = "List routes for a wall")
    @ApiResponse(responseCode = "404", description = "Wall not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @GetMapping("/{wallId}/routes")
    fun getRoutes(@PathVariable wallId: Int): List<RouteResponse> =
        wallService.getRoutes(wallId).map { routeMapper.toResponse(it) }

    @Operation(summary = "Create a wall")
    @ApiResponse(responseCode = "400", description = "Validation error",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @PostMapping
    fun create(@Valid @RequestBody request: CreateWallRequest, ucb: UriComponentsBuilder): ResponseEntity<WallResponse> {
        val created = wallMapper.toResponse(wallService.create(request))
        val location = ucb.path("/api/walls/{id}").buildAndExpand(created.id).toUri()
        return ResponseEntity.created(location).body(created)
    }

    @Operation(summary = "Update a wall")
    @ApiResponse(responseCode = "404", description = "Wall not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "400", description = "Validation error",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @PutMapping("/{id}")
    fun update(@PathVariable id: Int, @Valid @RequestBody request: UpdateWallRequest): WallResponse =
        wallMapper.toResponse(wallService.update(id, request))

    @Operation(summary = "Delete a wall")
    @ApiResponse(responseCode = "204", description = "Wall deleted")
    @ApiResponse(responseCode = "404", description = "Wall not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Int) = wallService.delete(id)
}
