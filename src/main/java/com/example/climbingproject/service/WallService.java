package com.example.climbingproject.service;

import com.example.climbingproject.dto.CreateWallRequest;
import com.example.climbingproject.exception.NotFoundException;
import com.example.climbingproject.model.Wall;
import com.example.climbingproject.model.Route;
import com.example.climbingproject.repository.WallRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WallService {

    private final WallRepository wallRepository;
    private final RouteService routeService;

    public WallService(WallRepository wallRepository, RouteService routeService) {
        this.wallRepository = wallRepository;
        this.routeService = routeService;
    }

    public List<Wall> getAll() {
        return wallRepository.getAll();
    }

    public Wall getById(Integer id) {
        return wallRepository.getById(id)
                .orElseThrow(() -> new NotFoundException("Wall not found: " + id));
    }

    public List<Route> getRoutes(Integer wallId) {
        getById(wallId);
        return routeService.getByWallId(wallId);
    }

    public Wall create(CreateWallRequest request) {
        Wall wall = new Wall(
                null,
                request.areaId(),
                request.name().trim(),
                request.description(),
                request.latitude(),
                request.longitude(),
                request.approachInfo(),
                null
        );

        return wallRepository.create(wall);
    }
}
