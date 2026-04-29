package com.example.climbingapi.dto

import java.math.BigDecimal
import java.time.OffsetDateTime

data class WallResponse(
    val id: Int?,
    val areaId: Int?,
    val name: String?,
    val description: String?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val approachInfo: String?,
    val createdAt: OffsetDateTime?
)
