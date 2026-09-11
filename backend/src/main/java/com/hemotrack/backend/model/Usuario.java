package com.hemotrack.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity 
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

    @NotBlank(message = "CPF é obrigatório")
    private String cpf;

    @NotBlank(message = "Id da Instituição é obrigatório")
    private Long idInstituicao;
    
}
