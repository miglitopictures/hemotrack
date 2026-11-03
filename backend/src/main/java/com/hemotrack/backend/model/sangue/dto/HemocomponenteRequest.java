package com.hemotrack.backend.model.sangue.dto;

import java.time.LocalDate;

import com.hemotrack.backend.model.sangue.TipoHemocomponente;
import com.hemotrack.backend.model.shared.FatorRh;
import com.hemotrack.backend.model.shared.TipoABO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * HU02: dados para cadastrar uma bolsa no estoque.
 *
 * @param volumeMl opcional; sem ele, usa o volume padrao do componente
 */
public record HemocomponenteRequest(
    @NotBlank(message = "Informe o código da bolsa.") String codigoBolsa,
    @NotNull(message = "Informe o hemocomponente.") TipoHemocomponente tipo,
    @NotNull(message = "Informe o grupo ABO.") TipoABO abo,
    @NotNull(message = "Informe o fator Rh.") FatorRh rh,
    @NotNull(message = "Informe a data de coleta.") LocalDate dataColeta,
    @NotNull(message = "Informe a data de validade.") LocalDate dataValidade,
    @Positive(message = "O volume precisa ser maior que zero.") Double volumeMl
) { }
