package com.example.climbingapi.config

import com.example.climbingapi.exception.ErrorResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.time.OffsetDateTime

@Configuration
@EnableWebSecurity
class SecurityConfig(
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}") private val issuerUri: String,
    @Value("\${auth0.audience}") private val audience: String
) {

    private val rolesClaim = "https://climbing-api/roles"

    @Bean
    fun securityFilterChain(http: HttpSecurity, accessDeniedHandler: AccessDeniedHandler): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .headers { headers ->
                headers.frameOptions { it.deny() }
                headers.contentTypeOptions { }
                headers.httpStrictTransportSecurity { hsts ->
                    hsts.includeSubDomains(true)
                    hsts.maxAgeInSeconds(31536000)
                }
                headers.cacheControl { }
                headers.xssProtection { it.disable() }
            }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.GET, "/health").permitAll()
                it.requestMatchers(HttpMethod.POST, "/api/climbing-areas/**", "/api/walls/**", "/api/routes/**").hasRole("admin")
                it.requestMatchers(HttpMethod.PUT, "/api/climbing-areas/**", "/api/walls/**", "/api/routes/**").hasRole("admin")
                it.requestMatchers(HttpMethod.DELETE, "/api/climbing-areas/**", "/api/walls/**", "/api/routes/**").hasRole("admin")
                it.anyRequest().authenticated()
            }
            .oauth2ResourceServer {
                it.jwt { jwt ->
                    jwt.decoder(jwtDecoder())
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }
            .exceptionHandling {
                it.authenticationEntryPoint(BearerTokenAuthenticationEntryPoint())
                it.accessDeniedHandler(accessDeniedHandler)
            }
        return http.build()
    }

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { jwt ->
            (jwt.getClaimAsStringList(rolesClaim) ?: emptyList())
                .map { SimpleGrantedAuthority("ROLE_$it") }
        }
        return converter
    }

    @Bean
    fun accessDeniedHandler(objectMapper: ObjectMapper): AccessDeniedHandler =
        AccessDeniedHandler { request, response, _ ->
            response.status = HttpStatus.FORBIDDEN.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            val body = ErrorResponse(
                timestamp = OffsetDateTime.now(),
                status = HttpStatus.FORBIDDEN.value(),
                error = HttpStatus.FORBIDDEN.reasonPhrase,
                errorCode = "FORBIDDEN",
                message = "Admin role required",
                path = request.requestURI
            )
            objectMapper.writeValue(response.outputStream, body)
        }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        // Derive JWK set URI from issuer URI — avoids OIDC discovery at startup (safe for tests)
        val jwksUri = issuerUri.trimEnd('/') + "/.well-known/jwks.json"
        val decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build()
        val validators = DelegatingOAuth2TokenValidator(
            JwtValidators.createDefaultWithIssuer(issuerUri),
            JwtClaimValidator<List<String>>("aud") { aud -> aud.contains(audience) }
        )
        decoder.setJwtValidator(validators)
        return decoder
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOrigins = listOf(
            "http://localhost:5173",
            "http://localhost:8000",
            System.getenv("ALLOW_ORIGINS") ?: ""
        ).filter { it.isNotBlank() }
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With")
        config.exposedHeaders = listOf("Location")
        config.allowCredentials = true
        config.maxAge = 3600L
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/api/**", config)
        return source
    }
}
