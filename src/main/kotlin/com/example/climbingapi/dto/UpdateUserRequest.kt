package com.example.climbingapi.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UpdateUserRequest(
    @field:NotBlank @field:Email
    @field:Size(max = 255, message = "email must be at most 255 characters.")
    val email: String,

    @field:NotBlank
    @field:Size(max = 100, message = "displayName must be at most 100 characters.")
    @field:Pattern(regexp = "^[\\p{L}\\p{N}\\s'\\-_.]*$", message = "displayName contains invalid characters.")
    val displayName: String
)