package com.example.climbingapi.controller

import com.example.climbingapi.dto.CreateTickRequest
import com.example.climbingapi.dto.TickResponse
import com.example.climbingapi.dto.UpdateTickRequest
import com.example.climbingapi.exception.ErrorResponse
import com.example.climbingapi.mapper.TickMapper
import com.example.climbingapi.service.TickService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder

@Tag(name = "Ticks", description = "Track ascended routes")
@RestController
@RequestMapping("/api/users/{userId}/ticks")
class TickController(
    private val tickService: TickService,
    private val tickMapper: TickMapper
) {

    @Operation(summary = "Tick a route")
    @ApiResponse(responseCode = "404", description = "User or route not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "400", description = "Validation error",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @PostMapping
    fun create(
        @PathVariable userId: Int,
        @Valid @RequestBody request: CreateTickRequest,
        ucb: UriComponentsBuilder
    ): ResponseEntity<TickResponse> {
        val created = tickMapper.toResponse(tickService.create(userId, request))
        val location = ucb.path("/api/users/{userId}/ticks/{tickId}").buildAndExpand(userId, created.id).toUri()
        return ResponseEntity.created(location).body(created)
    }

    @Operation(summary = "Get a tick by ID")
    @ApiResponse(responseCode = "404", description = "User or tick not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @GetMapping("/{tickId}")
    fun getById(@PathVariable userId: Int, @PathVariable tickId: Int): TickResponse =
        tickMapper.toResponse(tickService.getById(userId, tickId))

    @Operation(summary = "Update a tick")
    @ApiResponse(responseCode = "404", description = "User or tick not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "400", description = "Validation error",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @PutMapping("/{tickId}")
    fun update(
        @PathVariable userId: Int,
        @PathVariable tickId: Int,
        @Valid @RequestBody request: UpdateTickRequest
    ): TickResponse = tickMapper.toResponse(tickService.update(userId, tickId, request))

    @Operation(summary = "Delete a tick")
    @ApiResponse(responseCode = "204", description = "Tick deleted")
    @ApiResponse(responseCode = "404", description = "User or tick not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @DeleteMapping("/{tickId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable userId: Int, @PathVariable tickId: Int) =
        tickService.delete(userId, tickId)
}
