package com.example.climbingapi.dto

import java.time.LocalDate
import java.time.OffsetDateTime

/** A Session in the Feed. Never add a Personal Note here: Followers see this. */
data class SessionResponse(
    val id: Int,
    val type: String = "session",
    val date: LocalDate,
    val lastTickAt: OffsetDateTime,
    /** The stored per-Session value; only meaningful to the owner. */
    val shared: Boolean,
    val kudosCount: Int,
    /** Whether you have given this Session Kudos. */
    val hasGivenKudos: Boolean,
    val user: PublicUserResponse,
    val area: SessionAreaResponse,
    val ticks: List<SessionTickResponse>
)

data class SessionAreaResponse(
    val id: Int,
    val name: String,
    val type: String
)

data class SessionTickResponse(
    val id: Int,
    val tickedAt: OffsetDateTime,
    val style: String?,
    val rating: Int?,
    val photoUrl: String?,
    val route: SessionRouteResponse
)

data class SessionRouteResponse(
    val id: Int,
    val name: String?,
    val grade: String?,
    val gradeBand: String?,
    val holdColor: String?,
    val style: String?,
    val wallId: Int,
    val wallName: String?,
    val retired: Boolean
)
