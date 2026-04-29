package com.example.climbingapi.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

data class CreateWallRequest(
    @field:Min(value = 1, message = "areaId must be at least 1.")
    val areaId: Int?,

    @field:NotBlank(message = "name is required.")
    val name: String?,

    val description: String?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val approachInfo: String?
)
