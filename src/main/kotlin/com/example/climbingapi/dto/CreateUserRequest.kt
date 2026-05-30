package com.example.climbingapi.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class CreateUserRequest(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val displayName: String
)