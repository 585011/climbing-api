package com.example.climbingapi

import com.example.climbingapi.dto.CreateUserRequest
import com.example.climbingapi.dto.UpdateUserRequest
import com.example.climbingapi.exception.ForbiddenException
import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.model.User
import com.example.climbingapi.repository.UserRepository
import com.example.climbingapi.service.UserService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.OffsetDateTime

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    lateinit var userRepository: UserRepository

    @InjectMocks
    lateinit var userService: UserService

    private val sampleUser = User(id = 1, email = "alice@example.com", displayName = "Alice", createdAt = OffsetDateTime.now(), auth0Id = "google-oauth2|alice-123")

    @Test
    fun `getById returns user when found`() {
        `when`(userRepository.getById(1)).thenReturn(sampleUser)
        val result = userService.getById(1)
        assertEquals(sampleUser, result)
    }

    @Test
    fun `getById throws NotFoundException when not found`() {
        `when`(userRepository.getById(99)).thenReturn(null)
        assertThrows(NotFoundException::class.java) { userService.getById(99) }
    }

    @Test
    fun `create trims whitespace and delegates to repository`() {
        val request = CreateUserRequest(displayName = "  Bob  ", email = "  bob@example.com  ")
        val expected = sampleUser.copy(email = "bob@example.com", displayName = "Bob", auth0Id = "google-oauth2|bob-123")
        `when`(userRepository.create(User(null, "bob@example.com", "Bob", null, "google-oauth2|bob-123"))).thenReturn(expected)

        val result = userService.create(request, "google-oauth2|bob-123", "  bob@example.com  ")

        assertEquals(expected, result)
    }

    @Test
    fun `update returns updated user when found`() {
        val request = UpdateUserRequest(email = "alice2@example.com", displayName = "Alice2")
        val updated = sampleUser.copy(email = "alice2@example.com", displayName = "Alice2")
        `when`(userRepository.update(1, User(null, "alice2@example.com", "Alice2", null, null))).thenReturn(updated)

        val result = userService.update(1, request)

        assertEquals(updated, result)
    }

    @Test
    fun `update throws NotFoundException when user not found`() {
        val request = UpdateUserRequest(email = "x@example.com", displayName = "X")
        `when`(userRepository.update(99, User(null, "x@example.com", "X", null, null))).thenReturn(null)

        assertThrows(NotFoundException::class.java) { userService.update(99, request) }
    }

    @Test
    fun `delete throws NotFoundException when user not found`() {
        `when`(userRepository.deleteById(99)).thenReturn(false)
        assertThrows(NotFoundException::class.java) { userService.delete(99) }
    }

    @Test
    fun `delete succeeds when user exists`() {
        `when`(userRepository.deleteById(1)).thenReturn(true)
        userService.delete(1)
        verify(userRepository).deleteById(1)
    }

    @Test
    fun `assertOwner passes when auth0Id matches`() {
        `when`(userRepository.getById(1)).thenReturn(sampleUser)
        userService.assertOwner(1, "google-oauth2|alice-123")
    }

    @Test
    fun `assertOwner throws ForbiddenException when auth0Id does not match`() {
        `when`(userRepository.getById(1)).thenReturn(sampleUser)
        assertThrows(ForbiddenException::class.java) { userService.assertOwner(1, "google-oauth2|other-user") }
    }

    @Test
    fun `assertOwner throws NotFoundException when user does not exist`() {
        `when`(userRepository.getById(99)).thenReturn(null)
        assertThrows(NotFoundException::class.java) { userService.assertOwner(99, "any-sub") }
    }
}