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

class RouteControllerIT : IntegrationTestBase() {

    private val baseUrl = "/api/routes"
    private var wallId = 0

    @BeforeEach
    fun setup() {
        val areaId = extractId(postJson("/api/climbing-areas", """{"name":"Test Area"}"""))
        wallId = extractId(postJson("/api/walls", """{"areaId":$areaId,"name":"Test Wall"}"""))
    }

    @Test
    fun `POST route returns 201 with Location header`() {
        mockMvc.perform(
            post(baseUrl).contentType(MediaType.APPLICATION_JSON)
                .content("""{"wallId":$wallId,"name":"Slab Route","grade":"6a"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/routes/")))
            .andExpect(jsonPath("$.name").value("Slab Route"))
            .andExpect(jsonPath("$.grade").value("6a"))
    }

    @Test
    fun `POST route with wallId zero returns 400`() {
        mockMvc.perform(
            post(baseUrl).contentType(MediaType.APPLICATION_JSON)
                .content("""{"wallId":0}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    @Test
    fun `POST route with non-existent wallId returns 404`() {
        // RouteService explicitly validates wall existence before inserting
        mockMvc.perform(
            post(baseUrl).contentType(MediaType.APPLICATION_JSON)
                .content("""{"wallId":9999,"name":"Ghost Route"}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
    }

    @Test
    fun `GET all routes returns paged response`() {
        postJson(baseUrl, """{"wallId":$wallId,"name":"Slab Route"}""")

        mockMvc.perform(get(baseUrl))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.data[0].name").value("Slab Route"))
    }

    @Test
    fun `GET route by id returns 200`() {
        postJson(baseUrl, """{"wallId":$wallId,"name":"Slab Route"}""")

        mockMvc.perform(get("$baseUrl/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Slab Route"))
    }

    @Test
    fun `GET route by unknown id returns 404`() {
        mockMvc.perform(get("$baseUrl/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
    }

    @Test
    fun `PUT route returns updated route`() {
        postJson(baseUrl, """{"wallId":$wallId,"name":"Slab Route","grade":"6a"}""")

        mockMvc.perform(
            put("$baseUrl/1").contentType(MediaType.APPLICATION_JSON)
                .content("""{"wallId":$wallId,"name":"Overhang","grade":"7b"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Overhang"))
            .andExpect(jsonPath("$.grade").value("7b"))
    }

    @Test
    fun `PUT route with unknown id returns 404`() {
        mockMvc.perform(
            put("$baseUrl/999").contentType(MediaType.APPLICATION_JSON)
                .content("""{"wallId":$wallId,"name":"Overhang"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PUT route with non-existent wallId returns 404`() {
        postJson(baseUrl, """{"wallId":$wallId,"name":"Slab Route"}""")

        mockMvc.perform(
            put("$baseUrl/1").contentType(MediaType.APPLICATION_JSON)
                .content("""{"wallId":9999,"name":"Overhang"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE route returns 204`() {
        postJson(baseUrl, """{"wallId":$wallId,"name":"Slab Route"}""")

        mockMvc.perform(delete("$baseUrl/1"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE route with unknown id returns 404`() {
        mockMvc.perform(delete("$baseUrl/999"))
            .andExpect(status().isNotFound)
    }
}
