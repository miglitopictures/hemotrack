package com.hemotrack.backend.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.hemotrack.backend.model.requisicao.Requisicao;
import com.hemotrack.backend.model.requisicao.StatusRequisicao;
import com.hemotrack.backend.model.sangue.Hemocomponente;
import com.hemotrack.backend.repositories.HemocomponenteRepository;
import com.hemotrack.backend.repositories.RequisicaoRepository;

/**
 * HU08: transporte do lote. O hemocentro despacha (ALOCADA -> EM_TRANSITO) e o hospital
 * confirma o recebimento (EM_TRANSITO -> ATENDIDA, o "Entregue" da historia).
 */
@Service
public class TransporteService {

    private final RequisicaoRepository requisicaoRepository;
    private final HemocomponenteRepository hemocomponenteRepository;
    private final Clock relogio;

    public TransporteService(RequisicaoRepository requisicaoRepository,
                             HemocomponenteRepository hemocomponenteRepository,
                             Clock relogio) {
        this.requisicaoRepository = requisicaoRepository;
        this.hemocomponenteRepository = hemocomponenteRepository;
        this.relogio = relogio;
    }

    // despacha o lote: so depois que as bolsas foram selecionadas (HU06).
    @Transactional
    public Requisicao enviar(Long requisicaoId) {
        Requisicao requisicao = buscarComStatus(requisicaoId, StatusRequisicao.ALOCADA);

        List<Hemocomponente> bolsas = hemocomponenteRepository.findByRequisicaoAlocadaId(requisicaoId);
        for (Hemocomponente bolsa : bolsas) {
            bolsa.setEmTransito(true);
        }
        hemocomponenteRepository.saveAll(bolsas);

        requisicao.setStatus(StatusRequisicao.EM_TRANSITO);
        requisicao.setDataEnvio(Instant.now(relogio));
        return requisicaoRepository.save(requisicao);
    }

    // cenario 3: entrega registrada -> transporte finalizado e requisicao "Entregue".
    // as bolsas passam para o estoque do hospital e continuam RESERVADAS ate a transfusao.
    @Transactional
    public Requisicao receber(Long requisicaoId) {
        Requisicao requisicao = buscarComStatus(requisicaoId, StatusRequisicao.EM_TRANSITO);

        List<Hemocomponente> bolsas = hemocomponenteRepository.findByRequisicaoAlocadaId(requisicaoId);
        for (Hemocomponente bolsa : bolsas) {
            bolsa.setEmTransito(false);
            bolsa.setInstituicaoAtualId(requisicao.getHospitalId());
        }
        hemocomponenteRepository.saveAll(bolsas);

        requisicao.setStatus(StatusRequisicao.ATENDIDA);
        requisicao.setDataEntrega(Instant.now(relogio));
        return requisicaoRepository.save(requisicao);
    }

    private Requisicao buscarComStatus(Long requisicaoId, StatusRequisicao esperado) {
        Requisicao requisicao = requisicaoRepository.findById(requisicaoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisição não encontrada"));

        if (requisicao.getStatus() != esperado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Requisição precisa estar " + esperado + " (está " + requisicao.getStatus() + ")");
        }
        return requisicao;
    }
}
