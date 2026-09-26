package com.hemotrack.backend.cotrollers;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hemotrack.backend.model.instituicao.dto.CadastroRequest;
import com.hemotrack.backend.model.instituicao.dto.CadastroResponse;
import com.hemotrack.backend.service.CadastroService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/instituicoes")
public class InstituicaoController {

    private final CadastroService cadastroService;

    public InstituicaoController(CadastroService cadastroService) {
        this.cadastroService = cadastroService;
    }

    @PostMapping
    public ResponseEntity<CadastroResponse> cadastrar(@Valid @RequestBody CadastroRequest requisicao) {
        CadastroResponse criado = cadastroService.cadastrar(requisicao);

        URI localizacao = URI.create("/instituicoes/" + criado.instituicao().id());
        return ResponseEntity.created(localizacao).body(criado);
    }
}