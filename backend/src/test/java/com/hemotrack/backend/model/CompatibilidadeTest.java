// criando teste de compatibilidade para testar o TDD.

package com.hemotrack.backend.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.hemotrack.backend.model.sangue.TipoHemocomponente;
import com.hemotrack.backend.model.shared.FatorRh;
import com.hemotrack.backend.model.shared.TipoABO;

class CompatibilidadeTest {
    
    // hemacias

    @Test 
    void hemacias_oNegativoDoaPraTodos() {
        for (TipoABO receptorABO : TipoABO.values()) {
            for (FatorRh receptorRh : FatorRh.values()) {
                assertTrue(Compatibilidade.podeReceber(TipoHemocomponente.HEMACIAS, TipoABO.O, FatorRh.NEGATIVO, receptorABO, receptorRh),
                "O- deveria doar hemacias para " + receptorABO + " " + receptorRh);
            }
        }
    }

    @Test 
    void hemacias_abPositivoRecebeDeTodos() {
        for (TipoABO doadorABO : TipoABO.values()) {
            for (FatorRh doadorRh : FatorRh.values()) {
                assertTrue(Compatibilidade.podeReceber(TipoHemocomponente.HEMACIAS, doadorABO, doadorRh, TipoABO.AB, FatorRh.POSITIVO),
                "AB+ deveria receber hemacias de " + doadorABO + " " + doadorRh);
            }
        }
    }

    @Test 
    void hemacias_aNaoDoaPraB() {
        assertFalse(Compatibilidade.podeReceber(TipoHemocomponente.HEMACIAS, TipoABO.A, FatorRh.POSITIVO, TipoABO.B, FatorRh.POSITIVO));
    }

    @Test 
    void hemacias_rhPositivoNaoDoaParaRhNegativo() {
        assertFalse(Compatibilidade.podeReceber(TipoHemocomponente.HEMACIAS, TipoABO.A, FatorRh.POSITIVO, TipoABO.A, FatorRh.NEGATIVO));
    }

        @Test
    void plasma_abDoaParaTodosIgnorandoRh() {
        for (TipoABO receptor : TipoABO.values()) {
            assertTrue(Compatibilidade.podeReceber(TipoHemocomponente.PLASMA,
                    TipoABO.AB, FatorRh.POSITIVO, receptor, FatorRh.NEGATIVO),
                    "plasma AB deveria servir para " + receptor);
        }
    }

    // plasma

    @Test
    void plasma_oNaoDoaParaA() {
        assertFalse(Compatibilidade.podeReceber(TipoHemocomponente.PLASMA,
                TipoABO.O, FatorRh.NEGATIVO, TipoABO.A, FatorRh.NEGATIVO));
    }

    @Test
    void crioprecipitado_segueRegraDoPlasma() {
        assertTrue(Compatibilidade.podeReceber(TipoHemocomponente.CRIOPRECIPITADO,
                TipoABO.AB, FatorRh.POSITIVO, TipoABO.O, FatorRh.NEGATIVO));
        assertFalse(Compatibilidade.podeReceber(TipoHemocomponente.CRIOPRECIPITADO,
                TipoABO.O, FatorRh.NEGATIVO, TipoABO.B, FatorRh.NEGATIVO));
    }

    // plaquetas

    @Test
    void plaquetas_abPositivoParaOPositivo() {
        assertTrue(Compatibilidade.podeReceber(TipoHemocomponente.PLAQUETAS,
                TipoABO.AB, FatorRh.POSITIVO, TipoABO.O, FatorRh.POSITIVO));
    }

    @Test
    void plaquetas_rhPositivoNaoServeParaRhNegativo() {
        assertFalse(Compatibilidade.podeReceber(TipoHemocomponente.PLAQUETAS,
                TipoABO.A, FatorRh.POSITIVO, TipoABO.A, FatorRh.NEGATIVO));
    }
}
