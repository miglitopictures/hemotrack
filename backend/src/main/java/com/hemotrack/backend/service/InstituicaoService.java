package com.hemotrack.backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.hemotrack.backend.model.instituicao.Instituicao;
import com.hemotrack.backend.model.instituicao.StatusInstituicao;
import com.hemotrack.backend.model.instituicao.dto.InstituicaoResponse;
import com.hemotrack.backend.model.usuario.Papel;
import com.hemotrack.backend.model.usuario.UsuarioAutenticado;
import com.hemotrack.backend.repositories.InstituicaoRepository;

@Service 
public class InstituicaoService {
    private final InstituicaoRepository instituicoes;

    public InstituicaoService(InstituicaoRepository instituicoes) {
        this.instituicoes = instituicoes;
    }


    public List<InstituicaoResponse> listar(UsuarioAutenticado autenticado, StatusInstituicao status) {
        StatusInstituicao filtro = status == null ? StatusInstituicao.APROVADA : status;
    
        if (filtro != StatusInstituicao.APROVADA && autenticado.papel() != Papel.ADMIN_SISTEMA) {
            throw new AccessDeniedException("Apenas a operação do HemoTrack lista instituições não aprovadas.");
        }

        List<InstituicaoResponse> resposta = new ArrayList<>();
    
        for (Instituicao instituicao: instituicoes.findByStatus(filtro)) {
            resposta.add(InstituicaoResponse.de(instituicao));
        }

        return resposta;
    }
}
