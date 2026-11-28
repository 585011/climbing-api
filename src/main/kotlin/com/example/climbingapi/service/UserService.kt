package com.example.climbingapi.service

import com.example.climbingapi.dto.CreateUserRequest
import com.example.climbingapi.dto.PagedResponse
import com.example.climbingapi.dto.UpdateUserRequest
import com.example.climbingapi.exception.ForbiddenException
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.model.User
import com.example.climbingapi.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository
) {

    /**
     * Lists the users [viewerAuth0Id] may see, optionally filtered by a display-name search [query]
     * (trimmed, at least 2 characters). Users who blocked the viewer are left out.
     */
    fun getAll(page: Int, size: Int, query: String? = null, viewerAuth0Id: String? = null): PagedResponse<User> {
        val trimmed = query?.trim()
        require(trimmed == null || trimmed.length >= 2) { "q must be at least 2 characters." }
        val effectiveSize = size.coerceIn(1, 100)
        val viewerId = viewerAuth0Id?.let { userRepository.findByAuth0Id(it)?.id }
        val data = userRepository.getAll(page, effectiveSize, trimmed, viewerId)
        val total = userRepository.count(trimmed, viewerId)
        return PagedResponse(data, page, effectiveSize, total)
    }

    fun getById(id: Int): User {
        return userRepository.getById(id) ?: throw NotFoundException("User not found: $id")
    }

    /** Like [getById], but a user who blocked [viewerId] looks like they don't exist (404). */
    fun getVisibleById(id: Int, viewerId: Int?): User {
        return userRepository.getVisibleById(id, viewerId) ?: throw NotFoundException("User not found: $id")
    }

    /** [getVisibleById] for the caller behind [viewerAuth0Id], who may not have registered yet. */
    fun getVisibleById(id: Int, viewerAuth0Id: String): User =
        getVisibleById(id, userRepository.findByAuth0Id(viewerAuth0Id)?.id)

    fun getByAuth0Id(auth0Id: String): User? = userRepository.findByAuth0Id(auth0Id)

    fun assertOwner(userId: Int, callerAuth0Id: String) {
        val user = getById(userId)
        if (user.auth0Id != callerAuth0Id) throw ForbiddenException("Access denied")
    }

    fun create(request: CreateUserRequest, auth0Id: String, email: String): User {
        return userRepository.create(User(
            id = null,
            email = email.trim(),
            displayName = request.displayName.trim(),
            createdAt = null,
            auth0Id = auth0Id
        ))
    }

    fun update(id: Int, request: UpdateUserRequest): User {
        return userRepository.update(id, User(
            id = null,
            email = request.email.trim(),
            displayName = request.displayName.trim(),
            createdAt = null,
            auth0Id = null
        )) ?: throw NotFoundException("User not found: $id")
    }

    fun setShareActivity(id: Int, shareActivity: Boolean): User {
        return userRepository.updateShareActivity(id, shareActivity) ?: throw NotFoundException("User not found: $id")
    }

    fun delete(id: Int) {
        if (!userRepository.deleteById(id)) throw NotFoundException("User not found: $id")
    }
}
