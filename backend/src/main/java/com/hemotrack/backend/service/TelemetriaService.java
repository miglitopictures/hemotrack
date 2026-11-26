package com.hemotrack.backend.service;

import java.util.function.Supplier;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Metricas de dominio sem IDs, emails, tokens ou dados clinicos nas tags/logs. */
@Component
public class TelemetriaService {
    private static final Logger log = LoggerFactory.getLogger(TelemetriaService.class);
    private final Counter criadas;
    private final Timer consulta;
    private final ObservationRegistry observacoes;

    public TelemetriaService(MeterRegistry metricas, ObservationRegistry observacoes) {
        this.observacoes = observacoes;
        criadas = Counter.builder("hemotrack.requisicoes.criadas")
            .description("Requisicoes de hemocomponentes persistidas com sucesso desde a partida")
            .register(metricas);
        consulta = Timer.builder("hemotrack.requisicoes.consulta")
            .description("Duracao das consultas de requisicoes de hemocomponentes")
            .publishPercentileHistogram().register(metricas);
    }

    public void requisicaoCriada() { criadas.increment(); }

    public <T> T observar(String operacao, Supplier<T> acao) {
        return Observation.createNotStarted("hemotrack.operacao." + operacao, observacoes)
            .observe(() -> {
                try {
                    T resultado = acao.get();
                    log.info("Operacao de requisicoes concluida: {}", operacao);
                    return resultado;
                } catch (RuntimeException erro) {
                    log.warn("Operacao de requisicoes rejeitada: {} tipo={}", operacao,
                        erro.getClass().getSimpleName());
                    throw erro;
                }
            });
    }

    public <T> T consultar(Supplier<T> acao) {
        return consulta.record(() -> observar("consulta", acao));
    }
}
