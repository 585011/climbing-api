package com.example.climbingapi.service

import com.example.climbingapi.dto.PagedResponse
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.model.User
import com.example.climbingapi.repository.FollowRepository
import com.example.climbingapi.repository.KudosRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** One-way Follows without approval (ADR-0003). The caller is always the registered user behind the JWT. */
@Service
class FollowService(
    private val followRepository: FollowRepository,
    private val kudosRepository: KudosRepository,
    private val userService: UserService
) {

    @Transactional
    fun follow(callerAuth0Id: String, followeeId: Int) {
        val caller = requireCaller(callerAuth0Id)
        require(caller.id != followeeId) { "You cannot follow yourself." }
        // A climber who blocked the caller looks like they don't exist (ADR-0003).
        userService.getVisibleById(followeeId, caller.id)
        followRepository.follow(caller.id!!, followeeId)
    }

    fun unfollow(callerAuth0Id: String, followeeId: Int) {
        followRepository.unfollow(requireCaller(callerAuth0Id).id!!, followeeId)
    }

    /**
     * Removes [followerId] as the caller's Follower, which blocks them (ADR-0003), and deletes their Kudos on the
     * caller's Sessions. Idempotent. The block is saved even if they don't follow the caller right now, so it sticks.
     */
    @Transactional
    fun removeFollower(callerAuth0Id: String, followerId: Int) {
        val callerId = requireCaller(callerAuth0Id).id!!
        require(callerId != followerId) { "You cannot remove yourself as a Follower." }
        // Not getVisibleById: you can block someone who has blocked you.
        userService.getById(followerId)
        followRepository.block(followerId, callerId)
        kudosRepository.deleteByGiverOnSessionsOf(followerId, callerId)
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
