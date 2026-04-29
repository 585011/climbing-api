package com.example.climbingapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    private Integer id;
    private Integer wallId;
    private String name;
    private String grade;
    private Integer length;
    private String style;
    private Integer bolts;
    private Integer ropeLengths;
    private String firstAscendant;
    private String description;
    private OffsetDateTime createdAt;
}
