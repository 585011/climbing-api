package com.example.climbingapi.integration

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UserControllerIT : IntegrationTestBase() {

    private val baseUrl = "/api/users"
    private val meUrl = "$baseUrl/me"
    private val validUser = """{"displayName":"Alex Honnold","email":"test@example.com"}"""

    @Test
    fun `POST me returns 201 with Location header`() {
        mockMvc.perform(post(meUrl).with(testJwt()).contentType(MediaType.APPLICATION_JSON).content(validUser))
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/users/")))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.displayName").value("Alex Honnold"))
            .andExpect(jsonPath("$.auth0Id").value("google-oauth2|test-user-123"))
    }

    @Test
    fun `POST me without JWT returns 401`() {
        mockMvc.perform(post(meUrl).contentType(MediaType.APPLICATION_JSON).content(validUser))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST me with blank displayName returns 400`() {
        mockMvc.perform(
            post(meUrl).with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"","email":"test@example.com"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    @Test
    fun `POST me with same auth0Id twice returns 409`() {
        postJson(meUrl, validUser)

        mockMvc.perform(post(meUrl).with(testJwt()).contentType(MediaType.APPLICATION_JSON).content(validUser))
            .andExpect(status().isConflict)
    }

    @Test
    fun `GET me returns current user`() {
        postJson(meUrl, validUser)

        mockMvc.perform(get(meUrl).with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.auth0Id").value("google-oauth2|test-user-123"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.shareActivity").value(true))
    }

    @Test
    fun `PUT me sharing toggles the Sharing setting and returns the full profile`() {
        postJson(meUrl, validUser)

        for (on in listOf(false, true)) {
            mockMvc.perform(
                put("$meUrl/sharing").with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                    .content("""{"shareActivity":$on}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.shareActivity").value(on))
                .andExpect(jsonPath("$.email").value("test@example.com"))
            mockMvc.perform(get(meUrl).with(testJwt())).andExpect(jsonPath("$.shareActivity").value(on))
        }
    }

    @Test
    fun `PUT me sharing without shareActivity returns 400`() {
        postJson(meUrl, validUser)

        mockMvc.perform(put("$meUrl/sharing").with(testJwt()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    @Test
    fun `PUT me sharing when not registered returns 404`() {
        mockMvc.perform(
            put("$meUrl/sharing").with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"shareActivity":false}""")
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `GET me returns 404 when user not registered`() {
        mockMvc.perform(get(meUrl).with(testJwt()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET all users returns paged response`() {
        postJson(meUrl, validUser)

        mockMvc.perform(get(baseUrl).with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].displayName").value("Alex Honnold"))
            .andExpect(jsonPath("$.data[0].email").doesNotExist())
            .andExpect(jsonPath("$.data[0].auth0Id").doesNotExist())
            .andExpect(jsonPath("$.data[0].createdAt").doesNotExist())
    }

    @Test
    fun `GET user by id returns 200`() {
        postJson(meUrl, validUser)

        mockMvc.perform(get("$baseUrl/1").with(testJwt(sub = "google-oauth2|other-999")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.displayName").value("Alex Honnold"))
            .andExpect(jsonPath("$.email").doesNotExist())
            .andExpect(jsonPath("$.auth0Id").doesNotExist())
    }

    @Test
    fun `GET own user by id also returns the public profile only`() {
        postJson(meUrl, validUser)

        mockMvc.perform(get("$baseUrl/1").with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").doesNotExist())
            .andExpect(jsonPath("$.auth0Id").doesNotExist())
    }

    private fun registerUser(displayName: String, n: Int) = postJson(
        meUrl, """{"displayName":"$displayName","email":"user$n@example.com"}""",
        testJwt(sub = "google-oauth2|user-$n", email = "user$n@example.com")
    )

    @Test
    fun `GET users with q searches display names case-insensitively`() {
        registerUser("Martin", 1)
        registerUser("Marte", 2)
        registerUser("Alex", 3)

        for (q in listOf("mar", "MAR", "  mar  ")) {
            mockMvc.perform(get(baseUrl).param("q", q).with(testJwt()))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.data[*].displayName").value(org.hamcrest.Matchers.containsInAnyOrder("Martin", "Marte")))
                .andExpect(jsonPath("$.data[0].email").doesNotExist())
        }
    }

    @Test
    fun `GET users with q matches LIKE wildcards literally`() {
        registerUser("a_b", 1)
        registerUser("axb", 2)

        mockMvc.perform(get(baseUrl).param("q", "a_").with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.data[0].displayName").value("a_b"))

        mockMvc.perform(get(baseUrl).param("q", "%%").with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(0))
    }

    @Test
    fun `GET users with q shorter than 2 characters returns 400`() {
        for (q in listOf("a", " a ", "")) {
            mockMvc.perform(get(baseUrl).param("q", q).with(testJwt()))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
        }
    }

    @Test
    fun `GET user by unknown id returns 404`() {
        mockMvc.perform(get("$baseUrl/999").with(testJwt()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
    }

    @Test
    fun `PUT user returns updated user`() {
        postJson(meUrl, validUser)

        mockMvc.perform(
            put("$baseUrl/1").with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"tommy@example.com","displayName":"Tommy Caldwell"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("tommy@example.com"))
            .andExpect(jsonPath("$.displayName").value("Tommy Caldwell"))
    }

    @Test
    fun `PUT user with different user returns 403`() {
        postJson(meUrl, validUser)

        mockMvc.perform(
            put("$baseUrl/1").with(testJwt(sub = "google-oauth2|other-999")).contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"hacker@example.com","displayName":"Hacker"}""")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `PUT user with unknown id returns 404`() {
        mockMvc.perform(
            put("$baseUrl/999").with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"tommy@example.com","displayName":"Tommy"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE user returns 204`() {
        postJson(meUrl, validUser)

        mockMvc.perform(delete("$baseUrl/1").with(testJwt()))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE user with different user returns 403`() {
        postJson(meUrl, validUser)

        mockMvc.perform(delete("$baseUrl/1").with(testJwt(sub = "google-oauth2|other-999")))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `DELETE user with unknown id returns 404`() {
        mockMvc.perform(delete("$baseUrl/999").with(testJwt()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE user with ticks cascades and returns 204`() {
        val userId = extractId(postJson(meUrl, validUser))
        val areaId = extractId(postJson("/api/climbing-areas", """{"name":"Area"}"""))
        val wallId = extractId(postJson("/api/walls", """{"areaId":$areaId,"name":"Wall"}"""))
        val routeId = extractId(postJson("/api/routes", """{"wallId":$wallId,"name":"Route"}"""))
        postJson("/api/users/$userId/ticks", """{"routeId":$routeId}""")

        mockMvc.perform(delete("$baseUrl/$userId").with(testJwt()))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `GET user ticks returns paged response`() {
        val userId = extractId(postJson(meUrl, validUser))
        val areaId = extractId(postJson("/api/climbing-areas", """{"name":"Area"}"""))
        val wallId = extractId(postJson("/api/walls", """{"areaId":$areaId,"name":"Wall"}"""))
        val routeId = extractId(postJson("/api/routes", """{"wallId":$wallId,"name":"Route"}"""))
        postJson("/api/users/$userId/ticks", """{"routeId":$routeId}""")

        mockMvc.perform(get("$baseUrl/$userId/ticks").with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
    }

    @Test
    fun `GET user ticks with different user returns 403`() {
        val userId = extractId(postJson(meUrl, validUser))
        val areaId = extractId(postJson("/api/climbing-areas", """{"name":"Area"}"""))
        val wallId = extractId(postJson("/api/walls", """{"areaId":$areaId,"name":"Wall"}"""))
        val routeId = extractId(postJson("/api/routes", """{"wallId":$wallId,"name":"Route"}"""))
        postJson("/api/users/$userId/ticks", """{"routeId":$routeId}""")

        mockMvc.perform(get("$baseUrl/$userId/ticks").with(testJwt(sub = "google-oauth2|other-999")))
            .andExpect(status().isForbidden)
    }
}
