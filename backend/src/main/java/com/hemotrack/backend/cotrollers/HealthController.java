package com.hemotrack.backend.cotrollers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sonda publica minima para o Render e para o teste de fumaca, sem expor detalhes do Actuator. */
@RestController
public class HealthController {
    private final HealthEndpoint health;
    private final String versao;

    public HealthController(HealthEndpoint health, @Value("${hemotrack.versao:local}") String versao) {
        this.health = health;
        this.versao = versao;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return resposta(health.health().getStatus().getCode(), versao);
    }

    // regra da sonda, sem Spring: UP -> 200, qualquer outro -> 503; sempre informa a versao.
    static ResponseEntity<Map<String, String>> resposta(String status, String versao) {
        return ResponseEntity.status("UP".equals(status) ? 200 : 503)
            .body(Map.of("status", status, "versao", versao));
    }
}