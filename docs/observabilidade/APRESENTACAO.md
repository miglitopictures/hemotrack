# Guia de apresentacao - HemoTrack observabilidade

Publico: integrantes que nao implementaram a stack. Duracao sugerida: 8 a 10 minutos.
Antes de apresentar, leiam este guia e facam uma passagem completa sem o professor.
O prazo informado foi 27/11/2026; a janela exata de acesso remoto ainda deve ser confirmada.

## O que foi feito, em uma frase

"Instrumentamos o backend Java do HemoTrack e reunimos metricas, logs e traces em
um Grafana, com ambiente reproduzivel por Docker Compose e acesso de leitura."

O HemoTrack organiza estoque e requisicoes de hemocomponentes entre hospitais e
hemocentros. Esta entrega observa o sistema: responde se esta saudavel, onde esta
lento, quais operacoes falham e como uma requisicao percorre servico e banco.
A interface React e o sistema monitorado; o Grafana e a interface de diagnostico.

## Divisao sugerida entre apresentadores

| Pessoa | Tempo | Responsabilidade |
|---|---|---|
| A | 0:00-1:30 | Contexto e arquitetura |
| B | 1:30-4:00 | Metricas, Prometheus e dashboards |
| C | 4:00-7:00 | Logs, traces e correlacao |
| D ou A | 7:00-9:00 | Seguranca, acesso remoto, testes e encerramento |

Uma mesma pessoa pode acumular papeis. Escolham quem controla o navegador;
quem fala nao deve ficar procurando menus durante a apresentacao.

## Preparacao, 20 minutos antes

1. Ligue a maquina na energia, abra Docker Desktop e mantenha a tampa aberta.
2. Na raiz do repositorio: `docker compose up -d --build`.
3. Confira `docker compose ps`; a API deve ficar healthy. Aguarde alguns minutos.
4. Execute `docker compose exec -T carga python /scripts/verificar.py`.
5. Abra Grafana em http://localhost:3000 e entre com as credenciais locais.
6. Deixe abertas as abas: dashboard Aplicacao; Infraestrutura; Explore Loki; trace.
7. Se for remoto, inicie o tunel e teste a URL com o usuario Viewer em outra rede.
8. Tenha o PDF e os prints reais salvos como apoio. Nunca mostre o `.env` na tela.

Se qualquer validacao falhar, consulte a tabela de problemas abaixo antes de falar
que a etapa esta concluida. O relatorio de verificacao mostra o que foi testado.

## Roteiro falado e cliques

### 1. Contexto e arquitetura (0:00-1:30)

Mostre o diagrama do relatorio.

Fala sugerida: "O backend Spring Boot usa Postgres. O Actuator disponibiliza
metricas internas; Prometheus coleta a cada 15 segundos. Node Exporter observa a
VM Linux e cAdvisor observa os containers. Os logs JSON passam pelo Alloy para o
Loki. Os traces seguem por OTLP para o Tempo. O Grafana consulta as tres fontes."

Explique os termos: metrica e um numero ao longo do tempo; log e um registro de um
acontecimento; trace e o caminho de uma requisicao, dividido em spans, suas etapas.
Um log e uma linha de evento, e nao uma serie numerica nem o trace completo.

### 2. Compose e targets (1:30-2:30)

Mostre `docker compose ps`, com data/hora visivel. Mostre a captura de
Prometheus Status -> Targets (ou a tela local com o override de evidencias).
No Grafana -> Explore -> Prometheus, execute `up`.

Fala: "Os quatro jobs de coleta estao UP. Isso confirma a coleta da aplicacao,
do host, dos containers e do proprio Prometheus. A configuracao e versionada,
incluindo fontes de dados e dashboards; nao precisamos montar tudo manualmente."

Nao diga que UP prova que todo o sistema funciona: prova que o scrape responde.

### 3. Dashboard da aplicacao e adicional (2:30-4:00)

Dashboards -> HemoTrack -> HemoTrack - Aplicacao e negocio.
Mostre requisicoes por rota, latencia p95, respostas por status, heap e HikariCP.
Role ate os paineis de negocio e explique as duas metricas.

Fala: "O contador registra requisicoes de hemocomponentes gravadas com sucesso.
O timer mede duracao e quantidade de consultas dessas requisicoes. Sao metricas
de dominio criadas com Micrometer e tem paineis proprios. A carga sintetica faz
operacoes reais, excluindo a requisicao de teste depois para nao acumular dados."

Counter cresce desde a partida e pode zerar com reinicio. Timer gera count/sum e
histograma. p95 significa que 95% das observacoes ficaram abaixo daquele tempo
estimado. O grafico de negocio nao representa todo o historico anterior do banco.

Abra o dashboard Infraestrutura. Mostre CPU/memoria do host e por container.
Diga explicitamente: "Neste Mac, os exporters observam a VM Linux do Docker."

### 4. Logs no Loki (4:00-5:00)

Explore -> Loki. Execute:

```logql
{application="hemotrack-backend"} | json | traceId != ""
```

Expanda uma linha de `TelemetriaService`. Mostre message, level e traceId.
Para o erro controlado:

```logql
{application="hemotrack-backend"} | json | level="WARN"
```

Fala: "O filtro application identifica nossa aplicacao. O gerador consulta um
ID inexistente, que retorna 404 e produz WARN; e um erro esperado de dominio.
O sistema continua atendendo, e o evento pode ser investigado pelo traceId."

Nao chame esse caso de erro 500: ele e 404. Nao invente indisponibilidade do banco.

### 5. Do log ao trace (5:00-7:00)

Escolha um log de consulta bem-sucedida, expanda e clique em **Abrir trace no Tempo**.
Mostre a linha do tempo, o span HTTP, a observacao de servico e os spans JDBC.
Compare o traceId exibido no log com o trace aberto.

Fala: "O mesmo identificador liga o evento do log ao caminho da requisicao.
Os spans mostram em que etapa o tempo foi gasto: HTTP, consulta de negocio ou
banco. Assim investigamos uma falha especifica sem procurar manualmente em tudo."

Use um evento recente, do intervalo de tempo selecionado. Nao use um log de
partida do Spring: ele pode nao ter contexto de requisicao nem traceId.

### 6. Seguranca, acesso e testes (7:00-9:00)

Mostre a URL remota ja conferida e o usuario Viewer, sem exibir senha em publico.
Mostre a tentativa bloqueada de `http://localhost:8080/actuator/prometheus` e
explique que o scrape usa internamente a porta 8082, nao a porta publica da API.

Fala: "Publicamos remotamente somente Grafana, por HTTPS, com usuario de leitura.
Os coletores e os endpoints de gerenciamento permanecem na rede interna. Senhas
ficam em `.env` ignorado pelo Git. Os testes verificam as regras existentes, as
metricas de negocio e o isolamento dos endpoints de gerenciamento."

Feche apontando o README, as configuracoes reproduziveis, os JSONs e as evidencias.
Informe o periodo em que o professor podera acessar. Se o remoto ainda nao estiver
validado, diga que esta pendente; nao o apresente como concluido.

## Perguntas provaveis

| Pergunta | Resposta objetiva |
|---|---|
| Por que Prometheus e Grafana? | Prometheus coleta series temporais; Grafana consulta e apresenta metricas, logs e traces no mesmo lugar. |
| O Grafana armazena tudo? | Nao. Metricas ficam no Prometheus, logs no Loki e traces no Tempo. |
| Por que Alloy? | Le o arquivo JSON por volume e envia ao Loki; evita dar ao coletor acesso ao socket Docker. |
| Actuator pode ficar publico? | Os endpoints de gerenciamento usam 8082 interna; a API em 8080 bloqueia `/actuator/**`. |
| Por que ha 404 no dashboard? | A carga busca um ID inexistente de proposito para demonstrar o tratamento de erro e os logs. |
| Por que nao aparecem dados imediatamente? | Os scrapes sao a cada 15s e as taxas precisam de mais de uma amostra. |
| A metrica de criacao e o total no banco? | Nao. Conta eventos de criacao bem-sucedidos desde a partida; exclusoes nao diminuem o contador. |
| Por que contar consultas que falharam no timer? | Tentativas com erro tambem consomem tempo e sao relevantes para diagnostico. |
| Qual e a diferenca entre traceId e spanId? | traceId identifica o caminho completo; spanId identifica uma etapa desse caminho. |
| O frontend precisa de Actuator? | Nao e Java; o requisito se aplica ao backend Spring Boot. |
| A coleta mede o Mac todo? | Nao; no Docker Desktop mede a VM Linux. |
| O que acontece se desligar a maquina? | O ambiente e o tunel ficam indisponiveis; precisamos manter a maquina ligada na janela de correcao. |
| O tunel e permanente? | O Quick Tunnel e temporario e pode mudar de URL; para estabilidade maior, usar tunel nomeado ou VM. |
| O Render foi substituido? | O laboratorio e separado; o perfil de traces so e ativado pelo Compose. |

## Se algo der errado durante a demo

| Sintoma | Acao |
|---|---|
| Grafana nao abre | Verifique Docker e `docker compose ps grafana`; confira a porta 3000. |
| API unhealthy | `docker compose logs --tail=80 api db`; nao mostre segredos na projecao. |
| Graficos vazios | Aguarde 2 minutos; selecione Last 15 minutes; confira `up` e logs de `carga`. |
| Loki sem eventos | Confira `docker compose logs --tail=40 alloy` e a existencia do volume de logs. |
| Log sem link de trace | Escolha um evento de consulta/criacao com traceId; nao um log de startup. |
| Trace nao encontrado | Aguarde alguns segundos para exportacao; escolha log recente e confira Tempo. |
| URL externa parou | Veja os logs de tunnel, confira se a URL mudou e se a maquina nao dormiu. |
| Viewer sem menus de administracao | Esperado: o professor tem permissao de leitura. |

Se precisar usar prints, diga que sao evidencias de execucao previamente capturadas.
Nao simule resultados ao vivo nem atribua ao ambiente uma validacao que nao ocorreu.

## Arquivos que cada apresentador deve conhecer

- `compose.yaml`: os servicos e suas conexoes.
- `prometheus/prometheus.yml`: os quatro jobs de coleta.
- `grafana/provisioning/`: fontes de dados, correlacao e carga dos dashboards.
- `grafana/dashboards/`: os dois dashboards exportados.
- `loki/`: armazenamento dos logs e configuracao do Alloy.
- `tempo/tempo.yml`: recepcao e armazenamento de traces.
- `TelemetriaService.java`: duas metricas de negocio e observacoes de servico.
- `SecurityConfig.java`: separacao entre porta publica e gerenciamento.
- `scripts/carga.py`: carga sintetica e caso 404.
- `scripts/verificar.py`: verificacao datada de coleta, logs e traces reais.

## Integracao com a main e Render

A alteracao de observabilidade esta em branch separada para revisao. Na migracao,
o responsavel pelo Render deve deixar Health Check Path temporariamente vazio (TCP),
publicar a nova versao e testar `/health` (HTTP 200). So depois deve configurar
Health Check Path como `/health`; a sonda
publica minima retorna somente UP/DOWN (200/503), sem expor o Actuator.
O PDF privado inclui a conta do professor; nao deve entrar no repositorio publico.
