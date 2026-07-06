package com.example.climbingapi

import com.example.climbingapi.mapper.WallMapper
import com.example.climbingapi.model.Wall
import com.example.climbingapi.service.StorageService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import java.time.OffsetDateTime

@ExtendWith(MockitoExtension::class)
class WallMapperTest {

    @Mock lateinit var storageService: StorageService

    private fun mapper() = WallMapper(storageService)

    @Test
    fun `imageUrl prefers optimized key, thumbnailUrl prefers thumbnail key`() {
        lenient().`when`(storageService.presignGet("opt")).thenReturn("http://opt")
        lenient().`when`(storageService.presignGet("thumb")).thenReturn("http://thumb")
        val wall = Wall(1, 1, "W", null, null, null, null, "orig", OffsetDateTime.now(), "opt", "thumb")

        val res = mapper().toResponse(wall)

        assertEquals("http://opt", res.imageUrl)
        assertEquals("http://thumb", res.thumbnailUrl)
    }

    @Test
    fun `falls back to the original key when variants are missing`() {
        lenient().`when`(storageService.presignGet("orig")).thenReturn("http://orig")
        val wall = Wall(1, 1, "W", null, null, null, null, "orig", OffsetDateTime.now())

        val res = mapper().toResponse(wall)

        assertEquals("http://orig", res.imageUrl)
        assertEquals("http://orig", res.thumbnailUrl)
    }

    @Test
    fun `null image produces null urls`() {
        val wall = Wall(1, 1, "W", null, null, null, null, null, OffsetDateTime.now())
        val res = mapper().toResponse(wall)
        assertNull(res.imageUrl)
        assertNull(res.thumbnailUrl)
    }
}
