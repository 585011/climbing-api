package com.example.climbingapi.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class UpdateClimbingAreaRequest(
    @field:NotBlank(message = "name is required.")
    @field:Size(max = 200, message = "name must be at most 200 characters.")
    val name: String,

    @field:Size(max = 2000, message = "description must be at most 2000 characters.")
    val description: String?,

    @field:DecimalMin(value = "-90.0", message = "latitude must be between -90 and 90.")
    @field:DecimalMax(value = "90.0", message = "latitude must be between -90 and 90.")
    val latitude: BigDecimal?,

    @field:DecimalMin(value = "-180.0", message = "longitude must be between -180 and 180.")
    @field:DecimalMax(value = "180.0", message = "longitude must be between -180 and 180.")
    val longitude: BigDecimal?,

    @field:Size(max = 100, message = "region must be at most 100 characters.")
    @field:Pattern(regexp = "^[\\p{L}\\s'\\-.,]*$", message = "region contains invalid characters.")
    val region: String?
)
