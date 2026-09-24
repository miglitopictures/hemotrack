package com.hemotrack.backend.service;

import org.springframework.transaction.annotation.Transactional;

import com.hemotrack.backend.exception.ConflitoException;
import com.hemotrack.backend.model.instituicao.Instituicao;
import com.hemotrack.backend.model.instituicao.dto.CadastroRequest;
import com.hemotrack.backend.model.instituicao.dto.CadastroResponse;
import com.hemotrack.backend.model.instituicao.dto.InstituicaoRequest;
import com.hemotrack.backend.model.instituicao.dto.InstituicaoResponse;
import com.hemotrack.backend.model.usuario.Papel;
import com.hemotrack.backend.model.usuario.Usuario;
import com.hemotrack.backend.model.usuario.dto.AdministradorRequest;
import com.hemotrack.backend.model.usuario.dto.UsuarioResponse;
import com.hemotrack.backend.repositories.InstituicaoRepository;
import com.hemotrack.backend.repositories.UsuarioRepository;

public class CadastroService {
    

    private final InstituicaoRepository instituicoes;
    private final UsuarioRepository usuarios;

    public CadastroService(InstituicaoRepository instituicoes, UsuarioRepository usuarios) {
        this.instituicoes = instituicoes;
        this.usuarios = usuarios;
    }



    @Transactional
    public CadastroResponse cadastrar(CadastroRequest req) {
        InstituicaoRequest dadosInstituicao = req.instituicao();
        AdministradorRequest dadosAdministrador = req.administrador();
    
        String cnpj = normalizarCnpj(dadosInstituicao.cnpj());
        String email = normalizarEmail(dadosAdministrador.email());
        
        // validamos o cnpj
        if (instituicoes.existsByCnpj(cnpj)) {
            throw ConflitoException.cnpjDuplicado(cnpj);
        }

        // e o email
        if (usuarios.existsByEmail(email)) {
            throw ConflitoException.emailEmUso(email);
        }

        Instituicao instituicao = instituicoes.save(new Instituicao(dadosInstituicao.razaoSocial().trim(), cnpj, dadosInstituicao.tipo(), dadosInstituicao.endereco(), dadosInstituicao.municipio(), dadosInstituicao.telefone()));
        Usuario administrador = usuarios.save(new Usuario(dadosAdministrador.nome().trim(), email, Papel.ADMIN_INSTITUICAO, instituicao.getId()));
    
    
        return new CadastroResponse(
            InstituicaoResponse.de(instituicao),
            UsuarioResponse.de(administrador));
    
    }

    /** "12.345.678/0001-90" e "12345678000190" têm que colidir no unique. */
    private String normalizarCnpj(String cnpj) {
        return cnpj.replaceAll("\\D", "");
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }

    

}
