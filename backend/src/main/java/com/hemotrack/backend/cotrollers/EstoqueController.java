package com.hemotrack.backend.cotrollers;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hemotrack.backend.model.sangue.dto.HemocomponenteRequest;
import com.hemotrack.backend.model.sangue.dto.HemocomponenteResponse;
import com.hemotrack.backend.model.usuario.UsuarioAutenticado;
import com.hemotrack.backend.service.EstoqueService;

import jakarta.validation.Valid;

// HU02: estoque de hemocomponentes da instituicao
@RestController
@RequestMapping("/instituicoes/{instituicaoId}/estoque")
public class EstoqueController {

    private final EstoqueService estoqueService;

    public EstoqueController(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    @GetMapping
    public List<HemocomponenteResponse> listar(@AuthenticationPrincipal UsuarioAutenticado autenticado,
                                               @PathVariable Long instituicaoId) {
        return estoqueService.listar(autenticado, instituicaoId);
    }

    @PostMapping
    public ResponseEntity<HemocomponenteResponse> cadastrar(@AuthenticationPrincipal UsuarioAutenticado autenticado,
                                                            @PathVariable Long instituicaoId,
                                                            @Valid @RequestBody HemocomponenteRequest dados) {
        HemocomponenteResponse criada = estoqueService.cadastrar(autenticado, instituicaoId, dados);
        URI localizacao = URI.create("/instituicoes/" + instituicaoId + "/estoque/" + criada.id());
        return ResponseEntity.created(localizacao).body(criada);
    }
}
