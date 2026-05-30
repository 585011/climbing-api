package com.example.climbingapi.mapper

import com.example.climbingapi.dto.UserResponse
import com.example.climbingapi.model.User
import org.springframework.stereotype.Component

@Component
class UserMapper {

    fun toResponse(user: User): UserResponse {
        return UserResponse(
            id = user.id!!,
            email = user.email!!,
            displayName = user.displayName!!,
            createdAt = user.createdAt!!
        )
    }
}
