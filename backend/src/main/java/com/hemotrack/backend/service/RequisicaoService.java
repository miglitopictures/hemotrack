package com.hemotrack.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.hemotrack.backend.model.requisicao.Requisicao;
import com.hemotrack.backend.model.requisicao.StatusRequisicao;
import com.hemotrack.backend.repositories.RequisicaoRepository;


@Service 
public class RequisicaoService {
    private final RequisicaoRepository repository;

    public RequisicaoService(RequisicaoRepository repository) {
        this.repository = repository;
    }

    public Requisicao salvar(Requisicao requisicao) {
        return repository.save(requisicao);
    }

    public Requisicao buscarPorId(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisição não encontrada"));
    }

    public Requisicao atualizar(Requisicao novaRequisicao, Long id) {
        return repository.findById(id)
                .map(requisicao -> {
                    requisicao.setPrioridade(novaRequisicao.getPrioridade());
                    return repository.save(requisicao);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisição não encontrada"));
    }

    public Requisicao aceitar(Long id) {
        return repository.findById(id)
                .map(requisicao -> {
                    requisicao.setStatus(StatusRequisicao.ACEITA);
                    return repository.save(requisicao);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisição não encontrada"));
    }

    public Requisicao recusar(String motivoRecusa, Long id) {
        return repository.findById(id)
            .map(requisicao -> {
                requisicao.setMotivoRecusa(motivoRecusa);
                requisicao.setStatus(StatusRequisicao.RECUSADA);
                return repository.save(requisicao);
            })
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisição não encontrada"));
    }


    public List<Requisicao> listarTodos() {
        return repository.findAll();
    }

    public  void remover(Long id) {
        repository.deleteById(id);
    }
}
