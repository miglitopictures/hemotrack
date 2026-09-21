package com.hemotrack.backend.relatorio;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.stereotype.Service;

import com.hemotrack.backend.model.requisicao.Prioridade;
import com.hemotrack.backend.model.shared.FatorRh;
import com.hemotrack.backend.model.shared.TipoABO;

/**
 * Cruza cada requisição com todo o estoque: para cada requisição percorre o estoque e conta
 * as unidades compatíveis (mesmo tipo, ABO e Rh compatíveis). Custo O(R * E).
 * Cada requisição é independente das outras, então o vetor de requisições pode ser dividido em
 * blocos processados por threads distintas; os parciais são somados no final.
 */
@Service
public class RelatorioService {

    private static final int PRIORIDADES = Prioridade.values().length;
    private static final int CAMPOS = 3; // requisicoes, atendiveis, unidadesCompativeis
    // [0]=volume compatível, [1]=sem compatibilidade, [2]=checksum; depois PRIORIDADES*CAMPOS
    private static final int TAM_PARCIAL = 3 + PRIORIDADES * CAMPOS;

    /** Versão 1: uma thread só. */
    public RelatorioResultado sequencial(List<RegistroRequisicao> reqs, List<UnidadeEstoque> estoque) {
        return montar(processarBloco(reqs, 0, reqs.size(), estoque), reqs.size());
    }

    /** Versão 2: divide as requisições em {@code threads} blocos contíguos e junta os parciais. */
    public RelatorioResultado paralelo(List<RegistroRequisicao> reqs, List<UnidadeEstoque> estoque,
                                       int threads, boolean virtual) {
        if (threads <= 1 && !virtual) {
            return sequencial(reqs, estoque);
        }
        int n = reqs.size();
        int partes = Math.max(1, threads);
        List<Callable<long[]>> tarefas = new ArrayList<>(partes);
        for (int i = 0; i < partes; i++) {
            int ini = (int) ((long) n * i / partes);
            int fim = (int) ((long) n * (i + 1) / partes);
            tarefas.add(() -> processarBloco(reqs, ini, fim, estoque));
        }
        long[] total = new long[TAM_PARCIAL];
        try (ExecutorService pool = virtual
                ? Executors.newVirtualThreadPerTaskExecutor()
                : Executors.newFixedThreadPool(partes)) {
            for (Future<long[]> f : pool.invokeAll(tarefas)) {
                long[] parcial = f.get();
                for (int k = 0; k < TAM_PARCIAL; k++) {
                    total[k] += parcial[k];
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Processamento interrompido", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Falha no processamento paralelo", e.getCause());
        }
        return montar(total, n);
    }

    private long[] processarBloco(List<RegistroRequisicao> reqs, int ini, int fim, List<UnidadeEstoque> estoque) {
        long[] p = new long[TAM_PARCIAL];
        UnidadeEstoque[] est = estoque.toArray(new UnidadeEstoque[0]);
        for (int i = ini; i < fim; i++) {
            RegistroRequisicao r = reqs.get(i);
            int compat = 0;
            long volume = 0;
            for (UnidadeEstoque u : est) {
                if (u.tipo() == r.tipo() && aboCompativel(u.abo(), r.abo()) && rhCompativel(u.rh(), r.rh())) {
                    compat++;
                    volume += u.volumeMl();
                }
            }
            int base = 3 + r.prioridade().ordinal() * CAMPOS;
            p[base]++;
            p[base + 2] += compat;
            if (compat > 0 && volume >= r.volumeMl()) {
                p[base + 1]++;
            }
            if (compat == 0) {
                p[1]++;
            }
            p[0] += volume;
            p[2] += r.id() * (compat + 1); // checksum sensível a cada requisição
        }
        return p;
    }

    private RelatorioResultado montar(long[] p, int total) {
        Map<Prioridade, RelatorioResultado.ResumoPrioridade> map = new EnumMap<>(Prioridade.class);
        long atendiveis = 0;
        long unidades = 0;
        for (Prioridade pr : Prioridade.values()) {
            int b = 3 + pr.ordinal() * CAMPOS;
            map.put(pr, new RelatorioResultado.ResumoPrioridade(p[b], p[b + 1], p[b + 2]));
            atendiveis += p[b + 1];
            unidades += p[b + 2];
        }
        return new RelatorioResultado(total, atendiveis, p[1], unidades, p[0], p[2], map);
    }

    /** Doador -> receptor. */
    static boolean aboCompativel(TipoABO doador, TipoABO receptor) {
        return doador == TipoABO.O || doador == receptor || receptor == TipoABO.AB;
    }

    static boolean rhCompativel(FatorRh doador, FatorRh receptor) {
        return doador == FatorRh.NEGATIVO || receptor == FatorRh.POSITIVO;
    }
}
