package com.hemotrack.backend.cotrollers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hemotrack.backend.model.indicadores.dto.IndicadoresResponse;
import com.hemotrack.backend.service.IndicadoresService;

// HU09: painel de indicadores da operacao (qualquer usuario autenticado)
@RestController
@RequestMapping("/indicadores")
public class IndicadoresController {

    private final IndicadoresService service;

    IndicadoresController(IndicadoresService service) {
        this.service = service;
    }

    @GetMapping
    IndicadoresResponse mostrar() {
        return service.calcular();
    }
}
