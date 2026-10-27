package com.hemotrack.backend.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.hemotrack.backend.model.requisicao.Requisicao;
import com.hemotrack.backend.model.requisicao.StatusRequisicao;
import com.hemotrack.backend.model.sangue.Compatibilidade;
import com.hemotrack.backend.model.sangue.Hemocomponente;
import com.hemotrack.backend.model.sangue.StatusHemocomponente;
import com.hemotrack.backend.repositories.HemocomponenteRepository;
import com.hemotrack.backend.repositories.RequisicaoRepository;

/**
 * HU06: seleciona hemocomponentes compativeis para uma requisicao aceita
 * e reserva os escolhidos pelo hemocentro.
 */
@Service
public class SelecaoService {
    private final RequisicaoRepository requisicaoRepository;
    private final HemocomponenteRepository hemocomponenteRepository;

    public SelecaoService(RequisicaoRepository requisicaoRepository,
                          HemocomponenteRepository hemocomponenteRepository) {
        this.requisicaoRepository = requisicaoRepository;
        this.hemocomponenteRepository = hemocomponenteRepository;
    }

    // bolsas do hemocentro que servem para a requisicao, validade mais proxima primeiro (FEFO).
    public List<Hemocomponente> listarCompativeis(Long requisicaoId) {
        Requisicao requisicao = buscarRequisicaoAceita(requisicaoId);
        LocalDate hoje = LocalDate.now();

        List<Hemocomponente> doEstoque = hemocomponenteRepository.findByInstituicaoAtualIdAndStatus(
            requisicao.getHemocentroId(), StatusHemocomponente.DISPONIVEL);

        List<Hemocomponente> compativeis = new ArrayList<>();
        for (Hemocomponente h : doEstoque) {
            if (servePara(h, requisicao, hoje)) {
                compativeis.add(h);
            }
        }

        compativeis.sort(Comparator.comparing(Hemocomponente::getDataValidade));
        return compativeis;
    }

    // reserva as bolsas escolhidas; tudo ou nada (@Transactional).
    @Transactional
    public Requisicao alocar(Long requisicaoId, List<Long> hemocomponenteIds) {
        Requisicao requisicao = buscarRequisicaoAceita(requisicaoId);
        LocalDate hoje = LocalDate.now();

        List<Hemocomponente> selecionados = hemocomponenteRepository.findAllById(hemocomponenteIds);
        if (selecionados.size() != hemocomponenteIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hemocomponente não encontrado");
        }

        // primeiro valida tudo; so depois altera alguma coisa.
        double volumeTotal = 0;
        for (Hemocomponente h : selecionados) {
            if (!h.getInstituicaoAtualId().equals(requisicao.getHemocentroId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Hemocomponente " + h.getId() + " não pertence ao hemocentro da requisição");
            }
            if (!servePara(h, requisicao, hoje)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Hemocomponente " + h.getId() + " não está disponível ou não é compatível");
            }
            volumeTotal += h.getVolumeMl();
        }

        if (volumeTotal < requisicao.getVolumeMl()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Volume selecionado (" + volumeTotal + " ml) não atende ao solicitado ("
                + requisicao.getVolumeMl() + " ml)");
        }

        for (Hemocomponente h : selecionados) {
            h.setStatus(StatusHemocomponente.RESERVADO);
            h.setRequisicaoAlocadaId(requisicao.getId());
        }
        hemocomponenteRepository.saveAll(selecionados);

        requisicao.setStatus(StatusRequisicao.ALOCADA);
        return requisicaoRepository.save(requisicao);
    }

    private Requisicao buscarRequisicaoAceita(Long id) {
        Requisicao requisicao = requisicaoRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisição não encontrada"));

        if (requisicao.getStatus() != StatusRequisicao.ACEITA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Requisição não está aceita");
        }
        return requisicao;
    }

    // mesmo tipo, disponivel (nao reservada e nao vencida) e compativel em ABO/Rh.
    private boolean servePara(Hemocomponente h, Requisicao requisicao, LocalDate hoje) {
        if (h.getTipo() != requisicao.getTipo()) {
            return false;
        }
        if (!h.estaDisponivel(hoje)) {
            return false;
        }
        return Compatibilidade.podeReceber(requisicao.getTipo(),
            h.getAbo(), h.getRh(), requisicao.getAbo(), requisicao.getRh());
    }
}