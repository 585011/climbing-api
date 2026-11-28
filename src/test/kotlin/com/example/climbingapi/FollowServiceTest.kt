package com.example.climbingapi

import com.example.climbingapi.exception.NotFoundException
import com.example.climbingapi.model.User
import com.example.climbingapi.repository.FollowRepository
import com.example.climbingapi.service.FollowService
import com.example.climbingapi.service.UserService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.OffsetDateTime

@ExtendWith(MockitoExtension::class)
class FollowServiceTest {

    @Mock
    lateinit var followRepository: FollowRepository

    @Mock
    lateinit var userService: UserService

    @InjectMocks
    lateinit var followService: FollowService

    private val alice = User(id = 1, email = "alice@example.com", displayName = "Alice", createdAt = OffsetDateTime.now(), auth0Id = "auth0|alice")
    private val bob = User(id = 2, email = "bob@example.com", displayName = "Bob", createdAt = OffsetDateTime.now(), auth0Id = "auth0|bob")

    @Test
    fun `follow inserts a Follow from the caller to the followee`() {
        `when`(userService.getByAuth0Id("auth0|alice")).thenReturn(alice)
        `when`(userService.getById(2)).thenReturn(bob)

        followService.follow("auth0|alice", 2)

        verify(followRepository).follow(1, 2)
    }

    @Test
    fun `follow yourself throws IllegalArgumentException`() {
        `when`(userService.getByAuth0Id("auth0|alice")).thenReturn(alice)

        assertThrows(IllegalArgumentException::class.java) { followService.follow("auth0|alice", 1) }
        verifyNoInteractions(followRepository)
    }

    @Test
    fun `follow an unknown user throws NotFoundException`() {
        `when`(userService.getByAuth0Id("auth0|alice")).thenReturn(alice)
        `when`(userService.getById(99)).thenThrow(NotFoundException("User not found: 99"))

        assertThrows(NotFoundException::class.java) { followService.follow("auth0|alice", 99) }
        verify(followRepository, never()).follow(1, 99)
    }

    @Test
    fun `follow by an unregistered caller throws NotFoundException`() {
        `when`(userService.getByAuth0Id("auth0|nobody")).thenReturn(null)

        assertThrows(NotFoundException::class.java) { followService.follow("auth0|nobody", 2) }
        verifyNoInteractions(followRepository)
    }

    @Test
    fun `unfollow deletes the caller's Follow`() {
        `when`(userService.getByAuth0Id("auth0|alice")).thenReturn(alice)

        followService.unfollow("auth0|alice", 2)

        verify(followRepository).unfollow(1, 2)
    }

    @Test
    fun `getFollowing pages the caller's Follows and clamps size to 100`() {
        `when`(userService.getByAuth0Id("auth0|alice")).thenReturn(alice)
        `when`(followRepository.getFollowing(1, 0, 100)).thenReturn(listOf(bob))
        `when`(followRepository.countFollowing(1)).thenReturn(1)

        val result = followService.getFollowing("auth0|alice", 0, 500)

        assertEquals(listOf(bob), result.data)
        assertEquals(100, result.pageSize)
        assertEquals(1, result.total)
    }

    @Test
    fun `getFollowers pages the caller's Followers`() {
        `when`(userService.getByAuth0Id("auth0|bob")).thenReturn(bob)
        `when`(followRepository.getFollowers(2, 0, 20)).thenReturn(listOf(alice))
        `when`(followRepository.countFollowers(2)).thenReturn(1)

        val result = followService.getFollowers("auth0|bob", 0, 20)

        assertEquals(listOf(alice), result.data)
        assertEquals(1, result.total)
    }
}
