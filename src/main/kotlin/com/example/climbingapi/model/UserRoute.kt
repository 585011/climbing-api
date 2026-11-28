package com.example.climbingapi.model

import java.time.OffsetDateTime

data class UserRoute(
    val id: Int?,
    val userId: Int?,
    val routeId: Int?,
    val tickedAt: OffsetDateTime?,
    val style: String?,
    val rating: Int?,
    val personalNote: String?,
    val photoKey: String? = null,
    /** The Session this Tick belongs to; null for Ticks logged before the Feed existed. */
    val activityId: Int? = null
)
