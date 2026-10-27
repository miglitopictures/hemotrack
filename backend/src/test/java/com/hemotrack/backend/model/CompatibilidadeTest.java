// criando teste de compatibilidade para testar o TDD.

package com.hemotrack.backend.model;

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
                assertTrue(Compatibilidade.podeReceber(TipoHemocomponente.HEMACIAS, TipoABO.O, FatorRh.NEGATIVO, receptorABO, receptorRh));
            }
        }
    }
}
