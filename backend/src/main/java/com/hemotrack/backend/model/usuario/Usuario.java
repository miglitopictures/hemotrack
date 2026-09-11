package com.hemotrack.backend.model.usuario;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Nome Completo é obrigatório")
    @Size(min = 3, max = 80, message = "Nome deve ter entre 3 e 80 caracteres")
    private String nomeCompleto;
        
    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
    private String password;

    @NotBlank(message = "CPF é obrigatório")
    private String cpf;

    @NotNull(message = "Id da Instituição é obrigatório")
    private Long idInstituicao;

    @Nullable 
    private TipoUsuario tipo = TipoUsuario.PADRAO;
    
}
