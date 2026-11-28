package com.example.climbingapi.integration

import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Import(WallControllerIT.FakeStorageConfig::class)
class KudosControllerIT : IntegrationTestBase() {

    private val alice = testJwt(sub = "auth0|alice", email = "alice@example.com")
    private val bob = testJwt(sub = "auth0|bob", email = "bob@example.com")
    private val carol = testJwt(sub = "auth0|carol", email = "carol@example.com")
    private var aliceId = 0
    private var bobId = 0
    private var carolId = 0
    private var routeId = 0
    private var bobsSession = 0
    private var bobsTick = 0

    @BeforeEach
    fun setup() {
        aliceId = register("Alice", "alice", alice)
        bobId = register("Bob", "bob", bob)
        carolId = register("Carol", "carol", carol)
        val gymId = extractId(postJson("/api/climbing-areas", """{"name":"BKS Laksevåg","type":"gym"}"""))
        val wallId = extractId(postJson("/api/walls", """{"areaId":$gymId,"name":"Buldring"}"""))
        routeId = extractId(postJson("/api/routes", """{"wallId":$wallId,"name":"Gul","grade":"6a"}"""))
        bobsTick = extractId(postJson("/api/users/$bobId/ticks", """{"routeId":$routeId}""", bob))
        bobsSession = jdbcTemplate.queryForObject(
            "SELECT activity_id FROM user_route_ticks WHERE id = ?", Int::class.java, bobsTick
        )!!
        follow(alice, bobId)
        follow(carol, bobId)
    }

    private fun register(name: String, sub: String, jwt: RequestPostProcessor) =
        extractId(postJson("/api/users/me", """{"displayName":"$name","email":"$sub@example.com"}""", jwt))

    private fun follow(jwt: RequestPostProcessor, userId: Int) {
        mockMvc.perform(put("/api/users/me/following/$userId").with(jwt)).andExpect(status().isNoContent)
    }

    private fun kudosUrl(sessionId: Int = bobsSession) = "/api/activities/$sessionId/kudos"

    private fun kudosRows(): Int = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kudos", Int::class.java)!!

    private fun setShared(shared: Boolean) {
        mockMvc.perform(
            put("/api/activities/$bobsSession/shared").with(bob).contentType(MediaType.APPLICATION_JSON)
                .content("""{"shared":$shared}""")
        ).andExpect(status().isOk)
    }

    @Test
    fun `giving Kudos twice counts once`() {
        repeat(2) { mockMvc.perform(put(kudosUrl()).with(alice)).andExpect(status().isNoContent) }

        assertEquals(1, kudosRows())
        mockMvc.perform(get("/api/activities/$bobsSession").with(alice))
            .andExpect(jsonPath("$.kudosCount").value(1))
            .andExpect(jsonPath("$.hasGivenKudos").value(true))
        mockMvc.perform(get("/api/activities/$bobsSession").with(carol))
            .andExpect(jsonPath("$.kudosCount").value(1))
            .andExpect(jsonPath("$.hasGivenKudos").value(false))
    }

    @Test
    fun `the Feed carries kudosCount and hasGivenKudos`() {
        mockMvc.perform(put(kudosUrl()).with(alice)).andExpect(status().isNoContent)
        mockMvc.perform(put(kudosUrl()).with(carol)).andExpect(status().isNoContent)

        mockMvc.perform(get("/api/feed").with(alice))
            .andExpect(jsonPath("$.data[0].id").value(bobsSession))
            .andExpect(jsonPath("$.data[0].kudosCount").value(2))
            .andExpect(jsonPath("$.data[0].hasGivenKudos").value(true))
        mockMvc.perform(get("/api/feed").with(bob))
            .andExpect(jsonPath("$.data[0].kudosCount").value(2))
            .andExpect(jsonPath("$.data[0].hasGivenKudos").value(false))
    }

    @Test
    fun `Kudos on your own Session returns 400`() {
        mockMvc.perform(put(kudosUrl()).with(bob))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
        assertEquals(0, kudosRows())
    }

    @Test
    fun `Kudos on a Session you can't see returns 404`() {
        val stranger = testJwt(sub = "auth0|dave", email = "dave@example.com")
        register("Dave", "dave", stranger)

        mockMvc.perform(put(kudosUrl()).with(stranger)).andExpect(status().isNotFound)
        mockMvc.perform(delete(kudosUrl()).with(stranger)).andExpect(status().isNotFound)
        mockMvc.perform(get(kudosUrl()).with(stranger)).andExpect(status().isNotFound)
        mockMvc.perform(put(kudosUrl(9999)).with(alice)).andExpect(status().isNotFound)
        assertEquals(0, kudosRows())
    }

    @Test
    fun `taking back Kudos is idempotent`() {
        mockMvc.perform(put(kudosUrl()).with(alice)).andExpect(status().isNoContent)

        repeat(2) { mockMvc.perform(delete(kudosUrl()).with(alice)).andExpect(status().isNoContent) }

        assertEquals(0, kudosRows())
    }

    @Test
    fun `GET kudos lists the givers newest first without private fields`() {
        mockMvc.perform(put(kudosUrl()).with(alice)).andExpect(status().isNoContent)
        mockMvc.perform(put(kudosUrl()).with(carol)).andExpect(status().isNoContent)

        mockMvc.perform(get(kudosUrl()).with(bob))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.data[*].displayName").value(contains("Carol", "Alice")))
            .andExpect(jsonPath("$.data[0].email").doesNotExist())
            .andExpect(jsonPath("$.data[0].auth0Id").doesNotExist())
    }

    @Test
    fun `hiding a Session keeps its Kudos and sharing it again shows them`() {
        mockMvc.perform(put(kudosUrl()).with(alice)).andExpect(status().isNoContent)

        setShared(false)
        assertEquals(1, kudosRows())
        mockMvc.perform(get("/api/activities/$bobsSession").with(alice)).andExpect(status().isNotFound)
        mockMvc.perform(get("/api/activities/$bobsSession").with(bob)).andExpect(jsonPath("$.kudosCount").value(1))

        setShared(true)
        mockMvc.perform(get("/api/activities/$bobsSession").with(alice))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.kudosCount").value(1))
            .andExpect(jsonPath("$.hasGivenKudos").value(true))
    }

    @Test
    fun `deleting the Session's last Tick deletes its Kudos`() {
        mockMvc.perform(put(kudosUrl()).with(alice)).andExpect(status().isNoContent)

        mockMvc.perform(delete("/api/users/$bobId/ticks/$bobsTick").with(bob)).andExpect(status().isNoContent)

        assertEquals(0, kudosRows())
        mockMvc.perform(get(kudosUrl()).with(alice)).andExpect(status().isNotFound)
    }

    @Test
    fun `deleting the giver deletes their Kudos`() {
        mockMvc.perform(put(kudosUrl()).with(carol)).andExpect(status().isNoContent)

        mockMvc.perform(delete("/api/users/$carolId").with(carol)).andExpect(status().isNoContent)

        assertEquals(0, kudosRows())
    }
}
