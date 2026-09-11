
```
Coleta ────► Hemocentro ────► Hospital
                 |
                 └─ processamento
```
# Hemotrack - API REST

Descricao basica das APIs do nosso sistema.

## **Sistema**

### class `Usuario`
**Campos (resposta):** Id, Nome Completo, Email, CPF, InstituicaoId

**Campos (entrada — POST/PUT/PATCH):** Nome Completo, Email, Senha, CPF, InstituicaoId

A senha nunca é devolvida pela API — schema de resposta e de entrada são diferentes.

**Tipos de usuário** (definidos pelo `TipoInstituicao` da instituição vinculada):
- **Usuário Hemocentro** — visualiza requisições recebidas, aceita/recusa, aloca hemocomponentes, gerencia estoque.
- **Usuário Hospital** — cria Requisições De Transfusão (RT), acompanha suas próprias requisições.

### enum `TipoPerfil`
`PADRAO`, `ADMIN`

Campo novo em `Usuario`, ortogonal ao `TipoInstituicao`. `TipoInstituicao` diz *o que* o usuário faz no domínio (Hospital pede, Hemocentro atende); `TipoPerfil` diz *o nível de acesso* — `ADMIN` é operação/suporte do próprio HemoTrack, não um papel de negócio. Um usuário `ADMIN` continua vinculado a uma instituição (pode ser uma instituição interna "HemoTrack Sistema") mas pode chamar rotas que um usuário `PADRAO` não pode — hoje, basicamente os `DELETE`.

Cada rota abaixo indica quem pode chamá-la (HU01: "cada perfil deve ter acesso às funcionalidades correspondentes").

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

# Detalhamento da API

## `/login`

| Método | Rota          | Body | Quem chama | Descrição |
|--------|---------------|------|------------|-----------|
| POST   | `/login` | `{email, senha}` | Público | Autentica o usuário e devolve token. Necessário para HU01 ("informar suas credenciais... permitir o acesso"). |

## `/usuarios`

| Método | Rota          | Body                   | Quem chama | Descrição                                                     |
|--------|---------------|------------------------|------------|---------------------------------------------------------------|
| GET    | `/usuarios`      | —                      | `ADMIN` | Lista todos os **Usuarios** cadastrados (sem senha). Ver todos os usuários do sistema, entre instituições, é sensível — não é operação de um usuário `PADRAO`. |
| POST   | `/usuarios`      | json | Público | Cria um **Usuario** vinculado a uma `instituicaoId` já existente (cadastro, HU01). |
| GET    | `/usuarios/{id}` | —                      | Próprio usuário / `ADMIN` | Retorna **Usuario** com `id`, caso exista (sem senha). |
| DELETE | `/usuarios/{id}` | —                      | `ADMIN` | Ver **Dúvida: DELETE para quem?** abaixo. |
| PUT    | `/usuarios/{id}` | json | Próprio usuário | Atualiza completamente o **Usuario** com `id`, com infos do Body. |
| PATCH  | `/usuarios/{id}` | json (parcial) | Próprio usuário | Atualiza parcialmente **Usuario** com `id`, com dados do Body. |

## `/instituicoes`

| Método | Rota          | Body                   | Quem chama | Descrição                                                     |
|--------|---------------|------------------------|------------|---------------------------------------------------------------|
| GET    | `/instituicoes`| — | Qualquer usuário autenticado | Retorna todas as **Instituicoes** cadastradas. |
| POST   | `/instituicoes`      | json | Público | Cria uma **Instituicao** com infos do Body (cadastro, HU01, antes de existir qualquer usuário). CNPJ deve ser único. |
| GET    | `/instituicoes/{id}` | —    | Qualquer usuário autenticado | Retorna **Instituicao** com `id`, caso exista.|
| GET    | `/instituicoes/{id}/estoque` | —    | Hemocentro dono | Retorna a visão agregada do estoque **calculado** da instituição (só para `HEMOCENTRO`) — contagem por `tipoHemocomponente` + `tipoABO` + `fatorRh`. Parâmetros opcionais: `tipoHemocomponente`, `tipoABO`, `fatorRh` (filtram a agregação); `detalhado=true` (devolve a lista de `Hemocomponente`, não só a contagem). Atende HU02 ("consultar estoque disponível por tipo sanguíneo/componente"). |
| DELETE | `/instituicoes/{id}` | —    | `ADMIN` | Ver **Dúvida: DELETE para quem?** abaixo. |
| PUT    | `/instituicoes/{id}` | json | Usuário da própria instituição | Atualiza completamente a **Instituicao** com `id`, com infos do Body.|
| PATCH  | `/instituicoes/{id}` | json (parcial) | Usuário da própria instituição | Atualiza parcialmente a **Instituicao** com `id`, com dados do Body. |

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
| DELETE | `/requisicoes/{id}` | — | `ADMIN` | Ver **Dúvida: DELETE para quem?** abaixo — não é o caminho normal de cancelamento (isso é `status = CANCELADA`). |

## `/bolsas`

| Método | Rota          | Body                   | Quem chama | Descrição                                                     |
|--------|---------------|------------------------|------------|---------------------------------------------------------------|
| GET    | `/bolsas`| — | Hemocentro / Ponto de Coleta (da própria instituição) | Retorna as **Bolsas** da instituição do usuário. Filtros: `tipoABO`, `fatorRh`, `instituicaoAtualId` (`ADMIN` pode ver todas). |
| POST   | `/bolsas`      | json | Ponto de Coleta / Hemocentro | Cria uma **Bolsa** com infos do Body (coleta). `instituicaoAtualId` = ponto de coleta/hemocentro que recebeu. |
| POST   | `/bolsas/{id}/hemocomponentes`      | json | Hemocentro | Processa **Bolsa** indicada e retorna os ids dos **Hemocomponentes** resultantes, já com `instituicaoAtualId` herdado da bolsa. |
| GET    | `/bolsas/{id}` | —    | Hemocentro / Ponto de Coleta dono | Retorna **Bolsa** com `id`, caso exista.|
| PUT    | `/bolsas/{id}` | json | Hemocentro / Ponto de Coleta dono | Atualiza completamente a **Bolsa** com `id`, com infos do Body.|
| PATCH  | `/bolsas/{id}` | json (parcial) | Hemocentro / Ponto de Coleta dono | Atualiza parcialmente **Bolsa** com `id` — inclui mover `instituicaoAtualId` (transporte) e marcar descarte. |
| DELETE | `/bolsas/{id}` | — | `ADMIN` | Ver **Dúvida: DELETE para quem?** abaixo — não é o caminho normal de descarte (isso é status). |

## `/hemocomponentes`

| Método | Rota          | Body                   | Quem chama | Descrição                                                     |
|--------|---------------|------------------------|------------|---------------------------------------------------------------|
| GET    | `/hemocomponentes`| — | Hemocentro (da própria instituição) | Retorna os **Hemocomponentes** da instituição do usuário. Filtros: `tipoHemocomponente`, `tipoABO`, `fatorRh`, `instituicaoAtualId`, `statusQualidade` (`ADMIN` pode ver todos). |
| ~~POST~~   | ~~`/hemocomponentes`~~      | ~~json~~ | — | Não existe — só nasce de `/bolsas/{id}/hemocomponentes`. |
| GET    | `/hemocomponentes/{id}` | —    | Hemocentro dono | Retorna **Hemocomponente** com `id`, caso exista.|
| PUT    | `/hemocomponentes/{id}` | json | Hemocentro dono | Atualiza completamente o **Hemocomponente** com `id`, com infos do Body.|
| PATCH  | `/hemocomponentes/{id}` | json (parcial) | Hemocentro dono | Atualiza parcialmente **Hemocomponente** com `id` — inclui `statusQualidade` (ex.: marcar `DESCARTADO`) e `instituicaoAtualId`. |
| DELETE | `/hemocomponentes/{id}` | — | `ADMIN` | Ver **Dúvida: DELETE para quem?** abaixo — não é o caminho normal de descarte (isso é `statusQualidade = DESCARTADO`). |

## Dúvida: DELETE para quem?

Faz sentido, sim — mas com duas ressalvas:

1. **Precisa existir o papel.** Hoje o domínio só tem Hospital e Hemocentro (via `TipoInstituicao`), que são papéis de negócio, não de acesso. "Admin do sistema" é um conceito novo — por isso o `TipoPerfil` (`PADRAO`/`ADMIN`) acima. Sem isso modelado, "só admin pode deletar" não tem como ser verificado em lugar nenhum.
2. **Mesmo o `ADMIN` deletando por engano/erro, considerar soft delete por baixo do capô.** Pra `Usuario` e `Instituicao` um `DELETE` físico tende a ser inofensivo. Já pra `Bolsa`, `Hemocomponente` e `Requisicao` — que têm indicadores (HU09, mais pra frente) e potencialmente auditoria de saúde pública — a recomendação é a rota continuar respondendo `204` normalmente, mas internamente marcar o registro como removido (`removidoEm`, por exemplo) em vez de apagar a linha. Do ponto de vista do contrato da API não muda nada; muda só a implementação, e evita que uma correção de erro vire perda de histórico.

Pro escopo do projeto (rubrica não cobra RBAC granular), não vale super-engenhar isso: um campo `TipoPerfil` + uma checagem simples no controller/service já resolve.
