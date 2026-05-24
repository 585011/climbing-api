package com.example.climbingapi.dto

import jakarta.validation.constraints.NotBlank

data class UpdateUserRequest(
    @field:NotBlank val email: String?,
    @field:NotBlank val displayName: String?
)