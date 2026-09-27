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

| Variável | Obrigatória | Descrição |
|---|---|---|
| `DB_URL` | sim | URL JDBC do Postgres no Neon (`jdbc:postgresql://.../neondb?sslmode=require&channel_binding=require`) |
| `DB_USERNAME` | sim | Usuário do banco no Neon |
| `DB_PASSWORD` | sim | Senha do banco no Neon |
| `JWT_SECRET` | sim | Chave que assina os tokens de login (HS256). Mínimo de 32 caracteres |
| `ADMIN_EMAIL` | não | E-mail do `ADMIN_SISTEMA` criado na primeira subida |
| `ADMIN_SENHA` | não | Senha desse admin |
| `GRAFANA_OTLP_ENABLED` | não | `true` para exportar métricas. Ver [`telemetria.md`](./telemetria.md) |
| `GRAFANA_OTLP_ENDPOINT` | não | Endpoint OTLP do Grafana Cloud |
| `GRAFANA_OTLP_TOKEN` | não | Token do Grafana Cloud, no formato `Basic <base64>` |

O `backend/src/main/resources/application-prod.properties` lê as três variáveis do banco (`${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`) e ativa o driver do Postgres (`org.postgresql.Driver`) no lugar do H2 usado em desenvolvimento.

### `JWT_SECRET` — sem ela o serviço não sobe

O `application.properties` declara `hemotrack.jwt.segredo=${JWT_SECRET}` **sem valor padrão**, de propósito: um segredo padrão esquecido em produção é uma falha de segurança clássica. Se a variável não existir, a aplicação falha na inicialização com `Could not resolve placeholder 'JWT_SECRET'` — em produção, o container reinicia em loop.

Gere com:

```bash
openssl rand -base64 48
```

O valor precisa ser **diferente** do que está no `backend/.env` de cada desenvolvedor: se o de desenvolvimento vazar, os tokens de produção continuam íntegros.

> **Trocar o `JWT_SECRET` invalida todos os tokens em circulação.** Todo mundo que estava logado recebe `401` e precisa entrar de novo. É o procedimento correto em caso de suspeita de vazamento, e uma boa razão para não mexer sem necessidade.

### `ADMIN_EMAIL` e `ADMIN_SENHA` — quem aprova instituições

Sem elas, a aplicação sobe e avisa no log, mas nenhum `ADMIN_SISTEMA` é criado e não há como aprovar cadastros pela API. O admin é criado uma única vez, na primeira subida em que as variáveis existam; depois disso o seed é ignorado, e **alterar `ADMIN_SENHA` não redefine a senha** de um admin já criado — isso evita que a variável de ambiente vire um mecanismo permanente de troca de senha para quem tiver acesso ao painel.

---

## Como reproduzir o deploy do zero

1. Criar um projeto no [Neon](https://neon.tech) (região São Paulo) e copiar a connection string.
2. Converter a connection string pro formato JDBC (`postgresql://` → `jdbc:postgresql://`) e separar usuário/senha.
3. Criar um Web Service no [Render](https://render.com), apontando para este repositório, com:
   - Root Directory: `backend`
   - Environment: Docker
4. Adicionar as variáveis de ambiente obrigatórias (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`) nas configurações do serviço. As `ADMIN_*` e `GRAFANA_*` são opcionais.
5. Copiar o **Deploy Hook** (Render → o serviço → Settings) e salvá-lo como secret `RENDER_DEPLOY_HOOK_URL` no GitHub (Settings → Secrets and variables → Actions).
6. A partir daí, todo merge na `main` dispara build, testes e deploy automaticamente.

---

## Robustez / reprodutibilidade

O pipeline foi executado múltiplas vezes durante a configuração, sem falhas intermitentes — o `build-and-test` compila e roda os testes de forma determinística (não depende de estado externo), e o `deploy` é uma chamada HTTP simples e idempotente ao Deploy Hook.

Se o job `deploy` falhar, o motivo mais provável é o secret `RENDER_DEPLOY_HOOK_URL` estar ausente ou desatualizado (por exemplo, se o serviço no Render for recriado, o hook muda). Nesse caso, o `build-and-test` continua passando normalmente — só o gatilho de deploy fica pendente até o secret ser atualizado.

### Falha que o pipeline não detecta

O job `deploy` só dispara o hook: ele reporta sucesso assim que o Render aceita a chamada, **antes** de o container subir. Se faltar uma variável de ambiente obrigatória no Render, o GitHub Actions fica todo verde e o serviço morre na inicialização.

Por isso, ao introduzir qualquer configuração obrigatória nova, a variável precisa existir no Render **antes** do merge na `main`. Depois de um deploy, a verificação rápida é:

```bash
curl https://hemotrack-8ecl.onrender.com/actuator/health
```

Esperado: `{"status":"UP"}`. Se não responder (descontado o cold start de 30–60s), veja os logs em Render → o serviço → **Logs**.

---

## Local vs. Produção

| | Local (dev) | Produção (Render) |
|---|---|---|
| Banco | H2 em arquivo (`./data/hemotrack-db`) | Postgres (Neon) |
| Profile Spring | padrão (`application.properties`) | `prod` (`application-prod.properties`) |
| `JWT_SECRET` | `backend/.env` (fora do Git) | variável de ambiente do Render |
| Como rodar | `./mvnw spring-boot:run` | `--spring.profiles.active=prod`, automático via Docker |

Instruções completas de execução local já estão na seção ["Como rodar na sua máquina"](../README.md#como-rodar-na-sua-máquina) do README principal.