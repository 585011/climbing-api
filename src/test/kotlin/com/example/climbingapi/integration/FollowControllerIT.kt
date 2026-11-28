package com.example.climbingapi.integration

import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class FollowControllerIT : IntegrationTestBase() {

    private val followingUrl = "/api/users/me/following"
    private val followersUrl = "/api/users/me/followers"

    private val alice = testJwt(sub = "auth0|alice", email = "alice@example.com")
    private val bob = testJwt(sub = "auth0|bob", email = "bob@example.com")
    private val carol = testJwt(sub = "auth0|carol", email = "carol@example.com")
    private var aliceId = 0
    private var bobId = 0
    private var carolId = 0

    @BeforeEach
    fun registerClimbers() {
        aliceId = extractId(postJson("/api/users/me", """{"displayName":"Alice","email":"alice@example.com"}""", alice))
        bobId = extractId(postJson("/api/users/me", """{"displayName":"Bob","email":"bob@example.com"}""", bob))
        carolId = extractId(postJson("/api/users/me", """{"displayName":"Carol","email":"carol@example.com"}""", carol))
    }

    private fun countFollows(): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM follows", Int::class.java)!!

    @Test
    fun `following twice keeps one Follow and returns 204 both times`() {
        repeat(2) {
            mockMvc.perform(put("$followingUrl/$bobId").with(alice)).andExpect(status().isNoContent)
        }
        assertEquals(1, countFollows())
    }

    @Test
    fun `A follows B shows up in both lists without private fields`() {
        mockMvc.perform(put("$followingUrl/$bobId").with(alice)).andExpect(status().isNoContent)

        mockMvc.perform(get(followersUrl).with(bob))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.data[0].id").value(aliceId))
            .andExpect(jsonPath("$.data[0].displayName").value("Alice"))
            .andExpect(jsonPath("$.data[0].email").doesNotExist())
            .andExpect(jsonPath("$.data[0].auth0Id").doesNotExist())

        mockMvc.perform(get(followingUrl).with(alice))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.data[0].id").value(bobId))
            .andExpect(jsonPath("$.data[0].email").doesNotExist())

        // One-way: Bob doesn't follow Alice back.
        mockMvc.perform(get(followingUrl).with(bob)).andExpect(jsonPath("$.total").value(0))
    }

    @Test
    fun `following list is newest first`() {
        mockMvc.perform(put("$followingUrl/$bobId").with(alice)).andExpect(status().isNoContent)
        mockMvc.perform(put("$followingUrl/$carolId").with(alice)).andExpect(status().isNoContent)

        mockMvc.perform(get(followingUrl).with(alice))
            .andExpect(jsonPath("$.data[*].displayName").value(contains("Carol", "Bob")))
    }

    @Test
    fun `following yourself returns 400`() {
        mockMvc.perform(put("$followingUrl/$aliceId").with(alice))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
        assertEquals(0, countFollows())
    }

    @Test
    fun `following an unknown user returns 404`() {
        mockMvc.perform(put("$followingUrl/9999").with(alice))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `unregistered caller gets 404`() {
        val stranger = testJwt(sub = "auth0|stranger")
        mockMvc.perform(put("$followingUrl/$bobId").with(stranger)).andExpect(status().isNotFound)
        mockMvc.perform(get(followingUrl).with(stranger)).andExpect(status().isNotFound)
        mockMvc.perform(get(followersUrl).with(stranger)).andExpect(status().isNotFound)
    }

    @Test
    fun `missing JWT returns 401`() {
        mockMvc.perform(get(followingUrl)).andExpect(status().isUnauthorized)
    }

    @Test
    fun `unfollow removes the Follow and is idempotent`() {
        mockMvc.perform(put("$followingUrl/$bobId").with(alice)).andExpect(status().isNoContent)

        repeat(2) {
            mockMvc.perform(delete("$followingUrl/$bobId").with(alice)).andExpect(status().isNoContent)
        }
        assertEquals(0, countFollows())
        mockMvc.perform(get(followersUrl).with(bob)).andExpect(jsonPath("$.total").value(0))
    }

    @Test
    fun `deleting a user removes their Follows`() {
        mockMvc.perform(put("$followingUrl/$bobId").with(alice)).andExpect(status().isNoContent)
        mockMvc.perform(put("$followingUrl/$aliceId").with(carol)).andExpect(status().isNoContent)

        mockMvc.perform(delete("/api/users/$aliceId").with(alice)).andExpect(status().isNoContent)

        assertEquals(0, countFollows())
        mockMvc.perform(get(followersUrl).with(bob)).andExpect(jsonPath("$.total").value(0))
        mockMvc.perform(get(followingUrl).with(carol)).andExpect(jsonPath("$.total").value(0))
    }
}
