# Como rodar o HemoTrack em uma máquina nova

Guia mínimo para clonar e subir o HemoTrack localmente. O projeto tem dois
processos separados — **backend** (Spring Boot, porta `8080`) e **frontend**
(Vite + React, porta `8081`) — que rodam ao mesmo tempo, em dois terminais.

## Pré-requisitos

- **Git**
- **JDK 17 ou superior** (qualquer distribuição — Temurin/Adoptium é a mais comum)
- **Node.js 20 ou superior** e **npm** — só pra rodar o frontend. Mais fácil
  instalar com [nvm](https://github.com/nvm-sh/nvm)
- **Alguma ferramenta de HTTP** para testar a API — *opcional, e a escolha é sua*: Postman, Insomnia, Bruno, `curl`, HTTPie, ou a extensão **REST Client** do VS Code (`humao.rest-client`), que roda direto o arquivo `.http` do repo. Ver passo 5.

Não é preciso instalar o Maven: o projeto já traz o Maven Wrapper (`mvnw` / `mvnw.cmd`), que baixa a versão correta sozinho no primeiro uso.

### Instalando o Git

| SO | Comando |
|---|---|
| Linux (Debian/Ubuntu) | `sudo apt install git` |
| Linux (Fedora) | `sudo dnf install git` |
| macOS | `xcode-select --install` (ou `brew install git`) |
| Windows | `winget install Git.Git` (ou instalador em [git-scm.com](https://git-scm.com/download/win)) |

### Instalando o JDK 17+

| SO | Comando |
|---|---|
| Linux (Debian/Ubuntu) | `sudo apt install openjdk-17-jdk` |
| Linux (Fedora) | `sudo dnf install java-17-openjdk-devel` |
| macOS | `brew install openjdk@17` (depois seguir a instrução de PATH que o brew imprime) |
| Windows | `winget install EclipseAdoptium.Temurin.17.JDK` (ou instalador em [adoptium.net](https://adoptium.net)) |

Confira com `java -version` — precisa aparecer `17` ou mais recente (o `pom.xml` fixa `<java.version>17</java.version>` como alvo de compilação, não como teto; versões mais novas da JDK rodam o projeto normalmente).

### Instalando o Node.js 20+

Mais fácil com o **nvm** (funciona igual em Linux/macOS):

```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
nvm install --lts
```

Windows: [nvm-windows](https://github.com/coreybutler/nvm-windows) ou instalador direto em [nodejs.org](https://nodejs.org).

Confira com `node -v` (precisa ser 20+) e `npm -v`.

### Instalando o REST Client (VS Code) — opcional

Só se você quiser rodar o arquivo `.http` do repo dentro do editor. Extensões → buscar por **REST Client** (autor `Huachao Mao`), ou pelo terminal:

```bash
code --install-extension humao.rest-client
```

No IntelliJ IDEA não precisa de nada — o formato é nativo. E se você já usa Postman, Insomnia, `curl` ou HTTPie, siga com o que você tem.

## 1. Clonar o repositório

```bash
git clone https://github.com/miglitopictures/hemotrack.git
cd hemotrack
```

## 2. Rodar o backend (terminal 1)

Todos os comandos abaixo devem ser executados **dentro da pasta `backend/`** (é onde fica o `pom.xml` e o wrapper):

```bash
cd backend
```

**Linux / macOS:**

```bash
./mvnw spring-boot:run
```

Se der erro de permissão (`Permission denied`), rode antes: `chmod +x mvnw`.

**Windows (cmd ou PowerShell):**

```bat
mvnw.cmd spring-boot:run
```

Na primeira execução o wrapper baixa o Maven e todas as dependências — precisa de internet. As próximas execuções são bem mais rápidas (fica em cache no `~/.m2`, ou `%USERPROFILE%\.m2` no Windows).

Quando o log mostrar `Started BackendApplication`, a aplicação está no ar em `http://localhost:8080`.

Para parar: `Ctrl+C` no terminal.

## 3. Rodar o frontend (terminal 2)

Abra **um segundo terminal** (não feche o primeiro — o backend precisa ficar rodando, é ele que fornece os dados) e rode:

```bash
cd frontend
npm install   # só na primeira vez
npm run dev
```

**Esperado:** uma linha parecida com:
VITE v8.X.X ready in XXX ms
➜ Local: http://localhost:8081/


O frontend já sabe chamar o backend em `http://localhost:8080` (configurado em `frontend/.env`, variável `VITE_API_URL`) — não precisa mudar nada se você seguiu os passos acima na ordem.

Para parar: `Ctrl+C` no terminal.

## 4. Acessar a aplicação

- **App do HemoTrack (hospital/hemocentro):** `http://localhost:8081` — é aqui que fica o sistema de verdade.
- **Lista de usuários** (tela administrativa do backend, via Thymeleaf, sem relação com o app novo): `http://localhost:8080/usuarios`
- **Console do H2:** `http://localhost:8080/h2-console` (credenciais no passo 6)

## 5. Testar a API

O roteiro de teste está em **`docs/api/requisicoes.http`**: cada chamada, na ordem, com o resultado esperado.

**A ferramenta é sua escolha** — Postman, Insomnia, Bruno, `curl`, HTTPie, ou a extensão REST Client. O que vale como artefato do projeto é o `.http`: ele versiona junto com o código e aparece no diff do PR, então quem mexer no controller vê na hora o que quebrou. Mesmo sem usar o formato, vale abrir e ler — é a lista do que precisa ser verificado.

### Com REST Client (VS Code) ou IntelliJ

Abrir o arquivo e clicar em **Send Request** acima de cada bloco `###` (no IntelliJ, o ▶ na margem). Roda o arquivo do repo sem conversão.

### Editando o `.http`

- **O corpo JSON é sempre a última coisa do bloco.** Depois da linha em branco que fecha os headers tudo vira corpo, inclusive linhas `@variavel` — por isso todo comentário fica acima da linha do método.
- `@baseUrl` e `@contentType` estão no topo; não repita a URL nos blocos.
- Para encadear um id: `# @name criar` no bloco de origem, `{{criar.response.body.$.id}}` nos seguintes.
- Enums vão como string. O ordinal (`"tipo": 0`) funciona mas amarra o JSON à ordem de declaração. Não use.
- Bloco novo = `### <n>. <o que faz>` + um comentário com o esperado (`# Esperado: 409 ...`). É o esperado que transforma o arquivo em teste.

## 6. Conferir o banco

`http://localhost:8080/h2-console` → JDBC `jdbc:h2:file:./data/hemotrack-db`, user `sa`, senha em branco.

```sql
SELECT * FROM REQUISICOES;
```

`TIPO`, `ABO`, `RH`, `PRIORIDADE` e `STATUS` aparecem como números, não texto — os enums são persistidos como `ORDINAL` (ver `docs/api/decisoes.md`).

**Para começar do zero:** pare a aplicação e apague `backend/data/hemotrack-db.mv.db`. Com `ddl-auto=update` o schema é recriado no próximo start.

## 7. Telemetria (opcional)

O backend pode enviar métricas para o Grafana Cloud. Vem desligado: sem configurar nada, tudo acima funciona igual. Para ligar, exporte `GRAFANA_OTLP_ENABLED`, `GRAFANA_OTLP_ENDPOINT` e `GRAFANA_OTLP_TOKEN` **no mesmo terminal**, antes do `./mvnw spring-boot:run`. Passo a passo, queries e problemas comuns em [`telemetria.md`](./telemetria.md).

Mesmo sem Grafana, dá pra ver as métricas localmente: `http://localhost:8080/actuator/metrics`.

## Observações importantes

- **Backend sem hot-reload:** o projeto não tem `spring-boot-devtools`. Alterou código Java? Precisa parar (`Ctrl+C`) e rodar `./mvnw spring-boot:run` de novo — só salvar o arquivo não atualiza o servidor em execução.
- **Frontend com hot-reload:** o Vite já atualiza a tela sozinho ao salvar (`npm run dev`) — não precisa reiniciar nada pra ver mudanças de tela.
- **Boa parte das telas ainda é mockada:** o frontend hoje só fala de verdade com o backend nas rotas de Solicitações (`/requisicoes`); Estoque, Transportes e Indicadores continuam com dados fictícios até o backend implementar essas rotas (ver os comentários `TODO(back)` em `frontend/src/lib/api.ts`).