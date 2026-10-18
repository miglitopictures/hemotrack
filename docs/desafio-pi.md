# Desafio do PI: Container, TDD e Deploy automatizados

CESAR School · Infraestrutura de Software · 2026.2 · Unidade 2

Este documento detalha a entrega do desafio. O resumo das seções pedidas está no [README principal](../README.md#desafio-do-pi-container-tdd-e-deploy).

- **Repositório:** https://github.com/miglitopictures/hemotrack
- **URL pública (API):** https://hemotrack-8ecl.onrender.com — verificação em [`/actuator/health`](https://hemotrack-8ecl.onrender.com/actuator/health)
- **Integrantes:** ver [Desenvolvido por](../README.md#desenvolvido-por)
- **Como rodar:** ver [Jeito rápido: Docker Compose](../README.md#jeito-rápido-docker-compose)

**Sumário:** [Testes](#testes) · [Pipeline e deploy](#pipeline-e-deploy) · [Decisões](#decisões) · [Uso de IA](#uso-de-ia)

---

## Testes

### Como rodar

```bash
cd backend
./mvnw test          # todos os níveis
./mvnw test -Dtest=RequisicaoServiceTest   # uma classe
```

Os testes rodam também **a cada push e pull request** no GitHub Actions e **dentro do build da imagem Docker**. Um teste vermelho barra o merge na `main` e, por consequência, o deploy.

### Testes por nível

| Nível | Onde | Ferramentas | O que cobre |
|---|---|---|---|
| **Unitário** | `service/*Test`, `config/InstituicaoAprovadaTest` | JUnit 5, AssertJ, Mockito | regras de cada service, isoladas do banco (repositórios mockados) |
| **Integração** | `integracao/InstituicaoIntegracaoTest` | `@SpringBootTest` + H2 em memória | service + repositório + banco de verdade: gravação pelo `@Transactional` e constraint `unique` do CNPJ |
| **Aceitação** | `aceitacao/HistoriasAceitacaoTest` | `@SpringBootTest(RANDOM_PORT)` + `HttpClient` | um cenário por história, via HTTP real (filtro JWT, segurança, controller, service, banco), no formato Dado / Quando / Então |

Testes unitários por classe:

| Classe | Testes (execuções) | Regras principais |
|---|---|---|
| `RequisicaoService` | 22 | aceitar/recusar só se ABERTA, motivo obrigatório, volume > 0, atualizar só se ABERTA |
| `InstituicaoService` | 7 | listagem padrão só de aprovadas; pendentes só para `ADMIN_SISTEMA`; aprovar duas vezes dá 409 |
| `MembroService` | 11 | acesso à instituição (dono, aprovada, `ADMIN_SISTEMA`), e-mail duplicado, senha codificada, admin não pode ser desativado |
| `CadastroService` | 10 | CNPJ e e-mail normalizados, CNPJ com 14 dígitos, conflitos, admin criado com senha codificada |
| `AuthService` | 7 | mesma resposta para usuário inexistente, senha errada e conta desativada; `/me` |
| `JwtService` | 8 | claims, expiração, token adulterado / de outra chave / vencido, segredo curto (sem mocks) |
| `InstituicaoAprovada` | 9 | rotas `/requisicoes/**` só para membros de instituição aprovada |

### Story map

**Backbone** (o que os usuários fazem, da esquerda para a direita) e **releases**:

| | Acessar a plataforma | Gerenciar estoque | Solicitar hemocomponentes | Atender solicitações | Distribuir e transportar | Acompanhar a operação |
|---|---|---|---|---|---|---|
| **Release 1** (no ar) | HU01 Cadastro e login | | HU03 Criar requisição · HU04 Acompanhar requisições | HU05 Aceitar ou recusar | | |
| **Release 2** | | HU02 Estoque | | HU06 Selecionar bolsas compatíveis | HU07 Planejar rota · HU08 Monitorar transporte | HU09 Indicadores |

Detalhes e critérios de aceite de cada história em [`docs/historias.md`](./historias.md).

### Histórias da Release 1 em tarefas e testes

**HU01 — Cadastro e acesso à plataforma**

| Tarefa | Teste |
|---|---|
| Cadastrar instituição pendente + admin com senha codificada | unit `CadastroServiceTest` |
| Normalizar CNPJ e impedir duplicado em qualquer formato | unit `CadastroServiceTest` · integração (constraint `unique`) |
| Rejeitar CNPJ sem 14 dígitos (**TDD, ciclo C**) | unit `CadastroServiceTest` · aceitação `hu01_cnpjIncompleto...` |
| Login com resposta genérica de erro | unit `AuthServiceTest` |
| Gerar e validar token JWT | unit `JwtServiceTest` |
| Aprovação pelo `ADMIN_SISTEMA` | unit `InstituicaoServiceTest` · integração `aprovar_gravaStatus...` |
| **Aceitação** | `hu01_cadastroELogin...`, `hu01_cnpjJaCadastradoEmOutroFormato...` |

**HU03 — Criar requisição de hemocomponentes**

| Tarefa | Teste |
|---|---|
| Criar requisição com status ABERTA ("Pendente") | unit `RequisicaoServiceTest` |
| Rejeitar volume zero ou negativo (**TDD, ciclo A**) | unit `RequisicaoServiceTest` · aceitação `hu03_volumeZero...` |
| Só instituição aprovada usa `/requisicoes` | unit `InstituicaoAprovadaTest` |
| Atualizar só requisição ABERTA (**TDD, ciclo B**) | unit `RequisicaoServiceTest` |
| **Aceitação** | `hu03_hospitalAprovadoCriaRequisicao...`, `hu03_instituicaoAindaPendente...` |

**HU05 — Aceitar ou recusar requisições**

| Tarefa | Teste |
|---|---|
| Aceitar só requisição ABERTA e guardar o hemocentro | unit `RequisicaoServiceTest` |
| Recusar só requisição ABERTA, com motivo obrigatório | unit `RequisicaoServiceTest` (inclui `@NullAndEmptySource`) |
| **Aceitação** | `hu05_hemocentroAceita...` (inclui a visão do hospital, HU04), `hu05_recusaExigeMotivo...` |

### TDD: histórico red → green

Três regras novas entraram com o teste **antes** do código. Cada uma tem um commit `test:` (vermelho) seguido de um commit `feat:` (verde):

| Ciclo | Regra | Commit vermelho | Commit verde | Tipo de vermelho |
|---|---|---|---|---|
| A | requisição com volume zero ou negativo → 400 | `test: requisição com volume zero ou negativo deve ser rejeitada` | `feat: rejeita requisição com volume zero ou negativo` | falha de assert |
| B | `atualizar` só com requisição ABERTA → 409 | `test: só requisição ABERTA pode ser atualizada` | `feat: só permite atualizar requisição ABERTA` | falha de assert |
| C | CNPJ precisa ter 14 dígitos → 400 | `test: CNPJ precisa ter 14 dígitos depois de normalizado` | `feat: rejeita CNPJ que não tenha 14 dígitos` | falha de compilação (a exceção ainda não existia) |

Evidências: ciclo A no CI [vermelho](./evidencias/e2-ci-vermelho.png) ([log](./evidencias/e2-ci-vermelho-logs.png)) e [verde](./evidencias/e2-ci-verde.png); histórico de commits red -> green em [`ciclo-red-green.png`](./evidencias/ciclo-red-green.png); PR dos testes unitários verde em [`e2-ci-00-passed1.png`](./evidencias/e2-ci-00-passed1.png).

### Quais regras ganharam teste primeiro, e por quê

Priorizamos pelo **risco**: o que, se quebrar, põe dado clínico errado no sistema ou abre acesso indevido.

1. **Transições de status da requisição** (aceitar, recusar, atualizar): uma requisição já atendida ou recusada não pode mudar; um erro aqui altera um pedido de sangue já fechado.
2. **Controle de acesso** (`InstituicaoAprovada`, `MembroService`, `InstituicaoService`): instituição não aprovada não pode operar; um admin não pode mexer em outra instituição.
3. **Autenticação** (`AuthService`, `JwtService`): token adulterado ou vencido precisa ser recusado.
4. **Cadastro** (`CadastroService`): CNPJ duplicado ou inválido gera instituição fantasma.

As regras dos ciclos A e B **já estavam no contrato da API** ([`contrato-api.md`](./api/contrato-api.md)) e não estavam implementadas: escrever o teste antes foi o que expôs essa divergência.

### O que ficou sem teste

- **Frontend**: sem testes automatizados; o CI só garante que compila (`npm run build`).
- **Controllers e DTOs**: cobertos indiretamente pelos testes de aceitação, sem testes unitários próprios.
- **`RelatorioService`** (benchmark de paralelismo) e **models** sem regra: fora do escopo de regra de negócio.
- **Histórias da Release 2** (HU02, HU06 a HU09): ainda não implementadas no backend.

## Pipeline e deploy

### Esteira

Arquivo: [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) (GitHub Actions).

```
push / pull request para main
        |
        +--> build-and-test   (./mvnw clean verify: compila e roda todos os testes)
        |         |
        |         +--> docker-image   (docker build do backend; o 1o estágio roda os testes de novo)
        |                   |
        +--> build-frontend (npm ci + npm run build)
                            |
                            v
                 deploy   (só em push na main, e só se os 3 jobs acima passarem)
                          chama o Deploy Hook do Render
```

**Gates:**

- `build-and-test`, `build-frontend` e `docker-image` são **checks obrigatórios** na `main` protegida: PR com teste vermelho não pode ser mergeado.
- O `deploy` depende dos três (`needs`) e só roda em `push` na `main`. Em PR ele aparece como *skipped*.
- O Render usa o mesmo Dockerfile, então o build de produção também roda os testes: uma imagem com teste vermelho nunca é publicada.

### Fluxo de branch: GitHub Flow

- A `main` está sempre publicável e é **protegida**: não aceita push direto nem force push, e exige PR com os 3 checks verdes (regra vale também para administradores).
- Cada mudança nasce numa branch curta (`tdd-validacoes`, `e1-container`, `e3-pipeline`...), vira PR, passa pela esteira e é mergeada.
- Cada merge na `main` vai sozinho para produção.

Com a regra do time, o PR exigiria 1 aprovação de revisor; durante a entrega deste desafio a aprovação ficou desligada para não bloquear o autor único das mudanças.

### Deploy

- **Serviço:** [Render](https://render.com) (Web Service, ambiente Docker, plano gratuito) — https://hemotrack-8ecl.onrender.com
- **Banco de produção:** Postgres no [Neon](https://neon.tech) (plano gratuito, sem expiração).
- **Gatilho:** o job `deploy` chama o Deploy Hook do Render; o Render faz o build da imagem e troca o container.
- Detalhes da configuração em [`docs/deploy.md`](./deploy.md).

> O plano gratuito do Render "dorme" após ~15 min sem tráfego: a primeira requisição pode levar 30 a 60 s.

### Onde ficam os segredos

| Segredo | Onde fica |
|---|---|
| `RENDER_DEPLOY_HOOK_URL` | GitHub → Settings → Secrets and variables → Actions |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_*` (produção) | variáveis de ambiente do serviço no Render |
| Valores locais (dev e Compose) | `.env` e `backend/.env`, ambos no `.gitignore`; modelos em `.env.example` |

Nenhum segredo está no repositório.

### Como voltar à versão anterior (rollback)

1. **Rápido (segundos):** Render → o serviço → **Events / Deploys** → escolher o último deploy que funcionou → **Rollback**. Não depende da esteira.
2. **Pelo Git (deixa registro):** `git revert <commit>` numa branch, PR, merge. A esteira roda e publica a versão sem o commit problemático.

### Evidências

| O quê | Arquivo |
|---|---|
| Execução **vermelha** que barrou o deploy (teste quebrado de propósito na `main`; `deploy` *skipped*) | [`e3-vermelho-barrou-deploy.png`](./evidencias/e3-vermelho-barrou-deploy.png) |
| Correção publicada de novo | [`e3-revert-change.png`](./evidencias/e3-revert-change.png) |
| Execução **verde** que publicou (4 jobs verdes) | [`e3-verde-deploy.png`](./evidencias/e3-verde-deploy.png) |
| Produção respondendo `UP` | [`e3-producao-up.png`](./evidencias/e3-producao-up.png) |
| `main` protegida com checks obrigatórios | [`e3-main-protegida.png`](./evidencias/e3-main-protegida.png) |
| `docker compose ps` com os serviços `healthy` | [`e1-compose-ps.png`](./evidencias/e1-compose-ps.png) |
| API do Compose respondendo `UP` | [`e1-api-health.png`](./evidencias/e1-api-health.png) |
| TDD: CI vermelho e verde do ciclo A | [`e2-ci-vermelho.png`](./evidencias/e2-ci-vermelho.png) · [`e2-ci-verde.png`](./evidencias/e2-ci-verde.png) |

## Decisões

| Decisão | Escolha | Alternativas | Por quê |
|---|---|---|---|
| Imagem base (build) | `eclipse-temurin:21-jdk` | `maven:3-eclipse-temurin-21` | usamos o Maven Wrapper do projeto (`mvnw`), então basta o JDK; a versão do Maven fica fixada no repositório |
| Imagem base (runtime) | `eclipse-temurin:21-jre-alpine` | `21-jre` (Ubuntu), distroless | só o JRE, base Alpine: imagem menor e com menos pacotes vulneráveis; roda com usuário sem root |
| Multi-stage | 2 estágios | 1 estágio | JDK, Maven, código-fonte e cache de dependências ficam no estágio 1; a imagem final leva só o `.jar` |
| Testes no build da imagem | estágio 1 roda `mvnw package` com testes | `-DskipTests` | imagem com teste vermelho não é gerada, nem no Render |
| Banco no Compose | Postgres 16 | H2, MySQL | é o mesmo motor da produção (Neon); H2 continua só no modo dev |
| Dados | volume nomeado `dados-postgres` | bind mount | os dados sobrevivem ao `docker compose down` e não dependem de caminho da máquina |
| Comunicação entre serviços | rede padrão do Compose (`db:5432`) | `network_mode: host`, IP fixo | o nome do serviço vira o host; funciona igual em qualquer máquina |
| Ordem de subida | `depends_on` + `condition: service_healthy` | só `depends_on` | a API só sobe quando o Postgres aceita conexões, não quando o container nasce |
| Configuração | `.env` (fora do Git) + `.env.example` | valores no `compose.yaml` | nenhum segredo no repositório; o Compose recusa subir sem as variáveis obrigatórias |
| Testes primeiro | transições de status, acesso, autenticação, CNPJ | ordem alfabética, cobertura | ordenados por risco (ver [Testes](#quais-regras-ganharam-teste-primeiro-e-por-quê)) |
| Unitário | JUnit 5 + Mockito + AssertJ | repositório falso à mão | isola a regra do banco; já vem no `spring-boot-starter-test` |
| Aceitação | HTTP real com `@SpringBootTest(RANDOM_PORT)` | MockMvc, Cucumber/Gherkin | testa a pilha inteira (inclusive o filtro JWT) sem dependência nova; os cenários seguem o Dado/Quando/Então das histórias |
| Fluxo de branch | GitHub Flow | Git Flow, GitLab Flow | time pequeno, uma versão no ar e deploy a cada merge; branches de release e `develop` seriam burocracia |
| CI/CD | GitHub Actions | GitLab CI, Jenkins | o código já está no GitHub; sem servidor para manter |
| Deploy | Render (Docker) + Neon (Postgres) | Railway, Fly.io, Postgres do Render | plano gratuito sem cartão e deploy por hook; o Postgres gratuito do Render expira em 30 dias |
| Segredos | GitHub Secrets + variáveis do Render | `.env` no repositório | segredo fora do código e fora do log da esteira |
| Rollback | botão do Render + `git revert` | redeploy manual | volta em segundos pelo painel, ou com registro no histórico pelo Git |
| Erro de regra num campo | 400 no formato `validacao` com `errors` | 400 genérico | o front já marca o campo certo com esse formato (ex.: CNPJ) |
| Erro de login | mesma mensagem para inexistente, senha errada e desativado | mensagens específicas | evita descobrir quais e-mails estão cadastrados |

## Uso de IA

Usamos o **Claude** (Anthropic) como assistente durante o desafio. A IA rascunhou; o grupo executou, revisou e assina o resultado.

### Especificações que guiaram o trabalho

A IA não partiu do zero: cada pedido apontava para uma especificação que já existia no projeto, e o código e os testes foram conferidos contra ela.

| Especificação | Usada para |
|---|---|
| PDF do desafio (seções 3 a 8) | escopo das entregas E1, E2 e E3 e critérios de avaliação |
| [`historias.md`](./historias.md) (critérios Dado/Quando/Então) | cenários dos testes de aceitação de HU01, HU03 e HU05 |
| [`contrato-api.md`](./api/contrato-api.md) | status HTTP e formato de erro esperados; origem das regras dos ciclos TDD A e B, que estavam no contrato mas não no código |
| Código existente (services, controllers, `SecurityConfig`) | mapa das regras já implementadas, antes de escrever qualquer teste |

### Prompts principais

Versões resumidas dos pedidos feitos ao longo do trabalho, na ordem em que aconteceram:

1. **Mapeamento:** "Segue o PDF do desafio. Quero começar o E2 pelos testes unitários das classes que já existem. Escaneie o backend, mapeie as classes que têm regra de negócio e proponha uma ordem de testes. Vamos passo a passo: você explica, eu escrevo."
2. **Entendimento:** "Por que estamos usando Mockito aqui, e não o banco?" · "Explique linha a linha o teste, com a intenção de cada parte." · "Separe o executar do verificar no teste de exceção."
3. **Revisão:** a cada teste escrito pelo integrante, "revise este arquivo". A IA apontava o problema e o porquê, e o integrante corrigia.
4. **Escala:** depois de entender o padrão, "implemente o `AuthServiceTest`" (e o mesmo para `JwtService` e `InstituicaoAprovada`), com revisão e commit pelo integrante.
5. **Plano da entrega:** "Compare o estado atual do repositório com os requisitos do PDF e diga o que falta para fechar E1, E2 e E3 hoje, em passos."
6. **TDD orientado pelo contrato:** para cada regra nova, primeiro o teste a partir do contrato e das histórias; o integrante rodava e confirmava o vermelho, commitava `test:`, e só então pedia a implementação mínima para o `feat:`.
7. **Impacto:** "Verifique se essas regras novas quebram algo no frontend ou nos scripts" — conferido contra o `contrato-api.md`, o `cadastro.tsx` e o `membros.sh`.
8. **Documentação:** "Avalie se precisamos de um README separado; quero manter a estrutura do README do projeto e só acrescentar onde for necessário."

### Como validamos

- Todo teste foi executado localmente (`./mvnw test`) e na esteira antes do merge; a contagem de testes executados era conferida a cada rodada.
- Cada regra nova foi conferida contra o contrato da API e contra o comportamento do frontend antes de entrar na `main`.
- A revisão humana pegou erros nos testes: stub de `save` faltando (`NullPointerException`), referência compartilhada entre objetos (`requisicaoNova = requisicaoSalva`), `@Test` esquecido e stub de acesso ausente.
- A IA também errou: uma ferramenta de sincronização entregou versões antigas de arquivos, o que só foi percebido porque o número de testes executados não batia com o esperado.
