package com.example.climbingapi.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateRouteRequest(
    @field:Min(value = 1, message = "wallId must be at least 1.")
    val wallId: Int,

    @field:Size(max = 200, message = "name must be at most 200 characters.")
    val name: String?,

    @field:Size(max = 20, message = "grade must be at most 20 characters.")
    @field:Pattern(regexp = "^[a-zA-Z0-9+\\-/. ]*$", message = "grade contains invalid characters.")
    val grade: String?,

    @field:Min(value = 1, message = "length must be at least 1.")
    val length: Int?,

    @field:Pattern(
        regexp = "^(sport|trad|boulder|toprope|speed)$",
        flags = [Pattern.Flag.CASE_INSENSITIVE],
        message = "style must be one of: sport, trad, boulder, toprope, speed."
    )
    val style: String?,

    @field:Min(value = 0, message = "bolts must be 0 or greater.")
    val bolts: Int?,

    @field:Min(value = 1, message = "ropeLengths must be at least 1.")
    val ropeLengths: Int?,

    @field:Size(max = 200, message = "firstAscendant must be at most 200 characters.")
    @field:Pattern(regexp = "^[\\p{L}\\s'\\-.,]*$", message = "firstAscendant contains invalid characters.")
    val firstAscendant: String?,

    @field:Size(max = 2000, message = "description must be at most 2000 characters.")
    val description: String?,

    @field:Size(max = 30, message = "holdColor must be at most 30 characters.")
    @field:Pattern(regexp = "^[\\p{L}\\s\\-]*$", message = "holdColor may only contain letters, spaces and hyphens.")
    val holdColor: String? = null
)
