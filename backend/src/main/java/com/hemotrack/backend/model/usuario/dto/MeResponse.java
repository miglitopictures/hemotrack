package com.hemotrack.backend.model.usuario.dto;

import com.hemotrack.backend.model.instituicao.dto.InstituicaoResponse;
import com.hemotrack.backend.model.usuario.Papel;

public record MeResponse(
    Long ig,
    String nome,
    String email,
    Papel papel,
    boolean ativo,
    Long instituicaoId,
    InstituicaoResponse instituicao
) { }
