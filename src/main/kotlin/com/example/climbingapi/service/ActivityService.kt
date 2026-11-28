package com.example.climbingapi.service

import com.example.climbingapi.dto.PagedResponse
import com.example.climbingapi.exception.ForbiddenException
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.model.Session
import com.example.climbingapi.model.User
import com.example.climbingapi.repository.ActivityRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** The Following Feed and single Sessions (ADR-0001, ADR-0002). The viewer is the registered user behind the JWT. */
@Service
class ActivityService(
    private val activityRepository: ActivityRepository,
    private val userService: UserService
) {

    fun getFeed(viewerAuth0Id: String, page: Int, size: Int): PagedResponse<Session> {
        val viewerId = requireViewer(viewerAuth0Id).id!!
        val effectiveSize = size.coerceIn(1, 100)
        val sessions = activityRepository.findFeed(viewerId, page, effectiveSize)
        val total = activityRepository.countFeed(viewerId)
        return PagedResponse(withTicks(sessions), page, effectiveSize, total)
    }

    fun getById(viewerAuth0Id: String, id: Int): Session {
        val session = requireVisible(id, requireViewer(viewerAuth0Id).id!!)
        return withTicks(listOf(session)).single()
    }

    /** Owner only. Allowed while the account Sharing setting is off; it then takes effect once that is turned on. */
    @Transactional
    fun setShared(viewerAuth0Id: String, id: Int, shared: Boolean): Session {
        val viewerId = requireViewer(viewerAuth0Id).id!!
        val session = requireVisible(id, viewerId)
        if (session.userId != viewerId) throw ForbiddenException("Only the climber can share or hide their Session")
        activityRepository.updateShared(id, shared)
        return withTicks(listOf(session.copy(shared = shared))).single()
    }

    /** 404 (never 403) when the viewer can't see the Session, so hidden Sessions look like missing ones. */
    fun requireVisible(id: Int, viewerId: Int): Session =
        activityRepository.findVisibleSession(id, viewerId) ?: throw NotFoundException("Session not found: $id")

    private fun withTicks(sessions: List<Session>): List<Session> {
        val ticksBySession = activityRepository.findTicksBySessionIds(sessions.map { it.id }).groupBy { it.sessionId }
        return sessions.map { it.copy(ticks = ticksBySession[it.id].orEmpty()) }
    }

    private fun requireViewer(auth0Id: String): User =
        userService.getByAuth0Id(auth0Id) ?: throw NotFoundException("User not found")
}
