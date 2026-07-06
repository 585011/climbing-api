package com.example.climbingapi

import com.example.climbingapi.dto.CreateWallRequest
import com.example.climbingapi.dto.UpdateWallRequest
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.exception.PayloadTooLargeException
import com.example.climbingapi.model.Route
import com.example.climbingapi.model.Wall
import com.example.climbingapi.repository.WallRepository
import com.example.climbingapi.service.ImageVariantService
import com.example.climbingapi.service.ImageVariants
import com.example.climbingapi.service.RouteService
import com.example.climbingapi.service.StorageService
import com.example.climbingapi.service.WallService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mock.web.MockMultipartFile
import java.time.OffsetDateTime

// Mockito's ArgumentMatchers.any() returns null, which Kotlin's non-null parameter
// types reject at the call site (NPE). This wrapper works around that interop gap.
private fun <T> any(): T = Mockito.any<T>()

@ExtendWith(MockitoExtension::class)
class WallServiceTest {

    @Mock lateinit var wallRepository: WallRepository
    @Mock lateinit var routeService: RouteService
    @Mock lateinit var storageService: StorageService
    @Mock lateinit var imageVariantService: ImageVariantService

    @InjectMocks
    lateinit var wallService: WallService

    private val sampleWall = Wall(1, 1, "Main Wall", null, null, null, null, null, OffsetDateTime.now())
    private val sampleRoute = Route(1, 1, "Test Route", "6a", 20, "sport", 8, null, null, null, OffsetDateTime.now())
    private val jpeg = MockMultipartFile("image", "photo.jpg", "image/jpeg", byteArrayOf(1, 2, 3))
    private val variants = ImageVariants(byteArrayOf(9), byteArrayOf(8), "image/jpeg")

    @Test
    fun `getById returns wall when found`() {
        `when`(wallRepository.getById(1)).thenReturn(sampleWall)
        assertEquals(sampleWall, wallService.getById(1))
    }

    @Test
    fun `getById throws NotFoundException when not found`() {
        `when`(wallRepository.getById(99)).thenReturn(null)
        assertThrows(NotFoundException::class.java) { wallService.getById(99) }
    }

    @Test
    fun `getByAreaId delegates to repository`() {
        `when`(wallRepository.findByAreaId(1)).thenReturn(listOf(sampleWall))
        assertEquals(listOf(sampleWall), wallService.getByAreaId(1))
    }

    @Test
    fun `getRoutes validates wall exists then delegates to routeService`() {
        `when`(wallRepository.getById(1)).thenReturn(sampleWall)
        `when`(routeService.getByWallId(1)).thenReturn(listOf(sampleRoute))
        assertEquals(listOf(sampleRoute), wallService.getRoutes(1))
    }

    @Test
    fun `getRoutes throws NotFoundException when wall missing`() {
        `when`(wallRepository.getById(99)).thenReturn(null)
        assertThrows(NotFoundException::class.java) { wallService.getRoutes(99) }
    }

    @Test
    fun `create returns created wall`() {
        val request = CreateWallRequest(areaId = 1, name = "New Wall", description = null,
            latitude = null, longitude = null, approachInfo = null)
        val expected = sampleWall.copy(name = "New Wall")
        `when`(wallRepository.create(Wall(null, 1, "New Wall", null, null, null, null, null, null))).thenReturn(expected)
        assertEquals(expected, wallService.create(request))
    }

    @Test
    fun `create with image uploads to storage and persists key`() {
        val request = CreateWallRequest(areaId = 1, name = "Photo Wall", description = null,
            latitude = null, longitude = null, approachInfo = null)
        val expected = sampleWall.copy(name = "Photo Wall", imageKey = "walls/abc.jpg")
        `when`(imageVariantService.generate(any(), any())).thenReturn(variants)
        `when`(storageService.upload(any(), any())).thenReturn("walls/abc.jpg", "walls/opt", "walls/thumb")
        `when`(wallRepository.create(any())).thenReturn(expected)

        assertEquals(expected, wallService.create(request, jpeg))
    }

    @Test
    fun `create with image deletes uploaded object when insert fails`() {
        val request = CreateWallRequest(areaId = 1, name = "Photo Wall", description = null,
            latitude = null, longitude = null, approachInfo = null)
        `when`(imageVariantService.generate(any(), any())).thenReturn(variants)
        `when`(storageService.upload(any(), any())).thenReturn("walls/abc.jpg", "walls/opt", "walls/thumb")
        `when`(wallRepository.create(any())).thenThrow(RuntimeException("insert failed"))

        assertThrows(RuntimeException::class.java) { wallService.create(request, jpeg) }
        verify(storageService).delete("walls/abc.jpg")
        verify(storageService).delete("walls/opt")
        verify(storageService).delete("walls/thumb")
    }

    @Test
    fun `create with unsupported image type throws IllegalArgumentException`() {
        val request = CreateWallRequest(areaId = 1, name = "Photo Wall", description = null,
            latitude = null, longitude = null, approachInfo = null)
        val text = MockMultipartFile("image", "note.txt", "text/plain", byteArrayOf(1))
        assertThrows(IllegalArgumentException::class.java) { wallService.create(request, text) }
    }

    @Test
    fun `create with oversized image throws PayloadTooLargeException`() {
        val request = CreateWallRequest(areaId = 1, name = "Photo Wall", description = null,
            latitude = null, longitude = null, approachInfo = null)
        val big = MockMultipartFile("image", "big.jpg", "image/jpeg", ByteArray(20 * 1024 * 1024 + 1))
        assertThrows(PayloadTooLargeException::class.java) { wallService.create(request, big) }
    }

    @Test
    fun `replaceImage uploads new key, updates wall, and deletes old object`() {
        val png = MockMultipartFile("image", "photo.png", "image/png", byteArrayOf(4, 5, 6))
        `when`(wallRepository.getById(1)).thenReturn(sampleWall.copy(imageKey = "old-key"))
        `when`(imageVariantService.generate(any(), any())).thenReturn(variants)
        `when`(storageService.upload(any(), any())).thenReturn("new-key", "new-opt", "new-thumb")
        `when`(wallRepository.updateImageKeys(1, "new-key", "new-opt", "new-thumb"))
            .thenReturn(sampleWall.copy(imageKey = "new-key"))

        val result = wallService.replaceImage(1, png)

        assertEquals("new-key", result.imageKey)
        verify(storageService).delete("old-key")
    }

    @Test
    fun `replaceImage on missing wall throws NotFoundException`() {
        `when`(wallRepository.getById(99)).thenReturn(null)
        assertThrows(NotFoundException::class.java) { wallService.replaceImage(99, jpeg) }
    }

    @Test
    fun `create stores original, optimized, and thumbnail keys`() {
        val request = CreateWallRequest(areaId = 1, name = "W", description = null,
            latitude = null, longitude = null, approachInfo = null)
        `when`(imageVariantService.generate(any(), any())).thenReturn(variants)
        `when`(storageService.upload(any(), any())).thenReturn("walls/orig", "walls/opt", "walls/thumb")
        var captured: Wall? = null
        `when`(wallRepository.create(any())).thenAnswer {
            captured = it.getArgument(0)
            captured
        }

        wallService.create(request, jpeg)

        assertEquals("walls/orig", captured?.imageKey)
        assertEquals("walls/opt", captured?.optimizedKey)
        assertEquals("walls/thumb", captured?.thumbnailKey)
    }

    @Test
    fun `replaceImage deletes the three old keys after a successful swap`() {
        val old = Wall(1, 1, "W", null, null, null, null, "walls/oldO", OffsetDateTime.now(),
            "walls/oldOpt", "walls/oldThumb")
        `when`(wallRepository.getById(1)).thenReturn(old)
        `when`(imageVariantService.generate(any(), any())).thenReturn(variants)
        `when`(storageService.upload(any(), any())).thenReturn("walls/newO", "walls/newOpt", "walls/newThumb")
        `when`(wallRepository.updateImageKeys(1, "walls/newO", "walls/newOpt", "walls/newThumb"))
            .thenReturn(old.copy(imageKey = "walls/newO"))

        wallService.replaceImage(1, jpeg)

        verify(storageService).delete("walls/oldO")
        verify(storageService).delete("walls/oldOpt")
        verify(storageService).delete("walls/oldThumb")
    }

    @Test
    fun `backfillImages processes only walls missing variants and is idempotent on a second run`() {
        val needs = Wall(5, 1, "W", null, null, null, null, "walls/orig.png", OffsetDateTime.now())
        `when`(wallRepository.findWallsNeedingBackfill()).thenReturn(listOf(needs), emptyList())
        `when`(storageService.get("walls/orig.png")).thenReturn(byteArrayOf(1))
        `when`(imageVariantService.generate(any(), any())).thenReturn(variants)
        `when`(storageService.upload(any(), any())).thenReturn("walls/opt", "walls/thumb")
        `when`(wallRepository.updateImageKeys(5, "walls/orig.png", "walls/opt", "walls/thumb"))
            .thenReturn(needs)

        val first = wallService.backfillImages()
        val second = wallService.backfillImages()

        assertEquals(1, first.processed)
        assertEquals(0, first.failed)
        assertEquals(0, second.processed)
    }

    @Test
    fun `backfillImages counts a per-wall failure without aborting`() {
        val a = Wall(6, 1, "A", null, null, null, null, "walls/a.png", OffsetDateTime.now())
        val b = Wall(7, 1, "B", null, null, null, null, "walls/b.png", OffsetDateTime.now())
        `when`(wallRepository.findWallsNeedingBackfill()).thenReturn(listOf(a, b))
        `when`(storageService.get("walls/a.png")).thenThrow(RuntimeException("boom"))
        `when`(storageService.get("walls/b.png")).thenReturn(byteArrayOf(1))
        `when`(imageVariantService.generate(any(), any())).thenReturn(variants)
        `when`(storageService.upload(any(), any())).thenReturn("walls/opt", "walls/thumb")
        `when`(wallRepository.updateImageKeys(eq(7), any(), any(), any())).thenReturn(b)

        val result = wallService.backfillImages()

        assertEquals(1, result.processed)
        assertEquals(1, result.failed)
    }

    @Test
    fun `update returns updated wall`() {
        val request = UpdateWallRequest(areaId = 1, name = "Updated Wall", description = null,
            latitude = null, longitude = null, approachInfo = null)
        val updated = sampleWall.copy(name = "Updated Wall")
        `when`(wallRepository.update(1, Wall(null, 1, "Updated Wall", null, null, null, null, null, null))).thenReturn(updated)
        assertEquals(updated, wallService.update(1, request))
    }

    @Test
    fun `update throws NotFoundException when wall not found`() {
        val request = UpdateWallRequest(areaId = 1, name = "X", description = null,
            latitude = null, longitude = null, approachInfo = null)
        `when`(wallRepository.update(99, Wall(null, 1, "X", null, null, null, null, null, null))).thenReturn(null)
        assertThrows(NotFoundException::class.java) { wallService.update(99, request) }
    }

    @Test
    fun `delete throws NotFoundException when wall not found`() {
        `when`(wallRepository.deleteById(99)).thenReturn(false)
        assertThrows(NotFoundException::class.java) { wallService.delete(99) }
    }

    @Test
    fun `getAll returns paged response`() {
        `when`(wallRepository.getAll(0, 20)).thenReturn(listOf(sampleWall))
        `when`(wallRepository.count()).thenReturn(1)
        val result = wallService.getAll(0, 20)
        assertEquals(listOf(sampleWall), result.data)
        assertEquals(0, result.page)
        assertEquals(20, result.pageSize)
        assertEquals(1, result.total)
    }

    @Test
    fun `getAll clamps size to max 100`() {
        `when`(wallRepository.getAll(0, 100)).thenReturn(emptyList())
        `when`(wallRepository.count()).thenReturn(0)
        val result = wallService.getAll(0, 500)
        assertEquals(100, result.pageSize)
    }
}
