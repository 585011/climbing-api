package com.example.climbingapi.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class UpdateTickRequest(
    val style: String?,
    @field:Min(1) @field:Max(5) val rating: Int?,
    val personalNote: String?
)