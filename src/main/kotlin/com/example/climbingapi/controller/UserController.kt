package com.example.climbingapi.controller

import com.example.climbingapi.dto.CreateUserRequest
import com.example.climbingapi.dto.PagedResponse
import com.example.climbingapi.dto.TickResponse
import com.example.climbingapi.dto.UpdateUserRequest
import com.example.climbingapi.dto.UserResponse
import com.example.climbingapi.exception.ErrorResponse
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.mapper.TickMapper
import com.example.climbingapi.mapper.UserMapper
import com.example.climbingapi.service.TickService
import com.example.climbingapi.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
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

@Tag(name = "Users", description = "Manage user accounts")
@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
    private val userMapper: UserMapper,
    private val tickService: TickService,
    private val tickMapper: TickMapper
) {

    @Operation(summary = "List all users")
    @GetMapping
    fun getAll(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PagedResponse<UserResponse> {
        val paged = userService.getAll(page, size)
        return PagedResponse(paged.data.map { userMapper.toResponse(it) }, paged.page, paged.pageSize, paged.total)
    }

    @Operation(summary = "Get a user by ID")
    @ApiResponse(responseCode = "404", description = "User not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Int): UserResponse = userMapper.toResponse(userService.getById(id))

    @Operation(summary = "Register or create the authenticated user's profile")
    @ApiResponse(responseCode = "400", description = "Validation error",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "409", description = "User already exists",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @PostMapping("/me")
    fun registerSelf(
        @Valid @RequestBody request: CreateUserRequest,
        jwt: JwtAuthenticationToken,
        ucb: UriComponentsBuilder
    ): ResponseEntity<UserResponse> {
        val auth0Id = jwt.token.subject
        val email = jwt.token.getClaimAsString("email")
        val created = userMapper.toResponse(userService.create(request, auth0Id, email))
        val location = ucb.path("/api/users/{id}").buildAndExpand(created.id).toUri()
        return ResponseEntity.created(location).body(created)
    }

    @Operation(summary = "Get the authenticated user's own profile")
    @ApiResponse(responseCode = "404", description = "User not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @GetMapping("/me")
    fun getSelf(jwt: JwtAuthenticationToken): UserResponse {
        val user = userService.getByAuth0Id(jwt.token.subject)
            ?: throw NotFoundException("User not found")
        return userMapper.toResponse(user)
    }

    @Operation(summary = "Update a user")
    @ApiResponse(responseCode = "403", description = "Not the owner",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "404", description = "User not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "400", description = "Validation error",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Int,
        @Valid @RequestBody request: UpdateUserRequest,
        jwt: JwtAuthenticationToken
    ): UserResponse {
        userService.assertOwner(id, jwt.token.subject)
        return userMapper.toResponse(userService.update(id, request))
    }

    @Operation(summary = "Delete a user")
    @ApiResponse(responseCode = "204", description = "User deleted")
    @ApiResponse(responseCode = "403", description = "Not the owner",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "404", description = "User not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Int, jwt: JwtAuthenticationToken) {
        userService.assertOwner(id, jwt.token.subject)
        userService.delete(id)
    }

    @Operation(summary = "List ticked routes for a user")
    @ApiResponse(responseCode = "404", description = "User not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @GetMapping("/{userId}/ticks")
    fun getTicks(
        @PathVariable userId: Int,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        jwt: JwtAuthenticationToken
    ): PagedResponse<TickResponse> {
        userService.assertOwner(userId, jwt.token.subject)
        val paged = tickService.getByUserId(userId, page, size)
        return PagedResponse(paged.data.map { tickMapper.toResponse(it) }, paged.page, paged.pageSize, paged.total)
    }
}
