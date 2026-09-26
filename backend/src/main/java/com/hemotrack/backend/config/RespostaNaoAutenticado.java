package com.hemotrack.backend.config;


import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component 
public class RespostaNaoAutenticado implements AuthenticationEntryPoint{
    @Override 
    public void commence(HttpServletRequest requisicao,
                         HttpServletResponse resposta,
                         AuthenticationException excexao) throws IOException {

        resposta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resposta.setContentType("application/problem+json");
        resposta.setCharacterEncoding("UTF-8");
        
        String corpo = """
                {
                  "type": "https://hemotrack.dev/erros/nao-autenticado",
                  "title": "Não autenticado",
                  "status": 401,
                  "detail": "Token ausente, inválido ou expirado."
                }
                """;

        resposta.getWriter().write(corpo);
    }
}
