package com.hemotrack.backend.cotrollers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hemotrack.backend.model.usuario.dto.LoginRequest;
import com.hemotrack.backend.model.usuario.dto.LoginResponse;
import com.hemotrack.backend.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController 
@RequestMapping ("/auth")
public class AuthController {
    


    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest requisicao) {
        return this.authService.autenticar(requisicao);
    }
    


}
