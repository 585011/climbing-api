package com.example.climbingapi.model

import java.math.BigDecimal
import java.time.OffsetDateTime

data class Wall(
    val id: Int?,
    val areaId: Int?,
    val name: String?,
    val description: String?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val approachInfo: String?,
    val createdAt: OffsetDateTime?
)
