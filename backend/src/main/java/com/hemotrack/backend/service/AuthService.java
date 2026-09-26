package com.hemotrack.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hemotrack.backend.exception.CredenciaisInvalidasException;
import com.hemotrack.backend.model.usuario.Usuario;
import com.hemotrack.backend.model.usuario.dto.LoginRequest;
import com.hemotrack.backend.model.usuario.dto.LoginResponse;
import com.hemotrack.backend.repositories.UsuarioRepository;

@Service 
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private  final JwtService jwtService;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, UsuarioService usuarioService, JwtService jwtService) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    public LoginResponse autenticar(LoginRequest requisicao) {
        String email = requisicao.email().trim().toLowerCase();
        Usuario usuario = usuarios.findByEmail(email).orElse(null);

        boolean usuarioNaoExiste = (usuario == null);
        boolean senhaIncorreta = !usuarioNaoExiste && !encoder.matches(requisicao.senha(), usuario.getSenha());
        boolean contaDesativada = !usuarioNaoExiste && !usuario.isAtivo();

        if (usuarioNaoExiste || senhaIncorreta || contaDesativada) {
            throw new CredenciaisInvalidasException();
        }

        JwtService.TokenGerado token = jwtService.gerar(usuario);

        return new LoginResponse(token.valor(), token.expiraEm());
    }
    
}
