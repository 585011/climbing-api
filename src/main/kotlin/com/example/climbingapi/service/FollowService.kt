package com.example.climbingapi.service

import com.example.climbingapi.dto.PagedResponse
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.model.User
import com.example.climbingapi.repository.FollowRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** One-way Follows without approval (ADR-0003). The caller is always the registered user behind the JWT. */
@Service
class FollowService(
    private val followRepository: FollowRepository,
    private val userService: UserService
) {

    @Transactional
    fun follow(callerAuth0Id: String, followeeId: Int) {
        val caller = requireCaller(callerAuth0Id)
        require(caller.id != followeeId) { "You cannot follow yourself." }
        userService.getById(followeeId)
        followRepository.follow(caller.id!!, followeeId)
    }

    fun unfollow(callerAuth0Id: String, followeeId: Int) {
        followRepository.unfollow(requireCaller(callerAuth0Id).id!!, followeeId)
    }

    fun getFollowing(callerAuth0Id: String, page: Int, size: Int): PagedResponse<User> {
        val callerId = requireCaller(callerAuth0Id).id!!
        val effectiveSize = size.coerceIn(1, 100)
        return PagedResponse(
            followRepository.getFollowing(callerId, page, effectiveSize), page, effectiveSize,
            followRepository.countFollowing(callerId)
        )
    }

    fun getFollowers(callerAuth0Id: String, page: Int, size: Int): PagedResponse<User> {
        val callerId = requireCaller(callerAuth0Id).id!!
        val effectiveSize = size.coerceIn(1, 100)
        return PagedResponse(
            followRepository.getFollowers(callerId, page, effectiveSize), page, effectiveSize,
            followRepository.countFollowers(callerId)
        )
    }

    private fun requireCaller(auth0Id: String): User =
        userService.getByAuth0Id(auth0Id) ?: throw NotFoundException("User not found")
}
