package com.example.climbingapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoute {

    private Integer id;
    private Integer userId;
    private Integer routeId;
    private OffsetDateTime tickedAt;
    private String style;
    private Integer rating;
    private String personalNote;
}
