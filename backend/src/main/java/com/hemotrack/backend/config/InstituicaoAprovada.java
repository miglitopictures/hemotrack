package com.hemotrack.backend.config;

import java.util.function.Supplier;


import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import com.hemotrack.backend.model.instituicao.Instituicao;
import com.hemotrack.backend.model.instituicao.StatusInstituicao;
import com.hemotrack.backend.model.usuario.Papel;
import com.hemotrack.backend.model.usuario.UsuarioAutenticado;
import com.hemotrack.backend.repositories.InstituicaoRepository;

@Component 
public class InstituicaoAprovada implements  AuthorizationManager<RequestAuthorizationContext>{
    
    private  final InstituicaoRepository instituicoes;

    public InstituicaoAprovada(InstituicaoRepository instituicoes) {
        this.instituicoes = instituicoes;
    }

    @Override 
    public AuthorizationResult authorize(Supplier<? extends Authentication> autenticacao, RequestAuthorizationContext contexto) {
        Authentication autenticado = autenticacao.get();

        if (autenticado == null || !autenticado.isAuthenticated()) {
            return  new AuthorizationDecision(false);
        }
        
        if (!(autenticado.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            return  new AuthorizationDecision(false);
        }

        if (usuario.papel() == Papel.ADMIN_SISTEMA || usuario.instituicaoId() == null) {
            return new AuthorizationDecision(false);
        }

        Instituicao instituicao = instituicoes.findById(usuario.instituicaoId()).orElse(null);
    
        boolean aprovada =
            instituicao != null &&
            instituicao.getStatus() == StatusInstituicao.APROVADA;
    
        return new AuthorizationDecision(aprovada);
    }

}
