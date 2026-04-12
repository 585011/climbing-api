package com.example.climbingproject.service;

import com.example.climbingproject.model.Wall;
import com.example.climbingproject.repository.WallRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WallService {

    private final WallRepository wallRepository;

    public WallService(WallRepository wallRepository) {
        this.wallRepository = wallRepository;
    }

    public List<Wall> getAll() {
        return wallRepository.getAll();
    }

    public Wall getById(Integer id) {
        return wallRepository.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Wall not found: " + id));
    }
}
