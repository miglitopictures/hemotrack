package com.hemotrack.backend.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.hemotrack.backend.model.requisicao.Requisicao;
import com.hemotrack.backend.model.requisicao.StatusRequisicao;
import com.hemotrack.backend.model.requisicao.dto.MonitoramentoResponse;
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

    private final RequisicaoRepository requisicaoRepository;
    private final RotaService rotaService;
    private final MapaTransporte mapa;
    private final SensorTemperatura sensor;
    private final Clock relogio;
    private final double kmPorMinuto;

    // velocidade da simulacao: 10 km/min deixa Recife -> Caruaru em ~15 minutos, bom para a demo.
    public MonitoramentoService(RequisicaoRepository requisicaoRepository,
                                RotaService rotaService,
                                MapaTransporte mapa,
                                SensorTemperatura sensor,
                                Clock relogio,
                                @Value("${hemotrack.simulacao.km-por-minuto:10}") double kmPorMinuto) {
        this.requisicaoRepository = requisicaoRepository;
        this.rotaService = rotaService;
        this.mapa = mapa;
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
        int indiceAtual = ultimoLocalAlcancado(locais, km);
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

        return new MonitoramentoResponse(
            requisicao.getId(),
            requisicao.getStatus(),
            rota.origem().municipio(),
            rota.destino().municipio(),
            locais,
            locais.get(indiceAtual),
            proximoLocal,
            progresso,
            Math.round(km * 10) / 10.0,
            rota.distanciaKm(),
            temperatura,
            faixa,
            !faixa.contem(temperatura),
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

    // a distancia da origem ate cada local da rota e o caminho minimo ate ele
    // (um trecho do caminho minimo tambem e minimo), entao o proprio mapa responde.
    private int ultimoLocalAlcancado(List<String> locais, double km) {
        String origem = locais.get(0);
        int indice = 0;
        for (int i = 0; i < locais.size(); i++) {
            double distanciaAteOLocal = mapa.calcularRota(origem, locais.get(i)).orElseThrow().pesoTotal();
            if (distanciaAteOLocal <= km) {
                indice = i;
            }
        }
        return indice;
    }
}
