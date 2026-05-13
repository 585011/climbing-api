package com.example.climbingapi.dto

import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

data class CreateClimbingAreaRequest(
    @field:NotBlank(message = "name is required.")
    val name: String?,

    val description: String?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val region: String?
)
