package com.hemotrack.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * HU07: mapa fixo e simplificado de municipios usado para planejar a rota
 * entre o hemocentro e o hospital.
 */
class MapaTransporteTest {

    private final MapaTransporte mapa = new MapaTransporte();

    // municipio vem de texto livre no cadastro: acento, maiuscula e espaco extra nao podem atrapalhar.
    @Test
    void localizaMunicipioIgnorandoAcentoMaiusculaEEspacos() {
        assertEquals(Optional.of("Jaboatão dos Guararapes"), mapa.localizar("jaboatao  dos guararapes"));
        assertEquals(Optional.of("Recife"), mapa.localizar("  RECIFE "));
    }

    @Test
    void municipioForaDoMapaRetornaVazio() {
        assertTrue(mapa.localizar("Manaus").isEmpty());
    }

    @Test
    void municipioNuloOuVazioRetornaVazio() {
        assertTrue(mapa.localizar(null).isEmpty());
        assertTrue(mapa.localizar("   ").isEmpty());
    }

    // garante que o mapa e conexo: qualquer hemocentro alcanca qualquer hospital do mapa.
    @Test
    void existeRotaEntreQuaisquerDoisMunicipiosDoMapa() {
        List<String> municipios = mapa.municipios();

        for (String origem : municipios) {
            for (String destino : municipios) {
                assertTrue(mapa.calcularRota(origem, destino).isPresent(),
                    "Sem rota de " + origem + " para " + destino);
            }
        }
    }

    @Test
    void rotaRecifeCaruaruSeguePelaBr232() {
        GrafoTransporte.Rota rota = mapa.calcularRota("Recife", "Caruaru").orElseThrow();

        assertEquals("Recife", rota.locais().get(0));
        assertEquals("Caruaru", rota.locais().get(rota.locais().size() - 1));
        assertTrue(rota.locais().contains("Vitória de Santo Antão"));
    }
}
