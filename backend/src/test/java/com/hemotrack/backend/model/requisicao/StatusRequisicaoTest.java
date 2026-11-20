package com.hemotrack.backend.model.requisicao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Requisicao.status e gravado no banco como ORDINAL (posicao no enum), sem @Enumerated(STRING).
 * Se alguem inserir um status no meio do enum, as requisicoes ja gravadas no banco de producao
 * mudam de status sozinhas. Este teste trava as posicoes: status novo so entra no FINAL.
 */
class StatusRequisicaoTest {

    @Test
    void statusExistentesMantemAPosicaoGravadaNoBanco() {
        assertEquals(0, StatusRequisicao.ABERTA.ordinal());
        assertEquals(1, StatusRequisicao.ACEITA.ordinal());
        assertEquals(2, StatusRequisicao.ALOCADA.ordinal());
        assertEquals(3, StatusRequisicao.ATENDIDA.ordinal());
        assertEquals(4, StatusRequisicao.RECUSADA.ordinal());
        assertEquals(5, StatusRequisicao.CANCELADA.ordinal());
    }

    // HU08: o status de transporte entra no final para nao deslocar os anteriores.
    @Test
    void emTransitoEntraNoFinalDoEnum() {
        assertEquals(6, StatusRequisicao.EM_TRANSITO.ordinal());
    }
}
