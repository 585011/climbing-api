package com.example.climbingapi.service

import com.example.climbingapi.dto.CreateUserRequest
import com.example.climbingapi.dto.PagedResponse
import com.example.climbingapi.dto.UpdateUserRequest
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.model.User
import com.example.climbingapi.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository
) {

    fun getAll(page: Int, size: Int): PagedResponse<User> {
        val effectiveSize = size.coerceIn(1, 100)
        val data = userRepository.getAll(page, effectiveSize)
        val total = userRepository.count()
        return PagedResponse(data, page, effectiveSize, total)
    }

    fun getById(id: Int): User {
        return userRepository.getById(id) ?: throw NotFoundException("User not found: $id")
    }

    fun create(request: CreateUserRequest): User {
        return userRepository.create(User(
            id = null,
            email = request.email?.trim(),
            displayName = request.displayName?.trim(),
            createdAt = null
        ))
    }

    fun update(id: Int, request: UpdateUserRequest): User {
        return userRepository.update(id, User(
            id = null,
            email = request.email?.trim(),
            displayName = request.displayName?.trim(),
            createdAt = null
        )) ?: throw NotFoundException("User not found: $id")
    }

    fun delete(id: Int) {
        if (!userRepository.deleteById(id)) throw NotFoundException("User not found: $id")
    }
}