# Processamento paralelo: cruzamento Requisições × Estoque

## 1. Operação escolhida
**Cruzar requisições com estoque** (`POST /relatorio`). Para cada requisição de hemocomponente, o serviço percorre
todo o estoque e conta as unidades compatíveis (mesmo tipo, ABO doador→receptor e Rh compatíveis) e o volume
disponível. No fim gera estatísticas: total, atendíveis, sem compatibilidade, unidades/volume compatíveis e
resumo por prioridade (NORMAL/EMERGENCIA/URGENCIA), mais um checksum para provar igualdade entre versões.

- **Big-O:** O(R · E), com R = nº de requisições e E = tamanho do estoque (estoque = 1.000 nos testes).
  Com E fixo, cresce linearmente em R; a constante é grande (R·E comparações).
- **Gargalo:** CPU pura da aplicação (laços, comparações de enums, acesso a objetos em memória). Não há banco
  nem rede na medição: `GET /relatorio/benchmark` gera os dados na própria JVM (semente fixa, reproduzível).
- **Por que dividir:** cada requisição só lê o estoque (imutável) e produz contadores próprios; não depende
  de nenhuma outra requisição. Logo o vetor de R requisições vira N blocos independentes. Cada thread acumula
  em um `long[]` privado (sem locks, sem estado compartilhado) e os parciais são somados no fim. Como só há
  inteiros, a soma é associativa e o resultado é **exatamente igual** (verificado por `equals` no benchmark).

## 2. Implementação
Pacote `com.hemotrack.backend.relatorio`:
- `RelatorioService.sequencial` (1 thread) e `RelatorioService.paralelo(threads, virtual)` (pool fixo de 2/4/8
  threads de plataforma, ou `newVirtualThreadPerTaskExecutor` para virtual threads).
- `POST /relatorio?threads=N&virtual=false` recebe `{requisicoes:[...], estoque:[...]}` e devolve o resultado.
- `GET /relatorio/benchmark?registros=1000000&estoque=1000&repeticoes=5` gera dados e mede todas as versões
  (aquecimento da JIT fora da medição; tempo = mediana de 5 execuções). Interface com tabela e gráfico: `/benchmark.html`.
- Java alterado para 21 no `pom.xml` (necessário para virtual threads).

## 3. Medições
Máquina: 16 processadores lógicos, Windows 11, JDK 21. Dados brutos em `docs/benchmark-*.json`.

| Registros | Sequencial | 2 threads | 4 threads | 8 threads | 2 virtuais | 4 virtuais | 8 virtuais |
|-----------|-----------:|----------:|----------:|----------:|-----------:|-----------:|-----------:|
| 100 mil   | 88 ms  | 50 ms  | 23 ms  | 12 ms  | 44 ms  | 23 ms  | 12 ms  |
| 1 milhão  | 898 ms | 468 ms | 251 ms | 127 ms | 470 ms | 232 ms | 132 ms |

**Speedup** (sequencial ÷ paralelo):

| Registros | 2 threads | 4 threads | 8 threads | 2 virt. | 4 virt. | 8 virt. |
|-----------|----------:|----------:|----------:|--------:|--------:|--------:|
| 100 mil   | 1,76 | 3,83 | 7,33 | 2,00 | 3,83 | 7,33 |
| 1 milhão  | 1,92 | 3,58 | 7,07 | 1,91 | 3,87 | 6,80 |

Gráfico de barras dos tempos: abrir `http://localhost:8080/benchmark.html` e clicar em *Rodar*.

```
1M registros (ms)     0        450       900
Sequencial            ████████████████████ 898
2 threads             ██████████▍ 468
4 threads             █████▌ 251
8 threads             ██▊ 127
```

## 4. Análise
O ganho foi **quase linear** (8 threads ≈ 7,1× em 1M), pois o problema é "embaraçosamente paralelo": blocos
independentes, sem locks e sem I/O. Não é perfeitamente linear porque restam partes sequenciais e custos de
paralelizar (Lei de Amdahl): criar o pool, submeter tarefas, somar os parciais, além de competição por cache
L3 e largura de banda de memória, o turbo boost menor com mais núcleos ativos e o desequilíbrio entre blocos.
Com poucos dados (100 mil) a parte fixa pesa mais; ruído de medição também explica variações (ex.: 2 threads
de 1,76× a 2,0×).

A **Big-O não mudou**: continua O(R·E). O paralelismo divide o trabalho total por N, mas o número de operações
é o mesmo (na verdade, um pouco maior pelo overhead); só o tempo de relógio cai, para ≈ O(R·E / N).

**Concorrência × paralelismo:** concorrência é lidar com várias tarefas no mesmo período (podem se intercalar
em um único núcleo); paralelismo é executá-las literalmente ao mesmo tempo em núcleos diferentes. Aqui há
paralelismo real (16 núcleos). As threads da Mesa DJ eram um problema de concorrência/sincronização: threads com
papéis diferentes, compartilhando recursos e coordenadas por exclusão mútua/semáforos, onde o objetivo era
correção e ordenação. Neste trabalho as threads executam a mesma função sobre dados disjuntos, sem
compartilhar estado, e o objetivo é desempenho.

**Virtual threads:** ficaram equivalentes às threads de plataforma (≈ 6,8–7,3× com 8 tarefas). Faz sentido: elas
brilham em tarefas que bloqueiam (I/O), pois desmontam do carrier ao esperar; em trabalho CPU-bound o limite
são os núcleos, não a criação/bloqueio de threads.

**Se 8 threads não bastarem:** aumentar até o nº de núcleos (`Runtime.availableProcessors()`), usar
`ForkJoinPool`/parallel streams com divisão adaptativa, melhorar o algoritmo (indexar o estoque por
tipo/ABO/Rh e pré-agregar: O(R + E) em vez de O(R·E), ganho maior que qualquer thread) e depois escalar
horizontalmente. **Arquitetura maior:** API stateless atrás de load balancer, várias instâncias; requisições
enviadas a uma fila (Kafka/RabbitMQ) e processadas por workers particionados (ex.: por hospital); estoque em
cache (Redis) e banco com réplicas de leitura; relatórios pesados assíncronos (retorna 202 + id do job),
resultados parciais agregados em estilo map-reduce (Spark/Flink), com métricas e autoescalonamento.
