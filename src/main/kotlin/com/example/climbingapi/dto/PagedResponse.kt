package com.example.climbingapi.dto

data class PagedResponse<T>(
    val data: List<T>,
    val page: Int,
    val pageSize: Int,
    val total: Int
)
