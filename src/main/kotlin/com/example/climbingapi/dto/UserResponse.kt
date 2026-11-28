package com.example.climbingapi.dto

import java.time.OffsetDateTime

data class UserResponse(
    val id: Int,
    val email: String,
    val displayName: String,
    val createdAt: OffsetDateTime,
    val auth0Id: String,
    /** Account-wide Sharing setting; the master switch for whether Followers see shared Sessions. */
    val shareActivity: Boolean
)
