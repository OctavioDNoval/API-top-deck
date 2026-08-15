package org.example.topdeckapi.src.controller;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.response.StatsResponse;
import org.example.topdeckapi.src.service.IMPL.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/admin")
    public ResponseEntity<StatsResponse> obtenerEstadisticas() {
        return ResponseEntity.ok(statsService.obtenerEstadisticas());
    }
}
