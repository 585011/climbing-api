package com.example.climbingapi.model

import java.time.LocalDate
import java.time.OffsetDateTime

/** A Session as read for the Feed: the Activity row plus its climber and Climbing Area. */
data class Session(
    val id: Int,
    val userId: Int,
    val userDisplayName: String,
    val areaId: Int,
    val areaName: String,
    val areaType: String,
    val date: LocalDate,
    val lastTickAt: OffsetDateTime,
    val shared: Boolean,
    val kudosCount: Int = 0,
    /** Whether the viewer the Session was read for has given it Kudos. */
    val hasGivenKudos: Boolean = false,
    val ticks: List<SessionTick> = emptyList()
)

/** A Tick inside a Session, with its Route. Deliberately has no Personal Note. */
data class SessionTick(
    val id: Int,
    val sessionId: Int,
    val tickedAt: OffsetDateTime,
    val style: String?,
    val rating: Int?,
    val photoKey: String?,
    val routeId: Int,
    val routeName: String?,
    val grade: String?,
    val gradeBand: String?,
    val holdColor: String?,
    val routeStyle: String?,
    val wallId: Int,
    val wallName: String?,
    val retired: Boolean
)
