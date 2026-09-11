package com.hemotrack.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "instituicoes")
public class Instituicao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull  
    private TipoInstituicao tipo;

    @NotBlank(message = "Razão Social é obrigatório")
    @Size(min = 3, max = 80, message = "Nome deve ter entre 3 e 80 caracteres")
    private String razaoSocial;

    @NotBlank(message = "CNPJ é obrigatório")
    private String cnpj;
}
