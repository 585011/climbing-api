package com.example.climbingapi.dto

import java.time.OffsetDateTime

data class TickResponse(
    val id: Int,
    val userId: Int,
    val routeId: Int,
    val tickedAt: OffsetDateTime,
    val style: String?,
    val rating: Int?,
    val personalNote: String?,
    /** Gym ticks only: short-lived presigned URL of the tick's photo, or null. */
    val photoUrl: String?
)
