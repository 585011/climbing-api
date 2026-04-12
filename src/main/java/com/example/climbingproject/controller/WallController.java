package com.example.climbingproject.controller;

import com.example.climbingproject.model.Wall;
import com.example.climbingproject.service.WallService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/walls")
public class WallController {

    private final WallService wallService;

    public WallController(WallService wallService) {
        this.wallService = wallService;
    }

    @GetMapping
    public List<Wall> getAll() {
        return wallService.getAll();
    }

    @GetMapping("/{id}")
    public Wall getById(@PathVariable Integer id) {
        try {
            return wallService.getById(id);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }
}
