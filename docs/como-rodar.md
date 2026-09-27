# Como rodar e testar o HemoTrack

Guia completo para subir o HemoTrack numa máquina nova, usar o sistema pela
primeira vez e testar a API. O projeto tem dois processos separados, que rodam
ao mesmo tempo em dois terminais:

| Processo | Stack | Endereço |
|---|---|---|
| **backend** | Spring Boot (só API, sem telas) | `http://localhost:8080` |
| **frontend** | Vite + React | `http://localhost:8081` |

**Sumário**

1. [Pré-requisitos](#1-pré-requisitos)
2. [Clonar e configurar](#2-clonar-e-configurar)
3. [Rodar o backend](#3-rodar-o-backend-terminal-1)
4. [Rodar o frontend](#4-rodar-o-frontend-terminal-2)
5. [Usar a aplicação pela primeira vez](#5-usar-a-aplicação-pela-primeira-vez)
6. [Testar a API](#6-testar-a-api)
7. [Conferir o banco](#7-conferir-o-banco-h2)
8. [Telemetria](#8-telemetria-opcional)
9. [Observações](#observações)

---

## 1. Pré-requisitos

- **Git**
- **JDK 21 ou superior** — Temurin/Adoptium é a distribuição mais comum
- **Node.js 20.19 ou superior** e **npm** — só para o frontend (o Vite 8 não
  roda em versões anteriores)
- **Para testar a API** (opcional): **Insomnia** ou **Postman**, e/ou **`curl`**,
  que já vem instalado no Linux, no macOS e no Windows 10+

Não instale o Maven: o projeto traz o Maven Wrapper (`mvnw` / `mvnw.cmd`), que
baixa a versão certa sozinho no primeiro uso.

### Git

| SO | Comando |
|---|---|
| Linux (Debian/Ubuntu) | `sudo apt install git` |
| Linux (Fedora) | `sudo dnf install git` |
| macOS | `xcode-select --install` (ou `brew install git`) |
| Windows | `winget install Git.Git` (ou [git-scm.com](https://git-scm.com/download/win)) |

### JDK 21+

| SO | Comando |
|---|---|
| Linux (Debian/Ubuntu) | `sudo apt install openjdk-21-jdk` |
| Linux (Fedora) | `sudo dnf install java-21-openjdk-devel` |
| macOS | `brew install openjdk@21` (e siga a instrução de `PATH` que o brew imprime) |
| Windows | `winget install EclipseAdoptium.Temurin.21.JDK` (ou [adoptium.net](https://adoptium.net)) |

Confira com `java -version`: precisa aparecer `21` ou mais. É o mínimo, não o
teto — JDKs mais novas rodam o projeto normalmente.

### Node.js 20.19+

Mais fácil com o **nvm** (Linux/macOS):

```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
nvm install --lts
```

Windows: [nvm-windows](https://github.com/coreybutler/nvm-windows) ou o
instalador de [nodejs.org](https://nodejs.org).

Confira com `node -v` e `npm -v`.

---

## 2. Clonar e configurar

### 2.1 Clonar o repositório

```bash
git clone https://github.com/miglitopictures/hemotrack.git
cd hemotrack
```

### 2.2 Criar o `backend/.env` (obrigatório)

O backend precisa de um segredo para assinar os tokens de login. **Sem ele a
aplicação não sobe.**

```bash
cp backend/.env.example backend/.env
openssl rand -base64 48    # cole o resultado em JWT_SECRET
```

O `JWT_SECRET` precisa ter pelo menos 32 caracteres. O `.env` está no
`.gitignore` — cada pessoa gera o seu.

### 2.3 `ADMIN_EMAIL` e `ADMIN_SENHA` (opcionais, mas você vai querer)

Toda instituição nasce `PENDENTE_APROVACAO` e não opera até a operação do
HemoTrack aprovar. Quem aprova é um usuário com papel `ADMIN_SISTEMA`, que
**não** pode ser criado pelo cadastro público: ele nasce na subida da aplicação,
a partir destas variáveis do mesmo `.env`:

```properties
ADMIN_EMAIL=admin@hemotrack.dev
ADMIN_SENHA=escolha-uma-senha
```

- Sem elas, a aplicação sobe e avisa no log, mas não há quem aprove cadastros
  (só por SQL no H2).
- O admin é criado **uma única vez**. Se já existir, o seed é ignorado — e
  trocar `ADMIN_SENHA` depois **não** redefine a senha dele.

As variáveis `GRAFANA_*` continuam opcionais: deixe como estão se não for usar
telemetria.

---

## 3. Rodar o backend (terminal 1)

Os comandos rodam **dentro de `backend/`**, onde ficam o `pom.xml` e o wrapper:

```bash
cd backend
./mvnw spring-boot:run      # Linux / macOS
mvnw.cmd spring-boot:run    # Windows (cmd ou PowerShell)
```

- `Permission denied` no Linux/macOS: rode `chmod +x mvnw` antes.
- A primeira execução baixa o Maven e as dependências (precisa de internet). As
  seguintes usam o cache em `~/.m2`.

**Esperado:** `Started BackendApplication in X seconds` no log, e, se você
configurou o passo 2.3, `ADMIN_SISTEMA criado para admin@hemotrack.dev` na
primeira subida.

Para parar: `Ctrl+C`.

---

## 4. Rodar o frontend (terminal 2)

Abra **outro terminal** — o backend precisa continuar rodando:

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

O frontend já aponta para `http://localhost:8080` (variável `VITE_API_URL` em
`frontend/.env`) — não precisa mudar nada.

Para parar: `Ctrl+C`.

---

## 5. Usar a aplicação pela primeira vez

Abra `http://localhost:8081`. O sistema exige conta, e toda instituição nova
precisa ser aprovada antes de operar. Use **duas janelas** (uma normal e uma
anônima) para não misturar as sessões:

**1. Cadastre uma instituição** — `http://localhost:8081/cadastro`

Escolha hospital ou hemocentro e preencha os dados da instituição e do
responsável. Ao concluir, você já entra logado e cai na tela **Cadastro em
análise**.

**2. Aprove como operação do HemoTrack** — na janela anônima

Entre em `http://localhost:8081/login` com o `ADMIN_EMAIL` e a `ADMIN_SENHA` do
seu `.env`. Você cai em **/admin**, com a lista de cadastros em análise. Clique
em **Aprovar**.

> O login do admin não funciona? As variáveis provavelmente não estavam no
> `.env` quando a aplicação subiu. Preencha e reinicie o backend.

**3. Volte à primeira janela** e clique em **Verificar novamente**. O painel
abre — de hospital ou de hemocentro, conforme o tipo cadastrado. Não precisa
sair e entrar de novo: o status da instituição é lido do banco a cada
requisição, não do token.

**4. Use o sistema.** No painel do hospital dá para criar requisições e
acompanhar o status; no do hemocentro, ver as requisições recebidas, o estoque
e os indicadores.

Para ver os dois lados, cadastre **um hospital e um hemocentro** e aprove os
dois.

---

## 6. Testar a API

### Como a autenticação funciona

Só duas rotas são públicas: `POST /auth/login` e `POST /instituicoes` (o
cadastro). Todo o resto exige o header:

```
Authorization: Bearer <token>
```

O token vem do login, vale **8 horas** e é recusado com `401` depois disso.
Todos os erros saem em `application/problem+json`, com `type`, `title`,
`status` e `detail` — a lista dos tipos está em
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
| 7 | `GET /requisicoes` | responsável | `403` — instituição não aprovada |
| 8 | `POST /auth/login` com `ADMIN_EMAIL` / `ADMIN_SENHA` | — | `200` |
| 9 | `GET /instituicoes?status=PENDENTE_APROVACAO` | admin | `200`, lista com a instituição do passo 1 |
| 10 | `PATCH /instituicoes/{id}/aprovar` | responsável | `403` — não é `ADMIN_SISTEMA` |
| 11 | `PATCH /instituicoes/{id}/aprovar` | admin | `200`, status `APROVADA` |
| 12 | Repetir o passo 11 | admin | `409` — já aprovada |
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
CNPJ volta só com dígitos — o backend normaliza para que `12.345.678/0001-90`
e `12345678000190` sejam reconhecidos como o mesmo.

Para rodar o roteiro de novo, troque CNPJ e e-mail ou
[zere o banco](#7-conferir-o-banco-h2).

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
herdando — *Inherit auth from parent*. Guarde o token do admin e o do
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

# 3. login do responsável — copie o "token" da resposta
curl -X POST $API/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "maria@hospitalteste.org", "senha": "senha-de-teste"}'

HOSPITAL=eyJ...   # cole o token aqui

# 5. quem sou eu
curl -i $API/auth/me -H "Authorization: Bearer $HOSPITAL"

# 7. bloqueado enquanto pendente (403)
curl -i $API/requisicoes -H "Authorization: Bearer $HOSPITAL"

# 8. login do admin
curl -X POST $API/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@hemotrack.dev", "senha": "escolha-uma-senha"}'

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

## 7. Conferir o banco (H2)

`http://localhost:8080/h2-console` → JDBC URL `jdbc:h2:file:./data/hemotrack-db`,
usuário `sa`, senha em branco.

```sql
SELECT * FROM INSTITUICOES;
SELECT * FROM USUARIOS;
SELECT * FROM REQUISICOES;
```

- Em `USUARIOS`, a coluna `SENHA` guarda o hash BCrypt (`$2a$...`), nunca a
  senha em texto.
- Em `INSTITUICOES` e `USUARIOS`, os enums (`STATUS`, `TIPO`, `PAPEL`) aparecem
  como texto. Em `REQUISICOES`, `TIPO`, `ABO`, `RH`, `PRIORIDADE` e `STATUS`
  ainda aparecem como números: essas entidades persistem os enums como
  `ORDINAL`, o que quebra os dados se alguém reordenar os valores do enum. A
  correção está no backlog.

**Para começar do zero:** pare o backend e apague
`backend/data/hemotrack-db.mv.db`. O schema é recriado na próxima subida
(`ddl-auto=update`), e o `ADMIN_SISTEMA` também, a partir do `.env`.

---

## 8. Telemetria (opcional)

O backend pode enviar métricas ao Grafana Cloud. Vem desligado: sem configurar
nada, tudo acima funciona igual. Para ligar, preencha as variáveis `GRAFANA_*`
no `backend/.env` (peça o token ao Miguel) e reinicie. Passo a passo, queries e
problemas comuns em [`telemetria.md`](./telemetria.md).

Sem Grafana, as métricas continuam visíveis localmente em
`http://localhost:8080/actuator/metrics`.

---

## Observações

- **Backend sem hot-reload.** O projeto não tem `spring-boot-devtools`: depois
  de alterar código Java, pare (`Ctrl+C`) e rode `./mvnw spring-boot:run` de
  novo.
- **Frontend com hot-reload.** O Vite atualiza a tela ao salvar — não precisa
  reiniciar.
- **Parte das telas ainda é mockada.** O frontend fala de verdade com o backend
  no cadastro, no login, na aprovação (`/admin`) e nas solicitações
  (`/requisicoes`). Estoque, Transportes e Indicadores seguem com dados
  fictícios até o backend implementar essas rotas (comentários `TODO(back)` em
  `frontend/src/lib/api.ts`).
- **Deploy e variáveis de produção** estão em [`deploy.md`](./deploy.md).
