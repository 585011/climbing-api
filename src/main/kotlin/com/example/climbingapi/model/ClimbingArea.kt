package com.example.climbingapi.model

import java.math.BigDecimal
import java.time.OffsetDateTime

data class ClimbingArea(
    val id: Int?,
    val name: String?,
    val description: String?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val region: String?,
    val createdAt: OffsetDateTime?
)
