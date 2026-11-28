package com.example.climbingapi

import com.example.climbingapi.dto.CreateTickRequest
import com.example.climbingapi.dto.NewGymRouteRequest
import com.example.climbingapi.dto.UpdateTickRequest
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.exception.PayloadTooLargeException
import com.example.climbingapi.model.ClimbingArea
import com.example.climbingapi.model.Route
import com.example.climbingapi.model.User
import com.example.climbingapi.model.UserRoute
import com.example.climbingapi.model.Wall
import com.example.climbingapi.repository.ActivityRepository
import com.example.climbingapi.repository.ClimbingAreaRepository
import com.example.climbingapi.repository.RouteRepository
import com.example.climbingapi.repository.TickRepository
import com.example.climbingapi.repository.UserRepository
import com.example.climbingapi.repository.WallRepository
import com.example.climbingapi.service.ImageVariantService
import com.example.climbingapi.service.ImageVariants
import com.example.climbingapi.service.StorageService
import com.example.climbingapi.service.TickService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mock.web.MockMultipartFile
import java.time.LocalDate
import java.time.OffsetDateTime

@ExtendWith(MockitoExtension::class)
class TickServiceTest {

    @Mock lateinit var tickRepository: TickRepository
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var routeRepository: RouteRepository
    @Mock lateinit var wallRepository: WallRepository
    @Mock lateinit var climbingAreaRepository: ClimbingAreaRepository
    @Mock lateinit var storageService: StorageService
    @Mock lateinit var imageVariantService: ImageVariantService
    @Mock lateinit var activityRepository: ActivityRepository

    @InjectMocks
    lateinit var tickService: TickService

    private val sampleUser = User(1, "alice@example.com", "Alice", OffsetDateTime.now(), "google-oauth2|alice-123")
    private val sampleRoute = Route(1, 1, "Test Route", "6a", 20, "sport", 8, null, null, null, OffsetDateTime.now())
    private val sampleTick = UserRoute(1, 1, 1, OffsetDateTime.now(), "onsight", 4, "Good crux at the top")

    /** Stubs joining [tick] to Session 7 and returns the linked Tick. */
    private fun stubSession(tick: UserRoute): UserRoute {
        val linked = tick.copy(activityId = 7)
        `when`(activityRepository.upsertSession(tick.userId!!, tick.routeId!!, TickService.sessionDate(tick.tickedAt!!), tick.tickedAt!!))
            .thenReturn(7)
        `when`(tickRepository.setActivityId(tick.id!!, 7)).thenReturn(linked)
        return linked
    }

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
        val linked = stubSession(expected)

        val result = tickService.create(1, request)

        assertEquals(linked, result)
    }

    @Test
    fun `create lowercases style before persisting`() {
        val request = CreateTickRequest(routeId = 1, style = "Free Solo", rating = null, personalNote = null)
        val expected = sampleTick.copy(style = "free solo")
        `when`(userRepository.getById(1)).thenReturn(sampleUser)
        `when`(routeRepository.getById(1)).thenReturn(sampleRoute)
        `when`(tickRepository.create(UserRoute(null, 1, 1, null, "free solo", null, null))).thenReturn(expected)
        val linked = stubSession(expected)

        val result = tickService.create(1, request)

        assertEquals(linked, result)
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

        // A Tick from before the Feed has no Session to clean up.
        verifyNoInteractions(activityRepository)
    }

    @Test
    fun `delete deletes or refreshes the Tick's Session`() {
        `when`(userRepository.getById(1)).thenReturn(sampleUser)
        `when`(tickRepository.getById(1)).thenReturn(sampleTick.copy(activityId = 7))

        tickService.delete(1, 1)

        verify(tickRepository).deleteById(1)
        verify(activityRepository).deleteOrRefreshSession(7)
    }

    @Test
    fun `sessionDate is the Norwegian calendar day`() {
        // Oslo is UTC+2 in summer: 23:30 UTC on 31 May is 01:30 on 1 June.
        assertEquals(LocalDate.of(2026, 6, 1), TickService.sessionDate(OffsetDateTime.parse("2026-05-31T23:30:00Z")))
        assertEquals(LocalDate.of(2026, 5, 31), TickService.sessionDate(OffsetDateTime.parse("2026-05-31T21:59:00Z")))
        // UTC+1 in winter.
        assertEquals(LocalDate.of(2026, 1, 2), TickService.sessionDate(OffsetDateTime.parse("2026-01-01T23:00:00Z")))
    }

    @Test
    fun `create files a late-evening UTC Tick under the next Oslo day`() {
        val tickedAt = OffsetDateTime.parse("2026-05-31T23:30:00Z")
        val created = sampleTick.copy(tickedAt = tickedAt)
        `when`(userRepository.getById(1)).thenReturn(sampleUser)
        `when`(routeRepository.getById(1)).thenReturn(sampleRoute)
        `when`(tickRepository.create(UserRoute(null, 1, 1, null, null, null, null))).thenReturn(created)
        `when`(activityRepository.upsertSession(1, 1, LocalDate.of(2026, 6, 1), tickedAt)).thenReturn(7)
        `when`(tickRepository.setActivityId(1, 7)).thenReturn(created.copy(activityId = 7))

        val result = tickService.create(1, CreateTickRequest(routeId = 1, style = null, rating = null, personalNote = null))

        assertEquals(7, result.activityId)
    }

    private val sampleWall = Wall(5, 1, "Buldring", null, null, null, null, null, OffsetDateTime.now())
    private fun area(type: String) = ClimbingArea(1, "BKS Laksevåg", null, null, null, null, OffsetDateTime.now(), type)
    private val newRoute = NewGymRouteRequest(wallId = 5, grade = " 6B ", style = "Boulder", holdColor = "Gul")

    @Test
    fun `create with newRoute on a gym wall creates the route and ticks it`() {
        val request = CreateTickRequest(routeId = null, style = "flash", rating = null, personalNote = null, newRoute = newRoute)
        val createdRoute = Route(42, 5, null, "6B", null, "boulder", null, null, null, null, OffsetDateTime.now(),
            holdColor = "Gul", createdBy = 1)
        val expected = sampleTick.copy(routeId = 42, style = "flash")
        `when`(userRepository.getById(1)).thenReturn(sampleUser)
        `when`(wallRepository.getById(5)).thenReturn(sampleWall)
        `when`(climbingAreaRepository.getById(1)).thenReturn(area("gym"))
        `when`(routeRepository.create(Route(null, 5, null, "6B", null, "boulder", null, null, null, null, null,
            holdColor = "Gul", createdBy = 1))).thenReturn(createdRoute)
        `when`(tickRepository.create(UserRoute(null, 1, 42, null, "flash", null, null))).thenReturn(expected)
        val linked = stubSession(expected)

        assertEquals(linked, tickService.create(1, request))
    }

    @Test
    fun `create with newRoute on a crag wall is rejected`() {
        val request = CreateTickRequest(routeId = null, style = null, rating = null, personalNote = null, newRoute = newRoute)
        `when`(userRepository.getById(1)).thenReturn(sampleUser)
        `when`(wallRepository.getById(5)).thenReturn(sampleWall)
        `when`(climbingAreaRepository.getById(1)).thenReturn(area("crag"))

        assertThrows(IllegalArgumentException::class.java) { tickService.create(1, request) }
        verifyNoInteractions(tickRepository)
    }

    @Test
    fun `create with newRoute throws NotFoundException when wall missing`() {
        val request = CreateTickRequest(routeId = null, style = null, rating = null, personalNote = null, newRoute = newRoute)
        `when`(userRepository.getById(1)).thenReturn(sampleUser)
        `when`(wallRepository.getById(5)).thenReturn(null)

        assertThrows(NotFoundException::class.java) { tickService.create(1, request) }
    }

    @Test
    fun `create requires exactly one of routeId and newRoute`() {
        `when`(userRepository.getById(1)).thenReturn(sampleUser)
        val both = CreateTickRequest(routeId = 1, style = null, rating = null, personalNote = null, newRoute = newRoute)
        val neither = CreateTickRequest(routeId = null, style = null, rating = null, personalNote = null)

        assertThrows(IllegalArgumentException::class.java) { tickService.create(1, both) }
        assertThrows(IllegalArgumentException::class.java) { tickService.create(1, neither) }
        verify(routeRepository, never()).getById(1)
    }

    @Test
    fun `getByUserId passes areaType filter to repository`() {
        `when`(userRepository.getById(1)).thenReturn(sampleUser)
        `when`(tickRepository.findByUserId(1, 0, 20, "gym")).thenReturn(listOf(sampleTick))
        `when`(tickRepository.countByUserId(1, "gym")).thenReturn(1)

        val result = tickService.getByUserId(1, 0, 20, "gym")

        assertEquals(1, result.total)
    }

    private val variants = ImageVariants(optimized = byteArrayOf(9), thumbnail = byteArrayOf(8), contentType = "image/jpeg")
    private val jpeg = MockMultipartFile("image", "route.jpg", "image/jpeg", byteArrayOf(1, 2, 3))

    /** Stubs the tick → route → wall → area chain behind replacePhoto's gym check. */
    private fun stubTickInArea(type: String, tick: UserRoute = sampleTick) {
        `when`(userRepository.getById(1)).thenReturn(sampleUser)
        `when`(tickRepository.getById(1)).thenReturn(tick)
        `when`(routeRepository.getById(1)).thenReturn(sampleRoute)
        `when`(wallRepository.getById(1)).thenReturn(sampleWall)
        `when`(climbingAreaRepository.getById(1)).thenReturn(area(type))
    }

    @Test
    fun `replacePhoto stores only the resized variant under ticks and deletes the old photo`() {
        stubTickInArea("gym", sampleTick.copy(photoKey = "ticks/old.jpg"))
        `when`(imageVariantService.generate(jpeg.bytes, "image/jpeg")).thenReturn(variants)
        `when`(storageService.upload(variants.optimized, "image/jpeg", "ticks")).thenReturn("ticks/new.jpg")
        `when`(tickRepository.updatePhotoKey(1, "ticks/new.jpg")).thenReturn(sampleTick.copy(photoKey = "ticks/new.jpg"))

        val result = tickService.replacePhoto(1, 1, jpeg)

        assertEquals("ticks/new.jpg", result.photoKey)
        verify(storageService).upload(variants.optimized, "image/jpeg", "ticks")
        verify(storageService).delete("ticks/old.jpg")
        verifyNoMoreInteractions(storageService) // no original or thumbnail kept
    }

    @Test
    fun `replacePhoto on a crag tick is rejected before anything is stored`() {
        stubTickInArea("crag")

        assertThrows(IllegalArgumentException::class.java) { tickService.replacePhoto(1, 1, jpeg) }
        verifyNoInteractions(imageVariantService, storageService)
    }

    @Test
    fun `replacePhoto rejects unsupported types and files over 5 MB`() {
        stubTickInArea("gym")
        val text = MockMultipartFile("image", "note.txt", "text/plain", byteArrayOf(1))
        val huge = MockMultipartFile("image", "big.jpg", "image/jpeg", ByteArray(5 * 1024 * 1024 + 1))

        assertThrows(IllegalArgumentException::class.java) { tickService.replacePhoto(1, 1, text) }
        assertThrows(PayloadTooLargeException::class.java) { tickService.replacePhoto(1, 1, huge) }
        verifyNoInteractions(imageVariantService, storageService)
    }

    @Test
    fun `replacePhoto deletes the uploaded object when saving its key fails`() {
        stubTickInArea("gym")
        `when`(imageVariantService.generate(jpeg.bytes, "image/jpeg")).thenReturn(variants)
        `when`(storageService.upload(variants.optimized, "image/jpeg", "ticks")).thenReturn("ticks/new.jpg")
        `when`(tickRepository.updatePhotoKey(1, "ticks/new.jpg")).thenReturn(null)

        assertThrows(NotFoundException::class.java) { tickService.replacePhoto(1, 1, jpeg) }
        verify(storageService).delete("ticks/new.jpg")
    }

    @Test
    fun `delete also removes the tick's photo from storage`() {
        `when`(userRepository.getById(1)).thenReturn(sampleUser)
        `when`(tickRepository.getById(1)).thenReturn(sampleTick.copy(photoKey = "ticks/old.jpg"))

        tickService.delete(1, 1)

        verify(tickRepository).deleteById(1)
        verify(storageService).delete("ticks/old.jpg")
    }
}
