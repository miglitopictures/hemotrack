package com.hemotrack.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.hemotrack.backend.exception.ConflitoException;
import com.hemotrack.backend.exception.DadosInvalidosException;
import com.hemotrack.backend.exception.NaoEncontradoException;
import com.hemotrack.backend.model.instituicao.Instituicao;
import com.hemotrack.backend.model.instituicao.StatusInstituicao;
import com.hemotrack.backend.model.instituicao.TipoInstituicao;
import com.hemotrack.backend.model.sangue.Hemocomponente;
import com.hemotrack.backend.model.sangue.SituacaoBolsa;
import com.hemotrack.backend.model.sangue.StatusHemocomponente;
import com.hemotrack.backend.model.sangue.TipoHemocomponente;
import com.hemotrack.backend.model.sangue.dto.HemocomponenteRequest;
import com.hemotrack.backend.model.sangue.dto.HemocomponenteResponse;
import com.hemotrack.backend.model.shared.FatorRh;
import com.hemotrack.backend.model.shared.TipoABO;
import com.hemotrack.backend.model.usuario.Papel;
import com.hemotrack.backend.model.usuario.UsuarioAutenticado;
import com.hemotrack.backend.repositories.HemocomponenteRepository;
import com.hemotrack.backend.repositories.InstituicaoRepository;

/**
 * HU02: o hemocentro cadastra e visualiza as bolsas do seu estoque.
 * Situacao exibida: Disponivel, Reservada, Utilizada, Vencida (e Descartada).
 */
@ExtendWith(MockitoExtension.class)
class EstoqueServiceTest {

    private static final Long HEMOCENTRO = 7L;
    private static final Long HOSPITAL = 3L;
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 9);

    @Mock
    private InstituicaoRepository instituicaoRepository;

    @Mock
    private HemocomponenteRepository hemocomponenteRepository;

    private EstoqueService service;

    private final UsuarioAutenticado operadorDoHemocentro = new UsuarioAutenticado(1L, HEMOCENTRO, Papel.OPERADOR);

    @BeforeEach
    void montarService() {
        Clock relogio = Clock.fixed(Instant.parse("2026-11-09T12:00:00Z"), ZoneOffset.UTC);
        service = new EstoqueService(instituicaoRepository, hemocomponenteRepository, relogio);
    }

    // ---------- montagem dos dados ----------

    private void existeInstituicao(Long id, TipoInstituicao tipo) {
        Instituicao i = new Instituicao("Instituicao " + id, "1111111100011" + id, tipo, "Rua A, 1", "Recife", null);
        i.setStatus(StatusInstituicao.APROVADA);
        when(instituicaoRepository.findById(id)).thenReturn(Optional.of(i));
    }

    private HemocomponenteRequest pedido(String codigo, LocalDate coleta, LocalDate validade) {
        return new HemocomponenteRequest(codigo, TipoHemocomponente.HEMACIAS, TipoABO.O, FatorRh.NEGATIVO,
            coleta, validade, null);
    }

    private void saveDevolveOQueRecebeu() {
        when(hemocomponenteRepository.save(any(Hemocomponente.class))).thenAnswer(chamada -> chamada.getArgument(0));
    }

    private Hemocomponente bolsa(String codigo, StatusHemocomponente status, int diasParaVencer) {
        Hemocomponente h = new Hemocomponente();
        h.setCodigoBolsa(codigo);
        h.setInstituicaoAtualId(HEMOCENTRO);
        h.setTipo(TipoHemocomponente.HEMACIAS);
        h.setAbo(TipoABO.A);
        h.setRh(FatorRh.POSITIVO);
        h.setVolumeMl(300);
        h.setDataColeta(HOJE.minusDays(10));
        h.setDataValidade(HOJE.plusDays(diasParaVencer));
        h.setStatus(status);
        return h;
    }

    // ---------- cenario 1: cadastrar hemocomponente ----------

    @Test
    void cadastrarBolsaValida_nasceDisponivel() {
        existeInstituicao(HEMOCENTRO, TipoInstituicao.HEMOCENTRO);
        saveDevolveOQueRecebeu();

        HemocomponenteResponse criada = service.cadastrar(operadorDoHemocentro, HEMOCENTRO,
            pedido("  hmp-2026-0001 ", HOJE.minusDays(1), HOJE.plusDays(34)));

        assertEquals("HMP-2026-0001", criada.codigoBolsa()); // sem espacos, maiusculo
        assertEquals(StatusHemocomponente.DISPONIVEL, criada.status());
        assertEquals(SituacaoBolsa.DISPONIVEL, criada.situacao());
        assertEquals(HEMOCENTRO, criada.instituicaoId());
        assertEquals(300.0, criada.volumeMl()); // volume padrao de hemacias quando nao informado
        assertEquals(34, criada.diasParaVencer());
    }

    @Test
    void cadastrarCodigoJaExistente_409() {
        existeInstituicao(HEMOCENTRO, TipoInstituicao.HEMOCENTRO);
        when(hemocomponenteRepository.existsByCodigoBolsa("HMP-1")).thenReturn(true);

        assertThrows(ConflitoException.class, () -> service.cadastrar(operadorDoHemocentro, HEMOCENTRO,
            pedido("hmp-1", HOJE.minusDays(1), HOJE.plusDays(30))));
        verify(hemocomponenteRepository, never()).save(any());
    }

    @Test
    void cadastrarComColetaNoFuturo_400() {
        existeInstituicao(HEMOCENTRO, TipoInstituicao.HEMOCENTRO);

        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class, () -> service.cadastrar(
            operadorDoHemocentro, HEMOCENTRO, pedido("HMP-1", HOJE.plusDays(1), HOJE.plusDays(30))));
        assertEquals("dataColeta", erro.getCampo());
    }

    @Test
    void cadastrarComValidadeAntesDaColeta_400() {
        existeInstituicao(HEMOCENTRO, TipoInstituicao.HEMOCENTRO);

        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class, () -> service.cadastrar(
            operadorDoHemocentro, HEMOCENTRO, pedido("HMP-1", HOJE.minusDays(5), HOJE.minusDays(6))));
        assertEquals("dataValidade", erro.getCampo());
    }

    // uma bolsa que ja chega vencida nao entra no estoque
    @Test
    void cadastrarBolsaJaVencida_400() {
        existeInstituicao(HEMOCENTRO, TipoInstituicao.HEMOCENTRO);

        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class, () -> service.cadastrar(
            operadorDoHemocentro, HEMOCENTRO, pedido("HMP-1", HOJE.minusDays(40), HOJE)));
        assertEquals("dataValidade", erro.getCampo());
    }

    @Test
    void hospitalNaoCadastraBolsa_403() {
        existeInstituicao(HOSPITAL, TipoInstituicao.HOSPITAL);
        UsuarioAutenticado operadorDoHospital = new UsuarioAutenticado(2L, HOSPITAL, Papel.OPERADOR);

        assertThrows(AccessDeniedException.class, () -> service.cadastrar(operadorDoHospital, HOSPITAL,
            pedido("HMP-1", HOJE.minusDays(1), HOJE.plusDays(30))));
    }

    @Test
    void naoCadastraNoEstoqueDeOutraInstituicao_403() {
        existeInstituicao(99L, TipoInstituicao.HEMOCENTRO);

        assertThrows(AccessDeniedException.class, () -> service.cadastrar(operadorDoHemocentro, 99L,
            pedido("HMP-1", HOJE.minusDays(1), HOJE.plusDays(30))));
    }

    // ---------- cenario 2 e 3: visualizar estoque, vencida identificada ----------

    @Test
    void listarEstoque_mostraCadaBolsaComSuaSituacao() {
        existeInstituicao(HEMOCENTRO, TipoInstituicao.HEMOCENTRO);
        when(hemocomponenteRepository.findByInstituicaoAtualId(HEMOCENTRO)).thenReturn(new ArrayList<>(List.of(
            bolsa("DISPONIVEL", StatusHemocomponente.DISPONIVEL, 20),
            bolsa("RESERVADA", StatusHemocomponente.RESERVADO, 15),
            bolsa("UTILIZADA", StatusHemocomponente.TRANSFUNDIDO, 10),
            bolsa("VENCIDA", StatusHemocomponente.DISPONIVEL, 0), // vence hoje: ja nao serve
            bolsa("DESCARTADA", StatusHemocomponente.DESCARTADO, 5)
        )));

        List<HemocomponenteResponse> estoque = service.listar(operadorDoHemocentro, HEMOCENTRO);

        assertThat(estoque).extracting(HemocomponenteResponse::codigoBolsa, HemocomponenteResponse::situacao)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("DISPONIVEL", SituacaoBolsa.DISPONIVEL),
                org.assertj.core.groups.Tuple.tuple("RESERVADA", SituacaoBolsa.RESERVADA),
                org.assertj.core.groups.Tuple.tuple("UTILIZADA", SituacaoBolsa.UTILIZADA),
                org.assertj.core.groups.Tuple.tuple("VENCIDA", SituacaoBolsa.VENCIDA),
                org.assertj.core.groups.Tuple.tuple("DESCARTADA", SituacaoBolsa.DESCARTADA));
    }

    // FEFO: o que vence primeiro aparece primeiro
    @Test
    void listarEstoque_ordenaPelaValidadeMaisProxima() {
        existeInstituicao(HEMOCENTRO, TipoInstituicao.HEMOCENTRO);
        when(hemocomponenteRepository.findByInstituicaoAtualId(HEMOCENTRO)).thenReturn(new ArrayList<>(List.of(
            bolsa("C", StatusHemocomponente.DISPONIVEL, 30),
            bolsa("A", StatusHemocomponente.DISPONIVEL, 2),
            bolsa("B", StatusHemocomponente.DISPONIVEL, 10)
        )));

        List<HemocomponenteResponse> estoque = service.listar(operadorDoHemocentro, HEMOCENTRO);

        assertThat(estoque).extracting(HemocomponenteResponse::codigoBolsa).containsExactly("A", "B", "C");
    }

    @Test
    void listarEstoqueDeInstituicaoInexistente_404() {
        when(instituicaoRepository.findById(HEMOCENTRO)).thenReturn(Optional.empty());

        assertThrows(NaoEncontradoException.class, () -> service.listar(operadorDoHemocentro, HEMOCENTRO));
    }
}
