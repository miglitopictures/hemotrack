package com.hemotrack.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.hemotrack.backend.model.requisicao.Requisicao;
import com.hemotrack.backend.model.requisicao.StatusRequisicao;
import com.hemotrack.backend.model.sangue.Hemocomponente;
import com.hemotrack.backend.model.sangue.StatusHemocomponente;
import com.hemotrack.backend.model.sangue.TipoHemocomponente;
import com.hemotrack.backend.model.shared.FatorRh;
import com.hemotrack.backend.model.shared.TipoABO;
import com.hemotrack.backend.repositories.HemocomponenteRepository;
import com.hemotrack.backend.repositories.RequisicaoRepository;

@ExtendWith(MockitoExtension.class)
class SelecaoServiceTest {

    private static final Long HEMOCENTRO = 7L;

    @Mock
    private RequisicaoRepository requisicaoRepository;

    @Mock
    private HemocomponenteRepository hemocomponenteRepository;

    @InjectMocks
    private SelecaoService service;

    // ---------- montagem dos dados ----------

    // requisicao aceita de hemacias A+ pelo HEMOCENTRO.
    private Requisicao requisicaoAceita(double volumeMl) {
        Requisicao r = new Requisicao();
        r.setId(1L);
        r.setTipo(TipoHemocomponente.HEMACIAS);
        r.setAbo(TipoABO.A);
        r.setRh(FatorRh.POSITIVO);
        r.setvolumeMl(volumeMl);
        r.setStatus(StatusRequisicao.ACEITA);
        r.setHemocentroId(HEMOCENTRO);
        return r;
    }

    private Hemocomponente bolsa(TipoHemocomponente tipo, TipoABO abo, FatorRh rh, int diasParaVencer, double volumeMl) {
        Hemocomponente h = new Hemocomponente();
        h.setTipo(tipo);
        h.setAbo(abo);
        h.setRh(rh);
        h.setInstituicaoAtualId(HEMOCENTRO);
        h.setVolumeMl(volumeMl);
        h.setDataValidade(LocalDate.now().plusDays(diasParaVencer));
        return h;
    }

    private Hemocomponente hemacias(TipoABO abo, FatorRh rh, int diasParaVencer) {
        return bolsa(TipoHemocomponente.HEMACIAS, abo, rh, diasParaVencer, 300);
    }

    private void estoqueDoHemocentro(List<Hemocomponente> bolsas) {
        when(hemocomponenteRepository.findByInstituicaoAtualIdAndStatus(HEMOCENTRO, StatusHemocomponente.DISPONIVEL))
            .thenReturn(bolsas);
    }

    // ---------- listarCompativeis ----------

    @Test
    void listarCompativeis_requisicaoAindaAberta_conflito() {
        Requisicao aberta = requisicaoAceita(300);
        aberta.setStatus(StatusRequisicao.ABERTA);
        when(requisicaoRepository.findById(1L)).thenReturn(Optional.of(aberta));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
            () -> service.listarCompativeis(1L));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    void listarCompativeis_naoApresentaIncompativelNemOutroTipo() {
        Hemocomponente oNegativo = hemacias(TipoABO.O, FatorRh.NEGATIVO, 10);
        Hemocomponente bPositivo = hemacias(TipoABO.B, FatorRh.POSITIVO, 10);
        Hemocomponente plasmaA = bolsa(TipoHemocomponente.PLASMA, TipoABO.A, FatorRh.POSITIVO, 10, 200);

        when(requisicaoRepository.findById(1L)).thenReturn(Optional.of(requisicaoAceita(300)));
        estoqueDoHemocentro(List.of(oNegativo, bPositivo, plasmaA));

        assertEquals(List.of(oNegativo), service.listarCompativeis(1L));
    }

    @Test
    void listarCompativeis_naoApresentaVencida() {
        Hemocomponente vencida = hemacias(TipoABO.A, FatorRh.POSITIVO, -1);
        Hemocomponente valida = hemacias(TipoABO.A, FatorRh.POSITIVO, 5);

        when(requisicaoRepository.findById(1L)).thenReturn(Optional.of(requisicaoAceita(300)));
        estoqueDoHemocentro(List.of(vencida, valida));

        assertEquals(List.of(valida), service.listarCompativeis(1L));
    }

    @Test
    void listarCompativeis_priorizaValidadeMaisProxima() {
        Hemocomponente vence20 = hemacias(TipoABO.A, FatorRh.POSITIVO, 20);
        Hemocomponente vence3 = hemacias(TipoABO.O, FatorRh.NEGATIVO, 3);
        Hemocomponente vence10 = hemacias(TipoABO.A, FatorRh.NEGATIVO, 10);

        when(requisicaoRepository.findById(1L)).thenReturn(Optional.of(requisicaoAceita(300)));
        estoqueDoHemocentro(List.of(vence20, vence3, vence10));

        assertEquals(List.of(vence3, vence10, vence20), service.listarCompativeis(1L));
    }

    // ---------- alocar ----------

    @Test
    void alocar_reservaBolsasEMarcaRequisicaoAlocada() {
        Hemocomponente b1 = hemacias(TipoABO.A, FatorRh.POSITIVO, 10);
        Hemocomponente b2 = hemacias(TipoABO.O, FatorRh.NEGATIVO, 5);
        List<Long> ids = List.of(10L, 11L);

        when(requisicaoRepository.findById(1L)).thenReturn(Optional.of(requisicaoAceita(500)));
        when(hemocomponenteRepository.findAllById(ids)).thenReturn(List.of(b1, b2));
        when(requisicaoRepository.save(any(Requisicao.class))).thenAnswer(inv -> inv.getArgument(0));

        Requisicao resultado = service.alocar(1L, ids);

        assertEquals(StatusRequisicao.ALOCADA, resultado.getStatus());
        assertEquals(StatusHemocomponente.RESERVADO, b1.getStatus());
        assertEquals(StatusHemocomponente.RESERVADO, b2.getStatus());
        assertEquals(1L, b1.getRequisicaoAlocadaId());
    }

    @Test
    void alocar_volumeInsuficiente_badRequestENaoReserva() {
        Hemocomponente b1 = hemacias(TipoABO.A, FatorRh.POSITIVO, 10); // 300 ml
        List<Long> ids = List.of(10L);

        when(requisicaoRepository.findById(1L)).thenReturn(Optional.of(requisicaoAceita(500)));
        when(hemocomponenteRepository.findAllById(ids)).thenReturn(List.of(b1));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
            () -> service.alocar(1L, ids));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        assertEquals(StatusHemocomponente.DISPONIVEL, b1.getStatus());
    }

    @Test
    void alocar_bolsaJaReservada_conflito() {
        Hemocomponente reservada = hemacias(TipoABO.A, FatorRh.POSITIVO, 10);
        reservada.setStatus(StatusHemocomponente.RESERVADO);
        List<Long> ids = List.of(10L);

        when(requisicaoRepository.findById(1L)).thenReturn(Optional.of(requisicaoAceita(300)));
        when(hemocomponenteRepository.findAllById(ids)).thenReturn(List.of(reservada));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
            () -> service.alocar(1L, ids));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }
}