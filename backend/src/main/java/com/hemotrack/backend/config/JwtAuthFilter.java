package com.hemotrack.backend.config;


import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.hemotrack.backend.model.usuario.Usuario;
import com.hemotrack.backend.model.usuario.UsuarioAutenticado;
import com.hemotrack.backend.repositories.UsuarioRepository;
import com.hemotrack.backend.service.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * roda uma vez por requisição, antes do controller.
 * se houver um token válido no header, registra quem é o usuário no contexto do Spring Security.
 */
@Component 
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UsuarioRepository usuarios;

    public  JwtAuthFilter(JwtService jwtService, UsuarioRepository usuarios){
        this.jwtService = jwtService;
        this.usuarios = usuarios;
    }

    @Override 
    protected  void doFilterInternal(HttpServletRequest requisicao,
                                     HttpServletResponse resposta,
                                     FilterChain cadeia) throws ServletException, IOException {

        String cabecalho = requisicao.getHeader("Authorization");

        if (cabecalho != null && cabecalho.startsWith("Bearer ")) {
            String token = cabecalho.substring("Bearer ".length());

            try {
                Claims claims = jwtService.validarEExtrair(token);

                Usuario usuario = usuarios.findById(Long.valueOf(claims.getSubject())).orElse(null);

                if (usuario != null && usuario.isAtivo()) {
                    UsuarioAutenticado autenticado = new UsuarioAutenticado(
                        usuario.getId(),
                        usuario.getInstituicaoId(),
                        usuario.getPapel());

                    var autoridades = List.of(new SimpleGrantedAuthority("ROLE_" + autenticado.papel().name()));
                    var autenticacao = new UsernamePasswordAuthenticationToken(autenticado, null, autoridades);

                    SecurityContextHolder.getContext().setAuthentication(autenticacao);
                } else {
                    // Usuario sumiu ou foi desativado: o token continua com
                    // assinatura valida, mas nao autentica mais ninguem.
                    SecurityContextHolder.clearContext();
                }
            } catch (JwtException | IllegalArgumentException erro) {
                SecurityContextHolder.clearContext();
            }
        }
    
        cadeia.doFilter(requisicao, resposta);
    }
}
