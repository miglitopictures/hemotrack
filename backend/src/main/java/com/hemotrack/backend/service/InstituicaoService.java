package com.hemotrack.backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hemotrack.backend.exception.ConflitoException;
import com.hemotrack.backend.exception.NaoEncontradoException;
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

    @Transactional 
    public  InstituicaoResponse aprovar(Long id) {

        Instituicao instituicao = instituicoes.findById(id).orElse(null);

        if (instituicao == null) throw NaoEncontradoException.instituicao(id);
        
        if (instituicao.getStatus() == StatusInstituicao.APROVADA) throw ConflitoException.instituicaoJaAprovada(id);

        instituicao.setStatus(StatusInstituicao.APROVADA);

        return InstituicaoResponse.de(instituicao);
    }
}
