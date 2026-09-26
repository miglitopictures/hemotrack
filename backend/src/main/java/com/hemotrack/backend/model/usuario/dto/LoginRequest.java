package com.hemotrack.backend.model.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank (message = "Email é obrigatorio!")
    @Email (message = "Email inválido")
    String email,

    @NotBlank (message = "Senha é obrigadtoria")
    String senha
) { }