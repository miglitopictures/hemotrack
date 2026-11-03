package com.hemotrack.backend.model.sangue.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.hemotrack.backend.model.sangue.Hemocomponente;
import com.hemotrack.backend.model.sangue.SituacaoBolsa;
import com.hemotrack.backend.model.sangue.StatusHemocomponente;
import com.hemotrack.backend.model.sangue.TipoHemocomponente;
import com.hemotrack.backend.model.shared.FatorRh;
import com.hemotrack.backend.model.shared.TipoABO;

/**
 * HU02: bolsa como aparece no estoque.
 *
 * @param status o que esta gravado no banco
 * @param situacao o que a tela mostra (inclui VENCIDA, calculada pela validade)
 * @param diasParaVencer negativo ou zero quando ja venceu
 */
public record HemocomponenteResponse(
    Long id,
    String codigoBolsa,
    TipoHemocomponente tipo,
    TipoABO abo,
    FatorRh rh,
    double volumeMl,
    LocalDate dataColeta,
    LocalDate dataValidade,
    StatusHemocomponente status,
    SituacaoBolsa situacao,
    long diasParaVencer,
    boolean emTransito,
    Long instituicaoId,
    Long requisicaoAlocadaId
) {
    public static HemocomponenteResponse de(Hemocomponente h, LocalDate hoje) {
        return new HemocomponenteResponse(
            h.getId(),
            h.getCodigoBolsa(),
            h.getTipo(),
            h.getAbo(),
            h.getRh(),
            h.getVolumeMl(),
            h.getDataColeta(),
            h.getDataValidade(),
            h.getStatus(),
            SituacaoBolsa.de(h, hoje),
            ChronoUnit.DAYS.between(hoje, h.getDataValidade()),
            h.isEmTransito(),
            h.getInstituicaoAtualId(),
            h.getRequisicaoAlocadaId()
        );
    }
}
