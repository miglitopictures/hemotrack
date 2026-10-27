// criando teste de compatibilidade para testar o TDD.

package com.hemotrack.backend.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.hemotrack.backend.model.sangue.TipoHemocomponente;
import com.hemotrack.backend.model.shared.FatorRh;
import com.hemotrack.backend.model.shared.TipoABO;

class CompatibilidadeTest {
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
}
