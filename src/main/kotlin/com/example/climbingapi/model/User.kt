package com.example.climbingapi.model

import java.time.OffsetDateTime

data class User(
    val id: Int?,
    val email: String?,
    val displayName: String?,
    val createdAt: OffsetDateTime?
)
