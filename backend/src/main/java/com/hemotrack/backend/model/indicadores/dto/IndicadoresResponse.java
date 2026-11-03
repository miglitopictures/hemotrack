package com.hemotrack.backend.model.indicadores.dto;

import java.time.Instant;
import java.util.List;

import com.hemotrack.backend.model.sangue.TipoHemocomponente;

/**
 * HU09: indicadores da operacao, calculados na hora da consulta.
 *
 * @param bolsasDisponiveis bolsas DISPONIVEL e nao vencidas, em todas as instituicoes
 * @param bolsasVencendo disponiveis que vencem em ate diasParaVencimento dias
 * @param demandaPorComponente pedidos e atendidos por componente, do mais pedido para o menos
 * @param estoquePorTipoSanguineo os 8 tipos (A+ ... O-), inclusive os zerados
 */
public record IndicadoresResponse(
    long bolsasDisponiveis,
    long bolsasVencendo,
    int diasParaVencimento,
    long requisicoesPendentes,
    long requisicoesEmTransporte,
    long requisicoesEntregues,
    List<DemandaComponente> demandaPorComponente,
    List<EstoqueTipoSanguineo> estoquePorTipoSanguineo,
    Instant geradoEm
) {
    public record DemandaComponente(TipoHemocomponente componente, long pedidos, long atendidos) { }

    public record EstoqueTipoSanguineo(String tipo, long bolsas) { }
}
