package com.hemotrack.backend.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.http.ResponseEntity;

// Teste de unidade, sem Spring: o /health informa qual commit esta no ar,
// para o teste de fumaca da esteira saber que a versao nova ja subiu.
class HealthControllerTest {

    private final HealthEndpoint health = mock(HealthEndpoint.class, RETURNS_DEEP_STUBS);

    @Test
    void appNoAr_respondeUpComAVersaoDoCommit() {
        when(health.health().getStatus()).thenReturn(Status.UP);
        HealthController controller = new HealthController(health, "abc123");

        ResponseEntity<Map<String, String>> resposta = controller.health();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).containsEntry("status", "UP").containsEntry("versao", "abc123");
    }

    @Test
    void bancoFora_responde503MasAindaInformaAVersao() {
        when(health.health().getStatus()).thenReturn(Status.DOWN);
        HealthController controller = new HealthController(health, "abc123");

        ResponseEntity<Map<String, String>> resposta = controller.health();

        assertThat(resposta.getStatusCode().value()).isEqualTo(503);
        assertThat(resposta.getBody()).containsEntry("versao", "abc123");
    }
}