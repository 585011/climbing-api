package com.example.climbingapi

import com.example.climbingapi.dto.CreateRouteRequest
import com.example.climbingapi.dto.UpdateRouteRequest
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.model.Route
import com.example.climbingapi.model.Wall
import com.example.climbingapi.repository.RouteRepository
import com.example.climbingapi.repository.WallRepository
import com.example.climbingapi.service.RouteService
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
class RouteServiceTest {

    @Mock lateinit var routeRepository: RouteRepository
    @Mock lateinit var wallRepository: WallRepository

    @InjectMocks
    lateinit var routeService: RouteService

    private val sampleWall = Wall(1, 1, "Main Wall", null, null, null, null, null, OffsetDateTime.now())
    private val sampleRoute = Route(1, 1, "Test Route", "6a", 20, "sport", 8, null, null, null, OffsetDateTime.now())

    @Test
    fun `getById returns route when found`() {
        `when`(routeRepository.getById(1)).thenReturn(sampleRoute)
        assertEquals(sampleRoute, routeService.getById(1))
    }

    @Test
    fun `getById throws NotFoundException when not found`() {
        `when`(routeRepository.getById(99)).thenReturn(null)
        assertThrows(NotFoundException::class.java) { routeService.getById(99) }
    }

    @Test
    fun `getByWallId delegates to repository`() {
        `when`(routeRepository.findByWallId(1)).thenReturn(listOf(sampleRoute))
        assertEquals(listOf(sampleRoute), routeService.getByWallId(1))
    }

    @Test
    fun `create validates wall exists and returns route`() {
        val request = CreateRouteRequest(wallId = 1, name = "New Route", grade = "6b", length = 15,
            style = "sport", bolts = 6, ropeLengths = null, firstAscendant = null, description = null)
        val expected = sampleRoute.copy(name = "New Route", grade = "6b")
        `when`(wallRepository.getById(1)).thenReturn(sampleWall)
        `when`(routeRepository.create(Route(null, 1, "New Route", "6b", 15, "sport", 6, null, null, null, null)))
            .thenReturn(expected)

        assertEquals(expected, routeService.create(request))
    }

    @Test
    fun `create throws NotFoundException when wall missing`() {
        val request = CreateRouteRequest(wallId = 99, name = "Route", grade = "6a", length = 10,
            style = "sport", bolts = 5, ropeLengths = null, firstAscendant = null, description = null)
        `when`(wallRepository.getById(99)).thenReturn(null)
        assertThrows(NotFoundException::class.java) { routeService.create(request) }
    }

    @Test
    fun `update throws NotFoundException when wall missing`() {
        val request = UpdateRouteRequest(wallId = 99, name = "Route", grade = "6a", length = 10,
            style = "sport", bolts = 5, ropeLengths = null, firstAscendant = null, description = null)
        `when`(wallRepository.getById(99)).thenReturn(null)
        assertThrows(NotFoundException::class.java) { routeService.update(1, request) }
    }

    @Test
    fun `update throws NotFoundException when route missing`() {
        val request = UpdateRouteRequest(wallId = 1, name = "Route", grade = "6a", length = 10,
            style = "sport", bolts = 5, ropeLengths = null, firstAscendant = null, description = null)
        `when`(wallRepository.getById(1)).thenReturn(sampleWall)
        `when`(routeRepository.update(99, Route(null, 1, "Route", "6a", 10, "sport", 5, null, null, null, null)))
            .thenReturn(null)
        assertThrows(NotFoundException::class.java) { routeService.update(99, request) }
    }

    @Test
    fun `delete throws NotFoundException when route not found`() {
        `when`(routeRepository.deleteById(99)).thenReturn(false)
        assertThrows(NotFoundException::class.java) { routeService.delete(99) }
    }

    @Test
    fun `getAll returns paged response`() {
        `when`(routeRepository.getAll(0, 20)).thenReturn(listOf(sampleRoute))
        `when`(routeRepository.count()).thenReturn(1)
        val result = routeService.getAll(0, 20)
        assertEquals(listOf(sampleRoute), result.data)
        assertEquals(0, result.page)
        assertEquals(20, result.pageSize)
        assertEquals(1, result.total)
    }

    @Test
    fun `getAll clamps size to max 100`() {
        `when`(routeRepository.getAll(0, 100)).thenReturn(emptyList())
        `when`(routeRepository.count()).thenReturn(0)
        val result = routeService.getAll(0, 200)
        assertEquals(100, result.pageSize)
    }
}
