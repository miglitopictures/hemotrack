# Como rodar e testar o HemoTrack

Guia completo para subir o HemoTrack numa máquina nova, usar o sistema pela
primeira vez, testar a API e rodar os testes automatizados.

Há dois jeitos de rodar, para objetivos diferentes:

| | [Docker Compose](#1-jeito-rápido-docker-compose) | [Modo desenvolvimento](#2-modo-desenvolvimento) |
|---|---|---|
| **Para quê** | ver o sistema completo funcionando, avaliar, apresentar | mexer no código |
| **Precisa instalar** | só o Docker | Git, JDK 21 e Node.js |
| **Banco** | Postgres (o mesmo motor da produção) | H2 em arquivo, com console no navegador |
| **Configuração** | `.env` na raiz do projeto | `backend/.env` |
| **Depois de alterar o código** | rebuild (`docker compose up -d --build`) | o frontend recarrega sozinho; o backend precisa reiniciar |

Nos dois casos, o sistema abre em `http://localhost:8081` e a API responde em
`http://localhost:8080`.

> **Versão publicada:** o sistema também está no ar em
> https://hemotrack-nrbm.onrender.com (API em
> https://hemotrack-api-3ns9.onrender.com). A API roda no plano gratuito do
> Render e "dorme" depois de ~15 min sem uso: a primeira requisição pode levar
> até 1 minuto. Detalhes em [`deploy.md`](./deploy.md).

**Sumário**

1. [Jeito rápido: Docker Compose](#1-jeito-rápido-docker-compose)
2. [Modo desenvolvimento](#2-modo-desenvolvimento)
3. [Usar a aplicação pela primeira vez](#3-usar-a-aplicação-pela-primeira-vez)
4. [Testar a API](#4-testar-a-api)
5. [Rodar os testes automatizados](#5-rodar-os-testes-automatizados)
6. [Conferir o banco](#6-conferir-o-banco)
7. [Telemetria](#7-telemetria-opcional)
8. [Observações](#8-observações)

---

## 1. Jeito rápido: Docker Compose

Sobe três containers: o banco (`db`, Postgres 16), a API (`api`, Spring Boot) e
o frontend (`web`, o build do React servido pelo nginx). Não precisa instalar
Java nem Node.

### 1.1 Instalar o Docker

| SO | Como instalar |
|---|---|
| Linux | Docker Engine com o plugin Compose ([guia oficial](https://docs.docker.com/engine/install/)) |
| macOS / Windows | [Docker Desktop](https://docs.docker.com/get-docker/) (já inclui o Compose) |

Confira com `docker compose version`.

No Linux, se aparecer `permission denied while trying to connect to the docker
API`, seu usuário não está no grupo `docker`:

```bash
sudo usermod -aG docker $USER
newgrp docker
```

### 1.2 Configurar e subir

```bash
git clone https://github.com/miglitopictures/hemotrack.git
cd hemotrack
cp .env.example .env
openssl rand -base64 48    # cole o resultado em JWT_SECRET
```

No `.env`, preencha:

| Variável | Obrigatória | O que colocar |
|---|---|---|
| `POSTGRES_PASSWORD` | sim | qualquer senha; é a do banco que roda no container |
| `JWT_SECRET` | sim | o resultado do `openssl` (mínimo de 32 caracteres) |
| `ADMIN_EMAIL`, `ADMIN_SENHA` | não, mas você vai querer | o usuário que aprova cadastros (ver [seção 3](#3-usar-a-aplicação-pela-primeira-vez)) |

O Compose se recusa a subir se faltar uma variável obrigatória, e diz qual.
O `.env` está no `.gitignore`: cada pessoa gera o seu.

```bash
docker compose up -d --build
```

O primeiro build leva alguns minutos: ele baixa as dependências, compila e
**roda todos os testes do backend**. Se algum teste falhar, a imagem não é
gerada e nada sobe.

### 1.3 Conferir

```bash
docker compose ps
```

Os três serviços devem aparecer como `healthy`. Eles sobem em ordem, cada um
esperando o healthcheck do anterior: `db`, depois `api`, depois `web`. Pode
levar até um minuto.

- Sistema: `http://localhost:8081`
- API: `http://localhost:8080/actuator/health` deve responder `{"status":"UP"}`

### 1.4 No dia a dia

| Para | Comando |
|---|---|
| Ver os logs da API | `docker compose logs -f api` |
| Aplicar uma alteração no código | `docker compose up -d --build` |
| Parar (os dados ficam guardados) | `docker compose down` |
| Parar e apagar o banco | `docker compose down -v` |

Os dados do Postgres ficam no volume `dados-postgres`: sobrevivem ao `down` e
só somem com `down -v`.

---

## 2. Modo desenvolvimento

Backend e frontend rodam direto na sua máquina, em dois terminais, e o banco é
um H2 em arquivo.

| Processo | Stack | Endereço |
|---|---|---|
| **backend** | Spring Boot (só API, sem telas) | `http://localhost:8080` |
| **frontend** | Vite + React | `http://localhost:8081` |

### 2.1 Pré-requisitos

- **Git**
- **JDK 21 ou superior** (Temurin/Adoptium é a distribuição mais comum)
- **Node.js 20.19 ou superior** e **npm** (o Vite 8 não roda em versões
  anteriores). Recomendado: Node 22, a versão usada no Docker e na produção.

Não instale o Maven: o projeto traz o Maven Wrapper (`mvnw` / `mvnw.cmd`), que
baixa a versão certa sozinho no primeiro uso.

**Git**

| SO | Comando |
|---|---|
| Linux (Debian/Ubuntu) | `sudo apt install git` |
| Linux (Fedora) | `sudo dnf install git` |
| macOS | `xcode-select --install` (ou `brew install git`) |
| Windows | `winget install Git.Git` (ou [git-scm.com](https://git-scm.com/download/win)) |

**JDK 21+**

| SO | Comando |
|---|---|
| Linux (Debian/Ubuntu) | `sudo apt install openjdk-21-jdk` |
| Linux (Fedora) | `sudo dnf install java-21-openjdk-devel` |
| macOS | `brew install openjdk@21` (e siga a instrução de `PATH` que o brew imprime) |
| Windows | `winget install EclipseAdoptium.Temurin.21.JDK` (ou [adoptium.net](https://adoptium.net)) |

Confira com `java -version`: precisa aparecer `21` ou mais. É o mínimo, não o
teto: JDKs mais novas rodam o projeto normalmente.

**Node.js**

Mais fácil com o **nvm** (Linux/macOS):

```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
nvm install 22
```

Windows: [nvm-windows](https://github.com/coreybutler/nvm-windows) ou o
instalador de [nodejs.org](https://nodejs.org).

Confira com `node -v` e `npm -v`.

### 2.2 Criar o `backend/.env` (obrigatório)

```bash
git clone https://github.com/miglitopictures/hemotrack.git
cd hemotrack
cp backend/.env.example backend/.env
openssl rand -base64 48    # cole o resultado em JWT_SECRET
```

No mesmo arquivo, preencha `ADMIN_EMAIL` e `ADMIN_SENHA` (ver
[seção 3](#3-usar-a-aplicação-pela-primeira-vez)). As variáveis `GRAFANA_*`
podem ficar como estão: a telemetria vem desligada.

Sem `JWT_SECRET`, o backend não sobe (`Could not resolve placeholder
'JWT_SECRET'`).

### 2.3 Rodar o backend (terminal 1)

```bash
cd backend
./mvnw spring-boot:run      # Linux / macOS
mvnw.cmd spring-boot:run    # Windows (cmd ou PowerShell)
```

- `Permission denied` no Linux/macOS: rode `chmod +x mvnw` antes.
- A primeira execução baixa o Maven e as dependências (precisa de internet). As
  seguintes usam o cache em `~/.m2`.

**Esperado:** `Started BackendApplication in X seconds` no log e, na primeira
subida com o admin configurado, `ADMIN_SISTEMA criado para ...`.

### 2.4 Rodar o frontend (terminal 2)

Abra **outro terminal**; o backend precisa continuar rodando:

```bash
cd frontend
npm install   # só na primeira vez
npm run dev
```

**Esperado:**

```
VITE v8.x.x  ready in XXX ms
➜  Local:   http://localhost:8081/
```

O frontend já aponta para `http://localhost:8080` (padrão em
`frontend/src/lib/httpClient.ts`; para trocar, defina `VITE_API_URL`).

Para parar qualquer um dos dois: `Ctrl+C`.

---

## 3. Usar a aplicação pela primeira vez

O fluxo é o mesmo nos dois jeitos de rodar.

Toda instituição nasce `PENDENTE_APROVACAO` e não opera até a operação do
HemoTrack aprovar. Quem aprova é um usuário com papel `ADMIN_SISTEMA`, que
**não** pode ser criado pelo cadastro público: ele nasce na subida da
aplicação, a partir de `ADMIN_EMAIL` e `ADMIN_SENHA` (no `.env` da raiz, no
Compose, ou no `backend/.env`, no modo desenvolvimento).

- Sem essas variáveis, a aplicação sobe e avisa no log, mas não há quem aprove
  cadastros.
- O admin é criado **uma única vez**. Se já existir, o seed é ignorado, e
  trocar `ADMIN_SENHA` depois **não** redefine a senha dele.

Abra `http://localhost:8081` e use **duas janelas** (uma normal e uma anônima)
para não misturar as sessões:

**1. Cadastre uma instituição** em `http://localhost:8081/cadastro`

Escolha hospital ou hemocentro e preencha os dados da instituição e do
responsável. Ao concluir, você já entra logado e cai na tela **Cadastro em
análise**.

**2. Aprove como operação do HemoTrack**, na janela anônima

Entre em `http://localhost:8081/login` com o `ADMIN_EMAIL` e a `ADMIN_SENHA`.
Você cai em **/admin**, com a lista de cadastros em análise. Clique em
**Aprovar**.

> O login do admin não funciona? As variáveis provavelmente não existiam quando
> a aplicação subiu. Preencha e reinicie (no Compose: `docker compose up -d`).

**3. Volte à primeira janela** e clique em **Verificar novamente**. O painel
abre (de hospital ou de hemocentro, conforme o tipo cadastrado). Não precisa
sair e entrar de novo: o status da instituição é lido do banco a cada
requisição, não do token.

**4. Use o sistema.** No painel do hospital dá para criar requisições e
acompanhar o status; no do hemocentro, aceitar ou recusar as requisições
recebidas (a escolha das bolsas ao aceitar ainda é só visual). O admin da instituição também gerencia os membros (operadores).

Para ver os dois lados, cadastre **um hospital e um hemocentro** e aprove os
dois.

---

## 4. Testar a API

A API é a mesma nos dois jeitos de rodar: `http://localhost:8080`. Para testar
a versão publicada, troque pela URL da API em produção.

### Como a autenticação funciona

Só duas rotas são públicas: `POST /auth/login` e `POST /instituicoes` (o
cadastro). Todo o resto exige o header:

```
Authorization: Bearer <token>
```

O token vem do login, vale **8 horas** e é recusado com `401` depois disso.
Todos os erros saem em `application/problem+json`, com `type`, `title`,
`status` e `detail`. A lista dos tipos está em
[`api/contrato-api.md`](./api/contrato-api.md#tipos-de-erro-emitidos).

Três cuidados que evitam a maioria dos erros de teste:

- **Sem barra no final.** `/instituicoes/` responde `404`; o certo é `/instituicoes`.
- **Sem prefixo `/api/v1`.** O contrato ainda mostra o prefixo, mas as rotas
  respondem na raiz.
- **Enums como texto**, em maiúsculas: `"tipo": "HOSPITAL"`, não `"tipo": 0`.

### Roteiro

Execute na ordem. Ele percorre o fluxo completo de cadastro e aprovação e
confere cada bloqueio.

| # | Chamada | Token | Esperado |
|---|---|---|---|
| 1 | `POST /instituicoes` (corpo abaixo) | — | `201`, instituição `PENDENTE_APROVACAO` |
| 2 | Repetir o passo 1 com o mesmo CNPJ | — | `409` |
| 3 | `POST /auth/login` com o e-mail do responsável | — | `200`, `{ token, expiraEm }` |
| 4 | `POST /auth/login` com senha errada | — | `401` |
| 5 | `GET /auth/me` | responsável | `200`, instituição `PENDENTE_APROVACAO` |
| 6 | `GET /auth/me` sem o header | — | `401` |
| 7 | `GET /requisicoes` | responsável | `403`: instituição não aprovada |
| 8 | `POST /auth/login` com `ADMIN_EMAIL` / `ADMIN_SENHA` | — | `200` |
| 9 | `GET /instituicoes?status=PENDENTE_APROVACAO` | admin | `200`, lista com a instituição do passo 1 |
| 10 | `PATCH /instituicoes/{id}/aprovar` | responsável | `403`: não é `ADMIN_SISTEMA` |
| 11 | `PATCH /instituicoes/{id}/aprovar` | admin | `200`, status `APROVADA` |
| 12 | Repetir o passo 11 | admin | `409`: já aprovada |
| 13 | `PATCH /instituicoes/999999/aprovar` | admin | `404` |
| 14 | `GET /requisicoes` | responsável (**mesmo token** do passo 3) | `200` |

O passo 14 mostra que a aprovação vale na hora, sem novo login.

**Corpo do cadastro (passo 1):**

```json
{
  "instituicao": {
    "razaoSocial": "Hospital Teste",
    "cnpj": "12.345.678/0001-90",
    "tipo": "HOSPITAL",
    "endereco": "Rua Teste, 100",
    "municipio": "Recife",
    "telefone": "(81) 3000-0000"
  },
  "administrador": {
    "nome": "Maria Teste",
    "email": "maria@hospitalteste.org",
    "senha": "senha-de-teste"
  }
}
```

**Corpo do login (passo 3):**

```json
{ "email": "maria@hospitalteste.org", "senha": "senha-de-teste" }
```

O `id` dos passos 10 a 13 está na resposta do passo 1 (`instituicao.id`). O
CNPJ volta só com dígitos: o backend normaliza para que `12.345.678/0001-90`
e `12345678000190` sejam reconhecidos como o mesmo. CNPJ que não tenha 14
dígitos depois de normalizado é recusado com `400`.

Para rodar o roteiro de novo, troque CNPJ e e-mail ou
[zere o banco](#6-conferir-o-banco).

### Com Insomnia ou Postman

O processo é o mesmo nas duas ferramentas; só os nomes dos menus mudam.

**1. Crie uma variável para a URL base.**

- Insomnia: *Base Environment* → `{ "baseUrl": "http://localhost:8080" }`
- Postman: *Environments* → novo ambiente → variável `baseUrl` com
  `http://localhost:8080` (e selecione o ambiente no canto superior direito)

Nas requisições, use `{{ baseUrl }}/auth/login` (Insomnia) ou
`{{baseUrl}}/auth/login` (Postman).

**2. Faça o login.** Nova requisição `POST {{baseUrl}}/auth/login` → aba
**Body** → **JSON** (no Postman: *raw* + *JSON*) → cole o corpo do login →
**Send**. Copie o valor de `token` da resposta, sem as aspas.

**3. Use o token.** Na requisição autenticada, aba **Auth** (Insomnia) ou
**Authorization** (Postman) → tipo **Bearer Token** → cole o token. No
Insomnia, deixe o campo *Prefix* vazio: ele já usa `Bearer`.

Para não colar o token em toda requisição, configure o Bearer uma vez na
**pasta** (Insomnia) ou na **collection** (Postman) e deixe as requisições
herdando (*Inherit auth from parent*). Guarde o token do admin e o do
responsável em duas variáveis (`tokenAdmin`, `tokenHospital`) e use
`{{tokenAdmin}}` no campo do Bearer para alternar entre eles.

**4. Leia a resposta.** Confira o **status** (canto superior da resposta)
contra a coluna *Esperado* do roteiro. Nos erros, o campo `detail` diz o
motivo.

### Com `curl`

Os exemplos usam sintaxe de shell bash (Linux, macOS ou Git Bash no Windows).
No PowerShell, chame `curl.exe` em vez de `curl` e troque `\` no fim da linha
por `` ` ``.

`-i` mostra o status e os headers da resposta; tire se quiser só o corpo.

```bash
API=http://localhost:8080

# 1. cadastro
curl -i -X POST $API/instituicoes \
  -H "Content-Type: application/json" \
  -d '{
    "instituicao": {
      "razaoSocial": "Hospital Teste", "cnpj": "12.345.678/0001-90",
      "tipo": "HOSPITAL", "endereco": "Rua Teste, 100",
      "municipio": "Recife", "telefone": "(81) 3000-0000"
    },
    "administrador": {
      "nome": "Maria Teste", "email": "maria@hospitalteste.org",
      "senha": "senha-de-teste"
    }
  }'

# 3. login do responsável: copie o "token" da resposta
curl -X POST $API/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "maria@hospitalteste.org", "senha": "senha-de-teste"}'

HOSPITAL=eyJ...   # cole o token aqui

# 5. quem sou eu
curl -i $API/auth/me -H "Authorization: Bearer $HOSPITAL"

# 7. bloqueado enquanto pendente (403)
curl -i $API/requisicoes -H "Authorization: Bearer $HOSPITAL"

# 8. login do admin (use o ADMIN_EMAIL e a ADMIN_SENHA do seu .env)
curl -X POST $API/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@hemotrack.local", "senha": "sua-senha-de-admin"}'

ADMIN=eyJ...      # cole o token aqui

# 9. quem está esperando aprovação
curl -i "$API/instituicoes?status=PENDENTE_APROVACAO" \
  -H "Authorization: Bearer $ADMIN"

# 11. aprovar (troque 1 pelo id do passo 1)
curl -i -X PATCH $API/instituicoes/1/aprovar \
  -H "Authorization: Bearer $ADMIN"

# 14. agora liberado, com o mesmo token de antes (200)
curl -i $API/requisicoes -H "Authorization: Bearer $HOSPITAL"
```

Se tiver o [`jq`](https://jqlang.org) instalado, dá para guardar o token direto,
sem copiar e colar:

```bash
HOSPITAL=$(curl -s -X POST $API/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "maria@hospitalteste.org", "senha": "senha-de-teste"}' \
  | jq -r .token)
```

As variáveis `HOSPITAL` e `ADMIN` só existem no terminal onde foram criadas.

---

## 5. Rodar os testes automatizados

Precisa do JDK 21 (não do Docker):

```bash
cd backend
./mvnw test                                  # todos
./mvnw test -Dtest=RequisicaoServiceTest     # uma classe
```

| Nível | Onde | O que cobre |
|---|---|---|
| Unitário | `service/*Test`, `config/InstituicaoAprovadaTest` | regras de cada service, com os repositórios mockados (Mockito) |
| Integração | `integracao/InstituicaoIntegracaoTest` | service + repositório + H2 em memória |
| Aceitação | `aceitacao/HistoriasAceitacaoTest` | um cenário por história (HU01, HU03, HU05) via HTTP real |

Os testes de integração e aceitação sobem o Spring com um H2 em memória
próprio: não usam nem alteram o banco do modo desenvolvimento nem o do
Compose.

Os mesmos testes rodam a cada push e pull request no GitHub Actions e dentro do
build da imagem Docker. Story map, ciclos de TDD e o que ficou sem teste estão
em [`desafio-pi.md`](./desafio-pi.md#testes).

---

## 6. Conferir o banco

### No Docker Compose (Postgres)

```bash
docker compose exec db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

```sql
\dt                          -- lista as tabelas
SELECT * FROM instituicoes;
SELECT * FROM usuarios;
SELECT * FROM requisicoes;
\q                           -- sai
```

**Para começar do zero:** `docker compose down -v` e
`docker compose up -d --build`. O schema e o `ADMIN_SISTEMA` são recriados na
subida.

### No modo desenvolvimento (H2)

`http://localhost:8080/h2-console` → JDBC URL `jdbc:h2:file:./data/hemotrack-db`,
usuário `sa`, senha em branco.

```sql
SELECT * FROM INSTITUICOES;
SELECT * FROM USUARIOS;
SELECT * FROM REQUISICOES;
```

**Para começar do zero:** pare o backend e apague
`backend/data/hemotrack-db.mv.db`. O schema é recriado na próxima subida
(`ddl-auto=update`), e o `ADMIN_SISTEMA` também, a partir do `.env`.

### O que observar (nos dois bancos)

- Em `usuarios`, a coluna `senha` guarda o hash BCrypt (`$2a$...`), nunca a
  senha em texto.
- Em `instituicoes` e `usuarios`, os enums (`status`, `tipo`, `papel`) aparecem
  como texto. Em `requisicoes`, `tipo`, `abo`, `rh`, `prioridade` e `status`
  ainda aparecem como números: essa entidade persiste os enums como `ORDINAL`,
  o que quebra os dados se alguém reordenar os valores do enum. A correção está
  no backlog.

---

## 7. Telemetria (opcional)

O backend pode enviar métricas ao Grafana Cloud. Vem desligado: sem configurar
nada, tudo acima funciona igual. No modo desenvolvimento, para ligar, preencha
as variáveis `GRAFANA_*` no `backend/.env` (peça o token ao Miguel) e reinicie.
No Compose, a telemetria fica sempre desligada. Passo a passo, queries e
problemas comuns em [`telemetria.md`](./telemetria.md).

Sem Grafana, as métricas continuam visíveis localmente em
`http://localhost:8080/actuator/metrics`.

---

## 8. Observações

- **Backend sem hot-reload.** O projeto não tem `spring-boot-devtools`: no modo
  desenvolvimento, depois de alterar código Java, pare (`Ctrl+C`) e rode
  `./mvnw spring-boot:run` de novo.
- **Frontend com hot-reload** no modo desenvolvimento: o Vite atualiza a tela
  ao salvar.
- **Parte das telas ainda é mockada.** O frontend fala de verdade com o backend
  no cadastro, no login, na aprovação (`/admin`), nos membros e nas
  solicitações (`/requisicoes`). Estoque, Transportes e Indicadores seguem com
  dados fictícios até o backend implementar essas rotas (comentários
  `TODO(back)` em `frontend/src/lib/api.ts`).
- **Deploy, pipeline e variáveis de produção** estão em [`deploy.md`](./deploy.md).
