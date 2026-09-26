package com.hemotrack.backend.config;


import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.hemotrack.backend.model.usuario.Papel;
import com.hemotrack.backend.model.usuario.UsuarioAutenticado;
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

    public  JwtAuthFilter(JwtService jwtService){
        this.jwtService = jwtService;
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

                Long instituicaoId = ((Number) claims.get("instituicaoId")).longValue();

                UsuarioAutenticado usuario = new UsuarioAutenticado(Long.valueOf(claims.getSubject()), instituicaoId, Papel.valueOf(claims.get("papel", String.class)));

                var autoridades = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.papel().name()));
                var autentucacao = new UsernamePasswordAuthenticationToken(usuario, null, autoridades);

                SecurityContextHolder.getContext().setAuthentication(autentucacao);
            } catch (JwtException | IllegalArgumentException erro) {
                SecurityContextHolder.clearContext();
            }
        }
    
        cadeia.doFilter(requisicao, resposta);
    }
}
