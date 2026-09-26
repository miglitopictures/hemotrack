package com.hemotrack.backend.model.usuario.dto;

import com.hemotrack.backend.model.usuario.Papel;
import com.hemotrack.backend.model.usuario.Usuario;


public record UsuarioResponse(Long id, String nome, String email,
                              Papel papel, boolean ativo, Long instituicaoId) {

    // note (mig): aqui ocultamos a senha para trasferir os dados.
    public static UsuarioResponse de(Usuario u) {
        UsuarioResponse convertedToResponse = new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPapel(), u.isAtivo(), u.getInstituicaoId());
        return convertedToResponse;
    }

}
