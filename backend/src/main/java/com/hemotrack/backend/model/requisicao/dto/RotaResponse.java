package com.hemotrack.backend.model.requisicao.dto;

import java.util.List;

/**
 * HU07: rota planejada do hemocentro (origem) ate o hospital (destino).
 *
 * @param locais municipios percorridos, da origem ao destino
 * @param kmAcumulado distancia da origem ate cada local (mesma ordem de locais)
 * @param distanciaKm distancia total aproximada
 */
public record RotaResponse(
    Long requisicaoId,
    Ponto origem,
    Ponto destino,
    List<String> locais,
    List<Double> kmAcumulado,
    double distanciaKm
) {
    public record Ponto(Long instituicaoId, String instituicao, String municipio) { }
}
