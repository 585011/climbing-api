package com.example.climbingapi.mapper;

import com.example.climbingapi.dto.RouteResponse;
import com.example.climbingapi.model.Route;
import org.springframework.stereotype.Component;

@Component
public class RouteMapper {

    public RouteResponse toResponse(Route route) {
        return new RouteResponse(
                route.getId(),
                route.getWallId(),
                route.getName(),
                route.getGrade(),
                route.getLength(),
                route.getStyle(),
                route.getBolts(),
                route.getRopeLengths(),
                route.getFirstAscendant(),
                route.getDescription(),
                route.getCreatedAt()
        );
    }
}
