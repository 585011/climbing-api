package com.example.climbingapi.dto

import jakarta.validation.constraints.NotBlank

data class CreateUserRequest(
    @field:NotBlank val email: String?,
    @field:NotBlank val displayName: String?
)