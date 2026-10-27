package com.example.climbingapi.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/** A gym route created by a user while ticking it. Only allowed on walls in a climbing gym. */
data class NewGymRouteRequest(
    @field:Min(value = 1, message = "wallId must be at least 1.")
    val wallId: Int,

    @field:NotBlank(message = "grade is required.")
    @field:Size(max = 20, message = "grade must be at most 20 characters.")
    @field:Pattern(regexp = "^[a-zA-Z0-9+\\-/. ]*$", message = "grade contains invalid characters.")
    val grade: String,

    @field:NotBlank(message = "style is required.")
    @field:Pattern(
        regexp = "^(sport|trad|boulder|toprope|speed)$",
        flags = [Pattern.Flag.CASE_INSENSITIVE],
        message = "style must be one of: sport, trad, boulder, toprope, speed."
    )
    val style: String,

    @field:Size(max = 30, message = "holdColor must be at most 30 characters.")
    @field:Pattern(regexp = "^[\\p{L}\\s\\-]*$", message = "holdColor may only contain letters, spaces and hyphens.")
    val holdColor: String? = null,

    @field:Size(max = 200, message = "name must be at most 200 characters.")
    val name: String? = null
)
