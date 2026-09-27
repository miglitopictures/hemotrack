package com.hemotrack.backend.cotrollers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hemotrack.backend.model.usuario.UsuarioAutenticado;
import com.hemotrack.backend.model.usuario.dto.AlterarMembroRequest;
import com.hemotrack.backend.model.usuario.dto.OperadorRequest;
import com.hemotrack.backend.model.usuario.dto.UsuarioResponse;
import com.hemotrack.backend.service.MembroService;

import jakarta.validation.Valid;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController 
@RequestMapping ("/instituicoes/{instituicaoId}/usuarios")
public class MembroController {

    private final MembroService membroService;

    public MembroController(MembroService membroService) {
        this.membroService = membroService;
    }

    @GetMapping
    public List<UsuarioResponse> listar(@AuthenticationPrincipal UsuarioAutenticado autenticado,
                                        @PathVariable Long instituicaoId) {
        return membroService.listar(autenticado, instituicaoId);
    }
    
    @PostMapping
    public ResponseEntity<UsuarioResponse> cadastrarOperador(@AuthenticationPrincipal UsuarioAutenticado autenticado,
                                                             @PathVariable Long instituicaoId,
                                                             @Valid @RequestBody OperadorRequest requisicao) {
        UsuarioResponse criado = membroService.cadastrarOperador(autenticado, instituicaoId, requisicao);                                          
        
        URI localizacao = URI.create("/instituicoes/" + instituicaoId + "/usuarios/" + criado.id());
        
        return ResponseEntity.created(localizacao).body(criado);
    }

    @PatchMapping ("/{usuarioId}")
    public  UsuarioResponse alterarAtivo(@AuthenticationPrincipal UsuarioAutenticado autenticado,
                                         @PathVariable Long instituicaoId,
                                         @PathVariable Long usuarioId,
                                         @Valid @RequestBody AlterarMembroRequest requisicao) {
        return  membroService.alternarAtivo(autenticado, instituicaoId, usuarioId, requisicao.ativo());
    }
    
}
