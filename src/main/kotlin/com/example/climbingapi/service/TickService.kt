package com.example.climbingapi.service

import com.example.climbingapi.dto.CreateTickRequest
import com.example.climbingapi.dto.NewGymRouteRequest
import com.example.climbingapi.dto.PagedResponse
import com.example.climbingapi.dto.UpdateTickRequest
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.exception.PayloadTooLargeException
import com.example.climbingapi.model.Route
import com.example.climbingapi.model.UserRoute
import com.example.climbingapi.repository.ClimbingAreaRepository
import com.example.climbingapi.repository.RouteRepository
import com.example.climbingapi.repository.TickRepository
import com.example.climbingapi.repository.UserRepository
import com.example.climbingapi.repository.WallRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.Locale

@Service
class TickService(
    private val tickRepository: TickRepository,
    private val userRepository: UserRepository,
    private val routeRepository: RouteRepository,
    private val wallRepository: WallRepository,
    private val climbingAreaRepository: ClimbingAreaRepository,
    private val storageService: StorageService,
    private val imageVariantService: ImageVariantService
) {

    fun getByUserId(userId: Int, page: Int, size: Int, areaType: String? = null): PagedResponse<UserRoute> {
        userRepository.getById(userId) ?: throw NotFoundException("User not found: $userId")
        val effectiveSize = size.coerceIn(1, 100)
        val data = tickRepository.findByUserId(userId, page, effectiveSize, areaType)
        val total = tickRepository.countByUserId(userId, areaType)
        return PagedResponse(data, page, effectiveSize, total)
    }

    fun getById(userId: Int, tickId: Int): UserRoute {
        userRepository.getById(userId) ?: throw NotFoundException("User not found: $userId")
        val tick = tickRepository.getById(tickId) ?: throw NotFoundException("Tick not found: $tickId")
        if (tick.userId != userId) throw NotFoundException("Tick not found: $tickId")
        return tick
    }

    @Transactional
    fun create(userId: Int, request: CreateTickRequest): UserRoute {
        userRepository.getById(userId) ?: throw NotFoundException("User not found: $userId")
        require((request.routeId == null) != (request.newRoute == null)) {
            "Exactly one of routeId or newRoute is required."
        }
        val routeId = if (request.routeId != null) {
            routeRepository.getById(request.routeId) ?: throw NotFoundException("Route not found: ${request.routeId}")
            request.routeId
        } else {
            createGymRoute(userId, request.newRoute!!).id
        }
        return tickRepository.create(UserRoute(
            id = null,
            userId = userId,
            routeId = routeId,
            tickedAt = null,
            style = request.style?.lowercase(Locale.ROOT),
            rating = request.rating,
            personalNote = request.personalNote
        ))
    }

    fun update(userId: Int, tickId: Int, request: UpdateTickRequest): UserRoute {
        val tick = getById(userId, tickId)
        return tickRepository.update(tickId, tick.copy(
            style = request.style?.lowercase(Locale.ROOT),
            rating = request.rating,
            personalNote = request.personalNote
        )) ?: throw NotFoundException("Tick not found: $tickId")
    }

    fun delete(userId: Int, tickId: Int) {
        val tick = getById(userId, tickId)
        tickRepository.deleteById(tickId)
        tick.photoKey?.let { storageService.delete(it) }
    }

    /** Adds or replaces a gym tick's photo; the previous photo object is deleted best-effort. */
    @Transactional
    fun replacePhoto(userId: Int, tickId: Int, photo: MultipartFile): UserRoute {
        val tick = getById(userId, tickId)
        requireGymRoute(tick.routeId!!)
        val key = uploadPhoto(photo)
        val updated = try {
            tickRepository.updatePhotoKey(tickId, key) ?: throw NotFoundException("Tick not found: $tickId")
        } catch (e: Exception) {
            storageService.delete(key)
            throw e
        }
        tick.photoKey?.let { storageService.delete(it) }
        return updated
    }

    private fun requireGymRoute(routeId: Int) {
        val route = routeRepository.getById(routeId) ?: throw NotFoundException("Route not found: $routeId")
        val wall = wallRepository.getById(route.wallId!!)
            ?: throw NotFoundException("Wall not found: ${route.wallId}")
        val area = climbingAreaRepository.getById(wall.areaId!!)
            ?: throw NotFoundException("Climbing area not found: ${wall.areaId}")
        require(area.type == "gym") { "Photos can only be added to ticks in a climbing gym." }
    }

    private fun uploadPhoto(photo: MultipartFile): String {
        if (photo.isEmpty) throw IllegalArgumentException("Photo file is empty.")
        val contentType = photo.contentType
        if (contentType == null || contentType !in ALLOWED_PHOTO_TYPES) {
            throw IllegalArgumentException("Unsupported image type. Allowed: image/jpeg, image/png, image/webp.")
        }
        if (photo.size > MAX_PHOTO_BYTES) {
            throw PayloadTooLargeException("Photo exceeds the maximum size of 5 MB.")
        }
        // Unlike walls, keep only the resized variant (no original, no thumbnail): it
        // bounds the stored size even when a client skips its own compression.
        val variants = imageVariantService.generate(photo.bytes, contentType)
        return storageService.upload(variants.optimized, variants.contentType, PHOTO_KEY_PREFIX)
    }

    private fun createGymRoute(userId: Int, newRoute: NewGymRouteRequest): Route {
        val wall = wallRepository.getById(newRoute.wallId)
            ?: throw NotFoundException("Wall not found: ${newRoute.wallId}")
        val area = climbingAreaRepository.getById(wall.areaId!!)
            ?: throw NotFoundException("Climbing area not found: ${wall.areaId}")
        require(area.type == "gym") { "New routes can only be added to walls in a climbing gym." }
        return routeRepository.create(Route(
            id = null,
            wallId = newRoute.wallId,
            name = newRoute.name?.trim()?.ifEmpty { null },
            grade = newRoute.grade.trim(),
            length = null,
            style = newRoute.style.lowercase(Locale.ROOT),
            bolts = null,
            ropeLengths = null,
            firstAscendant = null,
            description = null,
            createdAt = null,
            holdColor = newRoute.holdColor?.trim()?.ifEmpty { null },
            createdBy = userId
        ))
    }

    companion object {
        private val ALLOWED_PHOTO_TYPES = setOf("image/jpeg", "image/png", "image/webp")
        private const val MAX_PHOTO_BYTES = 5L * 1024 * 1024
        private const val PHOTO_KEY_PREFIX = "ticks"
    }
}
