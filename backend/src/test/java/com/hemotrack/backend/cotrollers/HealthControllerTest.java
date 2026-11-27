package com.hemotrack.backend.cotrollers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

// Teste de unidade, sem Spring nem mocks: o /health informa qual commit esta no ar,
// para o teste de fumaca da esteira saber que a versao nova ja subiu.
class HealthControllerTest {

    @Test
    void appNoAr_respondeUpComAVersaoDoCommit() {
        ResponseEntity<Map<String, String>> resposta = HealthController.resposta("UP", "abc123");

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).containsEntry("status", "UP").containsEntry("versao", "abc123");
    }

    @Test
    void bancoFora_responde503MasAindaInformaAVersao() {
        ResponseEntity<Map<String, String>> resposta = HealthController.resposta("DOWN", "abc123");

        assertThat(resposta.getStatusCode().value()).isEqualTo(503);
        assertThat(resposta.getBody()).containsEntry("versao", "abc123");
    }
}