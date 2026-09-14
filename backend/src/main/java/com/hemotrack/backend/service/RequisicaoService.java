package com.hemotrack.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

// note(mig): talvez devemos criar exceptions em vez de mandar uma resposta http direto do service, por enquanto deixei assim.
import org.springframework.http.HttpStatus;
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
        Requisicao requisicao = buscarPorId(id);
        // note(mig): no momento só atualizamos a prioridade. temos que pensar melhor sobre isso no desenho da API.
        requisicao.setPrioridade(novaRequisicao.getPrioridade());
        return repository.save(requisicao);
    }

    public Requisicao aceitar(Long id) {
        Requisicao requisicao = buscarPorId(id);
        // apenas aceitamos requisições que estão atualmente abertas, para evitar conflitos.
        if (requisicao.getStatus() != StatusRequisicao.ABERTA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Requisição não está aberta");
        }
        requisicao.setStatus(StatusRequisicao.ACEITA);
        return repository.save(requisicao);
    }
    
    public Requisicao recusar(String motivoRecusa, Long id) {
        Requisicao requisicao = buscarPorId(id);
        
        // podemos recusar uma requisicao apenas se ela estiver aberta.
        if (requisicao.getStatus() != StatusRequisicao.ABERTA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Requisição não está aberta");
        }
        
        // motivo da recusa é obrigatorio.
        if (motivoRecusa == null || motivoRecusa.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motivo da recusa é obrigatório");
        }

        requisicao.setMotivoRecusa(motivoRecusa);
        requisicao.setStatus(StatusRequisicao.RECUSADA);
        return repository.save(requisicao);
    }


    public List<Requisicao> listarTodos() {
        return repository.findAll();
    }

    public  void remover(Long id) {
        Requisicao requisicao = buscarPorId(id);
        repository.delete(requisicao);
    }
}
