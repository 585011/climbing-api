package com.example.climbingapi.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class CreateTickRequest(
    @field:NotNull @field:Min(1) val routeId: Int?,
    val style: String?,
    @field:Min(1) @field:Max(5) val rating: Int?,
    val personalNote: String?
)