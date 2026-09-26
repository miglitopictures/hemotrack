package com.hemotrack.backend.cotrollers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hemotrack.backend.model.usuario.UsuarioAutenticado;
import com.hemotrack.backend.model.usuario.dto.LoginRequest;
import com.hemotrack.backend.model.usuario.dto.LoginResponse;
import com.hemotrack.backend.model.usuario.dto.MeResponse;
import com.hemotrack.backend.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;


@RestController 
@RequestMapping ("/auth")
public class AuthController {
    


    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest requisicao) {
        return this.authService.autenticar(requisicao);
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return authService.dadosDoUsuarioLogado(autenticado);
    }

}
