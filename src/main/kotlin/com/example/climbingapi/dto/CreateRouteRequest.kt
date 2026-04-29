package com.example.climbingapi.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateRouteRequest(
    @field:NotNull(message = "wallId is required.")
    @field:Min(value = 1, message = "wallId must be at least 1.")
    val wallId: Int?,

    @field:NotBlank(message = "name is required.")
    val name: String?,

    @field:NotBlank(message = "grade is required.")
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
