package com.hemotrack.backend.model.sangue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * HU08: faixa de temperatura de transporte de cada hemocomponente
 * (hemacias 1 a 10 C, plaquetas 20 a 24 C, plasma e crio a -18 C ou menos).
 */
class FaixaTemperaturaTest {

    @Test
    void hemacias_de1a10Graus() {
        FaixaTemperatura faixa = FaixaTemperatura.de(TipoHemocomponente.HEMACIAS);

        assertTrue(faixa.contem(1.0));
        assertTrue(faixa.contem(4.0));
        assertTrue(faixa.contem(10.0));
        assertFalse(faixa.contem(0.5));
        assertFalse(faixa.contem(10.5));
    }

    @Test
    void plaquetas_de20a24Graus() {
        FaixaTemperatura faixa = FaixaTemperatura.de(TipoHemocomponente.PLAQUETAS);

        assertTrue(faixa.contem(22.0));
        assertFalse(faixa.contem(18.0));
        assertFalse(faixa.contem(25.0));
    }

    @Test
    void plasmaECrio_congelados_aMenos18OuMenos() {
        for (TipoHemocomponente tipo : new TipoHemocomponente[] {
                TipoHemocomponente.PLASMA, TipoHemocomponente.CRIOPRECIPITADO }) {
            FaixaTemperatura faixa = FaixaTemperatura.de(tipo);

            assertTrue(faixa.contem(-25.0));
            assertTrue(faixa.contem(-18.0));
            assertFalse(faixa.contem(-10.0));
        }
    }
}
