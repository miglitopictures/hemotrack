package com.hemotrack.backend.cotrollers;

import java.util.Map;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sonda publica minima para o Render, sem expor endpoints ou detalhes do Actuator. */
@RestController
public class HealthController {
    private final HealthEndpoint health;

    public HealthController(HealthEndpoint health) { this.health = health; }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        String status = health.health().getStatus().getCode();
        return ResponseEntity.status("UP".equals(status) ? 200 : 503)
            .body(Map.of("status", status));
    }
}
