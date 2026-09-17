# HemoTrack — Modelo de domínio v1.0

```
Instituicao (HOSPITAL | HEMOCENTRO)
     │
     ├── Usuario            (papel: ADMIN_SISTEMA | ADMIN_INSTITUICAO | OPERADOR)
     ├── Hemocomponente     ("estoque" = os hemocomponentes com instituicaoId = X)
     └── Requisicao         (hospitalId ─► hemocentroId)

HEMOCENTRO.estoque ──alocar──► Requisicao ──receber──► HOSPITAL.estoque
```
---

## Enums

| Enum | Valores |
|---|---|
| `TipoInstituicao` | `HOSPITAL`, `HEMOCENTRO` |
| `StatusInstituicao` | `PENDENTE_APROVACAO`, `APROVADA` |
| `Papel` | `ADMIN_SISTEMA`, `ADMIN_INSTITUICAO`, `OPERADOR` |
| `TipoABO` | `A`, `B`, `AB`, `O` |
| `FatorRh` | `POSITIVO`, `NEGATIVO` |
| `TipoHemocomponente` | `HEMACIAS`, `PLASMA`, `PLAQUETAS`, `CRIOPRECIPITADO` |
| `StatusHemocomponente` | `DISPONIVEL`, `RESERVADO`, `TRANSFUNDIDO`, `DESCARTADO` |
| `Prioridade` | `NORMAL`, `URGENCIA`, `EMERGENCIA` |
| `StatusRequisicao` | `ABERTA`, `ACEITA`, `ALOCADA`, `EM_TRANSITO`, `ATENDIDA`, `RECUSADA`, `CANCELADA` |
| `StatusTransporte` | `AGUARDANDO_SAIDA`, `EM_DESLOCAMENTO`, `ENTREGUE`, `ALERTA_TEMPERATURA |

### `StatusRequisicao` — máquina de estados

```
ABERTA → ACEITA → ALOCADA → EM_TRANSITO → ATENDIDA
   │        │
   │        └─────────► CANCELADA (hospital)
   ├──────────────────► CANCELADA (hospital)
   └──────────────────► RECUSADA  (hemocentro)
```

| Valor | Significado | Rótulo no HU04 | Rota que produz |
|---|---|---|---|
| `ABERTA` | criada pelo hospital, aguardando hemocentro | Pendente | `POST /requisicoes` |
| `ACEITA` | hemocentro assumiu; `hemocentroId` definido | Aceita | `PATCH /requisicoes/{id}/aceitar` |
| `ALOCADA` | hemocomponentes reservados no estoque do hemocentro | Em separação | `PATCH /requisicoes/{id}/alocar` |
| `EM_TRANSITO` | lote despachado | Em transporte | `PATCH /requisicoes/{id}/enviar` |
| `ATENDIDA` | recebido pelo hospital; hemocomponentes transferidos | Entregue | `PATCH /requisicoes/{id}/receber` |
| `RECUSADA` | terminal; exige `motivoRecusa` | Recusada | `PATCH /requisicoes/{id}/recusar` |
| `CANCELADA` | terminal; só antes de `EM_TRANSITO` | — | `PATCH /requisicoes/{id}/cancelar` |

### `StatusHemocomponente` — máquina de estados

```
DISPONIVEL ⇄ RESERVADO → TRANSFUNDIDO
     │            │
     └────────────┴────► DESCARTADO
```

| Transição | Quando |
|---|---|
| `DISPONIVEL → RESERVADO` | alocação da requisição (`/alocar`) |
| `RESERVADO → DISPONIVEL` | requisição cancelada depois de alocada |
| `RESERVADO → TRANSFUNDIDO` | uso no hospital, depois do `/receber` |
| qualquer → `DESCARTADO` | vencimento, quebra de cadeia de frio, descarte manual |

Transição inválida responde `409`. "Vencida" **não** é status: é `dataValidade < hoje`, calculado — um hemocomponente vencido continua `DISPONIVEL` até alguém descartar, mas não pode ser alocado.

---

## `Instituicao`

| Campo | Tipo | Validação |
|---|---|---|
| `id` | `Long` | `@GeneratedValue(IDENTITY)` |
| `razaoSocial` | `String` | `@NotBlank`, `@Size(3..80)` |
| `cnpj` | `String` | `@NotBlank`, `@Column(unique = true)` |
| `tipo` | `TipoInstituicao` | `@NotNull` |
| `status` | `StatusInstituicao` | `@NotNull`, default `PENDENTE_APROVACAO` |
| `endereco` | `String` | `@NotBlank` |
| `municipio` | `String` | `@NotBlank` — filtro de `/disponibilidade` e `GET /instituicoes` |
| `telefone` | `String` | `@Nullable` |

Nasce `PENDENTE_APROVACAO`. Enquanto estiver assim, seus usuários autenticam mas recebem `403` nas rotas de negócio. Só `ADMIN_SISTEMA` aprova.

CNPJ duplicado responde `409` — precisa de `unique = true` **e** `findByCnpj` no repository (decisão nº 9).

---

## `Usuario`

| Campo | Tipo | Validação |
|---|---|---|
| `id` | `Long` | `@GeneratedValue(IDENTITY)` |
| `nome` | `String` | `@NotBlank`, `@Size(3..80)` |
| `email` | `String` | `@NotBlank`, `@Email`, `@Column(unique = true)` |
| `senha` | `String` | `@NotBlank`, `@Size(min=6)`, hash BCrypt, `@JsonIgnore` |
| `papel` | `Papel` | `@NotNull`, default `OPERADOR` |
| `ativo` | `boolean` | default `true` |
| `instituicaoId` | `Long` | `@NotNull` |

Não existe `TipoUsuario`. O que o usuário pode fazer sai de duas coisas: `papel` (nível de acesso) e `instituicao.tipo` (hospital ou hemocentro). O JWT carrega `usuarioId`, `instituicaoId` e `papel`; `tipo` da instituição é resolvido no service.

O primeiro usuário de uma instituição nasce `ADMIN_INSTITUICAO`, criado na mesma transação do `POST /instituicoes`. Uma instituição nunca fica sem `ADMIN_INSTITUICAO`: rebaixar ou remover o último responde `409`.

`senha` nunca sai na resposta — toda saída é `UsuarioResponse`.

---

## `Hemocomponente`

Unidade de estoque. É a bolsa já pronta para uso — não há entidade de fracionamento.

| Campo | Tipo | Validação |
|---|---|---|
| `id` | `Long` | `@GeneratedValue(IDENTITY)` |
| `codigoBolsa` | `String` | `@NotBlank`, `@Column(unique = true)` — identificação do HU02 |
| `tipo` | `TipoHemocomponente` | `@NotNull` |
| `abo` | `TipoABO` | `@NotNull` |
| `rh` | `FatorRh` | `@NotNull` |
| `dataColeta` | `LocalDate` | `@NotNull`, `@PastOrPresent` |
| `dataValidade` | `LocalDate` | `@NotNull`, posterior a `dataColeta` |
| `status` | `StatusHemocomponente` | `@NotNull`, default `DISPONIVEL` |
| `instituicaoId` | `Long` | `@NotNull` — onde a bolsa está agora |
| `requisicaoId` | `Long` | `@Nullable` — preenchido enquanto `RESERVADO` |

`codigoBolsa` duplicado responde `409`.

`instituicaoId` muda uma vez: no `PATCH /requisicoes/{id}/receber`, do hemocentro para o hospital.

---

## `HistoricoHemocomponente`

Trilha de rastreio do HU08/HU09, exposta em `GET /hemocomponentes/{id}/historico`. Uma linha por mudança de `status` ou de `instituicaoId`.

| Campo | Tipo |
|---|---|
| `id` | `Long` |
| `hemocomponenteId` | `Long` |
| `data` | `Instant` (`@CreationTimestamp`) |
| `status` | `StatusHemocomponente` |
| `instituicaoId` | `Long` |
| `usuarioId` | `Long` — quem causou |

Append-only: nunca sofre `UPDATE` nem `DELETE`. É o que permite o delete lógico das outras entidades não perder rastreabilidade.

✗ Entidade nova — não existe no código.

---

## `Requisicao`

| Campo | Tipo | Validação |
|---|---|---|
| `id` | `Long` | `@GeneratedValue(IDENTITY)` |
| `hospitalId` | `Long` | `@NotNull` — do JWT, nunca do body |
| `hemocentroId` | `Long` | `@Nullable` — definido no `/aceitar` |
| `itens` | `List<ItemRequisicao>` | `@NotEmpty`, `@Valid` |
| `prioridade` | `Prioridade` | `@NotNull` |
| `status` | `StatusRequisicao` | `@NotNull`, default `ABERTA` |
| `observacoes` | `String` | `@Nullable`, `@Size(max=500)` |
| `motivoRecusa` | `String` | `@Nullable` — obrigatório quando `RECUSADA` |
| `hemocomponenteIds` | `List<Long>` | preenchido no `/alocar` |
| `criadaEm` | `Instant` | `@CreationTimestamp`, `updatable = false` |
| `atualizadaEm` | `Instant` | `@UpdateTimestamp` |

Só é editável (`PATCH /requisicoes/{id}`) enquanto `ABERTA`, e só `prioridade` e `observacoes`. `status` nunca vem do body — muda pelas rotas de ação.

### `ItemRequisicao`

| Campo | Tipo | Validação |
|---|---|---|
| `tipo` | `TipoHemocomponente` | `@NotNull` |
| `abo` | `TipoABO` | `@NotNull` |
| `rh` | `FatorRh` | `@NotNull` |
| `quantidade` | `int` | `@Min(1)` — número de bolsas |

A alocação fecha o pedido **contando bolsas**, não somando volume.

---

## Estoque

Continua sem ser entidade: é **visão calculada** sobre `Hemocomponente.instituicaoId`.

Diferença em relação ao modelo antigo: hospital também tem estoque — o `/receber` transfere as bolsas para ele. O que é exclusivo do hemocentro é *cadastrar* (`POST /instituicoes/{id}/estoque`) e *alocar*.

Rótulos do HU02:

| Rótulo (HU02) | Regra |
|---|---|
| Disponível | `status = DISPONIVEL` e `dataValidade >= hoje` |
| Reservada | `status = RESERVADO` |
| Utilizada | `status = TRANSFUNDIDO` |
| Vencida | `dataValidade < hoje`, independente do status |

Elegível para alocação = `status = DISPONIVEL` **e** `dataValidade >= hoje` **e** `instituicaoId` = hemocentro dono da requisição.

FEFO = `ORDER BY dataValidade ASC` sobre esse filtro. O índice por hash + fila de prioridade (entrega do time de Algoritmos) é interno ao serviço de alocação — não aparece na API, só o resultado.

`/disponibilidade` é a mesma visão agregada entre instituições: contagem de `DISPONIVEL` não vencidos, agrupada por instituição, com filtro de `tipo`, `abo`, `rh` e `municipio`.

---

## Compatibilidade ABO/Rh

Didática, conforme o escopo (HU06). Regra aplicada na alocação:

| Receptor | Doadores aceitos (hemácias) |
|---|---|
| `O` | `O` |
| `A` | `A`, `O` |
| `B` | `B`, `O` |
| `AB` | `A`, `B`, `AB`, `O` |

Rh: receptor `NEGATIVO` só aceita `NEGATIVO`; `POSITIVO` aceita ambos.

---

## Distribuicao / Transporte (HU07 e HU08)

| Campo | Tipo | Validação |
| ------ | ------ | ------ |
| `id` | `Long` | `@GeneratedValue(IDENTITY)` |
| `requisicaoId` | `Long` | `@NotNull`, `@Column`(`unique = true`) |
| `origemId` | `Long` | `@NotNull` (`hemocentroId`) |
| `destinoId` | `Long` | `@NotNull` (`hospitalId`) |
| `rotaCalculada` | `String` / `JSON` | `@NotBlank` — sequencia de nós/coordenadas do Grafo (Dijkstra) |
| `dataSaida` | `Instant` | `@Nullable` |
| `dataChegada` | `Instant` | `@Nullable` |
| `status` | `StatusTransporte` | `@NotNull`, default `AGUARDANDO_SAIDA` |

---

## RegistroTelemetria (HU08, SO e RSD)

| Campo | Tipo | Validação |
| ------ | ------ | ------ |
| `id` | `Long` | `@GeneratedValue`(`IDENTITY`) |
| `distribuicaoId` | `Long` | `@NotNull` — vinculo com o transporte |
| `dataHora` | `Instant` | `@CreationTimestamp` |
| `latitude` | `Double` | `@NotNull` |
| `longitude` | `Double` | `@NotNull` |
| `temperaturaAtual` | `Double` | `@NotNull` |
| `alertaTemperatura` | `boolean` | default `false` (`true` se fora da faixa de segurança) |
