package com.example.climbingapi.dto

import jakarta.validation.constraints.NotNull

data class UpdateSharingRequest(
    @field:NotNull(message = "shareActivity is required.")
    val shareActivity: Boolean?
)
