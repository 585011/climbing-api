package com.example.climbingapi

import com.example.climbingapi.dto.CreateWallRequest
import com.example.climbingapi.dto.UpdateWallRequest
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.model.Route
import com.example.climbingapi.model.Wall
import com.example.climbingapi.repository.WallRepository
import com.example.climbingapi.service.RouteService
import com.example.climbingapi.service.WallService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.OffsetDateTime

@ExtendWith(MockitoExtension::class)
class WallServiceTest {

    @Mock lateinit var wallRepository: WallRepository
    @Mock lateinit var routeService: RouteService

    @InjectMocks
    lateinit var wallService: WallService

    private val sampleWall = Wall(1, 1, "Main Wall", null, null, null, null, OffsetDateTime.now())
    private val sampleRoute = Route(1, 1, "Test Route", "6a", 20, "sport", 8, null, null, null, OffsetDateTime.now())

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
        `when`(wallRepository.create(Wall(null, 1, "New Wall", null, null, null, null, null))).thenReturn(expected)
        assertEquals(expected, wallService.create(request))
    }

    @Test
    fun `update returns updated wall`() {
        val request = UpdateWallRequest(areaId = 1, name = "Updated Wall", description = null,
            latitude = null, longitude = null, approachInfo = null)
        val updated = sampleWall.copy(name = "Updated Wall")
        `when`(wallRepository.update(1, Wall(null, 1, "Updated Wall", null, null, null, null, null))).thenReturn(updated)
        assertEquals(updated, wallService.update(1, request))
    }

    @Test
    fun `update throws NotFoundException when wall not found`() {
        val request = UpdateWallRequest(areaId = 1, name = "X", description = null,
            latitude = null, longitude = null, approachInfo = null)
        `when`(wallRepository.update(99, Wall(null, 1, "X", null, null, null, null, null))).thenReturn(null)
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
