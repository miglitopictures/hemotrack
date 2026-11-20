package com.hemotrack.backend.model.sangue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class HemocomponenteTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 27);

    private Hemocomponente comValidade(LocalDate dataValidade) {
        Hemocomponente h = new Hemocomponente();
        h.setDataValidade(dataValidade);
        return h;
    }

    @Test
    void novo_nasceDisponivel() {
        assertEquals(StatusHemocomponente.DISPONIVEL, new Hemocomponente().getStatus());
    }

    @Test
    void estaVencido_noDiaDaValidade() {
        assertTrue(comValidade(HOJE).estaVencido(HOJE));
    }

    @Test
    void estaVencido_falsoAntesDaValidade() {
        assertFalse(comValidade(HOJE.plusDays(1)).estaVencido(HOJE));
    }

    @Test
    void estaDisponivel_verdadeiroSeDisponivelENaoVencido() {
        assertTrue(comValidade(HOJE.plusDays(10)).estaDisponivel(HOJE));
    }

    @Test
    void estaDisponivel_falsoSeReservado() {
        Hemocomponente h = comValidade(HOJE.plusDays(10));
        h.setStatus(StatusHemocomponente.RESERVADO);
        assertFalse(h.estaDisponivel(HOJE));
    }

    @Test
    void estaDisponivel_falsoSeVencido() {
        assertFalse(comValidade(HOJE.minusDays(1)).estaDisponivel(HOJE));
    }
}