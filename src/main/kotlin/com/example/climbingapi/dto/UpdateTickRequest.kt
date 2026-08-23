package com.example.climbingapi.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UpdateTickRequest(
    @field:Pattern(
        regexp = "^(onsight|flash|redpoint|free solo)$",
        flags = [Pattern.Flag.CASE_INSENSITIVE],
        message = "style must be one of: onsight, flash, redpoint, free solo."
    )
    val style: String?,

    @field:Min(1) @field:Max(5) val rating: Int?,

    @field:Size(max = 500, message = "personalNote must be at most 500 characters.")
    @field:Pattern(
        regexp = "^[\\p{L}\\p{N}_\\s'.,!?()\\-]*$",
        message = "personalNote contains invalid characters."
    )
    val personalNote: String?
)