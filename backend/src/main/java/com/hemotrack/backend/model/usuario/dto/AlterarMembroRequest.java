package com.hemotrack.backend.model.usuario.dto;

import jakarta.validation.constraints.NotNull;

public record AlterarMembroRequest(
    @NotNull (message = "Informe se o membro fica ativo ou inativo")
    Boolean ativo
) { }
