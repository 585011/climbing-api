package com.example.climbingapi.controller;

import com.example.climbingapi.dto.CreateWallRequest;
import com.example.climbingapi.dto.RouteResponse;
import com.example.climbingapi.dto.WallResponse;
import com.example.climbingapi.mapper.RouteMapper;
import com.example.climbingapi.mapper.WallMapper;
import com.example.climbingapi.service.WallService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/walls")
public class WallController {

    private final WallService wallService;
    private final WallMapper wallMapper;
    private final RouteMapper routeMapper;

    public WallController(WallService wallService, WallMapper wallMapper, RouteMapper routeMapper) {
        this.wallService = wallService;
        this.wallMapper = wallMapper;
        this.routeMapper = routeMapper;
    }

    @GetMapping
    public List<WallResponse> getAll() {
        return wallService.getAll().stream()
                .map(wallMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public WallResponse getById(@PathVariable Integer id) {
        return wallMapper.toResponse(wallService.getById(id));
    }

    @GetMapping("/{wallId}/routes")
    public List<RouteResponse> getRoutes(@PathVariable Integer wallId) {
        return wallService.getRoutes(wallId).stream()
                .map(routeMapper::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WallResponse create(@Valid @RequestBody CreateWallRequest request) {
        return wallMapper.toResponse(wallService.create(request));
    }
}
