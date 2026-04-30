package com.example.climbingapi.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class UpdateRouteRequest(
    @field:NotNull(message = "wallId is required.")
    @field:Min(value = 1, message = "wallId must be at least 1.")
    val wallId: Int?,

    val name: String?,
    val grade: String?,

    @field:Min(value = 1, message = "length must be at least 1.")
    val length: Int?,

    val style: String?,

    @field:Min(value = 0, message = "bolts must be 0 or greater.")
    val bolts: Int?,

    @field:Min(value = 1, message = "ropeLengths must be at least 1.")
    val ropeLengths: Int?,

    val firstAscendant: String?,
    val description: String?
)
