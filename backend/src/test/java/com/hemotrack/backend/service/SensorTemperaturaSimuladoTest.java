package com.hemotrack.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.hemotrack.backend.model.sangue.FaixaTemperatura;
import com.hemotrack.backend.model.sangue.TipoHemocomponente;

/**
 * HU08: a temperatura e simulada. A leitura precisa ser estavel (mesmo minuto, mesma leitura,
 * para a tela nao "piscar") e realista: quase sempre dentro da faixa, com desvios de vez em quando.
 */
class SensorTemperaturaSimuladoTest {

    private static final Instant INICIO = Instant.parse("2026-11-09T12:00:00Z");

    private final SensorTemperaturaSimulado sensor = new SensorTemperaturaSimulado();

    @Test
    void mesmoMinuto_mesmaLeitura() {
        double primeira = sensor.ler(1L, TipoHemocomponente.HEMACIAS, INICIO);
        double segunda = sensor.ler(1L, TipoHemocomponente.HEMACIAS, INICIO.plusSeconds(30));

        assertEquals(primeira, segunda);
    }

    @Test
    void emDezHorasDeViagem_quaseTudoDentroDaFaixa_masComAlgunsDesvios() {
        for (TipoHemocomponente tipo : TipoHemocomponente.values()) {
            FaixaTemperatura faixa = FaixaTemperatura.de(tipo);
            int dentro = 0;
            int minutos = 600;

            for (int m = 0; m < minutos; m++) {
                double leitura = sensor.ler(1L, tipo, INICIO.plusSeconds(60L * m));
                if (faixa.contem(leitura)) {
                    dentro++;
                }
            }

            assertTrue(dentro >= minutos * 0.8, tipo + ": so " + dentro + " leituras dentro da faixa");
            assertTrue(dentro < minutos, tipo + ": nenhum desvio, o alerta nunca apareceria na demo");
        }
    }
}
