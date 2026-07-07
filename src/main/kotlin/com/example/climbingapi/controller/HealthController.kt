package com.example.climbingapi.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Unauthenticated liveness probe for the platform (Railway healthcheck).
 * Permitted in SecurityConfig; every other endpoint stays authenticated.
 */
@RestController
class HealthController {

    @GetMapping("/health")
    fun health(): Map<String, String> = mapOf("status" to "UP")
}
