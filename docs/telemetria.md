# Telemetria

O backend exporta métricas no padrão **OpenTelemetry (OTLP)** para o **Grafana Cloud**, onde são consultadas e visualizadas. É o primeiro passo da HU08 (monitorar transporte) e da HU09 (indicadores).

## O que está configurado

| Peça | Onde | Para quê |
|---|---|---|
| `spring-boot-starter-actuator` | `pom.xml` | Coleta métricas automaticamente (HTTP, JVM) e expõe `/actuator/health`, `/actuator/metrics`, `/actuator/info` |
| `spring-boot-starter-opentelemetry` | `pom.xml` | Envia as métricas via OTLP. No Spring Boot 4 esse starter é **obrigatório**: só o `micrometer-registry-otlp` não ativa o export |
| `datasource-micrometer-spring-boot` | `pom.xml` | Mede o tempo das queries JDBC/JPA |
| `management.otlp.metrics.export.*` | `application.properties` | Endpoint, token e intervalo de envio (10s), lidos do `backend/.env` ou de variáveis de ambiente |
| `management.metrics.tags.application` | `application.properties` | Marca toda métrica com `application="hemotrack-backend"` |

O export vem **desligado por padrão**. Sem o `.env` o backend sobe normalmente, só não envia nada.

## Como ligar

1. Peça um token a Miguel (ele administra a conta do Grafana Cloud).
2. Dentro de `backend/`, crie seu `.env` a partir do modelo e cole o token:

   ```bash
   cd backend
   cp .env.example .env
   ```

   ```properties
   GRAFANA_OTLP_ENABLED=true
   GRAFANA_OTLP_ENDPOINT=https://otlp-gateway-prod-sa-east-1.grafana.net/otlp/v1/metrics
   GRAFANA_OTLP_TOKEN=Basic <token-em-base64>
   ```

3. Rode normalmente: `./mvnw spring-boot:run`. O Spring lê o `.env` sozinho (`spring.config.import` no `application.properties`).

O `.env` está no `.gitignore`. **Nunca commite o token.** Se preferir, as mesmas chaves podem ser exportadas como variáveis de ambiente; elas têm prioridade sobre o `.env`.

## Como conferir

1. Gere tráfego: `curl http://localhost:8080/requisicoes` algumas vezes, ou com o Postman/Insomnia, ou como preferir!
2. Espere uns 20s e, no Grafana, abra **Explore** com o data source `grafanacloud-<stack>-prom`.

3. Queries úteis:
   ```
   target_info                                                   # a aplicação está enviando?
   sum by (uri) (http_server_requests_milliseconds_count)        # total de requisições por rota
   sum by (uri) (http_server_requests_milliseconds_sum)
     / sum by (uri) (http_server_requests_milliseconds_count)    # latência média por rota (ms)
   ```

## Armadilhas que já custaram tempo

- **Grafana não carrega no Firefox** (`RangeError: invalid language tag: ""`): em `about:config`, defina `intl.accept_languages` como `pt-BR, pt, en-US, en`.

## Próximos passos
- Métricas de negócio: transições de status da requisição (contador por status) e tempo entre `ALOCADA` e `ATENDIDA`.
- Status `EM_TRANSITO` com as rotas `/enviar` e `/receber` (ver [`api/dominio.md`](./api/dominio.md)), para instrumentar o transporte.
- Tracing do fluxo de transporte e um dashboard dedicado no Grafana.
