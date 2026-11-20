package com.hemotrack.backend.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrafoTransporteTest {

    @Test
    void encontraOCaminhoDeMenorPeso() {
        GrafoTransporte grafo = new GrafoTransporte();

        // Pelo norte: 8 + 7 = 15. Pelo sul: 5 + 4 = 9.
        grafo.adicionarConexaoBidirecional("HEMOCENTRO", "NORTE", 8.0);
        grafo.adicionarConexaoBidirecional("HEMOCENTRO", "SUL", 5.0);
        grafo.adicionarConexaoBidirecional("NORTE", "HOSPITAL", 7.0);
        grafo.adicionarConexaoBidirecional("SUL", "HOSPITAL", 4.0);

        GrafoTransporte.Rota rota = grafo
            .calcularCaminhoMinimo("HEMOCENTRO", "HOSPITAL")
            .orElseThrow();

        assertEquals(
            List.of("HEMOCENTRO", "SUL", "HOSPITAL"),
            rota.locais()
        );
        assertEquals(9.0, rota.pesoTotal(), 0.0001);
    }

    @Test
    void retornaVazioQuandoNaoExisteCaminho() {
        GrafoTransporte grafo = new GrafoTransporte();
        grafo.adicionarLocal("HEMOCENTRO");
        grafo.adicionarLocal("HOSPITAL");

        assertTrue(
            grafo.calcularCaminhoMinimo("HEMOCENTRO", "HOSPITAL").isEmpty()
        );
    }

    @Test
    void rejeitaPesoNegativo() {
        GrafoTransporte grafo = new GrafoTransporte();

        assertThrows(IllegalArgumentException.class,
            () -> grafo.adicionarConexao("HEMOCENTRO", "HOSPITAL", -1.0));
    }

    @Test
    void rejeitaOrigemNaoCadastrada() {
        GrafoTransporte grafo = new GrafoTransporte();
        grafo.adicionarLocal("HOSPITAL");

        assertThrows(IllegalArgumentException.class,
            () -> grafo.calcularCaminhoMinimo("HEMOCENTRO", "HOSPITAL"));
    }
}