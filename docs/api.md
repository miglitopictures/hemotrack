
# API REST

```
Coleta ────► Hemocentro ────► Hospital
                 └─ processamento
```

---

# Domínio

## Sistema

### `User`
**Campos:** Id, Email, Senha, CPF, Instituicao

**Tipos de usuário:**
- **Usuário Hemocentro** — ...
- **Usuário Hospital** — cria as `RequisicoesDeTransfusao` (RT).

| # | História | Ator principal |
|---|---|---|
| [HU01](#hu01--cadastro-e-acesso-à-plataforma) | Cadastro e acesso à plataforma | Representante de hospital/hemocentro |

### `RequisicaoDeTransfusao`
**Campos:** Id, Data, Paciente, Hemocomponente, Prioridade

## Produtos

### `Bolsa`
**Campos:** Id, DataDeColeta, Validade, boolEmTransito ... *(pesquisar)*

### `Hemocomponente`
**Campos:** Id, Infos, DataDeColeta, Validade, boolEmTransito ... *(pesquisar)*

## Instituições — pontos no grafo

### `PontoDeColeta`
**Campos:** Id, EstoqueBolsas, NumBolsas, Infos ... *(pesquisar)*

### `Hemocentro`
**Campos:** Id, CNPJ, EstoqueBolsas, EstoqueHemocomponentes, Requisicoes, Infos ... *(pesquisar)*

### `Hospital`
**Campos:** Id, CNPJ, EstoqueHemocomponentes, Infos ... *(pesquisar)*

---

# Detalhamento da API

## `/users`

| Método | Rota          | Body                              |
|--------|---------------|------------------------------------|
| GET    | `/users`      | —                                   |
| GET    | `/users/{id}` | —                                   |
| DELETE | `/users/{id}` | —                                   |
| POST   | `/users`      | json (infos do cliente)            |
| PUT    | `/users/{id}` | json (infos completas atualizadas) |
| PATCH  | `/users/{id}` | json (campo específico a atualizar)|

[HU01](#hu01--cadastro-e-acesso-à-plataforma)

## `/requisicoes`

| Método | Rota          | Body                              |
|--------|---------------|------------------------------------|
| GET    | `/requisicoes`      | —                                   |
| GET    | `/requisicoes/{id}` | —                                   |
| DELETE | `/requisicoes/{id}` | —                                   |
| POST   | `/requisicoes`      | json (infos do requisicao)            |
| PUT    | `/requisicoes/{id}` | json (infos completas atualizadas) |
| PATCH  | `/requisicoes/{id}` | json (campo específico a atualizar)|



## `/bolsas`

| Método | Rota                | Body                              |
|--------|---------------------|------------------------------------|
| GET    | `/bolsas`           | —                                   |
| GET    | `/bolsas/{id}`      | —                                   |
| DELETE | `/bolsas/{id}`      | —                                   |
| POST   | `/bolsas`           | json (infos da bolsa)              |
| PUT    | `/bolsas/{id}`      | json (infos completas atualizadas) |
| PATCH  | `/bolsas/{id}`      | json (campo específico a atualizar)|

## `/hemocomponentes`

| Método | Rota                        | Body                              |
|--------|-----------------------------|------------------------------------|
| GET    | `/hemocomponentes`          | —                                   |
| GET    | `/hemocomponentes/{id}`     | —                                   |
| DELETE | `/hemocomponentes/{id}`     | —                                   |
| POST   | `/hemocomponentes`          | json (infos do hemocomponente)     |
| PUT    | `/hemocomponentes/{id}`     | json (infos completas atualizadas) |
| PATCH  | `/hemocomponentes/{id}`     | json (campo específico a atualizar)|