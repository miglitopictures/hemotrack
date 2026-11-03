package com.hemotrack.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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

import com.hemotrack.backend.model.instituicao.Instituicao;
import com.hemotrack.backend.model.instituicao.TipoInstituicao;
import com.hemotrack.backend.model.requisicao.Requisicao;
import com.hemotrack.backend.model.requisicao.StatusRequisicao;
import com.hemotrack.backend.model.requisicao.dto.RotaResponse;
import com.hemotrack.backend.repositories.InstituicaoRepository;
import com.hemotrack.backend.repositories.RequisicaoRepository;

/**
 * HU07: planejar a distribuicao do hemocentro ate o hospital da requisicao.
 */
@ExtendWith(MockitoExtension.class)
class RotaServiceTest {

    private static final Long REQUISICAO = 1L;
    private static final Long HEMOCENTRO = 7L;
    private static final Long HOSPITAL = 3L;

    @Mock
    private RequisicaoRepository requisicaoRepository;

    @Mock
    private InstituicaoRepository instituicaoRepository;

    private RotaService service;

    // o mapa e real (nao mock): ele e fixo e ja tem os proprios testes.
    @BeforeEach
    void montarService() {
        service = new RotaService(requisicaoRepository, instituicaoRepository, new MapaTransporte());
    }

    // ---------- montagem dos dados ----------

    private void existeRequisicao(StatusRequisicao status) {
        Requisicao r = new Requisicao();
        r.setId(REQUISICAO);
        r.setHospitalId(HOSPITAL);
        r.setHemocentroId(HEMOCENTRO);
        r.setStatus(status);
        when(requisicaoRepository.findById(REQUISICAO)).thenReturn(Optional.of(r));
    }

    private void existemInstituicoes(String municipioHemocentro, String municipioHospital) {
        Instituicao hemocentro = new Instituicao("HEMOPE", "11111111000111", TipoInstituicao.HEMOCENTRO,
            "Rua Joaquim Nabuco, 171", municipioHemocentro, null);
        Instituicao hospital = new Instituicao("Hospital Regional", "22222222000122", TipoInstituicao.HOSPITAL,
            "Av. Principal, 100", municipioHospital, null);

        when(instituicaoRepository.findById(HEMOCENTRO)).thenReturn(Optional.of(hemocentro));
        when(instituicaoRepository.findById(HOSPITAL)).thenReturn(Optional.of(hospital));
    }

    // ---------- cenario 1: planejar distribuicao ----------

    @Test
    void requisicaoAlocadaRecebeRotaDoHemocentroAteOHospital() {
        existeRequisicao(StatusRequisicao.ALOCADA);
        existemInstituicoes("Recife", "Caruaru");

        RotaResponse rota = service.planejar(REQUISICAO);

        assertEquals(REQUISICAO, rota.requisicaoId());
        assertEquals("HEMOPE", rota.origem().instituicao());
        assertEquals("Recife", rota.origem().municipio());
        assertEquals("Hospital Regional", rota.destino().instituicao());
        assertEquals("Caruaru", rota.destino().municipio());

        List<String> locais = rota.locais();
        assertEquals("Recife", locais.get(0));
        assertEquals("Caruaru", locais.get(locais.size() - 1));
        assertTrue(rota.distanciaKm() > 0);
    }

    // municipio vem de texto livre no cadastro; a resposta devolve o nome oficial do mapa.
    @Test
    void municipioDigitadoSemAcentoEncontraONomeOficial() {
        existeRequisicao(StatusRequisicao.ALOCADA);
        existemInstituicoes("RECIFE", "jaboatao dos guararapes");

        RotaResponse rota = service.planejar(REQUISICAO);

        assertEquals("Recife", rota.origem().municipio());
        assertEquals("Jaboatão dos Guararapes", rota.destino().municipio());
    }

    // ---------- cenario 2: requisicao que ainda nao pode ser distribuida ----------

    @ParameterizedTest
    @EnumSource(value = StatusRequisicao.class, names = "ALOCADA", mode = EnumSource.Mode.EXCLUDE)
    void requisicaoQueNaoEstaAlocadaNaoTemRota(StatusRequisicao status) {
        existeRequisicao(status);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
            () -> service.planejar(REQUISICAO));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    // ---------- erros ----------

    @Test
    void requisicaoInexistenteRetorna404() {
        when(requisicaoRepository.findById(REQUISICAO)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
            () -> service.planejar(REQUISICAO));
        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }

    @Test
    void instituicaoInexistenteRetorna404() {
        existeRequisicao(StatusRequisicao.ALOCADA);
        when(instituicaoRepository.findById(HEMOCENTRO)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
            () -> service.planejar(REQUISICAO));
        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }

    @Test
    void municipioForaDoMapaRetorna422ComONomeDoMunicipio() {
        existeRequisicao(StatusRequisicao.ALOCADA);
        existemInstituicoes("Recife", "Manaus");

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
            () -> service.planejar(REQUISICAO));
        assertEquals(422, erro.getStatusCode().value());
        assertTrue(erro.getReason().contains("Manaus"));
    }
}
