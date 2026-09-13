package com.hemotrack.backend.cotrollers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hemotrack.backend.model.requisicao.Requisicao;
import com.hemotrack.backend.repositories.RequisicaoRepository;

@RestController  
@RequestMapping("/requisicoes")
public class RequisicaoController {
 
    private final RequisicaoRepository repository;

    // instância de RequisicaoRepository é injetada pelo Spring no controller
    RequisicaoController(RequisicaoRepository repository) {
        this.repository = repository;
    }

    @GetMapping("")
    List<Requisicao> listar() {
        return repository.findAll();
    }

    @PostMapping("")
    Requisicao newEmployee(@RequestBody Requisicao novaRequisicao) {
        return repository.save(novaRequisicao);
    }

}
