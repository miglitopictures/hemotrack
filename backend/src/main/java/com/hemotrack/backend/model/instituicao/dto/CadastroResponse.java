package com.hemotrack.backend.model.instituicao.dto;

import com.hemotrack.backend.model.usuario.dto.UsuarioResponse;

public record CadastroResponse(
        InstituicaoResponse instituicao,
        UsuarioResponse administrador
) { }