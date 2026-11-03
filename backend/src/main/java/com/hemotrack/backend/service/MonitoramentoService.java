package com.hemotrack.backend.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.hemotrack.backend.model.requisicao.Requisicao;
import com.hemotrack.backend.model.requisicao.StatusRequisicao;
import com.hemotrack.backend.model.requisicao.dto.MonitoramentoResponse;
import com.hemotrack.backend.model.requisicao.dto.MonitoramentoResponse.LeituraTemperatura;
import com.hemotrack.backend.model.requisicao.dto.RotaResponse;
import com.hemotrack.backend.model.sangue.FaixaTemperatura;
import com.hemotrack.backend.repositories.RequisicaoRepository;

/**
 * HU08: monitora o transporte em andamento.
 *
 * Localizacao simulada: o lote anda pela rota da HU07 numa velocidade fixa, a partir do
 * horario de envio. Temperatura simulada: vem do SensorTemperatura. Nada disso fica salvo;
 * tudo e calculado na hora da consulta.
 */
@Service
public class MonitoramentoService {

    // o grafico de temperatura mostra no maximo a ultima hora (uma leitura por minuto)
    private static final int MAXIMO_LEITURAS = 60;

    private final RequisicaoRepository requisicaoRepository;
    private final RotaService rotaService;
    private final SensorTemperatura sensor;
    private final Clock relogio;
    private final double kmPorMinuto;

    // velocidade da simulacao: 10 km/min deixa Recife -> Caruaru em ~15 minutos, bom para a demo.
    public MonitoramentoService(RequisicaoRepository requisicaoRepository,
                                RotaService rotaService,
                                SensorTemperatura sensor,
                                Clock relogio,
                                @Value("${hemotrack.simulacao.km-por-minuto:10}") double kmPorMinuto) {
        this.requisicaoRepository = requisicaoRepository;
        this.rotaService = rotaService;
        this.sensor = sensor;
        this.relogio = relogio;
        this.kmPorMinuto = kmPorMinuto;
    }

    public MonitoramentoResponse monitorar(Long requisicaoId) {
        Requisicao requisicao = requisicaoRepository.findById(requisicaoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisição não encontrada"));

        if (requisicao.getStatus() != StatusRequisicao.EM_TRANSITO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Só é possível monitorar requisição EM_TRANSITO (está " + requisicao.getStatus() + ")");
        }

        RotaResponse rota = rotaService.calcularRota(requisicao);
        List<String> locais = rota.locais();
        Instant agora = Instant.now(relogio);

        // ---------- localizacao simulada ----------
        double km = kmPercorridos(requisicao.getDataEnvio(), agora, rota.distanciaKm());
        int indiceAtual = ultimoLocalAlcancado(rota.kmAcumulado(), km);
        String proximoLocal = null;
        if (indiceAtual < locais.size() - 1) {
            proximoLocal = locais.get(indiceAtual + 1);
        }

        int progresso = 100;
        if (rota.distanciaKm() > 0) {
            progresso = (int) Math.floor(km / rota.distanciaKm() * 100);
        }

        // ---------- temperatura simulada ----------
        FaixaTemperatura faixa = FaixaTemperatura.de(requisicao.getTipo());
        double temperatura = sensor.ler(requisicao.getId(), requisicao.getTipo(), agora);
        List<LeituraTemperatura> historico = historico(requisicao, agora);

        return new MonitoramentoResponse(
            requisicao.getId(),
            requisicao.getStatus(),
            rota.origem().municipio(),
            rota.destino().municipio(),
            locais,
            rota.kmAcumulado(),
            locais.get(indiceAtual),
            proximoLocal,
            progresso,
            Math.round(km * 10) / 10.0,
            rota.distanciaKm(),
            temperatura,
            faixa,
            !faixa.contem(temperatura),
            historico,
            agora
        );
    }

    // tempo desde o envio x velocidade, sem passar do destino.
    private double kmPercorridos(Instant envio, Instant agora, double distanciaTotal) {
        if (envio == null) {
            return 0;
        }
        double minutos = Duration.between(envio, agora).toSeconds() / 60.0;
        double km = Math.max(minutos, 0) * kmPorMinuto;
        return Math.min(km, distanciaTotal);
    }

    // ultimo local da rota cuja distancia desde a origem ja foi percorrida.
    private int ultimoLocalAlcancado(List<Double> kmAcumulado, double km) {
        int indice = 0;
        for (int i = 0; i < kmAcumulado.size(); i++) {
            if (kmAcumulado.get(i) <= km) {
                indice = i;
            }
        }
        return indice;
    }

    // uma leitura por minuto, contando para tras a partir de agora (a ultima e a leitura atual).
    // o sensor simulado e deterministico por minuto, entao o historico nao muda entre consultas.
    private List<LeituraTemperatura> historico(Requisicao requisicao, Instant agora) {
        long minutosDeViagem = 0;
        if (requisicao.getDataEnvio() != null) {
            minutosDeViagem = Math.max(Duration.between(requisicao.getDataEnvio(), agora).toMinutes(), 0);
        }
        long anteriores = Math.min(minutosDeViagem, MAXIMO_LEITURAS - 1);

        List<LeituraTemperatura> leituras = new ArrayList<>();
        for (long k = anteriores; k >= 0; k--) {
            Instant instante = agora.minus(Duration.ofMinutes(k));
            leituras.add(new LeituraTemperatura(instante,
                sensor.ler(requisicao.getId(), requisicao.getTipo(), instante)));
        }
        return leituras;
    }
}
