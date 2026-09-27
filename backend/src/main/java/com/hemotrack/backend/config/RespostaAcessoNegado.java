package com.hemotrack.backend.config;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component 
public class RespostaAcessoNegado implements AccessDeniedHandler{
    @Override 
    public void handle(HttpServletRequest requisicao,
                       HttpServletResponse resposta,
                       AccessDeniedException excecao) throws IOException {
        resposta.setStatus(HttpServletResponse.SC_FORBIDDEN);
        resposta.setContentType("application/problem+json");
        resposta.setCharacterEncoding("UTF-8");

        String corpo = """
                {
                  "type": "https://hemotrack.dev/erros/acesso-negado",
                  "title": "Acesso negado",
                  "status": 403,
                  "detail": "Seu perfil não tem permissão para esta operação."
                }
                """;
        
        resposta.getWriter().write(corpo);
    }    
}
