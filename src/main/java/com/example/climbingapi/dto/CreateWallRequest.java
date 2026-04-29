package com.example.climbingapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateWallRequest(
        @Min(value = 1, message = "areaId must be at least 1.")
        Integer areaId,

        @NotBlank(message = "name is required.")
        String name,
        String description,
        BigDecimal latitude,
        BigDecimal longitude,
        String approachInfo
) {
}
