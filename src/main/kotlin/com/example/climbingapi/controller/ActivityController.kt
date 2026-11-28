package com.example.climbingapi.controller

import com.example.climbingapi.dto.PagedResponse
import com.example.climbingapi.dto.PublicUserResponse
import com.example.climbingapi.dto.SessionResponse
import com.example.climbingapi.dto.UpdateSessionSharedRequest
import com.example.climbingapi.exception.ErrorResponse
import com.example.climbingapi.mapper.SessionMapper
import com.example.climbingapi.mapper.UserMapper
import com.example.climbingapi.service.ActivityService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** The Following Feed and single Sessions. A Session the caller can't see is a 404, never a 403. */
@Tag(name = "Feed", description = "The Following Feed and Sessions")
@Validated
@RestController
@RequestMapping("/api")
class ActivityController(
    private val activityService: ActivityService,
    private val sessionMapper: SessionMapper,
    private val userMapper: UserMapper
) {

    @Operation(summary = "Your Following Feed: your own Sessions plus the shared Sessions of climbers you follow, most recently active first")
    @ApiResponse(responseCode = "404", description = "Caller not registered",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @GetMapping("/feed")
    fun getFeed(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        jwt: JwtAuthenticationToken
    ): PagedResponse<SessionResponse> {
        val paged = activityService.getFeed(jwt.token.subject, page, size)
        return PagedResponse(paged.data.map { sessionMapper.toResponse(it) }, paged.page, paged.pageSize, paged.total)
    }

    @Operation(summary = "Get a Session")
    @ApiResponse(responseCode = "404", description = "Session not found or not visible to you",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @GetMapping("/activities/{id}")
    fun getById(@PathVariable id: Int, jwt: JwtAuthenticationToken): SessionResponse =
        sessionMapper.toResponse(activityService.getById(jwt.token.subject, id))

    @Operation(summary = "Share or hide one of your Sessions")
    @ApiResponse(responseCode = "400", description = "Validation error",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "403", description = "Visible to you but not your Session",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "404", description = "Session not found or not visible to you",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @PutMapping("/activities/{id}/shared")
    fun setShared(
        @PathVariable id: Int,
        @Valid @RequestBody request: UpdateSessionSharedRequest,
        jwt: JwtAuthenticationToken
    ): SessionResponse = sessionMapper.toResponse(activityService.setShared(jwt.token.subject, id, request.shared!!))

    @Operation(summary = "Give Kudos to a Session (idempotent)")
    @ApiResponse(responseCode = "204", description = "Kudos given")
    @ApiResponse(responseCode = "400", description = "Your own Session",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "404", description = "Session not found or not visible to you",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @PutMapping("/activities/{id}/kudos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun giveKudos(@PathVariable id: Int, jwt: JwtAuthenticationToken) {
        activityService.giveKudos(jwt.token.subject, id)
    }

    @Operation(summary = "Take back your Kudos (idempotent)")
    @ApiResponse(responseCode = "204", description = "No Kudos from you")
    @ApiResponse(responseCode = "404", description = "Session not found or not visible to you",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @DeleteMapping("/activities/{id}/kudos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeKudos(@PathVariable id: Int, jwt: JwtAuthenticationToken) {
        activityService.removeKudos(jwt.token.subject, id)
    }

    @Operation(summary = "List who gave a Session Kudos, most recent first")
    @ApiResponse(responseCode = "404", description = "Session not found or not visible to you",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @GetMapping("/activities/{id}/kudos")
    fun getKudos(
        @PathVariable id: Int,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        jwt: JwtAuthenticationToken
    ): PagedResponse<PublicUserResponse> {
        val paged = activityService.getKudos(jwt.token.subject, id, page, size)
        return PagedResponse(paged.data.map { userMapper.toPublicResponse(it) }, paged.page, paged.pageSize, paged.total)
    }
}
