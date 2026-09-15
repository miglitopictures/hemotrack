!! DEPRECATED -- atualizar para novo contrato definifo em .ods (nova api simplificada)

# HemoTrack — Contrato API

---

## `/login`
Nas rotas de login, implementaremos metodos de autenticação.

>Stack e dependências de Auth e Criptografia ainda não escolhidas ou implementadas.

### **POST** `/login`
Lista todos os usuarios do sistema.

**@caller** `PUBLICO`
#### `body`
```json
{
    "email": "your@email.br",
    "password": "63ui8g3629f38v2v8TVD6i39h9&" // senha criptografada
}
```
#### `200` - HttpStatus.OK
**@retorna** token de autenticação
#### `401` - HttpStatus.UNAUTHORIZED
dados do body não correspondem a nenhum usuario

---

## `/usuarios`
Essa rota implementa metodos CRUD para usuários do sistema (aka cadastro).

> Hoje é MVC Thymeleaf (retorna uma pagina html a partir de um template). Devemos adaptar para uma API Rest usando @RestController?

### **GET** `/usuários`
Lista todos os usuários cadastrados no sistema.

#### `200` - HttpStatus.OK
**@retorna** array com usuários cadastrados
``` json
[
    {
        "id": 0,
        "nomeCompleto": "Pablo Tamborini",
        "email": "ptb@cesar.school",
        "cpf": "123.123.123-12",
        "password": "76fdasdaohw89ey1uj9&", // senha criptografada
        "instituicaoId": 0,
        "tipo": "ADMIN"
    },
    {
        "id": 1,
        "nomeCompleto": "Miguel Duarte",
        "email": "miguel@hemope.gov.br",
        "cpf": "321.321.321-32",
        "password": "di5iukf88832&2319uhf",
        "instituicaoId": 20,
        "tipo": "PADRAO"
    },
    // ...
    {
        "id": N,
        "nomeCompleto": "Enesimo Usuario de Souza",
        "email": "enesimo@hemope.gov.br",
        "cpf": "777.555.333-11",
        "password": "hdusydaoe&76sd488kd",
        "instituicaoId": 20,
        "tipo": "PADRAO"
    }
]
```

| Método | Rota | Body | Quem chama |
|---|---|---|---|
| POST | `/usuarios` | json | Público |
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
