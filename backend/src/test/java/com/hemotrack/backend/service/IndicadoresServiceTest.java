package com.hemotrack.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hemotrack.backend.model.indicadores.dto.IndicadoresResponse;
import com.hemotrack.backend.model.indicadores.dto.IndicadoresResponse.DemandaComponente;
import com.hemotrack.backend.model.indicadores.dto.IndicadoresResponse.EstoqueTipoSanguineo;
import com.hemotrack.backend.model.requisicao.Requisicao;
import com.hemotrack.backend.model.requisicao.StatusRequisicao;
import com.hemotrack.backend.model.sangue.Hemocomponente;
import com.hemotrack.backend.model.sangue.StatusHemocomponente;
import com.hemotrack.backend.model.sangue.TipoHemocomponente;
import com.hemotrack.backend.model.shared.FatorRh;
import com.hemotrack.backend.model.shared.TipoABO;
import com.hemotrack.backend.repositories.HemocomponenteRepository;
import com.hemotrack.backend.repositories.RequisicaoRepository;

/**
 * HU09: painel de indicadores da operacao (estoque, requisicoes e distribuicao).
 * Tudo e calculado na hora a partir do banco, entao o painel sempre reflete o estado atual.
 */
@ExtendWith(MockitoExtension.class)
class IndicadoresServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-09T12:00:00Z");
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 9);

    @Mock
    private RequisicaoRepository requisicaoRepository;

    @Mock
    private HemocomponenteRepository hemocomponenteRepository;

    private IndicadoresService service;

    @BeforeEach
    void montarService() {
        service = new IndicadoresService(requisicaoRepository, hemocomponenteRepository,
            Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    // ---------- montagem dos dados ----------

    private Hemocomponente bolsa(TipoABO abo, FatorRh rh, int diasParaVencer) {
        Hemocomponente h = new Hemocomponente();
        h.setTipo(TipoHemocomponente.HEMACIAS);
        h.setAbo(abo);
        h.setRh(rh);
        h.setStatus(StatusHemocomponente.DISPONIVEL);
        h.setDataValidade(HOJE.plusDays(diasParaVencer));
        return h;
    }

    private void estoque(Hemocomponente... bolsas) {
        when(hemocomponenteRepository.findByStatus(StatusHemocomponente.DISPONIVEL)).thenReturn(List.of(bolsas));
    }

    private Requisicao requisicao(TipoHemocomponente tipo, StatusRequisicao status) {
        Requisicao r = new Requisicao();
        r.setTipo(tipo);
        r.setStatus(status);
        return r;
    }

    private void requisicoes(Requisicao... lista) {
        when(requisicaoRepository.findAll()).thenReturn(new ArrayList<>(List.of(lista)));
    }

    // ---------- estoque ----------

    @Test
    void bolsasDisponiveis_naoContaAsVencidas() {
        estoque(
            bolsa(TipoABO.O, FatorRh.NEGATIVO, 30),
            bolsa(TipoABO.A, FatorRh.POSITIVO, 10),
            bolsa(TipoABO.B, FatorRh.POSITIVO, 0) // vence hoje: ja nao serve
        );

        IndicadoresResponse indicadores = service.calcular();

        assertEquals(2, indicadores.bolsasDisponiveis());
    }

    @Test
    void bolsasProximasDoVencimento_saoAsQueVencemEmAte7Dias() {
        estoque(
            bolsa(TipoABO.O, FatorRh.NEGATIVO, 3),
            bolsa(TipoABO.O, FatorRh.NEGATIVO, 7),
            bolsa(TipoABO.O, FatorRh.NEGATIVO, 8),
            bolsa(TipoABO.O, FatorRh.NEGATIVO, 30)
        );

        IndicadoresResponse indicadores = service.calcular();

        assertEquals(2, indicadores.bolsasVencendo());
        assertEquals(7, indicadores.diasParaVencimento());
    }

    @Test
    void estoquePorTipoSanguineo_mostraOs8TiposMesmoSemBolsa() {
        estoque(
            bolsa(TipoABO.O, FatorRh.NEGATIVO, 30),
            bolsa(TipoABO.O, FatorRh.NEGATIVO, 30),
            bolsa(TipoABO.A, FatorRh.POSITIVO, 30)
        );

        List<EstoqueTipoSanguineo> porTipo = service.calcular().estoquePorTipoSanguineo();

        assertEquals(8, porTipo.size());
        assertEquals(new EstoqueTipoSanguineo("A+", 1), porTipo.get(0));
        assertEquals(new EstoqueTipoSanguineo("B+", 0), porTipo.get(2));
        assertEquals(new EstoqueTipoSanguineo("O-", 2), porTipo.get(7));
    }

    // ---------- requisicoes e distribuicao ----------

    @Test
    void requisicoesPendentesEmTransporteEEntregues() {
        requisicoes(
            requisicao(TipoHemocomponente.HEMACIAS, StatusRequisicao.ABERTA),
            requisicao(TipoHemocomponente.HEMACIAS, StatusRequisicao.ABERTA),
            requisicao(TipoHemocomponente.PLASMA, StatusRequisicao.EM_TRANSITO),
            requisicao(TipoHemocomponente.PLAQUETAS, StatusRequisicao.ATENDIDA),
            requisicao(TipoHemocomponente.PLAQUETAS, StatusRequisicao.ACEITA)
        );

        IndicadoresResponse indicadores = service.calcular();

        assertEquals(2, indicadores.requisicoesPendentes());
        assertEquals(1, indicadores.requisicoesEmTransporte());
        assertEquals(1, indicadores.requisicoesEntregues());
    }

    @Test
    void componentesMaisSolicitados_doMaisPedidoParaOMenos() {
        requisicoes(
            requisicao(TipoHemocomponente.PLASMA, StatusRequisicao.ABERTA),
            requisicao(TipoHemocomponente.HEMACIAS, StatusRequisicao.ATENDIDA),
            requisicao(TipoHemocomponente.HEMACIAS, StatusRequisicao.ABERTA),
            requisicao(TipoHemocomponente.HEMACIAS, StatusRequisicao.RECUSADA)
        );

        List<DemandaComponente> demanda = service.calcular().demandaPorComponente();

        assertEquals(4, demanda.size()); // todos os componentes aparecem, mesmo sem pedido
        assertEquals(new DemandaComponente(TipoHemocomponente.HEMACIAS, 3, 1), demanda.get(0));
        assertEquals(new DemandaComponente(TipoHemocomponente.PLASMA, 1, 0), demanda.get(1));
        assertEquals(0, demanda.get(3).pedidos());
    }

    @Test
    void semDadosNenhum_tudoZeradoSemErro() {
        IndicadoresResponse indicadores = service.calcular();

        assertEquals(0, indicadores.bolsasDisponiveis());
        assertEquals(0, indicadores.requisicoesPendentes());
        assertEquals(8, indicadores.estoquePorTipoSanguineo().size());
        assertEquals(AGORA, indicadores.geradoEm());
    }
}
