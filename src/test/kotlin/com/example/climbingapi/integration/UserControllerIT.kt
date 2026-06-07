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
            .andExpect(jsonPath("$.data[0].email").value("test@example.com"))
    }

    @Test
    fun `GET user by id returns 200`() {
        postJson(meUrl, validUser)

        mockMvc.perform(get("$baseUrl/1").with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("test@example.com"))
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
