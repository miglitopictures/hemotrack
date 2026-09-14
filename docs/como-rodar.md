# Como rodar o HemoTrack em uma máquina nova

Guia mínimo para clonar e subir o backend localmente. O projeto ainda não tem frontend separado — as telas são servidas pelo próprio backend via Thymeleaf.

## Pré-requisitos

- **Git**
- **JDK 17 ou superior** (qualquer distribuição — Temurin/Adoptium é a mais comum)
- **Alguma ferramenta de HTTP** para testar a API — *opcional, e a escolha é sua*: Postman, Insomnia, Bruno, `curl`, HTTPie, ou a extensão **REST Client** do VS Code (`humao.rest-client`), que roda direto o arquivo `.http` do repo. Ver passo 4.

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


## 2. Rodar o backend

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

## 3. Acessar a aplicação

- **Lista de usuários:** `http://localhost:8080/usuarios`
- **Console do H2:** `http://localhost:8080/h2-console` (credenciais no passo 5)

O banco é um arquivo local (`data/hemotrack-db.mv.db`) criado automaticamente na pasta de onde o `mvnw` foi executado — por isso o passo 2 pede para rodar sempre a partir de `backend/`, senão cada execução cria o banco em um lugar diferente.

## 4. Testar a API

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

## 5. Conferir o banco

`http://localhost:8080/h2-console` → JDBC `jdbc:h2:file:./data/hemotrack-db`, user `sa`, senha em branco.

```sql
SELECT * FROM REQUISICOES;
```

`TIPO`, `ABO`, `RH`, `PRIORIDADE` e `STATUS` aparecem como números, não texto — os enums são persistidos como `ORDINAL` (ver `docs/api/decisoes.md`).

**Para começar do zero:** pare a aplicação e apague `backend/data/hemotrack-db.mv.db`. Com `ddl-auto=update` o schema é recriado no próximo start.

## Observações importantes

- **Sem hot-reload:** o projeto não tem `spring-boot-devtools`. Alterou código Java? Precisa parar (`Ctrl+C`) e rodar `./mvnw spring-boot:run` de novo — só salvar o arquivo não atualiza o servidor em execução.
