package com.example.climbingapi.dto

import jakarta.validation.constraints.NotNull

data class RetireRouteRequest(
    @field:NotNull(message = "retired is required.")
    val retired: Boolean?
)
