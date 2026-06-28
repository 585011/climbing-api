package com.example.climbingapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
abstract class IntegrationTestBase {

    companion object {
        @JvmStatic
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16")
            .also { it.start() }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") { "https://test.auth0.local/" }
            registry.add("auth0.audience") { "test-audience" }
            // Dummy R2 config so the S3 beans build; WallControllerIT swaps in a fake StorageService.
            registry.add("storage.r2.endpoint") { "http://localhost:9999" }
            registry.add("storage.r2.access-key-id") { "test" }
            registry.add("storage.r2.secret-access-key") { "test" }
            registry.add("storage.r2.bucket") { "test-bucket" }
        }
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jdbcTemplate: JdbcTemplate
    @Autowired lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun resetDatabase() {
        jdbcTemplate.execute(
            "TRUNCATE user_route_ticks, routes, walls, climbing_areas, users RESTART IDENTITY CASCADE"
        )
    }

    protected fun testJwt(
        sub: String = "google-oauth2|test-user-123",
        email: String = "test@example.com"
    ): RequestPostProcessor = jwt().jwt { builder ->
        builder.subject(sub)
        builder.claim("email", email)
    }

    protected fun adminJwt(
        sub: String = "google-oauth2|test-user-123",
        email: String = "test@example.com"
    ): RequestPostProcessor = jwt().jwt { builder ->
        builder.subject(sub)
        builder.claim("email", email)
        builder.claim("https://climbing-api/roles", listOf("admin"))
    }.authorities(SimpleGrantedAuthority("ROLE_admin"))

    protected fun postJson(url: String, body: String, jwtPp: RequestPostProcessor = adminJwt()): String =
        mockMvc.perform(
            post(url).with(jwtPp).contentType(MediaType.APPLICATION_JSON).content(body)
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

    protected fun extractId(json: String): Int =
        objectMapper.readTree(json).get("id").asInt()
}
