package com.hemotrack.backend.model.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OperadorRequest(

    @NotBlank (message = "Nome eh obrigatorio")
    @Size (min = 3, max = 80, message = "Nome deve ter no minimo 3 e no maximo 80 caracteres")
    String nome,

    @NotBlank (message = "Email eh obrigatorio")
    @Email (message = "Email invalido")
    String email,

    @NotBlank (message = "Senha eh obrigatoria")
    @Size (min = 6,message = "Senha deve ter no minimo 6 caracteres")
    String senha
    
) { }
