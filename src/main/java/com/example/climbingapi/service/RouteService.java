package com.example.climbingapi.service;

import com.example.climbingapi.dto.CreateRouteRequest;
import com.example.climbingapi.exception.NotFoundException;
import com.example.climbingapi.model.Route;
import com.example.climbingapi.repository.RouteRepository;
import com.example.climbingapi.repository.WallRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteService {

    private final RouteRepository routeRepository;
    private final WallRepository wallRepository;

    public RouteService(RouteRepository routeRepository, WallRepository wallRepository) {
        this.routeRepository = routeRepository;
        this.wallRepository = wallRepository;
    }

    public List<Route> getAll() {
        return routeRepository.getAll();
    }

    public Route getById(Integer id) {
        return routeRepository.getById(id)
                .orElseThrow(() -> new NotFoundException("Route not found: " + id));
    }

    public List<Route> getByWallId(Integer wallId) {
        return routeRepository.findByWallId(wallId);
    }

    public Route create(CreateRouteRequest request) {
        wallRepository.getById(request.wallId())
                .orElseThrow(() -> new NotFoundException("Wall not found: " + request.wallId()));

        Route route = new Route(
                null,
                request.wallId(),
                request.name().trim(),
                request.grade().trim(),
                request.length(),
                request.style(),
                request.bolts(),
                request.ropeLengths(),
                request.firstAscendant(),
                request.description(),
                null
        );

        return routeRepository.create(route);
    }
}
