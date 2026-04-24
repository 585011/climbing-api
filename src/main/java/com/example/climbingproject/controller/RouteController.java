package com.example.climbingproject.controller;

import com.example.climbingproject.dto.CreateRouteRequest;
import com.example.climbingproject.dto.RouteResponse;
import com.example.climbingproject.mapper.RouteMapper;
import com.example.climbingproject.service.RouteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;
    private final RouteMapper routeMapper;

    public RouteController(RouteService routeService, RouteMapper routeMapper) {
        this.routeService = routeService;
        this.routeMapper = routeMapper;
    }

    @GetMapping
    public List<RouteResponse> getAll() {
        return routeService.getAll().stream()
                .map(routeMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public RouteResponse getById(@PathVariable Integer id) {
        return routeMapper.toResponse(routeService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RouteResponse create(@Valid @RequestBody CreateRouteRequest request) {
        return routeMapper.toResponse(routeService.create(request));
    }
}
