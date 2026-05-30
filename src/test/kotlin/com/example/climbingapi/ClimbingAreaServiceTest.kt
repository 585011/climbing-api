package com.example.climbingapi

import com.example.climbingapi.dto.CreateClimbingAreaRequest
import com.example.climbingapi.dto.UpdateClimbingAreaRequest
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.model.ClimbingArea
import com.example.climbingapi.model.Wall
import com.example.climbingapi.repository.ClimbingAreaRepository
import com.example.climbingapi.service.ClimbingAreaService
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
class ClimbingAreaServiceTest {

    @Mock lateinit var climbingAreaRepository: ClimbingAreaRepository
    @Mock lateinit var wallService: WallService

    @InjectMocks
    lateinit var climbingAreaService: ClimbingAreaService

    private val sampleArea = ClimbingArea(1, "Test Crag", "A nice crag", null, null, "Oslo", OffsetDateTime.now())
    private val sampleWall = Wall(1, 1, "Main Wall", null, null, null, null, OffsetDateTime.now())

    @Test
    fun `getById returns area when found`() {
        `when`(climbingAreaRepository.getById(1)).thenReturn(sampleArea)
        assertEquals(sampleArea, climbingAreaService.getById(1))
    }

    @Test
    fun `getById throws NotFoundException when not found`() {
        `when`(climbingAreaRepository.getById(99)).thenReturn(null)
        assertThrows(NotFoundException::class.java) { climbingAreaService.getById(99) }
    }

    @Test
    fun `getWalls validates area exists then delegates to wallService`() {
        `when`(climbingAreaRepository.getById(1)).thenReturn(sampleArea)
        `when`(wallService.getByAreaId(1)).thenReturn(listOf(sampleWall))
        assertEquals(listOf(sampleWall), climbingAreaService.getWalls(1))
    }

    @Test
    fun `getWalls throws NotFoundException when area missing`() {
        `when`(climbingAreaRepository.getById(99)).thenReturn(null)
        assertThrows(NotFoundException::class.java) { climbingAreaService.getWalls(99) }
    }

    @Test
    fun `create trims name and returns area`() {
        val request = CreateClimbingAreaRequest(name = "  Test Crag  ", description = "A nice crag",
            latitude = null, longitude = null, region = "Oslo")
        val expected = sampleArea
        `when`(climbingAreaRepository.create(ClimbingArea(null, "Test Crag", "A nice crag", null, null, "Oslo", null)))
            .thenReturn(expected)
        assertEquals(expected, climbingAreaService.create(request))
    }

    @Test
    fun `update returns updated area`() {
        val request = UpdateClimbingAreaRequest(name = "Updated Crag", description = null,
            latitude = null, longitude = null, region = "Bergen")
        val updated = sampleArea.copy(name = "Updated Crag", region = "Bergen")
        `when`(climbingAreaRepository.update(1, ClimbingArea(null, "Updated Crag", null, null, null, "Bergen", null)))
            .thenReturn(updated)
        assertEquals(updated, climbingAreaService.update(1, request))
    }

    @Test
    fun `update throws NotFoundException when area not found`() {
        val request = UpdateClimbingAreaRequest(name = "X", description = null,
            latitude = null, longitude = null, region = null)
        `when`(climbingAreaRepository.update(99, ClimbingArea(null, "X", null, null, null, null, null)))
            .thenReturn(null)
        assertThrows(NotFoundException::class.java) { climbingAreaService.update(99, request) }
    }

    @Test
    fun `delete throws NotFoundException when area not found`() {
        `when`(climbingAreaRepository.deleteById(99)).thenReturn(false)
        assertThrows(NotFoundException::class.java) { climbingAreaService.delete(99) }
    }

    @Test
    fun `getAll returns paged response`() {
        `when`(climbingAreaRepository.getAll(0, 20)).thenReturn(listOf(sampleArea))
        `when`(climbingAreaRepository.count()).thenReturn(1)
        val result = climbingAreaService.getAll(0, 20)
        assertEquals(listOf(sampleArea), result.data)
        assertEquals(0, result.page)
        assertEquals(20, result.pageSize)
        assertEquals(1, result.total)
    }

    @Test
    fun `getAll clamps size to max 100`() {
        `when`(climbingAreaRepository.getAll(0, 100)).thenReturn(emptyList())
        `when`(climbingAreaRepository.count()).thenReturn(0)
        val result = climbingAreaService.getAll(0, 150)
        assertEquals(100, result.pageSize)
    }
}
