package com.hemotrack.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hemotrack.backend.exception.CredenciaisInvalidasException;
import com.hemotrack.backend.model.instituicao.Instituicao;
import com.hemotrack.backend.model.instituicao.dto.InstituicaoResponse;
import com.hemotrack.backend.model.usuario.Usuario;
import com.hemotrack.backend.model.usuario.UsuarioAutenticado;
import com.hemotrack.backend.model.usuario.dto.LoginRequest;
import com.hemotrack.backend.model.usuario.dto.LoginResponse;
import com.hemotrack.backend.model.usuario.dto.MeResponse;
import com.hemotrack.backend.repositories.InstituicaoRepository;
import com.hemotrack.backend.repositories.UsuarioRepository;

@Service 
public class AuthService {

    private final UsuarioRepository usuarios;
    private final InstituicaoRepository instituicoes;
    private final PasswordEncoder encoder;
    private  final JwtService jwtService;

    public AuthService(UsuarioRepository usuarios, InstituicaoRepository instituicoes, PasswordEncoder encoder, JwtService jwtService) {
        this.usuarios = usuarios;
        this.instituicoes = instituicoes;
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

    public MeResponse dadosDoUsuarioLogado(UsuarioAutenticado autenticado) {
        Usuario usuario = usuarios.findById(autenticado.usuarioId()).orElse(null);
        
        if (usuario == null) {
            throw new CredenciaisInvalidasException();
        }

        // Instituicao instituicao = instituicoes.findById(usuario.getInstituicaoId()).orElse(null);

        Instituicao instituicao =
            usuario.getInstituicaoId() == null
                ? null
                : instituicoes.findById(usuario.getInstituicaoId()).orElse(null);

        return new MeResponse(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getPapel(),
            usuario.isAtivo(),
            usuario.getInstituicaoId(),
            instituicao == null ? null : InstituicaoResponse.de(instituicao));
    }
    
}
