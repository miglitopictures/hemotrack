package com.hemotrack.backend.relatorio;

import java.util.Map;

import com.hemotrack.backend.model.requisicao.Prioridade;

/**
 * Resultado do cruzamento requisições x estoque. Só contém inteiros (long), então somar
 * resultados parciais em qualquer ordem dá exatamente o mesmo valor (sem erro de ponto flutuante).
 */
public record RelatorioResultado(long totalRequisicoes, long atendiveis, long semCompatibilidade,
                                 long unidadesCompativeisTotal, long volumeCompativelTotalMl,
                                 long checksum, Map<Prioridade, ResumoPrioridade> porPrioridade) {

    public record ResumoPrioridade(long requisicoes, long atendiveis, long unidadesCompativeis) {
    }
}
