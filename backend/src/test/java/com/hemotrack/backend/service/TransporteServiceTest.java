package com.hemotrack.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.hemotrack.backend.model.requisicao.Requisicao;
import com.hemotrack.backend.model.requisicao.StatusRequisicao;
import com.hemotrack.backend.model.sangue.Hemocomponente;
import com.hemotrack.backend.model.sangue.StatusHemocomponente;
import com.hemotrack.backend.repositories.HemocomponenteRepository;
import com.hemotrack.backend.repositories.RequisicaoRepository;

/**
 * HU08: o hemocentro despacha o lote (EM_TRANSITO) e o hospital confirma a entrega (ATENDIDA).
 */
@ExtendWith(MockitoExtension.class)
class TransporteServiceTest {

    private static final Long REQUISICAO = 1L;
    private static final Long HEMOCENTRO = 7L;
    private static final Long HOSPITAL = 3L;
    private static final Instant AGORA = Instant.parse("2026-11-09T12:00:00Z");

    @Mock
    private RequisicaoRepository requisicaoRepository;

    @Mock
    private HemocomponenteRepository hemocomponenteRepository;

    private TransporteService service;

    // relogio fixo: o teste sabe exatamente qual horario o service vai gravar.
    @BeforeEach
    void montarService() {
        Clock relogio = Clock.fixed(AGORA, ZoneOffset.UTC);
        service = new TransporteService(requisicaoRepository, hemocomponenteRepository, relogio);
    }

    // ---------- montagem dos dados ----------

    private Requisicao existeRequisicao(StatusRequisicao status) {
        Requisicao r = new Requisicao();
        r.setId(REQUISICAO);
        r.setHospitalId(HOSPITAL);
        r.setHemocentroId(HEMOCENTRO);
        r.setStatus(status);
        when(requisicaoRepository.findById(REQUISICAO)).thenReturn(Optional.of(r));
        return r;
    }

    private Hemocomponente bolsaReservada(boolean emTransito) {
        Hemocomponente h = new Hemocomponente();
        h.setInstituicaoAtualId(HEMOCENTRO);
        h.setStatus(StatusHemocomponente.RESERVADO);
        h.setRequisicaoAlocadaId(REQUISICAO);
        h.setEmTransito(emTransito);
        return h;
    }

    private List<Hemocomponente> bolsasDaRequisicao(boolean emTransito) {
        List<Hemocomponente> bolsas = List.of(bolsaReservada(emTransito), bolsaReservada(emTransito));
        when(hemocomponenteRepository.findByRequisicaoAlocadaId(REQUISICAO)).thenReturn(bolsas);
        return bolsas;
    }

    private void saveDevolveOQueRecebeu() {
        when(requisicaoRepository.save(any(Requisicao.class))).thenAnswer(chamada -> chamada.getArgument(0));
    }

    // ---------- enviar ----------

    @Test
    void enviarRequisicaoAlocada_ficaEmTransitoComHorarioDeEnvio() {
        existeRequisicao(StatusRequisicao.ALOCADA);
        bolsasDaRequisicao(false);
        saveDevolveOQueRecebeu();

        Requisicao enviada = service.enviar(REQUISICAO);

        assertEquals(StatusRequisicao.EM_TRANSITO, enviada.getStatus());
        assertEquals(AGORA, enviada.getDataEnvio());
    }

    @Test
    void enviar_marcaAsBolsasDaRequisicaoComoEmTransito() {
        existeRequisicao(StatusRequisicao.ALOCADA);
        List<Hemocomponente> bolsas = bolsasDaRequisicao(false);
        saveDevolveOQueRecebeu();

        service.enviar(REQUISICAO);

        for (Hemocomponente bolsa : bolsas) {
            assertTrue(bolsa.isEmTransito());
        }
        verify(hemocomponenteRepository).saveAll(bolsas);
    }

    @ParameterizedTest
    @EnumSource(value = StatusRequisicao.class, names = "ALOCADA", mode = EnumSource.Mode.EXCLUDE)
    void soEnviaRequisicaoAlocada(StatusRequisicao status) {
        existeRequisicao(status);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
            () -> service.enviar(REQUISICAO));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    void enviarRequisicaoInexistente_retorna404() {
        when(requisicaoRepository.findById(REQUISICAO)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
            () -> service.enviar(REQUISICAO));
        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }

    // ---------- receber (cenario 3: entrega concluida) ----------

    @Test
    void receberRequisicaoEmTransito_ficaAtendidaComHorarioDeEntrega() {
        existeRequisicao(StatusRequisicao.EM_TRANSITO);
        bolsasDaRequisicao(true);
        saveDevolveOQueRecebeu();

        Requisicao recebida = service.receber(REQUISICAO);

        assertEquals(StatusRequisicao.ATENDIDA, recebida.getStatus());
        assertEquals(AGORA, recebida.getDataEntrega());
    }

    @Test
    void receber_bolsasChegamAoEstoqueDoHospital() {
        existeRequisicao(StatusRequisicao.EM_TRANSITO);
        List<Hemocomponente> bolsas = bolsasDaRequisicao(true);
        saveDevolveOQueRecebeu();

        service.receber(REQUISICAO);

        for (Hemocomponente bolsa : bolsas) {
            assertFalse(bolsa.isEmTransito());
            assertEquals(HOSPITAL, bolsa.getInstituicaoAtualId());
            // continua reservada para o paciente da requisicao ate ser transfundida
            assertEquals(StatusHemocomponente.RESERVADO, bolsa.getStatus());
        }
        verify(hemocomponenteRepository).saveAll(bolsas);
    }

    @ParameterizedTest
    @EnumSource(value = StatusRequisicao.class, names = "EM_TRANSITO", mode = EnumSource.Mode.EXCLUDE)
    void soRecebeRequisicaoEmTransito(StatusRequisicao status) {
        existeRequisicao(status);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
            () -> service.receber(REQUISICAO));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }
}
