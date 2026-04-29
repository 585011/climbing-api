package com.example.climbingapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wall {

    private Integer id;
    private Integer areaId;
    private String name;
    private String description;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String approachInfo;
    private OffsetDateTime createdAt;
}
