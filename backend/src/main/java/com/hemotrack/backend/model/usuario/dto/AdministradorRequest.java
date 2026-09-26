package com.hemotrack.backend.model.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdministradorRequest (
    @NotBlank (message = "Nome é obrigatório")
    @Size(min = 3, max = 80, message = "Nome deve ter entre 3 e 80 caracteres")
    String nome,
    
    @NotBlank (message = "Email é obrigatório")
    @Email (message = "Email inválido")
    String email,
    
    @NotBlank (message = "Senha é obrigatória")
    @Size (min = 6, message = "A senha deve ter no mínimo 6 caracteres")
    String senha
) { }