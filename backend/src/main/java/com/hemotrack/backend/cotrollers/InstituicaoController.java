package com.hemotrack.backend.cotrollers;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.hemotrack.backend.model.instituicao.StatusInstituicao;
import com.hemotrack.backend.model.instituicao.dto.CadastroRequest;
import com.hemotrack.backend.model.instituicao.dto.CadastroResponse;
import com.hemotrack.backend.model.instituicao.dto.InstituicaoResponse;
import com.hemotrack.backend.model.usuario.UsuarioAutenticado;
import com.hemotrack.backend.service.CadastroService;
import com.hemotrack.backend.service.InstituicaoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/instituicoes")
public class InstituicaoController {

    private final CadastroService cadastroService;
    private final InstituicaoService instituicaoService;

    public InstituicaoController(CadastroService cadastroService, InstituicaoService instituicaoService) {
        this.cadastroService = cadastroService;
        this.instituicaoService = instituicaoService;
    }

    @PostMapping
    public ResponseEntity<CadastroResponse> cadastrar(@Valid @RequestBody CadastroRequest requisicao) {
        CadastroResponse criado = cadastroService.cadastrar(requisicao);

        URI localizacao = URI.create("/instituicoes/" + criado.instituicao().id());
        return ResponseEntity.created(localizacao).body(criado);
    }

    @GetMapping
    public List<InstituicaoResponse>  listar(
        @AuthenticationPrincipal UsuarioAutenticado autenticado,
        @RequestParam (required = false) StatusInstituicao status) {
        
        return instituicaoService.listar(autenticado,  status);
    }

    @PatchMapping ("/{id}/aprovar")
    public InstituicaoResponse aprovar(@PathVariable Long id) {
        return instituicaoService.aprovar(id);
    }
}