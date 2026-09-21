package com.hemotrack.backend.relatorio;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/relatorio")
public class RelatorioController {

    private final RelatorioService service;
    private final BenchmarkService benchmark;

    public RelatorioController(RelatorioService service, BenchmarkService benchmark) {
        this.service = service;
        this.benchmark = benchmark;
    }

    /** Recebe requisições + estoque, processa e devolve o relatório. threads=1 => sequencial. */
    @PostMapping
    public RelatorioResultado gerar(@RequestBody RelatorioRequest body,
                                    @RequestParam(defaultValue = "1") int threads,
                                    @RequestParam(defaultValue = "false") boolean virtual) {
        if (body.requisicoes() == null || body.estoque() == null || threads < 1 || threads > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "requisicoes/estoque obrigatórios; threads 1..64");
        }
        return service.paralelo(body.requisicoes(), body.estoque(), threads, virtual);
    }

    /** Gera dados sintéticos no servidor e mede todas as versões (sem custo de rede/JSON). */
    @GetMapping("/benchmark")
    public BenchmarkService.Linha benchmark(@RequestParam(defaultValue = "100000") int registros,
                                            @RequestParam(defaultValue = "1000") int estoque,
                                            @RequestParam(defaultValue = "5") int repeticoes) {
        if (registros < 1 || registros > 5_000_000 || estoque < 1 || estoque > 100_000
                || repeticoes < 1 || repeticoes > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "parâmetros fora do limite");
        }
        return benchmark.executar(registros, estoque, repeticoes);
    }
}
