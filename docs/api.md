
# API REST

```
Coleta ────► Hemocentro ────► Hospital
                 └─ processamento
```

---

# Domínio

## Sistema

### `User`
**Campos:** Id, Nome, Area, Instituicao ... *(pesquisar)*

**Tipos de usuário:**
- **Usuário Coletor** — está no ponto de coleta e pode aumentar/diminuir o número de bolsas no estoque.
- **Usuário Hemocentro** — usuário master.
- **Usuário Hospital** — cria as `RequisicoesDeTransfusao` (RT).

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
**Campos:** Id, EstoqueBolsas, EstoqueHemocomponentes, Requisicoes, Infos ... *(pesquisar)*

### `Hospital`
**Campos:** Id, EstoqueHemocomponentes, Infos ... *(pesquisar)*

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