package com.example.climbingapi.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateTickRequest(
    @field:Min(1) val routeId: Int,

    @field:Pattern(
        regexp = "^(onsight|flash|redpoint)$",
        message = "style must be one of: onsight, flash, redpoint."
    )
    val style: String?,

    @field:Min(1) @field:Max(5) val rating: Int?,

    @field:Size(max = 500, message = "personalNote must be at most 500 characters.")
    @field:Pattern(
        regexp = "^[\\w\\s'.,!?()\\-]*$",
        message = "personalNote contains invalid characters."
    )
    val personalNote: String?
)
