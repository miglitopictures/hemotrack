package com.hemotrack.backend.relatorio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

/** Mede sequencial x 2/4/8 threads (e virtual threads) sobre dados gerados em memória. */
@Service
public class BenchmarkService {

    private static final int[] THREADS = {2, 4, 8};

    private final RelatorioService relatorio;

    public BenchmarkService(RelatorioService relatorio) {
        this.relatorio = relatorio;
    }

    public record Medicao(String versao, int threads, boolean virtual, long tempoMs, double speedup,
                          boolean resultadoIgualAoSequencial) {
    }

    public record Linha(int registros, int estoque, int repeticoes, List<Medicao> medicoes,
                        RelatorioResultado resultado) {
    }

    public Linha executar(int registros, int estoque, int repeticoes) {
        List<RegistroRequisicao> reqs = GeradorDados.requisicoes(registros, 42);
        List<UnidadeEstoque> est = GeradorDados.estoque(estoque, 7);

        // aquecimento da JIT (fora da medição)
        RelatorioResultado ref = relatorio.sequencial(reqs, est);
        relatorio.paralelo(reqs, est, 4, false);

        List<Medicao> medicoes = new ArrayList<>();
        long base = mediana(repeticoes, () -> relatorio.sequencial(reqs, est), ref, new boolean[] {true});
        medicoes.add(new Medicao("Sequencial", 1, false, base, 1.0, true));
        for (boolean virtual : new boolean[] {false, true}) {
            for (int t : THREADS) {
                boolean[] igual = {true};
                long ms = mediana(repeticoes, () -> relatorio.paralelo(reqs, est, t, virtual), ref, igual);
                medicoes.add(new Medicao(virtual ? "Virtual threads" : "Threads", t, virtual, ms,
                        Math.round(100.0 * base / Math.max(1, ms)) / 100.0, igual[0]));
            }
        }
        return new Linha(registros, estoque, repeticoes, medicoes, ref);
    }

    private long mediana(int repeticoes, Supplier<RelatorioResultado> op, RelatorioResultado ref, boolean[] igual) {
        long[] tempos = new long[Math.max(1, repeticoes)];
        for (int i = 0; i < tempos.length; i++) {
            long t0 = System.nanoTime();
            RelatorioResultado r = op.get();
            tempos[i] = (System.nanoTime() - t0) / 1_000_000;
            if (!r.equals(ref)) {
                igual[0] = false;
            }
        }
        Arrays.sort(tempos);
        return tempos[tempos.length / 2];
    }
}
