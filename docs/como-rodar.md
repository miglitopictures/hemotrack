# Como rodar o HemoTrack em uma máquina nova

Guia mínimo para clonar e subir o backend localmente. O projeto ainda não tem frontend separado — as telas são servidas pelo próprio backend via Thymeleaf.

## Pré-requisitos

- **Git**
- **JDK 17 ou superior** (qualquer distribuição — Temurin/Adoptium é a mais comum)

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

## 1. Clonar o repositório

```bash
git clone https://github.com/miglitopictures/hemotrack.git
cd hemotrack
```

O trabalho mais recente está na branch `modelagem_dominio` (não na `main`). Confira com `git branch -a` e, se necessário:

```bash
git checkout modelagem_dominio
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
- **Console do H2** (banco embutido): `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:file:./data/hemotrack-db`
  - User: `sa`
  - Password: *(em branco)*

O banco é um arquivo local (`data/hemotrack-db.mv.db`) criado automaticamente na pasta de onde o `mvnw` foi executado — por isso o passo 2 pede para rodar sempre a partir de `backend/`, senão cada execução cria o banco em um lugar diferente.

## Observações importantes

- **Sem hot-reload:** o projeto não tem `spring-boot-devtools`. Alterou código Java? Precisa parar (`Ctrl+C`) e rodar `./mvnw spring-boot:run` de novo — só salvar o arquivo não atualiza o servidor em execução.
