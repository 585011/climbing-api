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

class ClimbingAreaControllerIT : IntegrationTestBase() {

    private val baseUrl = "/api/climbing-areas"
    private val validArea = """{"name":"Flatanger","region":"Trøndelag"}"""

    @Test
    fun `GET all areas without JWT returns 401`() {
        mockMvc.perform(get(baseUrl))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET all areas returns empty paged response`() {
        mockMvc.perform(get(baseUrl).with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.total").value(0))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.pageSize").value(20))
    }

    @Test
    fun `GET all areas returns inserted area`() {
        postJson(baseUrl, validArea)

        mockMvc.perform(get(baseUrl).with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.data[0].name").value("Flatanger"))
    }

    @Test
    fun `GET areas returns routeCount aggregated across walls`() {
        postJson(baseUrl, validArea)
        postJson(baseUrl, """{"name":"Other Area"}""")
        postJson("/api/walls", """{"areaId":1,"name":"Wall A"}""")
        postJson("/api/walls", """{"areaId":1,"name":"Wall B"}""")
        postJson("/api/routes", """{"wallId":1,"name":"Route 1","grade":"6a"}""")
        postJson("/api/routes", """{"wallId":1,"name":"Route 2","grade":"6b"}""")
        postJson("/api/routes", """{"wallId":2,"name":"Route 3","grade":"7a"}""")

        mockMvc.perform(get(baseUrl).with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].routeCount").value(3))
            .andExpect(jsonPath("$.data[1].routeCount").value(0))

        mockMvc.perform(get("$baseUrl/1").with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.routeCount").value(3))
    }

    @Test
    fun `POST area returns 201 with Location header`() {
        mockMvc.perform(post(baseUrl).with(adminJwt()).contentType(MediaType.APPLICATION_JSON).content(validArea))
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/climbing-areas/")))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Flatanger"))
    }

    @Test
    fun `POST area without admin role returns 403`() {
        mockMvc.perform(post(baseUrl).with(testJwt()).contentType(MediaType.APPLICATION_JSON).content(validArea))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"))
    }

    @Test
    fun `POST area with blank name returns 400`() {
        mockMvc.perform(post(baseUrl).with(adminJwt()).contentType(MediaType.APPLICATION_JSON).content("""{"name":""}"""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    @Test
    fun `GET area by id returns 200`() {
        postJson(baseUrl, validArea)

        mockMvc.perform(get("$baseUrl/1").with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Flatanger"))
    }

    @Test
    fun `GET area by unknown id returns 404`() {
        mockMvc.perform(get("$baseUrl/999").with(testJwt()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
    }

    @Test
    fun `PUT area returns updated area`() {
        postJson(baseUrl, validArea)

        mockMvc.perform(
            put("$baseUrl/1").with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Siurana","region":"Catalonia"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Siurana"))
            .andExpect(jsonPath("$.region").value("Catalonia"))
    }

    @Test
    fun `PUT area with unknown id returns 404`() {
        mockMvc.perform(
            put("$baseUrl/999").with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Siurana"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PUT area with blank name returns 400`() {
        postJson(baseUrl, validArea)

        mockMvc.perform(
            put("$baseUrl/1").with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":""}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    @Test
    fun `DELETE area returns 204`() {
        postJson(baseUrl, validArea)

        mockMvc.perform(delete("$baseUrl/1").with(adminJwt()))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE area with unknown id returns 404`() {
        mockMvc.perform(delete("$baseUrl/999").with(adminJwt()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE area cascades to walls`() {
        postJson(baseUrl, validArea)
        postJson("/api/walls", """{"areaId":1,"name":"Main Wall"}""")

        mockMvc.perform(delete("$baseUrl/1").with(adminJwt())).andExpect(status().isNoContent)

        mockMvc.perform(get("/api/walls/1").with(testJwt()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET area walls returns only walls for that area`() {
        postJson(baseUrl, validArea)
        postJson(baseUrl, """{"name":"Other Area"}""")
        postJson("/api/walls", """{"areaId":1,"name":"Wall A"}""")
        postJson("/api/walls", """{"areaId":2,"name":"Wall B"}""")

        mockMvc.perform(get("$baseUrl/1/walls").with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Wall A"))
    }

    @Test
    fun `GET walls for unknown area returns 404`() {
        mockMvc.perform(get("$baseUrl/999/walls").with(testJwt()))
            .andExpect(status().isNotFound)
    }
}
