
```
Coleta ────► Hemocentro ────► Hospital
                 |
                 └─ processamento
```
# Hemotrack - API REST 

Descricao basica das APIs do nosso sistema.
## **Sistema**

### class `Usuario`
**Campos:** Id, Nome Completo, Email, Senha, CPF, InstituicaoId

**Tipos de usuário:**
- **Usuário Hemocentro** - ...
- **Usuário Hospital** - cria as Requisicoes De Transfusao (RT), pode acompanhar sa requisicoes.

---
### enum `TipoABO`
`A`, `B`, `AB`, `O`
### enum `FatorRh`
`POSITIVO`, `NEGATIVO`
### enum `TipoHemocomponente`
`HEMACIAS`, `PLASMA_FRESCO_CONGELADO`, `PLAQUETAS`, `CRIOPRECIPITADO`

---
### enum `Prioridade`
`NORMAL`, `EMERGENCIA`,`URGENCIA`
### enum `StatusRequisicao`
`ABERTA`, `ALOCADA`,`ATENDIDA`, `CANCELADA`
### class `RequisicaoDeTransfusao`
**Campos:** Id, Data, **TipoHemocomponente**, **TipoABO**, **FatorRh**, volumeMl, **Prioridade**, **StatusRequisicao**

---
### class `Bolsa`
**Campos:** id, **TipoABO**, **FatorRh**, volumeMl, dataDeColeta, validade, boolEmTransito.

---
### enum `StatusQualidade`
`EM_ANALISE`, `APTO`, `DESCARTADO`
### class `Hemocomponente`
**Campos:** id, **TipoABO**, **FatorRh**, **TipoHemocomponente**, volumeMl, bolsaOrigemId, dataProcessamento, **StatusQualidade**, validade, requisicaoAlocadaId, boolEmTransito.

---
### enum `TipoInstituicao`
`PONTO_COLETA`, `HEMOCENTRO`, `HOSPITAL`
### class `Estoque`
O nosso estoque é um array de filas ordenadas por data de vencimento.

``` java
// pseudocode
class Estoque {
    Bolsas bolsas[],
    Hemocomponente hemacias[],
    Hemocomponente plasma[],
    Hemocomponente plaquetas[],
    Hemocomponente criopreciptado[],
}
```
**Campos:** bolsas[], hemacias[], plasma[], plaquetas[], cripreciptado[].

## **Instituições** - *pontos no grafo*



### class `Instituicao`
**Campos:** Id, Razao Social, CNPJ, **TipoInstituicao**, Estoque.

---

# Detalhamento da API WIP

## `/usuarios`

| Método | Rota          | Body                   | Descrição                                                     |
|--------|---------------|------------------------|---------------------------------------------------------------|
| GET    | `/usuarios`      | —                      | Retorna todos os **Usuarios** cadastrados.                       |
| POST   | `/usuarios`      | json | Cria um **Usuario** com infos do Body.                           |
| GET    | `/usuarios/{id}` | —                      | Retorna **Usuario** com `id`, caso exista.                       |
| DELETE | `/usuarios/{id}` | —                      | Deleta **Usuario** indicado, caso exista.                        |
| PUT    | `/usuarios/{id}` | json | Atualiza completamente o **Usuario** com `id`, com infos do Body.|
| PATCH  | `/usuarios/{id}` | json (parcial) | Atualiza parcialmente **Usuario** com `id`, com dados do Body    |


## `/instituicoes`

| Método | Rota          | Body                   | Descrição                                                     |
|--------|---------------|------------------------|---------------------------------------------------------------|
| GET    | `/instituicoes`| — | Retorna todos os **Instituicoes** cadastrados. |
| POST   | `/instituicoes`      | json | Cria um **Instituicao** com infos do Body. |
| GET    | `/instituicoes/{id}` | —    | Retorna **Instituicao** com `id`, caso exista.|
| GET    | `/instituicoes/{id}/estoque` | —    | Retorna o **Estoque** atual da **Instituicao** com `id`, caso exista.|
| DELETE | `/instituicoes/{id}` | —    | Deleta **Instituicao** indicado, caso exista. |
| PUT    | `/instituicoes/{id}` | json | Atualiza completamente o **Instituicao** com `id`, com infos do Body.|
| PATCH  | `/instituicoes/{id}` | json (parcial) | Atualiza parcialmente **Instituicao** com `id`, com dados do Body. |

## `/requisicoes`

| Método | Rota          | Body                   | Descrição                                                     |
|--------|---------------|------------------------|---------------------------------------------------------------|
| GET    | `/requisicoes`| — | Retorna todos as **RequisicoesDeTrasfusao** presentes no sistema. |
| POST   | `/requisicoes`      | json | Cria uma **RequisicaoDeTrasfusao** com infos do Body. |
| POST   | `/requisicoes/{id}/alocacoes`      | json | Aloca hemocomponentes, utilizando compatibilidade e FEFO, para a **RequisicaoDeTrasfusao** com `id`. Retorna as ids dos hemocomponentes alocados|
| GET    | `/requisicoes/{id}` | —    | Retorna **RequisicaoDeTrasfusao** com `id`, caso exista.|
| DELETE | `/requisicoes/{id}` | —    | Deleta **RequisicaoDeTrasfusao** indicado, caso exista. |
| PUT    | `/requisicoes/{id}` | json | Atualiza completamente a **RequisicaoDeTrasfusao** com `id`, com infos do Body.|
| PATCH  | `/requisicoes/{id}` | json (parcial) | Atualiza parcialmente **RequisicaoDeTrasfusao** com `id`, com dados do Body. |



## `/bolsas`

| Método | Rota          | Body                   | Descrição                                                     |
|--------|---------------|------------------------|---------------------------------------------------------------|
| GET    | `/bolsas`| — | Retorna todos as **Bolsas** presentes no sistema. |
| POST   | `/bolsas`      | json | Cria uma **Bolsa** com infos do Body. |
| POST   | `/bolsas/{id}/hemocomponentes`      | json | Processa **Bolsa** indicada e retorna os ids do **Hemocomponentes** resultantes. |
| GET    | `/bolsas/{id}` | —    | Retorna **Bolsa** com `id`, caso exista.|
| DELETE | `/bolsas/{id}` | —    | Deleta **Bolsa** indicado, caso exista. |
| PUT    | `/bolsas/{id}` | json | Atualiza completamente a **Bolsa** com `id`, com infos do Body.|
| PATCH  | `/bolsas/{id}` | json (parcial) | Atualiza parcialmente **Bolsa** com `id`, com dados do Body. |


## `/hemocomponentes`

| Método | Rota          | Body                   | Descrição                                                     |
|--------|---------------|------------------------|---------------------------------------------------------------|
| GET    | `/hemocomponentes`| — | Retorna todos as **Hemocomponentes** presentes no sistema. |
| ~~POST~~   | ~~`/hemocomponentes`~~      | ~~json~~ | ~~Cria uma **Hemocomponente** com infos do Body.~~ |
| GET    | `/hemocomponentes/{id}` | —    | Retorna **Hemocomponente** com `id`, caso exista.|
| DELETE | `/hemocomponentes/{id}` | —    | Deleta **Hemocomponente** indicado, caso exista. |
| PUT    | `/hemocomponentes/{id}` | json | Atualiza completamente a **Hemocomponente** com `id`, com infos do Body.|
| PATCH  | `/hemocomponentes/{id}` | json (parcial) | Atualiza parcialmente **Hemocomponente** com `id`, com dados do Body. |