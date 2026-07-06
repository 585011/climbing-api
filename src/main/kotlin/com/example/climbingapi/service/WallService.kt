package com.example.climbingapi.service

import com.example.climbingapi.dto.CreateWallRequest
import com.example.climbingapi.dto.PagedResponse
import com.example.climbingapi.dto.UpdateWallRequest
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.exception.PayloadTooLargeException
import com.example.climbingapi.model.Wall
import com.example.climbingapi.model.Route
import com.example.climbingapi.repository.WallRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class WallService(
    private val wallRepository: WallRepository,
    private val routeService: RouteService,
    private val storageService: StorageService,
    private val imageVariantService: ImageVariantService
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
        val stored = image?.let { uploadImage(it) }
        val wall = Wall(
            id = null,
            areaId = request.areaId,
            name = request.name?.trim(),
            description = request.description,
            latitude = request.latitude,
            longitude = request.longitude,
            approachInfo = request.approachInfo,
            imageKey = stored?.originalKey,
            createdAt = null,
            optimizedKey = stored?.optimizedKey,
            thumbnailKey = stored?.thumbnailKey
        )
        return try {
            wallRepository.create(wall)
        } catch (e: Exception) {
            stored?.let { deleteAll(it) }
            throw e
        }
    }

    @Transactional
    fun replaceImage(id: Int, image: MultipartFile): Wall {
        val existing = getById(id)
        val stored = uploadImage(image)
        val updated = try {
            wallRepository.updateImageKeys(id, stored.originalKey, stored.optimizedKey, stored.thumbnailKey)
                ?: throw NotFoundException("Wall not found: $id")
        } catch (e: Exception) {
            deleteAll(stored)
            throw e
        }
        listOfNotNull(existing.imageKey, existing.optimizedKey, existing.thumbnailKey)
            .forEach { storageService.delete(it) }
        return updated
    }

    fun backfillImages(): BackfillResult {
        var processed = 0
        var failed = 0
        for (wall in wallRepository.findWallsNeedingBackfill()) {
            val originalKey = wall.imageKey ?: continue
            try {
                val bytes = storageService.get(originalKey)
                val variants = imageVariantService.generate(bytes, contentTypeForKey(originalKey))
                val optimizedKey = storageService.upload(variants.optimized, variants.contentType)
                val thumbnailKey = storageService.upload(variants.thumbnail, variants.contentType)
                wallRepository.updateImageKeys(wall.id!!, originalKey, optimizedKey, thumbnailKey)
                processed++
            } catch (e: Exception) {
                logger.warn("Backfill failed for wall {}: {}", wall.id, e.message)
                failed++
            }
        }
        return BackfillResult(processed, failed)
    }

    private fun contentTypeForKey(key: String): String = when {
        key.endsWith(".png") -> "image/png"
        key.endsWith(".webp") -> "image/webp"
        else -> "image/jpeg"
    }

    data class BackfillResult(val processed: Int, val failed: Int)

    private data class StoredImage(val originalKey: String, val optimizedKey: String, val thumbnailKey: String)

    private fun uploadImage(image: MultipartFile): StoredImage {
        if (image.isEmpty) throw IllegalArgumentException("Image file is empty.")
        val contentType = image.contentType
        if (contentType == null || contentType !in ALLOWED_IMAGE_TYPES) {
            throw IllegalArgumentException("Unsupported image type. Allowed: image/jpeg, image/png, image/webp.")
        }
        if (image.size > MAX_IMAGE_BYTES) {
            throw PayloadTooLargeException("Image exceeds the maximum size of 20 MB.")
        }
        val variants = imageVariantService.generate(image.bytes, contentType)
        val originalKey = storageService.upload(image.bytes, contentType)
        val optimizedKey = storageService.upload(variants.optimized, variants.contentType)
        val thumbnailKey = storageService.upload(variants.thumbnail, variants.contentType)
        return StoredImage(originalKey, optimizedKey, thumbnailKey)
    }

    private fun deleteAll(s: StoredImage) {
        storageService.delete(s.originalKey)
        storageService.delete(s.optimizedKey)
        storageService.delete(s.thumbnailKey)
    }

    companion object {
        private val ALLOWED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp")
        private const val MAX_IMAGE_BYTES = 20L * 1024 * 1024
        private val logger = LoggerFactory.getLogger(WallService::class.java)
    }
}
