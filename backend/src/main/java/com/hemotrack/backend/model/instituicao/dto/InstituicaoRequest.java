package com.hemotrack.backend.model.instituicao.dto;

import com.hemotrack.backend.model.instituicao.TipoInstituicao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InstituicaoRequest(
    @NotBlank (message = "Razao social é obrigatoria")
    @Size (min = 3, max = 80, message = "Razão social deve ter no minimo 3 e no máximo 80 caractéres")
    String razaoSocial,

    @NotBlank (message = "CNPJ é obrigatoria")
    String cnpj,

    @NotNull  (message = "Tipo da instituição é obrigatoria")
    TipoInstituicao tipo,

    @NotBlank (message = "Endereço é obrigatório")
    String endereco,

    @NotBlank (message = "Município é obrigatório")
    String municipio,

    String telefone
) { }
