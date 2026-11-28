package com.example.climbingapi.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** Block (ADR-0003): Bob removes his Follower Alice. */
@Import(WallControllerIT.FakeStorageConfig::class)
class BlockIT : IntegrationTestBase() {

    private val alice = testJwt(sub = "auth0|alice", email = "alice@example.com")
    private val bob = testJwt(sub = "auth0|bob", email = "bob@example.com")
    private val carol = testJwt(sub = "auth0|carol", email = "carol@example.com")
    private var aliceId = 0
    private var bobId = 0
    private var carolId = 0
    private var routeId = 0
    private var bobsSession = 0

    @BeforeEach
    fun setup() {
        aliceId = register("Alice", "alice", alice)
        bobId = register("Bob", "bob", bob)
        carolId = register("Carol", "carol", carol)
        val gymId = extractId(postJson("/api/climbing-areas", """{"name":"BKS Laksevåg","type":"gym"}"""))
        val wallId = extractId(postJson("/api/walls", """{"areaId":$gymId,"name":"Buldring"}"""))
        routeId = extractId(postJson("/api/routes", """{"wallId":$wallId,"name":"Gul","grade":"6a"}"""))
        bobsSession = sessionOf(tick(bobId, bob))
    }

    private fun register(name: String, sub: String, jwt: RequestPostProcessor) =
        extractId(postJson("/api/users/me", """{"displayName":"$name","email":"$sub@example.com"}""", jwt))

    private fun tick(userId: Int, jwt: RequestPostProcessor): Int =
        extractId(postJson("/api/users/$userId/ticks", """{"routeId":$routeId}""", jwt))

    private fun sessionOf(tickId: Int): Int =
        jdbcTemplate.queryForObject("SELECT activity_id FROM user_route_ticks WHERE id = ?", Int::class.java, tickId)!!

    private fun follow(jwt: RequestPostProcessor, userId: Int) =
        mockMvc.perform(put("/api/users/me/following/$userId").with(jwt))

    private fun removeFollower(jwt: RequestPostProcessor, userId: Int) =
        mockMvc.perform(delete("/api/users/me/followers/$userId").with(jwt)).andExpect(status().isNoContent)

    private fun followRows(): List<Map<String, Any>> =
        jdbcTemplate.queryForList("SELECT follower_id, followee_id, status FROM follows ORDER BY follower_id, followee_id")

    private fun kudosRows(): Int = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kudos", Int::class.java)!!

    @Test
    fun `removing a Follower hides your Sessions from them and deletes their Kudos`() {
        follow(alice, bobId).andExpect(status().isNoContent)
        follow(carol, bobId).andExpect(status().isNoContent)
        mockMvc.perform(put("/api/activities/$bobsSession/kudos").with(alice)).andExpect(status().isNoContent)
        mockMvc.perform(put("/api/activities/$bobsSession/kudos").with(carol)).andExpect(status().isNoContent)

        repeat(2) { removeFollower(bob, aliceId) }

        mockMvc.perform(get("/api/feed").with(alice)).andExpect(jsonPath("$.total").value(0))
        mockMvc.perform(get("/api/activities/$bobsSession").with(alice)).andExpect(status().isNotFound)
        mockMvc.perform(put("/api/activities/$bobsSession/kudos").with(alice)).andExpect(status().isNotFound)
        mockMvc.perform(get("/api/activities/$bobsSession/kudos").with(bob))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.data[0].id").value(carolId))
        assertEquals(1, kudosRows())

        // Alice is gone from Bob's Followers, and Bob from Alice's following list.
        mockMvc.perform(get("/api/users/me/followers").with(bob))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.data[0].id").value(carolId))
        mockMvc.perform(get("/api/users/me/following").with(alice)).andExpect(jsonPath("$.total").value(0))
    }

    @Test
    fun `a blocked climber can't follow or find the blocker`() {
        follow(alice, bobId).andExpect(status().isNoContent)
        removeFollower(bob, aliceId)

        follow(alice, bobId).andExpect(status().isNotFound)
        mockMvc.perform(get("/api/users/$bobId").with(alice)).andExpect(status().isNotFound)
        mockMvc.perform(get("/api/users").param("q", "Bob").with(alice))
            .andExpect(jsonPath("$.total").value(0))
        mockMvc.perform(get("/api/users").with(alice))
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.data[?(@.id == $bobId)]").isEmpty)

        // Others still find Bob.
        mockMvc.perform(get("/api/users/$bobId").with(carol)).andExpect(status().isOk)
        mockMvc.perform(get("/api/users").param("q", "Bob").with(carol)).andExpect(jsonPath("$.total").value(1))
    }

    @Test
    fun `unfollowing doesn't clear a block`() {
        follow(alice, bobId).andExpect(status().isNoContent)
        removeFollower(bob, aliceId)

        mockMvc.perform(delete("/api/users/me/following/$bobId").with(alice)).andExpect(status().isNoContent)

        assertEquals(listOf(mapOf("follower_id" to aliceId, "followee_id" to bobId, "status" to "blocked")), followRows())
        follow(alice, bobId).andExpect(status().isNotFound)
    }

    @Test
    fun `removing a non-Follower changes nothing`() {
        follow(bob, aliceId).andExpect(status().isNoContent)
        val before = followRows()

        removeFollower(bob, aliceId)
        removeFollower(bob, 9999)

        assertEquals(before, followRows())
        follow(alice, bobId).andExpect(status().isNoContent)
    }

    @Test
    fun `the blocker can still follow and see the blocked climber`() {
        val alicesSession = sessionOf(tick(aliceId, alice))
        follow(alice, bobId).andExpect(status().isNoContent)
        removeFollower(bob, aliceId)

        follow(bob, aliceId).andExpect(status().isNoContent)

        mockMvc.perform(get("/api/users/$aliceId").with(bob)).andExpect(status().isOk)
        mockMvc.perform(get("/api/users").param("q", "Alice").with(bob)).andExpect(jsonPath("$.total").value(1))
        mockMvc.perform(get("/api/activities/$alicesSession").with(bob)).andExpect(status().isOk)
        mockMvc.perform(put("/api/activities/$alicesSession/kudos").with(bob)).andExpect(status().isNoContent)
        // Alice still can't see Bob's Sessions.
        mockMvc.perform(get("/api/activities/$bobsSession").with(alice)).andExpect(status().isNotFound)
    }

    @Test
    fun `unregistered caller gets 404`() {
        mockMvc.perform(delete("/api/users/me/followers/$aliceId").with(testJwt(sub = "auth0|stranger")))
            .andExpect(status().isNotFound)
    }
}
