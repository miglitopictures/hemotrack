package com.hemotrack.backend.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * HU07: mapa fixo e simplificado de municipios de Pernambuco.
 *
 * Cada no do grafo e um municipio; cada conexao e uma estrada com a
 * distancia aproximada em km. A HU pede um "grafo limitado e simplificado",
 * entao as distancias sao aproximadas e nao representam ruas reais.
 */
@Component
public class MapaTransporte {

    private final GrafoTransporte grafo = new GrafoTransporte();

    // chave normalizada ("jaboatao dos guararapes") -> nome oficial ("Jaboatão dos Guararapes").
    private final Map<String, String> nomesPorChave = new HashMap<>();

    private final List<String> municipios = new ArrayList<>();

    public MapaTransporte() {
        // Regiao Metropolitana do Recife
        conectar("Recife", "Olinda", 7);
        conectar("Recife", "Paulista", 17);
        conectar("Olinda", "Paulista", 12);
        conectar("Paulista", "Igarassu", 13);
        conectar("Recife", "Jaboatão dos Guararapes", 18);
        conectar("Jaboatão dos Guararapes", "Cabo de Santo Agostinho", 23);
        conectar("Recife", "Camaragibe", 16);
        conectar("Camaragibe", "São Lourenço da Mata", 7);

        // BR-232: Recife -> Agreste -> Sertao
        conectar("Jaboatão dos Guararapes", "Vitória de Santo Antão", 40);
        conectar("São Lourenço da Mata", "Vitória de Santo Antão", 35);
        conectar("Vitória de Santo Antão", "Gravatá", 35);
        conectar("Gravatá", "Caruaru", 50);
        conectar("Caruaru", "Garanhuns", 100);
        conectar("Caruaru", "Arcoverde", 125);
        conectar("Arcoverde", "Serra Talhada", 165);
        conectar("Serra Talhada", "Salgueiro", 100);
        conectar("Salgueiro", "Petrolina", 190);
    }

    /**
     * Encontra o nome oficial do municipio no mapa, ignorando acentos,
     * maiusculas e espacos extras. Vazio se o municipio nao estiver no mapa.
     */
    public Optional<String> localizar(String municipio) {
        if (municipio == null || municipio.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(nomesPorChave.get(normalizar(municipio)));
    }

    /** Nomes oficiais de todos os municipios do mapa. */
    public List<String> municipios() {
        return List.copyOf(municipios);
    }

    /**
     * Rota de menor distancia entre dois municipios.
     * Recebe nomes oficiais (use localizar antes).
     */
    public Optional<GrafoTransporte.Rota> calcularRota(String origem, String destino) {
        return grafo.calcularCaminhoMinimo(origem, destino);
    }

    private void conectar(String municipioA, String municipioB, double km) {
        registrar(municipioA);
        registrar(municipioB);
        grafo.adicionarConexaoBidirecional(municipioA, municipioB, km);
    }

    private void registrar(String municipio) {
        if (!municipios.contains(municipio)) {
            municipios.add(municipio);
            nomesPorChave.put(normalizar(municipio), municipio);
        }
    }

    // "  Jaboatão  dos GUARARAPES " -> "jaboatao dos guararapes"
    private static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
