# HemoTrack — API alvo

Rotas planejadas. **Nada aqui está implementado, exceto onde marcado ✓** — o que roda hoje está em [`api.md`](./api.md).

Este arquivo é a visão de leitura rápida. O mesmo contrato, formal e executável por ferramenta, está em [`openapi.yml`](./openapi.yml) — schemas, exemplos e códigos de erro por operação.

A coluna "Quem chama" é alvo: não há autenticação, nenhuma rota é protegida.

Legenda: ✓ existe · ⚠ existe de outra forma · ✗ não existe

Não há `PUT` em lugar nenhum: com tantos campos preenchidos pelo servidor, mandar a representação inteira de volta não faz sentido. Atualização parcial é `PATCH`, e cada rota diz quais campos aceita.

---

## `/login` ✗

| Método | Rota | Body | Quem chama |
|---|---|---|---|
| POST | `/login` | `{email, password}` | Público |

HU01. Enquanto não existir, `hospitalId` vem do body.

---

## `/usuarios` ⚠ hoje é MVC Thymeleaf

| Método | Rota | Body | Quem chama |
|---|---|---|---|
| GET | `/usuarios` | — | `ADMIN` |
| POST | `/usuarios` | json | Público (cadastro, HU01) |
| GET | `/usuarios/{id}` | — | Próprio usuário / `ADMIN` |
| PATCH | `/usuarios/{id}` | `nomeCompleto`, `email`, `password` | Próprio usuário |
| DELETE | `/usuarios/{id}` | — | `ADMIN` |

Ver todos os usuários entre instituições é operação sensível — por isso `ADMIN`.

**Para migrar, nesta ordem:** parar de serializar `password` na saída → `@RestController` → `DELETE /usuarios/{id}` no lugar de `GET /usuarios/remover/{id}` → `400` com a lista de erros de validação em vez de re-renderizar o formulário.

---

## `/instituicoes` ✗

| Método | Rota | Body | Quem chama |
|---|---|---|---|
| GET | `/instituicoes` | — | Autenticado |
| POST | `/instituicoes` | json | Público — HU01, antes de existir usuário |
| GET | `/instituicoes/{id}` | — | Autenticado |
| GET | `/instituicoes/{id}/estoque` | — | Hemocentro dono |
| PATCH | `/instituicoes/{id}` | `razaoSocial` | Usuário da própria instituição |
| DELETE | `/instituicoes/{id}` | — | `ADMIN` |

`POST` devolve `409` se o CNPJ já existir (HU01).

`GET /{id}/estoque` (HU02) devolve o estoque calculado: contagem por `tipo` + `abo` + `rh`. Filtros `tipo`, `abo`, `rh`; `detalhado=true` devolve a lista de `Hemocomponente` em vez da contagem. Só faz sentido para `HEMOCENTRO`.

---

## `/requisicoes`

| Método | Rota | Body | Quem chama | |
|---|---|---|---|---|
| GET | `/requisicoes` | — | Hospital (só as próprias) / Hemocentro (todas) | ✓ |
| POST | `/requisicoes` | json | Hospital | ✓ |
| GET | `/requisicoes/{id}` | — | Hospital dono / Hemocentro | ✓ |
| PATCH | `/requisicoes/{id}` | `prioridade`, `observacoes` | Hospital dono | ⚠ hoje é `PUT` |
| POST | `/requisicoes/{id}/aceitar` | — | Hemocentro | ✓ |
| POST | `/requisicoes/{id}/recusar` | `{motivoRecusa}` | Hemocentro | ✓ |
| POST | `/requisicoes/{id}/alocacoes` | — | Hemocentro | ✗ |
| POST | `/requisicoes/{id}/cancelar` | — | Hospital dono | ✗ |
| DELETE | `/requisicoes/{id}` | — | `ADMIN` | ✓ |

O que muda nas que já existem:

- `GET` — filtro `?status=ABERTA` para o hemocentro ver pendentes (HU05). Hoje o parâmetro é ignorado.
- `POST` — `hospitalId` do token; responder `201` + `Location`.
- `PATCH` no lugar de `PUT`. O comportamento (atualizar só campos do hospital, nunca `status`) já é o certo; o verbo não.
- `aceitar` — gravar também o `hemocentroId` responsável (HU04).

As que faltam:

- `alocacoes` (HU06) — só a partir de `ACEITA`. Aloca por compatibilidade ABO/Rh + FEFO, muda para `ALOCADA`, devolve os ids alocados. `409` se não houver hemocomponente compatível.
- `cancelar` — `ABERTA`/`ACEITA` → `CANCELADA`. `DELETE` não é caminho de cancelamento.

---

## `/bolsas` ✗

| Método | Rota | Body | Quem chama |
|---|---|---|---|
| GET | `/bolsas` | — | Hemocentro / Ponto de Coleta da própria instituição |
| POST | `/bolsas` | json | Ponto de Coleta / Hemocentro — registra a coleta |
| GET | `/bolsas/{id}` | — | Dono |
| POST | `/bolsas/{id}/hemocomponentes` | json | Hemocentro — fraciona |
| PATCH | `/bolsas/{id}` | `instituicaoAtualId`, `emTransito` | Dono |
| DELETE | `/bolsas/{id}` | — | `ADMIN` |

Filtros do `GET`: `abo`, `rh`, `instituicaoAtualId` (`ADMIN` vê todas).

`POST /{id}/hemocomponentes` devolve os ids dos `Hemocomponente` gerados, com `bolsaOrigemId` e `instituicaoAtualId` herdados da bolsa.

`PATCH` cobre mover `instituicaoAtualId` e marcar `emTransito`.

---

## `/hemocomponentes` ✗

| Método | Rota | Body | Quem chama |
|---|---|---|---|
| GET | `/hemocomponentes` | — | Hemocentro da própria instituição |
| GET | `/hemocomponentes/{id}` | — | Hemocentro dono |
| PATCH | `/hemocomponentes/{id}` | `status`, `instituicaoAtualId`, `emTransito` | Hemocentro dono |
| DELETE | `/hemocomponentes/{id}` | — | `ADMIN` |

Não há `POST`: hemocomponente só nasce de `POST /bolsas/{id}/hemocomponentes`.

Filtros do `GET`: `tipo`, `abo`, `rh`, `status`, `instituicaoAtualId` (`ADMIN` vê todos).

Descarte normal é `status = DESCARTADO` via `PATCH`, não `DELETE`.

---

## DELETE para quem?

`TipoUsuario.ADMIN` já está modelado — falta a checagem no service.

Para `Usuario` e `Instituicao`, delete físico é inofensivo. Para `Bolsa`, `Hemocomponente` e `Requisicao` — que alimentam os indicadores do HU09 e a rastreabilidade — a rota responde `204` normalmente mas marca `removidoEm` por baixo. O contrato não muda; um erro de operação deixa de virar perda de histórico.

A rubrica não cobra RBAC granular: `TipoUsuario` + uma checagem no service resolve.
