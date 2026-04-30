package com.example.climbingapi

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@OpenAPIDefinition(
    info = Info(
        title = "Climbing API",
        description = "REST API for managing climbing walls and routes.",
        version = "0.0.1"
    )
)
@SpringBootApplication
class ClimbingProjectApplication

fun main(args: Array<String>) {
    runApplication<ClimbingProjectApplication>(*args)
}
