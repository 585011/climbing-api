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
    fun `GET routes without JWT returns 401`() {
        mockMvc.perform(get(baseUrl))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST route returns 201 with Location header`() {
        mockMvc.perform(
            post(baseUrl).with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"wallId":$wallId,"name":"Slab Route","grade":"6a"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/routes/")))
            .andExpect(jsonPath("$.name").value("Slab Route"))
            .andExpect(jsonPath("$.grade").value("6a"))
    }

    @Test
    fun `POST route without admin role returns 403`() {
        mockMvc.perform(
            post(baseUrl).with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"wallId":$wallId,"name":"Slab Route"}""")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"))
    }

    @Test
    fun `POST route with wallId zero returns 400`() {
        mockMvc.perform(
            post(baseUrl).with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"wallId":0}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    @Test
    fun `POST route with non-existent wallId returns 404`() {
        mockMvc.perform(
            post(baseUrl).with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"wallId":9999,"name":"Ghost Route"}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
    }

    @Test
    fun `GET all routes returns paged response`() {
        postJson(baseUrl, """{"wallId":$wallId,"name":"Slab Route"}""")

        mockMvc.perform(get(baseUrl).with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.data[0].name").value("Slab Route"))
    }

    @Test
    fun `GET route by id returns 200`() {
        postJson(baseUrl, """{"wallId":$wallId,"name":"Slab Route"}""")

        mockMvc.perform(get("$baseUrl/1").with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Slab Route"))
    }

    @Test
    fun `GET route by unknown id returns 404`() {
        mockMvc.perform(get("$baseUrl/999").with(testJwt()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
    }

    @Test
    fun `PUT route returns updated route`() {
        postJson(baseUrl, """{"wallId":$wallId,"name":"Slab Route","grade":"6a"}""")

        mockMvc.perform(
            put("$baseUrl/1").with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"wallId":$wallId,"name":"Overhang","grade":"7b"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Overhang"))
            .andExpect(jsonPath("$.grade").value("7b"))
    }

    @Test
    fun `PUT route with unknown id returns 404`() {
        mockMvc.perform(
            put("$baseUrl/999").with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"wallId":$wallId,"name":"Overhang"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PUT route with non-existent wallId returns 404`() {
        postJson(baseUrl, """{"wallId":$wallId,"name":"Slab Route"}""")

        mockMvc.perform(
            put("$baseUrl/1").with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"wallId":9999,"name":"Overhang"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE route returns 204`() {
        postJson(baseUrl, """{"wallId":$wallId,"name":"Slab Route"}""")

        mockMvc.perform(delete("$baseUrl/1").with(adminJwt()))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE route with unknown id returns 404`() {
        mockMvc.perform(delete("$baseUrl/999").with(adminJwt()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET routes with negative page returns 400`() {
        mockMvc.perform(get("$baseUrl?page=-1").with(testJwt()))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    @Test
    fun `GET routes with size above 100 returns 400`() {
        mockMvc.perform(get("$baseUrl?size=101").with(testJwt()))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    private fun gymWall(): Int {
        val gymId = extractId(postJson("/api/climbing-areas", """{"name":"Vestveggen","type":"gym"}"""))
        return extractId(postJson("/api/walls", """{"areaId":$gymId,"name":"12.5m"}"""))
    }

    private fun retire(id: Int, retired: Boolean, jwtPp: org.springframework.test.web.servlet.request.RequestPostProcessor) =
        mockMvc.perform(
            put("$baseUrl/$id/retired").with(jwtPp).contentType(MediaType.APPLICATION_JSON)
                .content("""{"retired":$retired}""")
        )

    @Test
    fun `POST route lowercases style and returns holdColor`() {
        mockMvc.perform(
            post(baseUrl).with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"wallId":$wallId,"grade":"6a","style":"Boulder","holdColor":"Gul"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.style").value("boulder"))
            .andExpect(jsonPath("$.holdColor").value("Gul"))
            .andExpect(jsonPath("$.retiredAt").doesNotExist())
    }

    @Test
    fun `POST route with unknown style returns 400`() {
        mockMvc.perform(
            post(baseUrl).with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"wallId":$wallId,"style":"alpine"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    @Test
    fun `non-admin can retire a gym route`() {
        val routeId = extractId(postJson(baseUrl, """{"wallId":${gymWall()},"grade":"6a"}"""))

        retire(routeId, true, testJwt())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.retiredAt").exists())
    }

    @Test
    fun `non-admin cannot retire a crag route`() {
        val routeId = extractId(postJson(baseUrl, """{"wallId":$wallId,"grade":"6a"}"""))

        retire(routeId, true, testJwt())
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"))
    }

    @Test
    fun `non-admin cannot un-retire but admin can`() {
        val routeId = extractId(postJson(baseUrl, """{"wallId":${gymWall()},"grade":"6a"}"""))
        retire(routeId, true, testJwt()).andExpect(status().isOk)

        retire(routeId, false, testJwt()).andExpect(status().isForbidden)
        retire(routeId, false, adminJwt())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.retiredAt").doesNotExist())
    }

    @Test
    fun `retire unknown route returns 404`() {
        retire(9999, true, adminJwt()).andExpect(status().isNotFound)
    }

    @Test
    fun `retire without JWT returns 401`() {
        mockMvc.perform(
            put("$baseUrl/1/retired").contentType(MediaType.APPLICATION_JSON).content("""{"retired":true}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `non-admin PUT route is still 403`() {
        val routeId = extractId(postJson(baseUrl, """{"wallId":${gymWall()},"grade":"6a"}"""))
        mockMvc.perform(
            put("$baseUrl/$routeId").with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"wallId":$wallId,"grade":"7a"}""")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `retired routes are hidden from lists unless includeRetired`() {
        val gymWallId = gymWall()
        postJson(baseUrl, """{"wallId":$gymWallId,"grade":"6a"}""")
        val retiredId = extractId(postJson(baseUrl, """{"wallId":$gymWallId,"grade":"6b"}"""))
        retire(retiredId, true, testJwt()).andExpect(status().isOk)

        mockMvc.perform(get("/api/walls/$gymWallId/routes").with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
        mockMvc.perform(get("/api/walls/$gymWallId/routes?includeRetired=true").with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
        mockMvc.perform(get(baseUrl).with(testJwt()))
            .andExpect(jsonPath("$.total").value(1))
        mockMvc.perform(get("$baseUrl?includeRetired=true").with(testJwt()))
            .andExpect(jsonPath("$.total").value(2))
        mockMvc.perform(get("$baseUrl/$retiredId").with(testJwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.retiredAt").exists())
    }
}
