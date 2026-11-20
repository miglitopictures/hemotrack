package com.hemotrack.backend.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.hemotrack.backend.model.indicadores.dto.IndicadoresResponse;
import com.hemotrack.backend.model.indicadores.dto.IndicadoresResponse.DemandaComponente;
import com.hemotrack.backend.model.indicadores.dto.IndicadoresResponse.EstoqueTipoSanguineo;
import com.hemotrack.backend.model.requisicao.Requisicao;
import com.hemotrack.backend.model.requisicao.StatusRequisicao;
import com.hemotrack.backend.model.sangue.Hemocomponente;
import com.hemotrack.backend.model.sangue.StatusHemocomponente;
import com.hemotrack.backend.model.sangue.TipoHemocomponente;
import com.hemotrack.backend.model.shared.FatorRh;
import com.hemotrack.backend.repositories.HemocomponenteRepository;
import com.hemotrack.backend.repositories.RequisicaoRepository;

/**
 * HU09: indicadores da operacao. Calculados na hora a partir do banco (nada fica salvo),
 * entao uma requisicao nova ja aparece no proximo GET (cenario 2).
 */
@Service
public class IndicadoresService {

    // "proximas do vencimento" = vencem em ate 7 dias
    private static final int DIAS_PARA_VENCIMENTO = 7;

    // mesma ordem dos graficos do front
    private static final List<String> TIPOS_SANGUINEOS = List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");

    private final RequisicaoRepository requisicaoRepository;
    private final HemocomponenteRepository hemocomponenteRepository;
    private final Clock relogio;

    public IndicadoresService(RequisicaoRepository requisicaoRepository,
                              HemocomponenteRepository hemocomponenteRepository,
                              Clock relogio) {
        this.requisicaoRepository = requisicaoRepository;
        this.hemocomponenteRepository = hemocomponenteRepository;
        this.relogio = relogio;
    }

    public IndicadoresResponse calcular() {
        Instant agora = Instant.now(relogio);
        LocalDate hoje = LocalDate.now(relogio);

        // ---------- estoque ----------
        long disponiveis = 0;
        long vencendo = 0;
        Map<String, Long> porTipoSanguineo = new LinkedHashMap<>();
        for (String tipo : TIPOS_SANGUINEOS) {
            porTipoSanguineo.put(tipo, 0L);
        }

        LocalDate limiteVencimento = hoje.plusDays(DIAS_PARA_VENCIMENTO);
        for (Hemocomponente bolsa : hemocomponenteRepository.findByStatus(StatusHemocomponente.DISPONIVEL)) {
            if (!bolsa.estaDisponivel(hoje)) {
                continue; // vencida
            }
            disponiveis++;
            if (!bolsa.getDataValidade().isAfter(limiteVencimento)) {
                vencendo++;
            }
            porTipoSanguineo.merge(tipoSanguineo(bolsa), 1L, Long::sum);
        }

        List<EstoqueTipoSanguineo> estoque = new ArrayList<>();
        for (Map.Entry<String, Long> entrada : porTipoSanguineo.entrySet()) {
            estoque.add(new EstoqueTipoSanguineo(entrada.getKey(), entrada.getValue()));
        }

        // ---------- requisicoes e distribuicao ----------
        long pendentes = 0;
        long emTransporte = 0;
        long entregues = 0;
        Map<TipoHemocomponente, Long> pedidos = new LinkedHashMap<>();
        Map<TipoHemocomponente, Long> atendidos = new LinkedHashMap<>();
        for (TipoHemocomponente componente : TipoHemocomponente.values()) {
            pedidos.put(componente, 0L);
            atendidos.put(componente, 0L);
        }

        for (Requisicao requisicao : requisicaoRepository.findAll()) {
            StatusRequisicao status = requisicao.getStatus();
            if (status == StatusRequisicao.ABERTA) {
                pendentes++;
            } else if (status == StatusRequisicao.EM_TRANSITO) {
                emTransporte++;
            } else if (status == StatusRequisicao.ATENDIDA) {
                entregues++;
            }

            TipoHemocomponente componente = requisicao.getTipo();
            if (componente != null) {
                pedidos.merge(componente, 1L, Long::sum);
                if (status == StatusRequisicao.ATENDIDA) {
                    atendidos.merge(componente, 1L, Long::sum);
                }
            }
        }

        List<DemandaComponente> demanda = new ArrayList<>();
        for (TipoHemocomponente componente : TipoHemocomponente.values()) {
            demanda.add(new DemandaComponente(componente, pedidos.get(componente), atendidos.get(componente)));
        }
        // mais pedido primeiro; empate mantem a ordem do enum (sort e estavel)
        demanda.sort(Comparator.comparingLong(DemandaComponente::pedidos).reversed());

        return new IndicadoresResponse(
            disponiveis,
            vencendo,
            DIAS_PARA_VENCIMENTO,
            pendentes,
            emTransporte,
            entregues,
            demanda,
            estoque,
            agora
        );
    }

    // ABO + Rh, no formato do front: "O-", "AB+"
    private static String tipoSanguineo(Hemocomponente bolsa) {
        String sinal = bolsa.getRh() == FatorRh.POSITIVO ? "+" : "-";
        return bolsa.getAbo().name() + sinal;
    }
}
