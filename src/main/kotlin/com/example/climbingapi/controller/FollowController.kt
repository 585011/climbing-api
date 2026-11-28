package com.example.climbingapi.controller

import com.example.climbingapi.dto.PagedResponse
import com.example.climbingapi.dto.PublicUserResponse
import com.example.climbingapi.exception.ErrorResponse
import com.example.climbingapi.mapper.UserMapper
import com.example.climbingapi.service.FollowService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** Follows of the authenticated climber. Lists exist only for yourself, so strangers can't map someone's friend group. */
@Tag(name = "Follows", description = "Follow other climbers")
@Validated
@RestController
@RequestMapping("/api/users/me")
class FollowController(
    private val followService: FollowService,
    private val userMapper: UserMapper
) {

    @Operation(summary = "Follow a climber (idempotent)")
    @ApiResponse(responseCode = "204", description = "Following")
    @ApiResponse(responseCode = "400", description = "Tried to follow yourself",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "404", description = "Caller not registered, or user not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @PutMapping("/following/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun follow(@PathVariable userId: Int, jwt: JwtAuthenticationToken) {
        followService.follow(jwt.token.subject, userId)
    }

    @Operation(summary = "Unfollow a climber (idempotent, also when not following)")
    @ApiResponse(responseCode = "204", description = "Not following")
    @ApiResponse(responseCode = "404", description = "Caller not registered",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @DeleteMapping("/following/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unfollow(@PathVariable userId: Int, jwt: JwtAuthenticationToken) {
        followService.unfollow(jwt.token.subject, userId)
    }

    @Operation(summary = "List the climbers you follow, most recently followed first")
    @ApiResponse(responseCode = "404", description = "Caller not registered",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @GetMapping("/following")
    fun getFollowing(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        jwt: JwtAuthenticationToken
    ): PagedResponse<PublicUserResponse> {
        val paged = followService.getFollowing(jwt.token.subject, page, size)
        return PagedResponse(paged.data.map { userMapper.toPublicResponse(it) }, paged.page, paged.pageSize, paged.total)
    }

    @Operation(summary = "List your Followers, most recent first")
    @ApiResponse(responseCode = "404", description = "Caller not registered",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @GetMapping("/followers")
    fun getFollowers(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        jwt: JwtAuthenticationToken
    ): PagedResponse<PublicUserResponse> {
        val paged = followService.getFollowers(jwt.token.subject, page, size)
        return PagedResponse(paged.data.map { userMapper.toPublicResponse(it) }, paged.page, paged.pageSize, paged.total)
    }
}
