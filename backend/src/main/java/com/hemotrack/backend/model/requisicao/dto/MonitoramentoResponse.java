package com.hemotrack.backend.model.requisicao.dto;

import java.time.Instant;
import java.util.List;

import com.hemotrack.backend.model.requisicao.StatusRequisicao;
import com.hemotrack.backend.model.sangue.FaixaTemperatura;

/**
 * HU08: retrato do transporte em andamento. Localizacao e temperatura sao simuladas.
 *
 * @param localAtual ultimo municipio da rota por onde o lote ja passou
 * @param proximoLocal proximo municipio da rota; null quando ja chegou ao destino
 * @param progresso percentual da distancia percorrida (0 a 100)
 * @param kmAcumulado distancia da origem ate cada local de locais
 * @param historico leituras de temperatura, uma por minuto, no maximo a ultima hora; a ultima e a atual
 */
public record MonitoramentoResponse(
    Long requisicaoId,
    StatusRequisicao status,
    String origem,
    String destino,
    List<String> locais,
    List<Double> kmAcumulado,
    String localAtual,
    String proximoLocal,
    int progresso,
    double kmPercorridos,
    double distanciaKm,
    double temperaturaC,
    FaixaTemperatura faixa,
    boolean alertaTemperatura,
    List<LeituraTemperatura> historico,
    Instant atualizadoEm
) {
    public record LeituraTemperatura(Instant instante, double temperaturaC) { }
}
