package com.example.climbingapi.service

import com.example.climbingapi.dto.CreateWallRequest
import com.example.climbingapi.dto.PagedResponse
import com.example.climbingapi.dto.UpdateWallRequest
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.exception.PayloadTooLargeException
import com.example.climbingapi.model.Wall
import com.example.climbingapi.model.Route
import com.example.climbingapi.repository.WallRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class WallService(
    private val wallRepository: WallRepository,
    private val routeService: RouteService,
    private val storageService: StorageService
) {

    fun getAll(page: Int, size: Int): PagedResponse<Wall> {
        val effectiveSize = size.coerceIn(1, 100)
        val data = wallRepository.getAll(page, effectiveSize)
        val total = wallRepository.count()
        return PagedResponse(data, page, effectiveSize, total)
    }

    fun getById(id: Int): Wall {
        return wallRepository.getById(id)
            ?: throw NotFoundException("Wall not found: $id")
    }

    fun getByAreaId(areaId: Int): List<Wall> {
        return wallRepository.findByAreaId(areaId)
    }

    fun getRoutes(wallId: Int): List<Route> {
        getById(wallId)
        return routeService.getByWallId(wallId)
    }

    fun delete(id: Int) {
        if (!wallRepository.deleteById(id)) throw NotFoundException("Wall not found: $id")
    }

    fun update(id: Int, request: UpdateWallRequest): Wall {
        return wallRepository.update(id, Wall(
            id = null,
            areaId = request.areaId,
            name = request.name?.trim(),
            description = request.description,
            latitude = request.latitude,
            longitude = request.longitude,
            approachInfo = request.approachInfo,
            imageKey = null,
            createdAt = null
        )) ?: throw NotFoundException("Wall not found: $id")
    }

    fun create(request: CreateWallRequest, image: MultipartFile? = null): Wall {
        val imageKey = image?.let { uploadImage(it) }
        val wall = Wall(
            id = null,
            areaId = request.areaId,
            name = request.name?.trim(),
            description = request.description,
            latitude = request.latitude,
            longitude = request.longitude,
            approachInfo = request.approachInfo,
            imageKey = imageKey,
            createdAt = null
        )

        return try {
            wallRepository.create(wall)
        } catch (e: Exception) {
            // Compensate the orphaned object if the row insert fails (e.g. bad areaId FK).
            imageKey?.let { storageService.delete(it) }
            throw e
        }
    }

    @Transactional
    fun replaceImage(id: Int, image: MultipartFile): Wall {
        val oldKey = getById(id).imageKey
        val newKey = uploadImage(image)
        val updated = try {
            wallRepository.updateImageKey(id, newKey) ?: throw NotFoundException("Wall not found: $id")
        } catch (e: Exception) {
            storageService.delete(newKey)
            throw e
        }
        oldKey?.let { storageService.delete(it) }
        return updated
    }

    private fun uploadImage(image: MultipartFile): String {
        if (image.isEmpty) throw IllegalArgumentException("Image file is empty.")
        val contentType = image.contentType
        if (contentType == null || contentType !in ALLOWED_IMAGE_TYPES) {
            throw IllegalArgumentException("Unsupported image type. Allowed: image/jpeg, image/png, image/webp.")
        }
        if (image.size > MAX_IMAGE_BYTES) {
            throw PayloadTooLargeException("Image exceeds the maximum size of 20 MB.")
        }
        return storageService.upload(image.bytes, contentType)
    }

    companion object {
        private val ALLOWED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp")
        private const val MAX_IMAGE_BYTES = 20L * 1024 * 1024
    }
}
