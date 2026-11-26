package com.hemotrack.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import com.hemotrack.backend.model.requisicao.Requisicao;
import com.hemotrack.backend.repositories.RequisicaoRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;

class TelemetriaServiceTest {
    @Test
    void contadorIncluiSomenteCriacoesPersistidas() {
        var metricas = new SimpleMeterRegistry();
        var telemetria = new TelemetriaService(metricas, ObservationRegistry.create());
        var repository = mock(RequisicaoRepository.class);
        var service = new RequisicaoService(repository, telemetria);
        var requisicao = new Requisicao();
        requisicao.setvolumeMl(300);
        when(repository.save(any())).thenReturn(requisicao)
            .thenThrow(new IllegalStateException("banco indisponivel"));

        service.salvar(requisicao);
        assertThatThrownBy(() -> service.salvar(requisicao)).isInstanceOf(IllegalStateException.class);
        requisicao.setvolumeMl(0);
        assertThatThrownBy(() -> service.salvar(requisicao)).isInstanceOf(RuntimeException.class);
        assertThat(metricas.get("hemotrack.requisicoes.criadas").counter().count()).isEqualTo(1);
    }

    @Test
    void timerContaConsultasComSucessoEComErroESempreFechaObservacao() {
        var metricas = new SimpleMeterRegistry();
        var observacoes = ObservationRegistry.create();
        var telemetria = new TelemetriaService(metricas, observacoes);
        assertThat(telemetria.consultar(() -> "resultado")).isEqualTo("resultado");
        assertThatThrownBy(() -> telemetria.consultar(() -> {
            throw new IllegalArgumentException("consulta rejeitada");
        })).isInstanceOf(IllegalArgumentException.class);
        assertThat(metricas.get("hemotrack.requisicoes.consulta").timer().count()).isEqualTo(2);
        assertThat(observacoes.getCurrentObservation()).isNull();
    }
}
