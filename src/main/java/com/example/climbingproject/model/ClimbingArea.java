package com.example.climbingproject.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClimbingArea {

    private Integer id;
    private String name;
    private String description;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String region;
    private OffsetDateTime createdAt;
}
