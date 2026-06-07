package com.example.climbingapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TickControllerIT : IntegrationTestBase() {

    private var userId = 0
    private var routeId = 0

    @BeforeEach
    fun setup() {
        userId = extractId(postJson("/api/users/me", """{"displayName":"Alex"}"""))
        val areaId = extractId(postJson("/api/climbing-areas", """{"name":"Test Area"}"""))
        val wallId = extractId(postJson("/api/walls", """{"areaId":$areaId,"name":"Test Wall"}"""))
        routeId = extractId(postJson("/api/routes", """{"wallId":$wallId,"name":"Test Route"}"""))
    }

    private fun ticksUrl(uid: Int = userId) = "/api/users/$uid/ticks"

    @Test
    fun `POST tick returns 201 with Location header`() {
        mockMvc.perform(
            post(ticksUrl()).with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"routeId":$routeId,"style":"onsight","rating":4}""")
        )
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/users/$userId/ticks/")))
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.routeId").value(routeId))
            .andExpect(jsonPath("$.style").value("onsight"))
            .andExpect(jsonPath("$.rating").value(4))
    }

    @Test
    fun `POST tick without JWT returns 401`() {
        mockMvc.perform(
            post(ticksUrl()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"routeId":$routeId}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST tick with different user returns 403`() {
        mockMvc.perform(
            post(ticksUrl()).with(testJwt(sub = "google-oauth2|other-999")).contentType(MediaType.APPLICATION_JSON)
                .content("""{"routeId":$routeId}""")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `POST tick with routeId zero returns 400`() {
        mockMvc.perform(
            post(ticksUrl()).with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"routeId":0}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    @Test
    fun `POST tick with non-existent routeId returns 404`() {
        mockMvc.perform(
            post(ticksUrl()).with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"routeId":9999}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
    }

    @Test
    fun `POST tick for non-existent user returns 404`() {
        mockMvc.perform(
            post(ticksUrl(9999)).with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"routeId":$routeId}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
    }

    @Test
    fun `POST duplicate tick returns 409`() {
        postJson(ticksUrl(), """{"routeId":$routeId}""")

        mockMvc.perform(
            post(ticksUrl()).with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"routeId":$routeId}""")
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `GET tick by id returns 200`() {
        postJson(ticksUrl(), """{"routeId":$routeId,"style":"flash"}""")

        mockMvc.perform(get("${ticksUrl()}/1").with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.style").value("flash"))
    }

    @Test
    fun `GET tick by unknown id returns 404`() {
        mockMvc.perform(get("${ticksUrl()}/999").with(testJwt()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
    }

    @Test
    fun `GET tick with different user returns 403`() {
        postJson(ticksUrl(), """{"routeId":$routeId}""")

        mockMvc.perform(get("${ticksUrl()}/1").with(testJwt(sub = "google-oauth2|other-999")))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET ticks for unknown user returns 404`() {
        mockMvc.perform(get("${ticksUrl(9999)}/1").with(testJwt()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PUT tick returns updated tick`() {
        postJson(ticksUrl(), """{"routeId":$routeId,"style":"onsight","rating":3}""")

        mockMvc.perform(
            put("${ticksUrl()}/1").with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"style":"redpoint","rating":5,"personalNote":"Finally!"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.style").value("redpoint"))
            .andExpect(jsonPath("$.rating").value(5))
            .andExpect(jsonPath("$.personalNote").value("Finally!"))
    }

    @Test
    fun `PUT tick with rating above max returns 400`() {
        postJson(ticksUrl(), """{"routeId":$routeId}""")

        mockMvc.perform(
            put("${ticksUrl()}/1").with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"rating":6}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    @Test
    fun `DELETE tick returns 204`() {
        postJson(ticksUrl(), """{"routeId":$routeId}""")

        mockMvc.perform(delete("${ticksUrl()}/1").with(testJwt()))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE tick with unknown id returns 404`() {
        mockMvc.perform(delete("${ticksUrl()}/999").with(testJwt()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET user ticks returns paged response`() {
        postJson(ticksUrl(), """{"routeId":$routeId}""")

        mockMvc.perform(get(ticksUrl()).with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.data[0].routeId").value(routeId))
    }
}