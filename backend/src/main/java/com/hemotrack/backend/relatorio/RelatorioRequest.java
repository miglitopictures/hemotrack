package com.hemotrack.backend.relatorio;

import java.util.List;

public record RelatorioRequest(List<RegistroRequisicao> requisicoes, List<UnidadeEstoque> estoque) {
}
