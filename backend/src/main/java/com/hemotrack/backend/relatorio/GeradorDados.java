package com.hemotrack.backend.relatorio;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.hemotrack.backend.model.requisicao.Prioridade;
import com.hemotrack.backend.model.sangue.TipoHemocomponente;
import com.hemotrack.backend.model.shared.FatorRh;
import com.hemotrack.backend.model.shared.TipoABO;

/** Gera dados sintéticos reproduzíveis (mesma semente = mesmos dados). */
public final class GeradorDados {

    private GeradorDados() {
    }

    public static List<RegistroRequisicao> requisicoes(int n, long seed) {
        Random rnd = new Random(seed);
        List<RegistroRequisicao> lista = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            lista.add(new RegistroRequisicao(i + 1L, 1 + rnd.nextInt(500),
                    pick(TipoHemocomponente.values(), rnd), pick(TipoABO.values(), rnd),
                    pick(FatorRh.values(), rnd), 200 + rnd.nextInt(2000), pick(Prioridade.values(), rnd)));
        }
        return lista;
    }

    public static List<UnidadeEstoque> estoque(int n, long seed) {
        Random rnd = new Random(seed);
        List<UnidadeEstoque> lista = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            lista.add(new UnidadeEstoque(i + 1L, pick(TipoHemocomponente.values(), rnd), pick(TipoABO.values(), rnd),
                    pick(FatorRh.values(), rnd), 100 + rnd.nextInt(400)));
        }
        return lista;
    }

    private static <T> T pick(T[] valores, Random rnd) {
        return valores[rnd.nextInt(valores.length)];
    }
}
