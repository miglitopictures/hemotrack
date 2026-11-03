package com.hemotrack.backend.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.hemotrack.backend.exception.ConflitoException;
import com.hemotrack.backend.exception.DadosInvalidosException;
import com.hemotrack.backend.exception.NaoEncontradoException;
import com.hemotrack.backend.model.instituicao.Instituicao;
import com.hemotrack.backend.model.instituicao.StatusInstituicao;
import com.hemotrack.backend.model.instituicao.TipoInstituicao;
import com.hemotrack.backend.model.sangue.Hemocomponente;
import com.hemotrack.backend.model.sangue.StatusHemocomponente;
import com.hemotrack.backend.model.sangue.TipoHemocomponente;
import com.hemotrack.backend.model.sangue.dto.HemocomponenteRequest;
import com.hemotrack.backend.model.sangue.dto.HemocomponenteResponse;
import com.hemotrack.backend.model.usuario.Papel;
import com.hemotrack.backend.model.usuario.UsuarioAutenticado;
import com.hemotrack.backend.repositories.HemocomponenteRepository;
import com.hemotrack.backend.repositories.InstituicaoRepository;

/**
 * HU02: estoque de hemocomponentes. O hemocentro cadastra bolsas (nascem DISPONIVEL) e ve o
 * proprio estoque com a situacao de cada bolsa, inclusive as vencidas.
 */
@Service
public class EstoqueService {

    private final InstituicaoRepository instituicaoRepository;
    private final HemocomponenteRepository hemocomponenteRepository;
    private final Clock relogio;

    public EstoqueService(InstituicaoRepository instituicaoRepository,
                          HemocomponenteRepository hemocomponenteRepository,
                          Clock relogio) {
        this.instituicaoRepository = instituicaoRepository;
        this.hemocomponenteRepository = hemocomponenteRepository;
        this.relogio = relogio;
    }

    // cenario 2 e 3: bolsas da instituicao, a que vence primeiro no topo (FEFO)
    public List<HemocomponenteResponse> listar(UsuarioAutenticado autenticado, Long instituicaoId) {
        exigirAcesso(autenticado, instituicaoId);
        LocalDate hoje = LocalDate.now(relogio);

        List<Hemocomponente> bolsas = new ArrayList<>(hemocomponenteRepository.findByInstituicaoAtualId(instituicaoId));
        bolsas.sort(Comparator.comparing(Hemocomponente::getDataValidade));

        List<HemocomponenteResponse> estoque = new ArrayList<>();
        for (Hemocomponente bolsa : bolsas) {
            estoque.add(HemocomponenteResponse.de(bolsa, hoje));
        }
        return estoque;
    }

    // cenario 1: bolsa valida entra no estoque como DISPONIVEL
    public HemocomponenteResponse cadastrar(UsuarioAutenticado autenticado, Long instituicaoId,
                                            HemocomponenteRequest dados) {
        Instituicao instituicao = exigirAcesso(autenticado, instituicaoId);
        if (instituicao.getTipo() != TipoInstituicao.HEMOCENTRO) {
            throw new AccessDeniedException("Só hemocentro cadastra bolsas no estoque.");
        }

        LocalDate hoje = LocalDate.now(relogio);
        if (dados.dataColeta().isAfter(hoje)) {
            throw new DadosInvalidosException("dataColeta", "A data de coleta não pode estar no futuro.");
        }
        if (!dados.dataValidade().isAfter(dados.dataColeta())) {
            throw new DadosInvalidosException("dataValidade", "A validade precisa ser posterior à data de coleta.");
        }
        if (!dados.dataValidade().isAfter(hoje)) {
            throw new DadosInvalidosException("dataValidade", "A bolsa já está vencida.");
        }

        // o mesmo codigo digitado com espaco ou minusculo e a mesma bolsa
        String codigo = dados.codigoBolsa().trim().toUpperCase(Locale.ROOT);
        if (hemocomponenteRepository.existsByCodigoBolsa(codigo)) {
            throw ConflitoException.codigoBolsaEmUso(codigo);
        }

        Hemocomponente bolsa = new Hemocomponente();
        bolsa.setCodigoBolsa(codigo);
        bolsa.setInstituicaoAtualId(instituicaoId);
        bolsa.setTipo(dados.tipo());
        bolsa.setAbo(dados.abo());
        bolsa.setRh(dados.rh());
        bolsa.setVolumeMl(dados.volumeMl() != null ? dados.volumeMl() : volumePadrao(dados.tipo()));
        bolsa.setDataColeta(dados.dataColeta());
        bolsa.setDataValidade(dados.dataValidade());
        bolsa.setStatus(StatusHemocomponente.DISPONIVEL);

        return HemocomponenteResponse.de(hemocomponenteRepository.save(bolsa), hoje);
    }

    // mesmo criterio do MembroService: so a propria instituicao (aprovada), ou o ADMIN_SISTEMA
    private Instituicao exigirAcesso(UsuarioAutenticado autenticado, Long instituicaoId) {
        Instituicao instituicao = instituicaoRepository.findById(instituicaoId)
            .orElseThrow(() -> NaoEncontradoException.instituicao(instituicaoId));

        if (autenticado.papel() == Papel.ADMIN_SISTEMA) {
            return instituicao;
        }
        if (!instituicaoId.equals(autenticado.instituicaoId())) {
            throw new AccessDeniedException("Você só acessa o estoque da sua própria instituição.");
        }
        if (instituicao.getStatus() != StatusInstituicao.APROVADA) {
            throw new AccessDeniedException("A instituição ainda não foi aprovada.");
        }
        return instituicao;
    }

    // volume de uma bolsa padrao de cada componente (o front converte bolsas <-> mL com os mesmos valores)
    private static double volumePadrao(TipoHemocomponente tipo) {
        return switch (tipo) {
            case HEMACIAS -> 300;
            case PLASMA -> 200;
            case PLAQUETAS -> 50;
            case CRIOPRECIPITADO -> 15;
        };
    }
}
