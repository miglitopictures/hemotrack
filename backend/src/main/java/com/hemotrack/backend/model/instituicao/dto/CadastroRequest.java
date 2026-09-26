package com.hemotrack.backend.model.instituicao.dto;

import com.hemotrack.backend.model.usuario.dto.AdministradorRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CadastroRequest(

    @NotNull(message = "Dados da instituição são obrigatórios")
    @Valid
    InstituicaoRequest instituicao,
    
    @NotNull (message = "Dados do administrador são obrigatórios")
    @Valid 
    AdministradorRequest administrador
) { }
