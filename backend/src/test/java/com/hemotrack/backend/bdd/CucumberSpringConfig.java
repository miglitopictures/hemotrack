package com.hemotrack.backend.bdd;

import org.springframework.boot.test.context.SpringBootTest;

import io.cucumber.spring.CucumberContextConfiguration;

// Contexto Spring dos cenários: mesma configuração dos testes de aceitação
// (Spring de verdade em porta aleatória, H2 em memória, admin do sistema de teste),
// com banco próprio para não misturar dados com HistoriasAceitacaoTest.
@CucumberContextConfiguration
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:bdd;DB_CLOSE_DELAY=-1",
        "hemotrack.jwt.segredo=segredo-de-teste-com-pelo-menos-32-caracteres",
        "hemotrack.admin.email=admin@hemotrack.test",
        "hemotrack.admin.senha=senha-admin-123",
        "management.otlp.metrics.export.enabled=false"
    })
public class CucumberSpringConfig {
}