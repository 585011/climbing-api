package com.example.climbingapi.integration

import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Import(WallControllerIT.FakeStorageConfig::class)
class FeedControllerIT : IntegrationTestBase() {

    private val alice = testJwt(sub = "auth0|alice", email = "alice@example.com")
    private val bob = testJwt(sub = "auth0|bob", email = "bob@example.com")
    private val carol = testJwt(sub = "auth0|carol", email = "carol@example.com")
    private var aliceId = 0
    private var bobId = 0
    private var carolId = 0
    private var gymId = 0
    private var cragId = 0
    private val gymRoutes = mutableListOf<Int>()
    private val cragRoutes = mutableListOf<Int>()

    @BeforeEach
    fun setup() {
        aliceId = register("Alice", "alice", alice)
        bobId = register("Bob", "bob", bob)
        carolId = register("Carol", "carol", carol)
        gymId = extractId(postJson("/api/climbing-areas", """{"name":"BKS Laksevåg","type":"gym"}"""))
        val gymWall = extractId(postJson("/api/walls", """{"areaId":$gymId,"name":"Buldring"}"""))
        cragId = extractId(postJson("/api/climbing-areas", """{"name":"Hellesylt"}"""))
        val cragWall = extractId(postJson("/api/walls", """{"areaId":$cragId,"name":"Main Wall"}"""))
        gymRoutes.clear(); cragRoutes.clear()
        repeat(3) { gymRoutes += extractId(postJson("/api/routes", """{"wallId":$gymWall,"name":"Gym $it","grade":"6a"}""")) }
        repeat(3) { cragRoutes += extractId(postJson("/api/routes", """{"wallId":$cragWall,"name":"Crag $it","grade":"7a"}""")) }
    }

    private fun register(name: String, sub: String, jwt: RequestPostProcessor) =
        extractId(postJson("/api/users/me", """{"displayName":"$name","email":"$sub@example.com"}""", jwt))

    /** Ticks [routeId] as [userId] and returns the id of the Session it landed in. */
    private fun tick(userId: Int, jwt: RequestPostProcessor, routeId: Int, extra: String = ""): Int {
        val tickId = extractId(postJson("/api/users/$userId/ticks", """{"routeId":$routeId$extra}""", jwt))
        return jdbcTemplate.queryForObject("SELECT activity_id FROM user_route_ticks WHERE id = ?", Int::class.java, tickId)!!
    }

    private fun follow(jwt: RequestPostProcessor, userId: Int) {
        mockMvc.perform(put("/api/users/me/following/$userId").with(jwt)).andExpect(status().isNoContent)
    }

    private fun setShared(jwt: RequestPostProcessor, sessionId: Int, shared: Boolean) =
        mockMvc.perform(
            put("/api/activities/$sessionId/shared").with(jwt).contentType(MediaType.APPLICATION_JSON)
                .content("""{"shared":$shared}""")
        )

    private fun setSharing(jwt: RequestPostProcessor, on: Boolean) {
        mockMvc.perform(
            put("/api/users/me/sharing").with(jwt).contentType(MediaType.APPLICATION_JSON).content("""{"shareActivity":$on}""")
        ).andExpect(status().isOk)
    }

    private fun feedIds(jwt: RequestPostProcessor) =
        objectMapper.readTree(mockMvc.perform(get("/api/feed").with(jwt)).andExpect(status().isOk).andReturn().response.contentAsString)
            .get("data").map { it.get("id").asInt() }

    @Test
    fun `you see your own shared and hidden Sessions`() {
        val shared = tick(aliceId, alice, gymRoutes[0])
        val hidden = tick(aliceId, alice, cragRoutes[0])
        setShared(alice, hidden, false).andExpect(status().isOk).andExpect(jsonPath("$.shared").value(false))

        mockMvc.perform(get("/api/feed").with(alice))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.data[*].id").value(containsInAnyOrder(shared, hidden)))
    }

    @Test
    fun `a Follower sees shared Sessions only, and none while the master switch is off`() {
        follow(alice, bobId)
        val shared = tick(bobId, bob, gymRoutes[0])
        val hidden = tick(bobId, bob, cragRoutes[0])
        setShared(bob, hidden, false).andExpect(status().isOk)

        assertFeed(alice, listOf(shared))

        setSharing(bob, false)
        assertFeed(alice, emptyList())
        mockMvc.perform(get("/api/activities/$shared").with(alice)).andExpect(status().isNotFound)

        setSharing(bob, true)
        assertFeed(alice, listOf(shared))
    }

    @Test
    fun `a Session shared while the master switch is off appears once it is turned on`() {
        follow(alice, bobId)
        setSharing(bob, false)
        val session = tick(bobId, bob, gymRoutes[0])
        setShared(bob, session, true).andExpect(status().isOk)
        assertFeed(alice, emptyList())

        setSharing(bob, true)
        assertFeed(alice, listOf(session))
    }

    @Test
    fun `you see nothing of climbers you don't follow`() {
        val carols = tick(carolId, carol, gymRoutes[0])

        assertFeed(alice, emptyList())
        mockMvc.perform(get("/api/activities/$carols").with(alice))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
        setShared(alice, carols, false).andExpect(status().isNotFound)
    }

    @Test
    fun `a hidden Session is 404 for Followers and only its climber may change it`() {
        follow(alice, bobId)
        val session = tick(bobId, bob, gymRoutes[0])
        mockMvc.perform(get("/api/activities/$session").with(alice)).andExpect(status().isOk)

        setShared(alice, session, false).andExpect(status().isForbidden)

        setShared(bob, session, false).andExpect(status().isOk)
        mockMvc.perform(get("/api/activities/$session").with(alice)).andExpect(status().isNotFound)
        setShared(alice, session, true).andExpect(status().isNotFound)
        mockMvc.perform(get("/api/activities/$session").with(bob))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.shared").value(false))
    }

    @Test
    fun `the Session JSON has the agreed shape and never a Personal Note`() {
        follow(alice, bobId)
        val session = tick(bobId, bob, gymRoutes[0], ""","style":"flash","rating":4,"personalNote":"secret beta"""")
        tick(bobId, bob, gymRoutes[1])
        jdbcTemplate.update("UPDATE user_route_ticks SET photo_key = 'ticks/p.jpg' WHERE route_id = ?", gymRoutes[0])

        for (url in listOf("/api/feed", "/api/activities/$session")) {
            mockMvc.perform(get(url).with(alice))
                .andExpect(status().isOk)
                .andExpect(content().string(not(containsString("personalNote"))))
                .andExpect(content().string(not(containsString("secret beta"))))
                .andExpect(content().string(not(containsString("email"))))
        }

        mockMvc.perform(get("/api/activities/$session").with(alice))
            .andExpect(jsonPath("$.id").value(session))
            .andExpect(jsonPath("$.type").value("session"))
            .andExpect(jsonPath("$.date").exists())
            .andExpect(jsonPath("$.lastTickAt").exists())
            .andExpect(jsonPath("$.shared").value(true))
            .andExpect(jsonPath("$.user.id").value(bobId))
            .andExpect(jsonPath("$.user.displayName").value("Bob"))
            .andExpect(jsonPath("$.area.id").value(gymId))
            .andExpect(jsonPath("$.area.name").value("BKS Laksevåg"))
            .andExpect(jsonPath("$.area.type").value("gym"))
            .andExpect(jsonPath("$.ticks[*].route.id").value(contains(gymRoutes[0], gymRoutes[1])))
            .andExpect(jsonPath("$.ticks[0].style").value("flash"))
            .andExpect(jsonPath("$.ticks[0].rating").value(4))
            .andExpect(jsonPath("$.ticks[0].photoUrl").value("https://fake-r2.local/ticks/p.jpg?signed=true"))
            .andExpect(jsonPath("$.ticks[0].route.name").value("Gym 0"))
            .andExpect(jsonPath("$.ticks[0].route.grade").value("6a"))
            .andExpect(jsonPath("$.ticks[0].route.wallName").value("Buldring"))
            .andExpect(jsonPath("$.ticks[0].route.retired").value(false))
            .andExpect(jsonPath("$.ticks[1].photoUrl").doesNotExist())
    }

    @Test
    fun `a new Tick on an older Session moves it to the top`() {
        follow(alice, bobId)
        val older = tick(bobId, bob, cragRoutes[0])
        // Make the crag Session clearly older than the gym Session.
        jdbcTemplate.update("UPDATE activities SET last_tick_at = last_tick_at - INTERVAL '1 hour' WHERE id = ?", older)
        val newer = tick(aliceId, alice, gymRoutes[0])
        assertFeed(alice, listOf(newer, older))

        tick(bobId, bob, cragRoutes[1])

        assertFeed(alice, listOf(older, newer))
    }

    @Test
    fun `the Feed is paginated`() {
        follow(alice, bobId)
        val first = tick(aliceId, alice, gymRoutes[0])
        jdbcTemplate.update("UPDATE activities SET last_tick_at = last_tick_at - INTERVAL '1 hour' WHERE id = ?", first)
        val second = tick(bobId, bob, cragRoutes[0])

        mockMvc.perform(get("/api/feed").param("size", "1").param("page", "1").with(alice))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.pageSize").value(1))
            .andExpect(jsonPath("$.data[*].id").value(contains(first)))
        assertFeed(alice, listOf(second, first))
    }

    @Test
    fun `an unregistered caller gets 404 and a missing JWT 401`() {
        mockMvc.perform(get("/api/feed").with(testJwt(sub = "auth0|stranger"))).andExpect(status().isNotFound)
        mockMvc.perform(get("/api/feed")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `PUT shared without a body field returns 400`() {
        val session = tick(aliceId, alice, gymRoutes[0])
        mockMvc.perform(put("/api/activities/$session/shared").with(alice).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest)
    }

    private fun assertFeed(jwt: RequestPostProcessor, expected: List<Int>) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, feedIds(jwt))
    }
}
