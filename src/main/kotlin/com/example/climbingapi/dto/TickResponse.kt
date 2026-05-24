package com.example.climbingapi.dto

import java.time.OffsetDateTime

data class TickResponse(
    val id: Int?,
    val userId: Int?,
    val routeId: Int?,
    val tickedAt: OffsetDateTime?,
    val style: String?,
    val rating: Int?,
    val personalNote: String?
)