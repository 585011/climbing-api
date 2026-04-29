package com.example.climbingapi.model

import java.time.OffsetDateTime

data class Route(
    val id: Int?,
    val wallId: Int?,
    val name: String?,
    val grade: String?,
    val length: Int?,
    val style: String?,
    val bolts: Int?,
    val ropeLengths: Int?,
    val firstAscendant: String?,
    val description: String?,
    val createdAt: OffsetDateTime?
)
