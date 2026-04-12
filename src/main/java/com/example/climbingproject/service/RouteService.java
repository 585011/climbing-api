package com.example.climbingproject.service;

import com.example.climbingproject.model.Route;
import com.example.climbingproject.repository.RouteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteService {

    private final RouteRepository routeRepository;

    public RouteService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    public List<Route> getAll() {
        return routeRepository.getAll();
    }

    public Route getById(Integer id) {
        return routeRepository.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Route not found: " + id));
    }
}
