package com.hemotrack.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.hemotrack.backend.model.instituicao.Instituicao;
import com.hemotrack.backend.model.instituicao.TipoInstituicao;
import com.hemotrack.backend.model.requisicao.Requisicao;
import com.hemotrack.backend.model.requisicao.StatusRequisicao;
import com.hemotrack.backend.model.requisicao.dto.MonitoramentoResponse;
import com.hemotrack.backend.model.requisicao.dto.MonitoramentoResponse.LeituraTemperatura;
import com.hemotrack.backend.model.sangue.TipoHemocomponente;
import com.hemotrack.backend.repositories.InstituicaoRepository;
import com.hemotrack.backend.repositories.RequisicaoRepository;

/**
 * HU08: monitorar o transporte. Posicao e temperatura sao simuladas.
 *
 * Rota usada nos testes (mapa da HU07), Recife -> Caruaru, 143 km:
 * Recife (km 0) -> Jaboatao (18) -> Vitoria de Santo Antao (58) -> Gravata (93) -> Caruaru (143)
 * Velocidade simulada: 10 km por minuto.
 */
@ExtendWith(MockitoExtension.class)
class MonitoramentoServiceTest {

    private static final Long REQUISICAO = 1L;
    private static final Long HEMOCENTRO = 7L;
    private static final Long HOSPITAL = 3L;
    private static final double KM_POR_MINUTO = 10;
    private static final Instant ENVIO = Instant.parse("2026-11-09T12:00:00Z");

    @Mock
    private RequisicaoRepository requisicaoRepository;

    @Mock
    private InstituicaoRepository instituicaoRepository;

    // sensor falso: devolve exatamente a temperatura que o teste quer
    private double temperaturaDoSensor = 4.0;
    private final SensorTemperatura sensorFalso = (requisicaoId, tipo, instante) -> temperaturaDoSensor;

    // ---------- montagem ----------

    private MonitoramentoService serviceDepoisDe(Duration tempoDeViagem) {
        MapaTransporte mapa = new MapaTransporte();
        RotaService rotaService = new RotaService(requisicaoRepository, instituicaoRepository, mapa);
        Clock relogio = Clock.fixed(ENVIO.plus(tempoDeViagem), ZoneOffset.UTC);
        return new MonitoramentoService(requisicaoRepository, rotaService, sensorFalso, relogio, KM_POR_MINUTO);
    }

    private Requisicao existeRequisicao(StatusRequisicao status) {
        Requisicao r = new Requisicao();
        r.setId(REQUISICAO);
        r.setHospitalId(HOSPITAL);
        r.setHemocentroId(HEMOCENTRO);
        r.setTipo(TipoHemocomponente.HEMACIAS);
        r.setStatus(status);
        r.setDataEnvio(ENVIO);
        when(requisicaoRepository.findById(REQUISICAO)).thenReturn(Optional.of(r));
        return r;
    }

    private void transporteEmAndamentoDeRecifeParaCaruaru() {
        existeRequisicao(StatusRequisicao.EM_TRANSITO);
        Instituicao hemocentro = new Instituicao("HEMOPE", "11111111000111", TipoInstituicao.HEMOCENTRO,
            "Rua Joaquim Nabuco, 171", "Recife", null);
        Instituicao hospital = new Instituicao("Hospital Regional", "22222222000122", TipoInstituicao.HOSPITAL,
            "Av. Principal, 100", "Caruaru", null);
        when(instituicaoRepository.findById(HEMOCENTRO)).thenReturn(Optional.of(hemocentro));
        when(instituicaoRepository.findById(HOSPITAL)).thenReturn(Optional.of(hospital));
    }

    // ---------- cenario 1: transporte em andamento (status, localizacao e temperatura) ----------

    @Test
    void noMomentoDoEnvio_estaNaOrigem() {
        transporteEmAndamentoDeRecifeParaCaruaru();

        MonitoramentoResponse m = serviceDepoisDe(Duration.ZERO).monitorar(REQUISICAO);

        assertEquals(StatusRequisicao.EM_TRANSITO, m.status());
        assertEquals("Recife", m.origem());
        assertEquals("Caruaru", m.destino());
        assertEquals("Recife", m.localAtual());
        assertEquals("Jaboatão dos Guararapes", m.proximoLocal());
        assertEquals(0, m.progresso());
    }

    @Test
    void depoisDe6Minutos_esta60KmAdiante_jaPassouPorVitoria() {
        transporteEmAndamentoDeRecifeParaCaruaru();

        MonitoramentoResponse m = serviceDepoisDe(Duration.ofMinutes(6)).monitorar(REQUISICAO);

        assertEquals(60.0, m.kmPercorridos());
        assertEquals(143.0, m.distanciaKm());
        assertEquals("Vitória de Santo Antão", m.localAtual());
        assertEquals("Gravatá", m.proximoLocal());
        assertEquals(41, m.progresso()); // 60 / 143 = 41,9%
    }

    @Test
    void depoisDoTempoDaViagem_paraNoDestinoEsperandoORecebimento() {
        transporteEmAndamentoDeRecifeParaCaruaru();

        MonitoramentoResponse m = serviceDepoisDe(Duration.ofMinutes(30)).monitorar(REQUISICAO);

        assertEquals(143.0, m.kmPercorridos());
        assertEquals("Caruaru", m.localAtual());
        assertNull(m.proximoLocal());
        assertEquals(100, m.progresso());
        // so o hospital finaliza o transporte (PATCH /receber)
        assertEquals(StatusRequisicao.EM_TRANSITO, m.status());
    }

    @Test
    void temperaturaDentroDaFaixa_semAlerta() {
        transporteEmAndamentoDeRecifeParaCaruaru();
        temperaturaDoSensor = 4.0;

        MonitoramentoResponse m = serviceDepoisDe(Duration.ofMinutes(5)).monitorar(REQUISICAO);

        assertEquals(4.0, m.temperaturaC());
        assertEquals(1.0, m.faixa().minC());
        assertEquals(10.0, m.faixa().maxC());
        assertFalse(m.alertaTemperatura());
    }

    // ---------- cenario 2: temperatura fora do limite ----------

    @Test
    void temperaturaForaDaFaixa_geraAlerta() {
        transporteEmAndamentoDeRecifeParaCaruaru();
        temperaturaDoSensor = 12.5;

        MonitoramentoResponse m = serviceDepoisDe(Duration.ofMinutes(5)).monitorar(REQUISICAO);

        assertEquals(12.5, m.temperaturaC());
        assertTrue(m.alertaTemperatura());
    }

    // ---------- erros ----------

    @ParameterizedTest
    @EnumSource(value = StatusRequisicao.class, names = "EM_TRANSITO", mode = EnumSource.Mode.EXCLUDE)
    void soMonitoraRequisicaoEmTransito(StatusRequisicao status) {
        existeRequisicao(status);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
            () -> serviceDepoisDe(Duration.ZERO).monitorar(REQUISICAO));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    void requisicaoInexistente_retorna404() {
        when(requisicaoRepository.findById(REQUISICAO)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
            () -> serviceDepoisDe(Duration.ZERO).monitorar(REQUISICAO));
        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }

    // ---------- dados para a tela: rota com km e historico de temperatura ----------

    @Test
    void trazADistanciaAcumuladaAteCadaLocalDaRota() {
        transporteEmAndamentoDeRecifeParaCaruaru();

        MonitoramentoResponse m = serviceDepoisDe(Duration.ofMinutes(6)).monitorar(REQUISICAO);

        assertEquals(List.of(0.0, 18.0, 58.0, 93.0, 143.0), m.kmAcumulado());
    }

    // uma leitura por minuto desde o envio; a ultima e a leitura atual.
    @Test
    void historicoTemUmaLeituraPorMinutoDesdeOEnvio() {
        transporteEmAndamentoDeRecifeParaCaruaru();

        MonitoramentoResponse m = serviceDepoisDe(Duration.ofMinutes(6)).monitorar(REQUISICAO);

        List<LeituraTemperatura> historico = m.historico();
        assertEquals(7, historico.size()); // minutos 0, 1, ..., 6
        assertEquals(ENVIO, historico.get(0).instante());
        assertEquals(m.atualizadoEm(), historico.get(6).instante());
        assertEquals(m.temperaturaC(), historico.get(6).temperaturaC());
    }

    // viagem longa: o grafico mostra so a ultima hora.
    @Test
    void historicoGuardaNoMaximo60Leituras() {
        transporteEmAndamentoDeRecifeParaCaruaru();

        MonitoramentoResponse m = serviceDepoisDe(Duration.ofMinutes(90)).monitorar(REQUISICAO);

        assertEquals(60, m.historico().size());
        assertEquals(m.atualizadoEm(), m.historico().get(59).instante());
    }
}
