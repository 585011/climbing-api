package com.example.climbingapi.controller

import com.example.climbingapi.dto.ClimbingAreaResponse
import com.example.climbingapi.dto.CreateClimbingAreaRequest
import com.example.climbingapi.dto.PagedResponse
import com.example.climbingapi.dto.UpdateClimbingAreaRequest
import com.example.climbingapi.dto.WallResponse
import com.example.climbingapi.exception.ErrorResponse
import com.example.climbingapi.mapper.ClimbingAreaMapper
import com.example.climbingapi.mapper.WallMapper
import com.example.climbingapi.service.ClimbingAreaService
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

@Tag(name = "Climbing Areas", description = "Manage climbing areas")
@Validated
@RestController
@RequestMapping("/api/climbing-areas")
class ClimbingAreaController(
    private val climbingAreaService: ClimbingAreaService,
    private val climbingAreaMapper: ClimbingAreaMapper,
    private val wallMapper: WallMapper
) {

    @Operation(summary = "List all climbing areas")
    @GetMapping
    fun getAll(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): PagedResponse<ClimbingAreaResponse> {
        val paged = climbingAreaService.getAll(page, size)
        return PagedResponse(paged.data.map { climbingAreaMapper.toResponse(it) }, paged.page, paged.pageSize, paged.total)
    }

    @Operation(summary = "Get a climbing area by ID")
    @ApiResponse(responseCode = "404", description = "Climbing area not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Int): ClimbingAreaResponse =
        climbingAreaMapper.toResponse(climbingAreaService.getById(id))

    @Operation(summary = "List walls for a climbing area")
    @ApiResponse(responseCode = "404", description = "Climbing area not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @GetMapping("/{areaId}/walls")
    fun getWalls(@PathVariable areaId: Int): List<WallResponse> =
        climbingAreaService.getWalls(areaId).map { wallMapper.toResponse(it) }

    @Operation(summary = "Create a climbing area")
    @ApiResponse(responseCode = "400", description = "Validation error",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateClimbingAreaRequest,
        ucb: UriComponentsBuilder
    ): ResponseEntity<ClimbingAreaResponse> {
        val created = climbingAreaMapper.toResponse(climbingAreaService.create(request))
        val location = ucb.path("/api/climbing-areas/{id}").buildAndExpand(created.id).toUri()
        return ResponseEntity.created(location).body(created)
    }

    @Operation(summary = "Update a climbing area")
    @ApiResponse(responseCode = "404", description = "Climbing area not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "400", description = "Validation error",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @PutMapping("/{id}")
    fun update(@PathVariable id: Int, @Valid @RequestBody request: UpdateClimbingAreaRequest): ClimbingAreaResponse =
        climbingAreaMapper.toResponse(climbingAreaService.update(id, request))

    @Operation(summary = "Delete a climbing area")
    @ApiResponse(responseCode = "204", description = "Climbing area deleted")
    @ApiResponse(responseCode = "404", description = "Climbing area not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Int) = climbingAreaService.delete(id)
}
