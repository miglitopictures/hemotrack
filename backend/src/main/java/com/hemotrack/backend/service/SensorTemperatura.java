package com.hemotrack.backend.service;

import java.time.Instant;

import com.hemotrack.backend.model.sangue.TipoHemocomponente;

/**
 * HU08: fonte da temperatura do lote durante o transporte.
 * Hoje e simulada (SensorTemperaturaSimulado); um sensor real so precisaria implementar esta interface.
 */
public interface SensorTemperatura {

    double ler(Long requisicaoId, TipoHemocomponente tipo, Instant instante);
}
