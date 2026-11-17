# HemoTrack — Modelo de domínio v1.1

> v1.1 (17/11/2026): alinhado ao código. Requisição com um item só, medido em volume; "vencida" a partir do dia da validade; `HistoricoHemocomponente` mantido como planejado para a Parte 3 de SO; rotas corrigidas. Na Parte 3, a classe `Hemocomponente` passa a se chamar `Bolsa` (linguagem das histórias).

```
Instituicao (HOSPITAL | HEMOCENTRO)
     │
     ├── Usuario            (papel: ADMIN_SISTEMA | ADMIN_INSTITUICAO | OPERADOR)
     ├── Hemocomponente     ("estoque" = os hemocomponentes com instituicaoAtualId = X)
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
| `ACEITA` | hemocentro assumiu; `hemocentroId` definido | Aceita | `POST /requisicoes/{id}/aceitar` |
| `ALOCADA` | hemocomponentes reservados no estoque do hemocentro | Em separação | `PATCH /requisicoes/{id}/alocar` |
| `EM_TRANSITO` | lote despachado | Em transporte | `PATCH /requisicoes/{id}/enviar` |
| `ATENDIDA` | recebido pelo hospital; hemocomponentes transferidos | Entregue | `PATCH /requisicoes/{id}/receber` |
| `RECUSADA` | terminal; exige `motivoRecusa` | Recusada | `POST /requisicoes/{id}/recusar` |
| `CANCELADA` | terminal; só antes de `EM_TRANSITO` | — | sem rota ainda (hoje `DELETE /requisicoes/{id}` apaga o registro) |

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

Transição inválida responde `409`. "Vencida" **não** é status: é `dataValidade <= hoje` (vence **no** dia da validade, HU02 cenário 3), calculado — um hemocomponente vencido continua `DISPONIVEL` até alguém descartar, mas não pode ser alocado.

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
| `instituicaoId` | `Long` | `@Nullable` apenas para `ADMIN_SISTEMA` |

O `ADMIN_SISTEMA` é o único usuário sem `instituicaoId`: ele pertence à
operação do HemoTrack, não a um hospital ou hemocentro. Por isso o token
dele não carrega a claim `instituicaoId`, e `GET /auth/me` devolve
`instituicao: null`.
Não existe `TipoUsuario`. O que o usuário pode fazer sai de duas coisas: `papel` (nível de acesso) e `instituicao.tipo` (hospital ou hemocentro). O JWT carrega `usuarioId`, `instituicaoId` e `papel`; `tipo` da instituição é resolvido no service. `papel` e `instituicaoId` também são lidos do banco a cada requisição, e não das claims — mesma razão do status da instituição: o token vive 8 horas e essas coisas mudam no meio do caminho.

O primeiro usuário de uma instituição nasce `ADMIN_INSTITUICAO`, criado na mesma transação do `POST /instituicoes`. Nesta versão ele é o único administrador dela: todos os demais são `OPERADOR`, cadastrados por ele em `POST /instituicoes/{id}/usuarios` e já vinculados à instituição. Não existe promoção de operador a administrador, e o administrador não pode ser desativado — tentar responde `409`.

`OPERADOR` e `ADMIN_INSTITUICAO` operam as mesmas rotas de negócio; o que cada um faz nelas continua saindo de `instituicao.tipo`. A diferença entre os dois papéis é só a gestão de pessoas: criar e desativar operadores é exclusivo do administrador.

Usuário não é removido, apenas desativado — o histórico de hemocomponente aponta para `usuarioId`.

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
| `volumeMl` | `double` | obrigatório; somado na alocação |
| `dataColeta` | `LocalDate` | `@NotNull` |
| `dataValidade` | `LocalDate` | `@NotNull`, posterior a `dataColeta` |
| `dataProcessamento` | `Instant` | `@CreationTimestamp` |
| `status` | `StatusHemocomponente` | `@NotNull`, default `DISPONIVEL` |
| `emTransito` | `boolean` | `true` entre `/enviar` e `/receber` |
| `instituicaoAtualId` | `Long` | `@NotNull` — onde a bolsa está agora |
| `requisicaoAlocadaId` | `Long` | `@Nullable` — preenchido enquanto `RESERVADO` |
| `bolsaOrigemId` | `Long` | `@Nullable` — não usado no fluxo |

`codigoBolsa` duplicado responde `409`.

Regras no próprio objeto: `estaVencido(hoje)` e `estaDisponivel(hoje)` (= `DISPONIVEL` e não vencido).

`instituicaoAtualId` muda uma vez: no `PATCH /requisicoes/{id}/receber`, do hemocentro para o hospital.

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

Ainda não existe no código: decidido em 17/11 manter no modelo e implementar na Parte 3 de SO (DDD em código), no contexto Estoque.

---

## `Requisicao`

| Campo | Tipo | Validação |
|---|---|---|
| `id` | `Long` | `@GeneratedValue(IDENTITY)` |
| `hospitalId` | `Long` | `@NotNull` — do JWT, nunca do body |
| `hemocentroId` | `Long` | `@Nullable` — definido no `/aceitar` |
| `tipo` | `TipoHemocomponente` | `@NotNull` |
| `abo` | `TipoABO` | `@NotNull` |
| `rh` | `FatorRh` | `@NotNull` |
| `volumeMl` | `double` | maior que zero (senão `400`) |
| `prioridade` | `Prioridade` | `@NotNull` |
| `status` | `StatusRequisicao` | default `ABERTA`; gravado como ordinal (status novo entra sempre no fim do enum) |
| `observacoes` | `String` | `@Nullable` |
| `motivoRecusa` | `String` | `@Nullable` — obrigatório quando `RECUSADA` |
| `dataCriacao` | `Instant` | `@CreationTimestamp`, `updatable = false` |
| `dataEnvio` | `Instant` | `@Nullable` — preenchido no `/enviar` |
| `dataEntrega` | `Instant` | `@Nullable` — preenchido no `/receber` |

Uma requisição pede **um** item: um tipo de hemocomponente, um ABO/Rh e um volume. As bolsas reservadas não ficam na requisição: cada `Hemocomponente` aponta para ela por `requisicaoAlocadaId`.

Só é editável (`PUT /requisicoes/{id}`) enquanto `ABERTA`; hoje só `prioridade` muda. `status` nunca vem do body — muda pelas rotas de ação.

A alocação fecha o pedido **somando volume**: a soma de `volumeMl` das bolsas escolhidas precisa ser maior ou igual ao `volumeMl` pedido.

---

## Estoque

Continua sem ser entidade: é **visão calculada** sobre `Hemocomponente.instituicaoAtualId`.

Diferença em relação ao modelo antigo: hospital também tem estoque — o `/receber` transfere as bolsas para ele. O que é exclusivo do hemocentro é *cadastrar* (`POST /instituicoes/{id}/estoque`) e *alocar*.

Rótulos do HU02:

| Rótulo (HU02) | Regra |
|---|---|
| Disponível | `status = DISPONIVEL` e `dataValidade > hoje` |
| Reservada | `status = RESERVADO` |
| Utilizada | `status = TRANSFUNDIDO` |
| Descartada | `status = DESCARTADO` |
| Vencida | `status = DISPONIVEL` e `dataValidade <= hoje` |

Elegível para alocação = `status = DISPONIVEL` **e** `dataValidade > hoje` **e** `instituicaoAtualId` = hemocentro que aceitou a requisição **e** mesmo tipo **e** ABO/Rh compatível.

FEFO = `ORDER BY dataValidade ASC` sobre esse filtro. O índice por hash + fila de prioridade (entrega do time de Algoritmos) é interno ao serviço de alocação — não aparece na API, só o resultado.

`/disponibilidade` (planejada, ainda sem rota) seria a mesma visão agregada entre instituições: contagem de `DISPONIVEL` não vencidos, agrupada por instituição, com filtro de `tipo`, `abo`, `rh` e `municipio`.

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