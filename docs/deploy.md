# Deploy e Pipeline de CI/CD — HemoTrack Backend

## Aplicação em produção

**URL pública:** https://hemotrack-8ecl.onrender.com

Endpoint de verificação de saúde: `GET /actuator/health` → retorna `{"status":"UP"}` quando o backend e a conexão com o banco estão funcionando.

> **Nota sobre cold start:** o serviço roda no plano gratuito do Render, que "dorme" após ~15 minutos sem tráfego. A primeira requisição depois disso pode demorar 30–60 segundos para responder enquanto o serviço acorda. Se for demonstrar ao vivo, acesse a URL alguns minutos antes.

---

## Infraestrutura

| Componente | Serviço | Motivo da escolha |
|---|---|---|
| Backend (Web Service) | [Render](https://render.com), plano Free | Deploy grátis sem cartão de crédito, integra direto com o GitHub, redeploy automático a cada push |
| Banco de dados | [Neon](https://neon.tech), Postgres serverless, plano Free | O Postgres gratuito nativo do Render expira em 30 dias e é apagado — inviável para um projeto com entrega em dezembro. O Neon não tem essa expiração |
| Containerização | Docker (multi-stage build) | O runtime "Java" não estava disponível diretamente na interface do Render no momento da configuração; Docker garante o build reprodutível independente disso |

---

## Pipeline de CI/CD

Arquivo: [`.github/workflows/ci.yml`](../.github/workflows/ci.yml)

### Gatilhos
- `push` e `pull_request` para a branch `main`

### Job `build-and-test`
Roda sempre (push ou PR):
1. Checkout do código
2. Instala JDK 21 (Temurin)
3. Roda `./mvnw -B clean verify` dentro de `backend/` — compila e executa os testes
4. Sobe o relatório de testes (`surefire-reports`) como artefato do GitHub Actions

### Job `deploy`
Roda **somente** quando `build-and-test` passa **e** o evento é um `push` direto na `main` (não roda em Pull Requests, para não publicar código ainda não revisado):
1. Chama o **Deploy Hook** do Render via `curl -X POST`
2. A URL do hook fica no secret `RENDER_DEPLOY_HOOK_URL` (GitHub → repositório → Settings → Secrets and variables → Actions) — nunca é exposta no código

Esse desenho garante que nenhum deploy acontece se os testes falharem, e que o deploy é sempre consequência direta de um merge na `main`, não de uma ação manual.

---

## Configuração do Web Service no Render

| Campo | Valor |
|---|---|
| Environment | Docker |
| Root Directory | `backend` |
| Dockerfile Path | `backend/Dockerfile` (padrão) |
| Branch | `main` |
| Plano | Free (0.1 CPU / 512 MB RAM) |

### Variáveis de ambiente
Configuradas em Render → o serviço → **Environment**:

| Variável | Descrição |
|---|---|
| `DB_URL` | URL JDBC do Postgres no Neon (`jdbc:postgresql://.../neondb?sslmode=require&channel_binding=require`) |
| `DB_USERNAME` | Usuário do banco no Neon |
| `DB_PASSWORD` | Senha do banco no Neon |

O `backend/src/main/resources/application-prod.properties` lê essas três variáveis (`${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`) e ativa o driver do Postgres (`org.postgresql.Driver`) no lugar do H2 usado em desenvolvimento.

---

## Como reproduzir o deploy do zero

1. Criar um projeto no [Neon](https://neon.tech) (região São Paulo) e copiar a connection string.
2. Converter a connection string pro formato JDBC (`postgresql://` → `jdbc:postgresql://`) e separar usuário/senha.
3. Criar um Web Service no [Render](https://render.com), apontando para este repositório, com:
   - Root Directory: `backend`
   - Environment: Docker
4. Adicionar as três variáveis de ambiente (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) nas configurações do serviço.
5. Copiar o **Deploy Hook** (Render → o serviço → Settings) e salvá-lo como secret `RENDER_DEPLOY_HOOK_URL` no GitHub (Settings → Secrets and variables → Actions).
6. A partir daí, todo merge na `main` dispara build, testes e deploy automaticamente.

---

## Robustez / reprodutibilidade

O pipeline foi executado múltiplas vezes durante a configuração, sem falhas intermitentes — o `build-and-test` compila e roda os testes de forma determinística (não depende de estado externo), e o `deploy` é uma chamada HTTP simples e idempotente ao Deploy Hook.

Se o job `deploy` falhar, o motivo mais provável é o secret `RENDER_DEPLOY_HOOK_URL` estar ausente ou desatualizado (por exemplo, se o serviço no Render for recriado, o hook muda). Nesse caso, o `build-and-test` continua passando normalmente — só o gatilho de deploy fica pendente até o secret ser atualizado.

---

## Local vs. Produção

| | Local (dev) | Produção (Render) |
|---|---|---|
| Banco | H2 em arquivo (`./data/hemotrack-db`) | Postgres (Neon) |
| Profile Spring | padrão (`application.properties`) | `prod` (`application-prod.properties`) |
| Como rodar | `./mvnw spring-boot:run` | `--spring.profiles.active=prod`, automático via Docker |

Instruções completas de execução local já estão na seção ["Como rodar na sua máquina"](../README.md#como-rodar-na-sua-máquina) do README principal.