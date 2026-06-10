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

class WallControllerIT : IntegrationTestBase() {

    private val baseUrl = "/api/walls"
    private var areaId = 0

    @BeforeEach
    fun setup() {
        areaId = extractId(postJson("/api/climbing-areas", """{"name":"Test Area"}"""))
    }

    @Test
    fun `GET walls without JWT returns 401`() {
        mockMvc.perform(get(baseUrl))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST wall returns 201 with Location header`() {
        mockMvc.perform(
            post(baseUrl).with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"areaId":$areaId,"name":"North Face"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/walls/")))
            .andExpect(jsonPath("$.name").value("North Face"))
            .andExpect(jsonPath("$.areaId").value(areaId))
    }

    @Test
    fun `POST wall with areaId zero returns 400`() {
        mockMvc.perform(
            post(baseUrl).with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"areaId":0,"name":"North Face"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    @Test
    fun `POST wall with non-existent areaId returns 409 from FK violation`() {
        mockMvc.perform(
            post(baseUrl).with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"areaId":9999,"name":"Ghost Wall"}""")
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `POST wall with blank name returns 400`() {
        mockMvc.perform(
            post(baseUrl).with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"areaId":$areaId,"name":""}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    @Test
    fun `GET all walls returns paged response`() {
        postJson(baseUrl, """{"areaId":$areaId,"name":"North Face"}""")

        mockMvc.perform(get(baseUrl).with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.data[0].name").value("North Face"))
    }

    @Test
    fun `GET wall by id returns 200`() {
        postJson(baseUrl, """{"areaId":$areaId,"name":"North Face"}""")

        mockMvc.perform(get("$baseUrl/1").with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("North Face"))
    }

    @Test
    fun `GET wall by unknown id returns 404`() {
        mockMvc.perform(get("$baseUrl/999").with(testJwt()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
    }

    @Test
    fun `PUT wall returns updated wall`() {
        postJson(baseUrl, """{"areaId":$areaId,"name":"North Face"}""")

        mockMvc.perform(
            put("$baseUrl/1").with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"areaId":$areaId,"name":"South Slab"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("South Slab"))
    }

    @Test
    fun `PUT wall with unknown id returns 404`() {
        mockMvc.perform(
            put("$baseUrl/999").with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"areaId":$areaId,"name":"South Slab"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE wall returns 204`() {
        postJson(baseUrl, """{"areaId":$areaId,"name":"North Face"}""")

        mockMvc.perform(delete("$baseUrl/1").with(testJwt()))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE wall with unknown id returns 404`() {
        mockMvc.perform(delete("$baseUrl/999").with(testJwt()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE wall cascades to routes`() {
        postJson(baseUrl, """{"areaId":$areaId,"name":"North Face"}""")
        postJson("/api/routes", """{"wallId":1,"name":"Test Route"}""")

        mockMvc.perform(delete("$baseUrl/1").with(testJwt())).andExpect(status().isNoContent)

        mockMvc.perform(get("/api/routes/1").with(testJwt()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET routes for wall returns route list`() {
        postJson(baseUrl, """{"areaId":$areaId,"name":"North Face"}""")
        postJson("/api/routes", """{"wallId":1,"name":"Pitch 1"}""")
        postJson("/api/routes", """{"wallId":1,"name":"Pitch 2"}""")

        mockMvc.perform(get("$baseUrl/1/routes").with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `GET routes for unknown wall returns 404`() {
        mockMvc.perform(get("$baseUrl/999/routes").with(testJwt()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST wall with latitude above 90 returns 400`() {
        mockMvc.perform(
            post(baseUrl).with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"areaId":$areaId,"name":"North Face","latitude":91.0}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    @Test
    fun `POST wall with longitude below minus 180 returns 400`() {
        mockMvc.perform(
            post(baseUrl).with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"areaId":$areaId,"name":"North Face","longitude":-181.0}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }
}
