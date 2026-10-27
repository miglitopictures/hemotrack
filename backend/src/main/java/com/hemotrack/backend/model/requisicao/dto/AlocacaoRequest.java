package com.hemotrack.backend.model.requisicao.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record AlocacaoRequest(@NotEmpty List<Long> hemocomponenteIds) {
}