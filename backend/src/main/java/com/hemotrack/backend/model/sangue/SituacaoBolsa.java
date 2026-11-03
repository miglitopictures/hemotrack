package com.hemotrack.backend.model.sangue;

import java.time.LocalDate;

/**
 * HU02: situacao da bolsa como o hemocentro ve no estoque (Disponivel, Reservada, Utilizada, Vencida).
 *
 * Nao e gravada: e calculada a partir do status gravado + data de validade. "Vencida" nao e status
 * no banco (ver StatusHemocomponente): uma bolsa DISPONIVEL passa a ser VENCIDA no dia da validade.
 */
public enum SituacaoBolsa {
    DISPONIVEL,
    RESERVADA,
    UTILIZADA,
    VENCIDA,
    DESCARTADA;

    public static SituacaoBolsa de(Hemocomponente bolsa, LocalDate hoje) {
        return switch (bolsa.getStatus()) {
            case RESERVADO -> RESERVADA;
            case TRANSFUNDIDO -> UTILIZADA;
            case DESCARTADO -> DESCARTADA;
            case DISPONIVEL -> bolsa.estaVencido(hoje) ? VENCIDA : DISPONIVEL;
        };
    }
}
