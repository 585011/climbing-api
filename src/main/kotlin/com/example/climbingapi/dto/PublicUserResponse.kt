package com.example.climbingapi.dto

/** What any climber may see about another climber. Never add email or auth0Id here. */
data class PublicUserResponse(
    val id: Int,
    val displayName: String
)
