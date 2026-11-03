package com.hemotrack.backend.service;

import java.time.Instant;
import java.util.Objects;
import java.util.Random;

import org.springframework.stereotype.Component;

import com.hemotrack.backend.model.sangue.FaixaTemperatura;
import com.hemotrack.backend.model.sangue.TipoHemocomponente;

/**
 * HU08: temperatura simulada.
 *
 * A leitura depende so de (requisicao, minuto): consultar de novo no mesmo minuto devolve
 * o mesmo valor, entao a tela nao fica "pulando". Em ~1 de cada 12 minutos a leitura sai
 * acima da faixa, para o alerta aparecer na demonstracao.
 */
@Component
public class SensorTemperaturaSimulado implements SensorTemperatura {

    private static final int UM_DESVIO_A_CADA_MINUTOS = 12;

    @Override
    public double ler(Long requisicaoId, TipoHemocomponente tipo, Instant instante) {
        FaixaTemperatura faixa = FaixaTemperatura.de(tipo);
        long minuto = instante.getEpochSecond() / 60;
        Random aleatorio = new Random(Objects.hash(requisicaoId, minuto));

        // congelados nao tem minimo: usa uma faixa de referencia de 14 graus abaixo do maximo
        double minimo = faixa.minC() != null ? faixa.minC() : faixa.maxC() - 14;
        double maximo = faixa.maxC();

        double leitura;
        if (aleatorio.nextInt(UM_DESVIO_A_CADA_MINUTOS) == 0) {
            // desvio: de 1 a 4 graus acima do limite
            leitura = maximo + 1 + aleatorio.nextDouble() * 3;
        } else {
            // normal: perto do centro da faixa, sem encostar nas bordas
            double centro = (minimo + maximo) / 2;
            double margem = (maximo - minimo) / 2 * 0.8;
            leitura = centro + (aleatorio.nextDouble() * 2 - 1) * margem;
        }

        return Math.round(leitura * 10) / 10.0;
    }
}
