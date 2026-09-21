package com.hemotrack.backend.relatorio;

import com.hemotrack.backend.model.requisicao.Prioridade;
import com.hemotrack.backend.model.sangue.TipoHemocomponente;
import com.hemotrack.backend.model.shared.FatorRh;
import com.hemotrack.backend.model.shared.TipoABO;

/** Requisição de hemocomponente (versão leve, só para processamento em memória). */
public record RegistroRequisicao(long id, long hospitalId, TipoHemocomponente tipo, TipoABO abo,
                                 FatorRh rh, int volumeMl, Prioridade prioridade) {
}
