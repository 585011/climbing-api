package com.example.climbingapi

import com.example.climbingapi.dto.CreateTickRequest
import com.example.climbingapi.dto.UpdateTickRequest
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.model.Route
import com.example.climbingapi.model.User
import com.example.climbingapi.model.UserRoute
import com.example.climbingapi.repository.RouteRepository
import com.example.climbingapi.repository.TickRepository
import com.example.climbingapi.repository.UserRepository
import com.example.climbingapi.service.TickService
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
class TickServiceTest {

    @Mock lateinit var tickRepository: TickRepository
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var routeRepository: RouteRepository

    @InjectMocks
    lateinit var tickService: TickService

    private val sampleUser = User(1, "alice@example.com", "Alice", OffsetDateTime.now())
    private val sampleRoute = Route(1, 1, "Test Route", "6a", 20, "sport", 8, null, null, null, OffsetDateTime.now())
    private val sampleTick = UserRoute(1, 1, 1, OffsetDateTime.now(), "onsight", 4, "Good crux at the top")

    @Test
    fun `getByUserId returns ticks when user exists`() {
        `when`(userRepository.getById(1)).thenReturn(sampleUser)
        `when`(tickRepository.findByUserId(1, 0, 20)).thenReturn(listOf(sampleTick))
        `when`(tickRepository.countByUserId(1)).thenReturn(1)

        val result = tickService.getByUserId(1, 0, 20)

        assertEquals(listOf(sampleTick), result.data)
    }

    @Test
    fun `getByUserId throws NotFoundException when user missing`() {
        `when`(userRepository.getById(99)).thenReturn(null)
        assertThrows(NotFoundException::class.java) { tickService.getByUserId(99, 0, 20) }
    }

    @Test
    fun `getById throws NotFoundException when tick belongs to different user`() {
        val otherUserTick = sampleTick.copy(userId = 2)
        `when`(userRepository.getById(1)).thenReturn(sampleUser)
        `when`(tickRepository.getById(1)).thenReturn(otherUserTick)

        assertThrows(NotFoundException::class.java) { tickService.getById(1, 1) }
    }

    @Test
    fun `create validates user and route exist`() {
        val request = CreateTickRequest(routeId = 1, style = "onsight", rating = 4, personalNote = "Hard crux")
        val expected = sampleTick
        `when`(userRepository.getById(1)).thenReturn(sampleUser)
        `when`(routeRepository.getById(1)).thenReturn(sampleRoute)
        `when`(tickRepository.create(UserRoute(null, 1, 1, null, "onsight", 4, "Hard crux"))).thenReturn(expected)

        val result = tickService.create(1, request)

        assertEquals(expected, result)
    }

    @Test
    fun `create throws NotFoundException when route missing`() {
        val request = CreateTickRequest(routeId = 99, style = null, rating = null, personalNote = null)
        `when`(userRepository.getById(1)).thenReturn(sampleUser)
        `when`(routeRepository.getById(99)).thenReturn(null)

        assertThrows(NotFoundException::class.java) { tickService.create(1, request) }
    }

    @Test
    fun `update modifies style, rating, and personalNote`() {
        val request = UpdateTickRequest(style = "redpoint", rating = 5, personalNote = "Nailed the crux")
        val updated = sampleTick.copy(style = "redpoint", rating = 5, personalNote = "Nailed the crux")
        `when`(userRepository.getById(1)).thenReturn(sampleUser)
        `when`(tickRepository.getById(1)).thenReturn(sampleTick)
        `when`(tickRepository.update(1, sampleTick.copy(style = "redpoint", rating = 5, personalNote = "Nailed the crux"))).thenReturn(updated)

        val result = tickService.update(1, 1, request)

        assertEquals(updated, result)
    }

    @Test
    fun `delete succeeds when tick belongs to user`() {
        `when`(userRepository.getById(1)).thenReturn(sampleUser)
        `when`(tickRepository.getById(1)).thenReturn(sampleTick)

        tickService.delete(1, 1)
    }
}