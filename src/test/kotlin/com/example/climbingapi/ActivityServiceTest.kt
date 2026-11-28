package com.example.climbingapi

import com.example.climbingapi.exception.ForbiddenException
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.model.Session
import com.example.climbingapi.model.SessionTick
import com.example.climbingapi.model.User
import com.example.climbingapi.repository.ActivityRepository
import com.example.climbingapi.repository.KudosRepository
import com.example.climbingapi.service.ActivityService
import com.example.climbingapi.service.UserService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.OffsetDateTime

@ExtendWith(MockitoExtension::class)
class ActivityServiceTest {

    @Mock lateinit var activityRepository: ActivityRepository
    @Mock lateinit var kudosRepository: KudosRepository
    @Mock lateinit var userService: UserService

    @InjectMocks
    lateinit var activityService: ActivityService

    private val alice = User(1, "alice@example.com", "Alice", OffsetDateTime.now(), "auth0|alice")
    private fun session(id: Int, userId: Int) =
        Session(id, userId, "Bob", 3, "BKS", "gym", LocalDate.of(2026, 6, 1), OffsetDateTime.now(), shared = true)
    private fun tick(id: Int, sessionId: Int) =
        SessionTick(id, sessionId, OffsetDateTime.now(), "flash", null, null, 9, "R", "6a", null, null, "boulder", 4, "W", false)

    @Test
    fun `getFeed loads the Ticks of the whole page in one query and clamps size`() {
        `when`(userService.getByAuth0Id("auth0|alice")).thenReturn(alice)
        `when`(activityRepository.findFeed(1, 0, 100)).thenReturn(listOf(session(10, 2), session(11, 1)))
        `when`(activityRepository.countFeed(1)).thenReturn(2)
        `when`(activityRepository.findTicksBySessionIds(listOf(10, 11))).thenReturn(listOf(tick(100, 11), tick(101, 10), tick(102, 11)))

        val result = activityService.getFeed("auth0|alice", 0, 500)

        assertEquals(100, result.pageSize)
        assertEquals(2, result.total)
        assertEquals(listOf(101), result.data[0].ticks.map { it.id })
        assertEquals(listOf(100, 102), result.data[1].ticks.map { it.id })
    }

    @Test
    fun `getFeed for an unregistered caller throws NotFoundException`() {
        `when`(userService.getByAuth0Id("auth0|nobody")).thenReturn(null)
        assertThrows(NotFoundException::class.java) { activityService.getFeed("auth0|nobody", 0, 20) }
    }

    @Test
    fun `getById of a Session the viewer can't see throws NotFoundException`() {
        `when`(userService.getByAuth0Id("auth0|alice")).thenReturn(alice)
        `when`(activityRepository.findVisibleSession(10, 1)).thenReturn(null)
        assertThrows(NotFoundException::class.java) { activityService.getById("auth0|alice", 10) }
    }

    @Test
    fun `setShared by the owner updates the Session`() {
        `when`(userService.getByAuth0Id("auth0|alice")).thenReturn(alice)
        `when`(activityRepository.findVisibleSession(10, 1)).thenReturn(session(10, 1))
        `when`(activityRepository.findTicksBySessionIds(listOf(10))).thenReturn(listOf(tick(100, 10)))

        val result = activityService.setShared("auth0|alice", 10, false)

        verify(activityRepository).updateShared(10, false)
        assertEquals(false, result.shared)
        assertEquals(listOf(100), result.ticks.map { it.id })
    }

    @Test
    fun `setShared on someone else's visible Session throws ForbiddenException`() {
        `when`(userService.getByAuth0Id("auth0|alice")).thenReturn(alice)
        `when`(activityRepository.findVisibleSession(10, 1)).thenReturn(session(10, 2))

        assertThrows(ForbiddenException::class.java) { activityService.setShared("auth0|alice", 10, false) }
        verify(activityRepository, never()).updateShared(anyInt(), anyBoolean())
    }

    @Test
    fun `setShared on a Session the viewer can't see throws NotFoundException`() {
        `when`(userService.getByAuth0Id("auth0|alice")).thenReturn(alice)
        `when`(activityRepository.findVisibleSession(10, 1)).thenReturn(null)

        assertThrows(NotFoundException::class.java) { activityService.setShared("auth0|alice", 10, true) }
        verify(activityRepository, never()).updateShared(anyInt(), anyBoolean())
    }

    @Test
    fun `giveKudos on a visible Session of someone else stores it`() {
        `when`(userService.getByAuth0Id("auth0|alice")).thenReturn(alice)
        `when`(activityRepository.findVisibleSession(10, 1)).thenReturn(session(10, 2))

        activityService.giveKudos("auth0|alice", 10)

        verify(activityRepository).giveKudos(10, 1)
    }

    @Test
    fun `giveKudos on your own Session throws IllegalArgumentException`() {
        `when`(userService.getByAuth0Id("auth0|alice")).thenReturn(alice)
        `when`(activityRepository.findVisibleSession(10, 1)).thenReturn(session(10, 1))

        assertThrows(IllegalArgumentException::class.java) { activityService.giveKudos("auth0|alice", 10) }
        verify(activityRepository, never()).giveKudos(anyInt(), anyInt())
    }

    @Test
    fun `Kudos endpoints on a Session the viewer can't see throw NotFoundException`() {
        `when`(userService.getByAuth0Id("auth0|alice")).thenReturn(alice)
        `when`(activityRepository.findVisibleSession(10, 1)).thenReturn(null)

        assertThrows(NotFoundException::class.java) { activityService.giveKudos("auth0|alice", 10) }
        assertThrows(NotFoundException::class.java) { activityService.removeKudos("auth0|alice", 10) }
        assertThrows(NotFoundException::class.java) { activityService.getKudos("auth0|alice", 10, 0, 20) }
        verify(activityRepository, never()).giveKudos(anyInt(), anyInt())
        verify(kudosRepository, never()).remove(anyInt(), anyInt())
    }

    @Test
    fun `removeKudos deletes the viewer's Kudos`() {
        `when`(userService.getByAuth0Id("auth0|alice")).thenReturn(alice)
        `when`(activityRepository.findVisibleSession(10, 1)).thenReturn(session(10, 2))

        activityService.removeKudos("auth0|alice", 10)

        verify(kudosRepository).remove(10, 1)
    }

    @Test
    fun `getKudos pages the givers and clamps size`() {
        `when`(userService.getByAuth0Id("auth0|alice")).thenReturn(alice)
        `when`(activityRepository.findVisibleSession(10, 1)).thenReturn(session(10, 2))
        `when`(kudosRepository.findGivers(10, 1, 0, 100)).thenReturn(listOf(alice))
        `when`(kudosRepository.countGivers(10, 1)).thenReturn(1)

        val result = activityService.getKudos("auth0|alice", 10, 0, 500)

        assertEquals(listOf(alice), result.data)
        assertEquals(100, result.pageSize)
    }
}
