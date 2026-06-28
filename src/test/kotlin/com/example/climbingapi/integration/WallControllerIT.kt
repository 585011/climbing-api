package com.example.climbingapi.integration

import com.example.climbingapi.service.StorageService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@Import(WallControllerIT.FakeStorageConfig::class)
class WallControllerIT : IntegrationTestBase() {

    /** In-memory fake so the IT exercises the controller + DB without hitting R2. */
    @TestConfiguration
    class FakeStorageConfig {
        @Bean
        @Primary
        fun fakeStorageService(): StorageService = object : StorageService {
            override fun upload(bytes: ByteArray, contentType: String): String {
                val ext = when (contentType) {
                    "image/jpeg" -> "jpg"
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> throw IllegalArgumentException("Unsupported image type: $contentType")
                }
                return "walls/${UUID.randomUUID()}.$ext"
            }

            override fun delete(key: String) { /* no-op */ }

            override fun presignGet(key: String): String = "https://fake-r2.local/$key?signed=true"
        }
    }

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
            post(baseUrl).with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"areaId":$areaId,"name":"North Face"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/walls/")))
            .andExpect(jsonPath("$.name").value("North Face"))
            .andExpect(jsonPath("$.areaId").value(areaId))
    }

    @Test
    fun `POST wall without admin role returns 403`() {
        mockMvc.perform(
            post(baseUrl).with(testJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"areaId":$areaId,"name":"North Face"}""")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"))
    }

    @Test
    fun `POST wall with areaId zero returns 400`() {
        mockMvc.perform(
            post(baseUrl).with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"areaId":0,"name":"North Face"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    @Test
    fun `POST wall with non-existent areaId returns 409 from FK violation`() {
        mockMvc.perform(
            post(baseUrl).with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"areaId":9999,"name":"Ghost Wall"}""")
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `POST wall with blank name returns 400`() {
        mockMvc.perform(
            post(baseUrl).with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
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
            put("$baseUrl/1").with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"areaId":$areaId,"name":"South Slab"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("South Slab"))
    }

    @Test
    fun `PUT wall with unknown id returns 404`() {
        mockMvc.perform(
            put("$baseUrl/999").with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"areaId":$areaId,"name":"South Slab"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE wall returns 204`() {
        postJson(baseUrl, """{"areaId":$areaId,"name":"North Face"}""")

        mockMvc.perform(delete("$baseUrl/1").with(adminJwt()))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE wall with unknown id returns 404`() {
        mockMvc.perform(delete("$baseUrl/999").with(adminJwt()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE wall cascades to routes`() {
        postJson(baseUrl, """{"areaId":$areaId,"name":"North Face"}""")
        postJson("/api/routes", """{"wallId":1,"name":"Test Route"}""")

        mockMvc.perform(delete("$baseUrl/1").with(adminJwt())).andExpect(status().isNoContent)

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
            post(baseUrl).with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"areaId":$areaId,"name":"North Face","latitude":91.0}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    @Test
    fun `POST wall with longitude below minus 180 returns 400`() {
        mockMvc.perform(
            post(baseUrl).with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                .content("""{"areaId":$areaId,"name":"North Face","longitude":-181.0}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    private fun wallPart(name: String = "Photo Wall") =
        MockMultipartFile("wall", "", MediaType.APPLICATION_JSON_VALUE, """{"areaId":$areaId,"name":"$name"}""".toByteArray())

    private fun imagePart(filename: String = "photo.jpg", type: String = "image/jpeg", bytes: ByteArray = byteArrayOf(1, 2, 3)) =
        MockMultipartFile("image", filename, type, bytes)

    @Test
    fun `POST wall multipart with image returns 201 with imageUrl`() {
        mockMvc.perform(
            multipart(baseUrl).file(wallPart()).file(imagePart()).with(adminJwt())
        )
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/walls/")))
            .andExpect(jsonPath("$.name").value("Photo Wall"))
            .andExpect(jsonPath("$.imageUrl").isNotEmpty)
    }

    @Test
    fun `POST wall multipart without image returns 201 with null imageUrl`() {
        mockMvc.perform(
            multipart(baseUrl).file(wallPart("No Photo")).with(adminJwt())
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("No Photo"))
            .andExpect(jsonPath("$.imageUrl").doesNotExist())
    }

    @Test
    fun `POST wall multipart without admin role returns 403`() {
        mockMvc.perform(
            multipart(baseUrl).file(wallPart()).file(imagePart()).with(testJwt())
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"))
    }

    @Test
    fun `PUT wall image returns 200 with imageUrl`() {
        postJson(baseUrl, """{"areaId":$areaId,"name":"North Face"}""")

        mockMvc.perform(
            multipart(HttpMethod.PUT, "$baseUrl/1/image").file(imagePart("photo.png", "image/png")).with(adminJwt())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.imageUrl").isNotEmpty)
    }

    @Test
    fun `PUT wall image with unsupported type returns 400`() {
        postJson(baseUrl, """{"areaId":$areaId,"name":"North Face"}""")

        mockMvc.perform(
            multipart(HttpMethod.PUT, "$baseUrl/1/image").file(imagePart("note.txt", "text/plain")).with(adminJwt())
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    @Test
    fun `PUT wall image exceeding 5MB returns 413`() {
        postJson(baseUrl, """{"areaId":$areaId,"name":"North Face"}""")

        mockMvc.perform(
            multipart(HttpMethod.PUT, "$baseUrl/1/image")
                .file(imagePart("big.jpg", "image/jpeg", ByteArray(5 * 1024 * 1024 + 1))).with(adminJwt())
        )
            .andExpect(status().isPayloadTooLarge)
            .andExpect(jsonPath("$.errorCode").value("PAYLOAD_TOO_LARGE"))
    }

    @Test
    fun `PUT wall image on unknown wall returns 404`() {
        mockMvc.perform(
            multipart(HttpMethod.PUT, "$baseUrl/999/image").file(imagePart()).with(adminJwt())
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PUT wall image without admin role returns 403`() {
        postJson(baseUrl, """{"areaId":$areaId,"name":"North Face"}""")

        mockMvc.perform(
            multipart(HttpMethod.PUT, "$baseUrl/1/image").file(imagePart()).with(testJwt())
        )
            .andExpect(status().isForbidden)
    }
}
