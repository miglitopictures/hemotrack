package com.hemotrack.backend.relatorio;

import com.hemotrack.backend.model.sangue.TipoHemocomponente;
import com.hemotrack.backend.model.shared.FatorRh;
import com.hemotrack.backend.model.shared.TipoABO;

/** Unidade (hemocomponente) disponível em estoque. */
public record UnidadeEstoque(long id, TipoHemocomponente tipo, TipoABO abo, FatorRh rh, int volumeMl) {
}
