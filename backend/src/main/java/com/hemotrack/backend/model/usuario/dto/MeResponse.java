package com.hemotrack.backend.model.usuario.dto;

import com.hemotrack.backend.model.instituicao.dto.InstituicaoResponse;
import com.hemotrack.backend.model.usuario.Papel;

public record MeResponse(
    Long id,
    String nome,
    String email,
    Papel papel,
    boolean ativo,
    Long instituicaoId,
    InstituicaoResponse instituicao
) { }
