package com.hemotrack.backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hemotrack.backend.exception.ConflitoException;
import com.hemotrack.backend.exception.NaoEncontradoException;
import com.hemotrack.backend.model.instituicao.Instituicao;
import com.hemotrack.backend.model.instituicao.StatusInstituicao;
import com.hemotrack.backend.model.usuario.Papel;
import com.hemotrack.backend.model.usuario.Usuario;
import com.hemotrack.backend.model.usuario.UsuarioAutenticado;
import com.hemotrack.backend.model.usuario.dto.OperadorRequest;
import com.hemotrack.backend.model.usuario.dto.UsuarioResponse;
import com.hemotrack.backend.repositories.InstituicaoRepository;
import com.hemotrack.backend.repositories.UsuarioRepository;

@Service 
public class MembroService {
    private final UsuarioRepository usuarios;
    private final InstituicaoRepository instituicoes;
    private final PasswordEncoder encoder;

    public  MembroService(UsuarioRepository usuarios,
                          InstituicaoRepository instituicoes,
                          PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.instituicoes = instituicoes;
        this.encoder = encoder;
    }

    private Instituicao exigirAcessoAInstituicao(UsuarioAutenticado autenticado, Long instituicaoId) {
        Instituicao instituicao = instituicoes.findById(instituicaoId).orElse(null);

        if (instituicao == null) {
            throw NaoEncontradoException.instituicao(instituicaoId);
        }

        if (autenticado.papel() == Papel.ADMIN_SISTEMA) {
            return instituicao;
        }

        if (!instituicaoId.equals(autenticado.instituicaoId())) {
            throw new AccessDeniedException("Você só gerencia membros da sua própria instituição.");
        }

        if (instituicao.getStatus() != StatusInstituicao.APROVADA) {
            throw new AccessDeniedException("A instituição ainda não foi aprovada.");
        }

        return instituicao;
    }

    public List<UsuarioResponse> listar(UsuarioAutenticado autenticado, Long instituicaoId) {
        exigirAcessoAInstituicao(autenticado, instituicaoId);

        List<UsuarioResponse> resposta = new ArrayList<>();

        for (Usuario membro : usuarios.findByInstituicaoIdOrderByNomeAsc(instituicaoId)){
            resposta.add(UsuarioResponse.de(membro));
        }

        return resposta;
    }

    @Transactional
    public UsuarioResponse cadastrarOperador(UsuarioAutenticado autenticado,
                                             Long instituicaoId,
                                             OperadorRequest requisicao) {
        exigirAcessoAInstituicao(autenticado, instituicaoId);

        String email = requisicao.email().trim().toLowerCase();

        if (usuarios.existsByEmail(email)) {
            throw ConflitoException.emailEmUso(email);
        }

        Usuario novoOperador = usuarios.save(new Usuario(
            requisicao.nome().trim(), 
            email, 
            encoder.encode(requisicao.senha()), 
            Papel.OPERADOR, 
            instituicaoId));
        
        return UsuarioResponse.de(novoOperador);
    }


    @Transactional 
    public UsuarioResponse alternarAtivo(UsuarioAutenticado autenticado,
                                         Long instituicaoId,
                                         Long usuarioId,
                                         boolean ativo) {
        exigirAcessoAInstituicao(autenticado, instituicaoId);

        Usuario membro = usuarios.findById(usuarioId).orElse(null);

        if (membro == null || !instituicaoId.equals(membro.getInstituicaoId())) {
            throw NaoEncontradoException.usuario(usuarioId);
        }

        if (membro.getPapel() != Papel.OPERADOR) {
            throw ConflitoException.tentouDesativarAdmin(usuarioId);
        }

        membro.setAtivo(ativo);

        return UsuarioResponse.de(membro);
    }
}
