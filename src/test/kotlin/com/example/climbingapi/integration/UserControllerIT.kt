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
    private val validUser = """{"email":"alex@example.com","displayName":"Alex Honnold"}"""

    @Test
    fun `POST user returns 201 with Location header`() {
        mockMvc.perform(post(baseUrl).contentType(MediaType.APPLICATION_JSON).content(validUser))
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/users/")))
            .andExpect(jsonPath("$.email").value("alex@example.com"))
            .andExpect(jsonPath("$.displayName").value("Alex Honnold"))
    }

    @Test
    fun `POST user with blank email returns 400`() {
        mockMvc.perform(
            post(baseUrl).contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"","displayName":"Alex"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    @Test
    fun `POST user with invalid email format returns 400`() {
        mockMvc.perform(
            post(baseUrl).contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"not-an-email","displayName":"Alex"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    @Test
    fun `POST user with duplicate email returns 409`() {
        postJson(baseUrl, validUser)

        mockMvc.perform(post(baseUrl).contentType(MediaType.APPLICATION_JSON).content(validUser))
            .andExpect(status().isConflict)
    }

    @Test
    fun `GET all users returns paged response`() {
        postJson(baseUrl, validUser)

        mockMvc.perform(get(baseUrl))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.data[0].email").value("alex@example.com"))
    }

    @Test
    fun `GET user by id returns 200`() {
        postJson(baseUrl, validUser)

        mockMvc.perform(get("$baseUrl/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("alex@example.com"))
    }

    @Test
    fun `GET user by unknown id returns 404`() {
        mockMvc.perform(get("$baseUrl/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
    }

    @Test
    fun `PUT user returns updated user`() {
        postJson(baseUrl, validUser)

        mockMvc.perform(
            put("$baseUrl/1").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"tommy@example.com","displayName":"Tommy Caldwell"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("tommy@example.com"))
            .andExpect(jsonPath("$.displayName").value("Tommy Caldwell"))
    }

    @Test
    fun `PUT user with unknown id returns 404`() {
        mockMvc.perform(
            put("$baseUrl/999").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"tommy@example.com","displayName":"Tommy"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE user returns 204`() {
        postJson(baseUrl, validUser)

        mockMvc.perform(delete("$baseUrl/1"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE user with unknown id returns 404`() {
        mockMvc.perform(delete("$baseUrl/999"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE user with ticks cascades and returns 204`() {
        // V4 migration added ON DELETE CASCADE on user_route_ticks.user_id, so deleting a user
        // cascades their ticks. The Swagger annotation claiming 409 is outdated.
        val userId = extractId(postJson(baseUrl, validUser))
        val areaId = extractId(postJson("/api/climbing-areas", """{"name":"Area"}"""))
        val wallId = extractId(postJson("/api/walls", """{"areaId":$areaId,"name":"Wall"}"""))
        val routeId = extractId(postJson("/api/routes", """{"wallId":$wallId,"name":"Route"}"""))
        postJson("/api/users/$userId/ticks", """{"routeId":$routeId}""")

        mockMvc.perform(delete("$baseUrl/$userId"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `GET user ticks returns paged response`() {
        val userId = extractId(postJson(baseUrl, validUser))
        val areaId = extractId(postJson("/api/climbing-areas", """{"name":"Area"}"""))
        val wallId = extractId(postJson("/api/walls", """{"areaId":$areaId,"name":"Wall"}"""))
        val routeId = extractId(postJson("/api/routes", """{"wallId":$wallId,"name":"Route"}"""))
        postJson("/api/users/$userId/ticks", """{"routeId":$routeId}""")

        mockMvc.perform(get("$baseUrl/$userId/ticks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
    }
}
