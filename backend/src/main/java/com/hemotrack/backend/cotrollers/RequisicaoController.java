package com.hemotrack.backend.cotrollers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.hemotrack.backend.model.requisicao.Requisicao;
import com.hemotrack.backend.model.requisicao.StatusRequisicao;
import com.hemotrack.backend.model.requisicao.dto.RecusaRequest;
import com.hemotrack.backend.repositories.RequisicaoRepository;

@RestController  
@RequestMapping("/requisicoes")
public class RequisicaoController {
 
    private final RequisicaoRepository repository;

    // instância de RequisicaoRepository é injetada pelo Spring no controller
    RequisicaoController(RequisicaoRepository repository) {
        this.repository = repository;
    }

    
    // Rotas da nossa API de requisicoes

    // listar todas as requisicoes de trasfusao
    @GetMapping("")
    List<Requisicao> all() {
        return repository.findAll();
    }

    // criar nova requisicao
    @PostMapping("")
    Requisicao criarRequisicao(@RequestBody Requisicao novaRequisicao) {
        return repository.save(novaRequisicao);
    }

    // retorna uma requisicao, com id indicado
    @GetMapping("/{id}")
    Requisicao mostrarUma(@PathVariable Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisição não encontrada"));
    }

    // atualizar uma requisicao {id}
    @PutMapping("/{id}")
    Requisicao atualizarRequisicao(@RequestBody Requisicao novaRequisicao, @PathVariable Long id) {
            return repository.findById(id)
            .map(requisicao -> {
                requisicao.setPrioridade(novaRequisicao.getPrioridade());
                return repository.save(requisicao);
            })
            .orElseGet(() -> {
                return repository.save(novaRequisicao);
            });
    }

    // aceitar uma requisicao {id}
    @PostMapping ("/{id}/aceitar")
    Requisicao aceitarRequisicao(@PathVariable Long id) {
            return repository.findById(id)
            .map(requisicao -> {
                requisicao.setStatus(StatusRequisicao.ACEITA);
                return repository.save(requisicao);
            })
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisição não encontrada"));
    }

    // recusar uma requisicao {id}
    @PostMapping ("/{id}/recusar")
    Requisicao recusarRequisicao(@RequestBody RecusaRequest body, @PathVariable Long id) {
            return repository.findById(id)
            .map(requisicao -> {
                requisicao.setMotivoRecusa(body.getMotivoRecusa());
                requisicao.setStatus(StatusRequisicao.RECUSADA);
                return repository.save(requisicao);
            })
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisição não encontrada"));
    }

    // deletar uma requisicao {id}
    @DeleteMapping("/{id}")
    void deletarRequisicao(@PathVariable Long id) {
        repository.deleteById(id);
    }

}
