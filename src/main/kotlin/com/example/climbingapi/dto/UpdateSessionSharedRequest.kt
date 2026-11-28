package com.example.climbingapi.dto

import jakarta.validation.constraints.NotNull

data class UpdateSessionSharedRequest(
    @field:NotNull(message = "shared is required.")
    val shared: Boolean?
)
