package com.example.climbingproject.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRouteRequest(
        @NotNull(message = "wallId is required.")
        @Min(value = 1, message = "wallId must be at least 1.")
        Integer wallId,

        @NotBlank(message = "name is required.")
        String name,

        @NotBlank(message = "grade is required.")
        String grade,

        @Min(value = 1, message = "length must be at least 1.")
        Integer length,

        String style,

        @Min(value = 0, message = "bolts must be 0 or greater.")
        Integer bolts,

        @Min(value = 1, message = "ropeLengths must be at least 1.")
        Integer ropeLengths,

        String firstAscendant,
        String description
) {
}
