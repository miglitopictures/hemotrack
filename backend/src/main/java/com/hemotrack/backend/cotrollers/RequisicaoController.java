package com.hemotrack.backend.cotrollers;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hemotrack.backend.model.requisicao.Requisicao;
import com.hemotrack.backend.model.requisicao.dto.RecusaRequest;
import com.hemotrack.backend.service.RequisicaoService;

import jakarta.validation.Valid;

@RestController  
@RequestMapping("/requisicoes")
public class RequisicaoController {
 
    private final RequisicaoService service;

    // instância de RequisicaoService é injetada pelo Spring no controller
    RequisicaoController(RequisicaoService service) {
        this.service = service;
    }

    
    // Rotas da nossa API de requisicoes

    // listar todas as requisicoes de trasfusao
    @GetMapping("")
    List<Requisicao> mostrarTodas(@RequestParam(required = false) Long hospitalId) {
        return service.listarTodos(hospitalId);
}

    // criar nova requisicao
    @PostMapping("")
    Requisicao criarRequisicao(@Valid @RequestBody Requisicao novaRequisicao) {
        return service.salvar(novaRequisicao);
    }

    // retorna uma requisicao, com id indicado
    @GetMapping("/{id}")
    Requisicao mostrarUma(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    // atualizar uma requisicao {id}
    @PutMapping("/{id}")
    Requisicao atualizarRequisicao(@RequestBody Requisicao novaRequisicao, @PathVariable Long id) {
        return service.atualizar(novaRequisicao, id);
    }

    // aceitar uma requisicao {id}
    @PostMapping("/{id}/aceitar")
    Requisicao aceitarRequisicao(@PathVariable Long id, @RequestParam Long hemocentroId) {
    return service.aceitar(id, hemocentroId);
}

    // recusar uma requisicao {id}
    @PostMapping ("/{id}/recusar")
    Requisicao recusarRequisicao(@RequestBody RecusaRequest body, @PathVariable Long id) {
        return service.recusar(body.getMotivoRecusa(), id);
    }

    // deletar uma requisicao {id}
    @DeleteMapping("/{id}")
    void deletarRequisicao(@PathVariable Long id) {
        service.remover(id);
    }

}
