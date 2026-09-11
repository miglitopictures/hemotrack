
```
Coleta ────► Hemocentro ────► Hospital
                 |
                 └─ processamento
```
# Hemotrack - API REST

Descricao basica das APIs do nosso sistema.

> **Escopo desta revisão:** HU01–HU05 (cadastro/acesso, estoque, criar requisição, acompanhar requisição, aceitar/recusar). HU06 (seleção de compatíveis) já é coberta pelo endpoint de alocação existente. HU07, HU08 e HU09 (distribuição, transporte, indicadores) ficam para depois — não modeladas ainda.

> **O que mudou nesta revisão (pra referência do time):**
> - `RequisicaoDeTransfusao` ganhou `hospitalId` (sem isso não dá pra filtrar "minhas requisições", HU04) e `motivoRecusa`.
> - `StatusRequisicao` ganhou `ACEITA` (separa "hemocentro topou atender" — HU05 — de "hemocomponentes já reservados" — HU06/alocação) e `RECUSADA` (antes só existia `CANCELADA`, que agora fica só pra cancelamento do lado do hospital).
> - Ações dedicadas `POST /requisicoes/{id}/aceitar` e `/recusar`, em vez de mudar status via PATCH genérico.
> - `Estoque` deixou de ser uma classe/array embutido em `Instituicao`. Vira uma *view* calculada a partir de `Bolsa`/`Hemocomponente` (ver seção **Estoque** abaixo).
> - `Bolsa` e `Hemocomponente` ganharam `instituicaoAtualId`.
> - `DELETE` removido de `/requisicoes/{id}`, `/bolsas/{id}` e `/hemocomponentes/{id}` — essas entidades têm ciclo de vida com status; apagar o registro destruiria rastreabilidade. Continuam com `DELETE` só `/usuarios` e `/instituicoes`, que não têm esse problema.
> - `Usuario`: schema de resposta não inclui a senha.

## **Sistema**

### class `Usuario`
**Campos (resposta):** Id, Nome Completo, Email, CPF, InstituicaoId

**Campos (entrada — POST/PUT/PATCH):** Nome Completo, Email, Senha, CPF, InstituicaoId

A senha nunca é devolvida pela API — schema de resposta e de entrada são diferentes.

**Tipos de usuário** (definidos pelo `TipoInstituicao` da instituição vinculada):
- **Usuário Hemocentro** — visualiza requisições recebidas, aceita/recusa, aloca hemocomponentes, gerencia estoque.
- **Usuário Hospital** — cria Requisições De Transfusão (RT), acompanha suas próprias requisições.

Cada rota abaixo indica, quando relevante, qual tipo de usuário pode chamá-la (HU01: "cada perfil deve ter acesso às funcionalidades correspondentes").

---
### enum `TipoABO`
`A`, `B`, `AB`, `O`
### enum `FatorRh`
`POSITIVO`, `NEGATIVO`
### enum `TipoHemocomponente`
`HEMACIAS`, `PLASMA_FRESCO_CONGELADO`, `PLAQUETAS`, `CRIOPRECIPITADO`

---
### enum `Prioridade`
`NORMAL`, `URGENCIA`, `EMERGENCIA`

### enum `StatusRequisicao`
`ABERTA` → `ACEITA` → `ALOCADA` → `ATENDIDA`
ou `ABERTA` → `RECUSADA`
ou `ABERTA` / `ACEITA` → `CANCELADA` (cancelada pelo hospital)

- **ABERTA** — Pendente (HU03/HU04). Requisição criada, aguardando análise do hemocentro.
- **ACEITA** — hemocentro confirmou que consegue atender (HU05), ainda sem hemocomponentes específicos reservados.
- **ALOCADA** — hemocomponentes já reservados via compatibilidade + FEFO (HU06).
- **ATENDIDA** — entregue. *(A granularidade "em separação"/"em transporte" do HU04 entra quando HU07/HU08 forem modeladas; por ora ATENDIDA cobre tudo depois da alocação.)*
- **RECUSADA** — hemocentro não consegue atender; exige `motivoRecusa` preenchido (HU05).
- **CANCELADA** — cancelada pelo hospital antes de ser atendida.

### class `RequisicaoDeTransfusao`
**Campos:** Id, Data, **hospitalId**, **TipoHemocomponente**, **TipoABO**, **FatorRh**, volumeMl, observacoes (opcional), **Prioridade**, **StatusRequisicao**, **motivoRecusa** (preenchido só quando `RECUSADA`)

---
### class `Bolsa`
**Campos:** id, **TipoABO**, **FatorRh**, volumeMl, dataDeColeta, validade, **instituicaoAtualId**, boolEmTransito.

---
### enum `StatusQualidade`
`EM_ANALISE`, `APTO`, `DESCARTADO`

### class `Hemocomponente`
**Campos:** id, **TipoABO**, **FatorRh**, **TipoHemocomponente**, volumeMl, bolsaOrigemId, dataProcessamento, **StatusQualidade**, validade, **instituicaoAtualId**, requisicaoAlocadaId, boolEmTransito.

`StatusQualidade` descreve só o resultado do controle de qualidade (passou/não passou no laboratório). Ele **não** é o mesmo que disponibilidade de estoque — ver seção **Estoque**.

---
### enum `TipoInstituicao`
`PONTO_COLETA`, `HEMOCENTRO`, `HOSPITAL`

### class `Instituicao`
**Campos:** Id, Razao Social, CNPJ, **TipoInstituicao**.

*(Não tem mais campo `Estoque` — ver abaixo.)*

## **Estoque**

`Estoque` não é uma entidade persistida nem um array guardado dentro de `Instituicao`. É uma **visão calculada** a partir de `Hemocomponente.instituicaoAtualId`.

Só `HEMOCENTRO` tem estoque disponível para alocação (`PONTO_COLETA` só gera bolsas; `HOSPITAL` só recebe).

Disponibilidade de um `Hemocomponente` também é calculada, não é um campo próprio:

| "Status" exibido (HU02) | Regra |
|---|---|
| Disponível | `StatusQualidade = APTO` e `validade >= hoje` e `requisicaoAlocadaId = null` |
| Reservada | `requisicaoAlocadaId != null` |
| Vencida | `validade < hoje` |
| Utilizada | fora de escopo por enquanto (depende de HU08 — transporte/entrega) |

A estrutura de índice por hash + fila de prioridade por validade (FEFO), que o time de Algoritmos precisa entregar, é interna ao serviço de alocação — não é exposta na API, só o resultado (bolsas alocadas).

---

# Detalhamento da API WIP

## `/login`

| Método | Rota          | Body | Descrição |
|--------|---------------|------|-----------|
| POST   | `/login` | `{email, senha}` | Autentica o usuário e devolve token. Necessário para HU01 ("informar suas credenciais... permitir o acesso"). |

## `/usuarios`

| Método | Rota          | Body                   | Quem chama | Descrição                                                     |
|--------|---------------|------------------------|------------|---------------------------------------------------------------|
| GET    | `/usuarios`      | —                      | — | Retorna todos os **Usuarios** cadastrados (sem senha). |
| POST   | `/usuarios`      | json | — | Cria um **Usuario** com infos do Body (cadastro, HU01). |
| GET    | `/usuarios/{id}` | —                      | — | Retorna **Usuario** com `id`, caso exista (sem senha). |
| DELETE | `/usuarios/{id}` | —                      | — | Deleta **Usuario** indicado, caso exista. |
| PUT    | `/usuarios/{id}` | json | — | Atualiza completamente o **Usuario** com `id`, com infos do Body. |
| PATCH  | `/usuarios/{id}` | json (parcial) | — | Atualiza parcialmente **Usuario** com `id`, com dados do Body. |

## `/instituicoes`

| Método | Rota          | Body                   | Descrição                                                     |
|--------|---------------|------------------------|---------------------------------------------------------------|
| GET    | `/instituicoes`| — | Retorna todos os **Instituicoes** cadastrados. |
| POST   | `/instituicoes`      | json | Cria um **Instituicao** com infos do Body (cadastro, HU01). CNPJ deve ser único. |
| GET    | `/instituicoes/{id}` | —    | Retorna **Instituicao** com `id`, caso exista.|
| DELETE | `/instituicoes/{id}` | —    | Deleta **Instituicao** indicado, caso exista. |
| PUT    | `/instituicoes/{id}` | json | Atualiza completamente o **Instituicao** com `id`, com infos do Body.|
| PATCH  | `/instituicoes/{id}` | json (parcial) | Atualiza parcialmente **Instituicao** com `id`, com dados do Body. |

## `/requisicoes`

| Método | Rota          | Body                   | Quem chama | Descrição                                                     |
|--------|---------------|------------------------|------------|---------------------------------------------------------------|
| GET    | `/requisicoes`| — | Hospital (só as próprias, filtrado por `hospitalId` do token) / Hemocentro (todas, ou `?status=ABERTA` para ver pendentes — HU05) | Lista requisições. Suporta filtro `?status=`. |
| POST   | `/requisicoes`      | json (`hospitalId` vem do usuário autenticado, não do body) | Hospital | Cria uma **RequisicaoDeTransfusao**, status inicial `ABERTA` (HU03). Campos obrigatórios: tipoHemocomponente, tipoABO, fatorRh, volumeMl, prioridade. |
| GET    | `/requisicoes/{id}` | —    | Hospital dono / Hemocentro | Retorna **RequisicaoDeTransfusao** com `id`, caso exista (HU04). |
| POST   | `/requisicoes/{id}/aceitar` | — | Hemocentro | `ABERTA → ACEITA`. Confirma que o hemocentro vai atender (HU05). 409 se a requisição não estiver `ABERTA`. |
| POST   | `/requisicoes/{id}/recusar` | `{motivo}` | Hemocentro | `ABERTA → RECUSADA`. `motivo` obrigatório (HU05). |
| POST   | `/requisicoes/{id}/alocacoes` | — | Hemocentro | Só permitido com status `ACEITA`. Aloca hemocomponentes compatíveis (ABO/Rh + FEFO), muda status para `ALOCADA` e retorna os ids alocados (HU06). 409 se não houver hemocomponente compatível disponível. |
| PATCH  | `/requisicoes/{id}` | json (parcial, campos como `prioridade`, `observacoes`) | Hospital dono | Atualiza dados da requisição — **não altera `status`**, que só muda pelas ações acima. |
| — | ~~DELETE `/requisicoes/{id}`~~ | — | — | Removido: cancelamento é `status = CANCELADA` (via PATCH ou ação dedicada), não remoção do registro. |

## `/bolsas`

| Método | Rota          | Body                   | Descrição                                                     |
|--------|---------------|------------------------|---------------------------------------------------------------|
| GET    | `/bolsas`| — | Retorna todas as **Bolsas** presentes no sistema. Filtros: `tipoABO`, `fatorRh`, `instituicaoAtualId`. |
| POST   | `/bolsas`      | json | Cria uma **Bolsa** com infos do Body (coleta). `instituicaoAtualId` = ponto de coleta/hemocentro que recebeu. |
| POST   | `/bolsas/{id}/hemocomponentes`      | json | Processa **Bolsa** indicada e retorna os ids dos **Hemocomponentes** resultantes, já com `instituicaoAtualId` herdado da bolsa. |
| GET    | `/bolsas/{id}` | —    | Retorna **Bolsa** com `id`, caso exista.|
| PUT    | `/bolsas/{id}` | json | Atualiza completamente a **Bolsa** com `id`, com infos do Body.|
| PATCH  | `/bolsas/{id}` | json (parcial) | Atualiza parcialmente **Bolsa** com `id` — inclui mover `instituicaoAtualId` (transporte) e marcar descarte. |
| — | ~~DELETE `/bolsas/{id}`~~ | — | Removido — mesma razão de `/requisicoes`. Bolsa vencida/contaminada é status, não remoção. |

## `/hemocomponentes`

| Método | Rota          | Body                   | Descrição                                                     |
|--------|---------------|------------------------|---------------------------------------------------------------|
| GET    | `/hemocomponentes`| — | Retorna todos os **Hemocomponentes** presentes no sistema. Filtros: `tipoHemocomponente`, `tipoABO`, `fatorRh`, `instituicaoAtualId`, `statusQualidade`. |
| ~~POST~~   | ~~`/hemocomponentes`~~      | ~~json~~ | ~~Cria uma **Hemocomponente** com infos do Body.~~ Não existe — só nasce de `/bolsas/{id}/hemocomponentes`. |
| GET    | `/hemocomponentes/{id}` | —    | Retorna **Hemocomponente** com `id`, caso exista.|
| PUT    | `/hemocomponentes/{id}` | json | Atualiza completamente o **Hemocomponente** com `id`, com infos do Body.|
| PATCH  | `/hemocomponentes/{id}` | json (parcial) | Atualiza parcialmente **Hemocomponente** com `id` — inclui `statusQualidade` (ex.: marcar `DESCARTADO`) e `instituicaoAtualId`. |
| — | ~~DELETE `/hemocomponentes/{id}`~~ | — | Removido — descarte é `statusQualidade = DESCARTADO`, não remoção do registro. |
