package com.example.climbingapi.model

import java.time.OffsetDateTime

data class UserRoute(
    val id: Int?,
    val userId: Int?,
    val routeId: Int?,
    val tickedAt: OffsetDateTime?,
    val style: String?,
    val rating: Int?,
    val personalNote: String?
)
