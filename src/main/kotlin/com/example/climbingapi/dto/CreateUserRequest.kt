package com.example.climbingapi.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class CreateUserRequest(
    @field:NotBlank val displayName: String,
    @field:Email @field:NotBlank val email: String
)