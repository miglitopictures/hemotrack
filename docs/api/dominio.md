!! DEPRECATED - ATUALIZAR A PARTIR de contrato-api.ods (nova api simplificada)

# HemoTrack — Modelo de domínio

Entidades, campos e enums **como estão no código**, em `backend/src/main/java/com/hemotrack/backend/model/`.

```
Coleta ────► Hemocentro ────► Hospital
                 │
                 └─ processamento (bolsa → hemocomponentes)
```

---

## Agregados

| Pacote `model/` | Conteúdo | Repository | Service | Controller |
|---|---|---|---|---|
| `requisicao/` | `Requisicao`, `StatusRequisicao`, `Prioridade`, `dto/RecusaRequest` | ✓ | ✓ | ✓ REST |
| `usuario/` | `Usuario`, `TipoUsuario` | ✓ | ✓ | ⚠ MVC |
| `instituicao/` | `Instituicao`, `TipoInstituicao` | ✓ | ✗ | ✗ |
| `sangue/` | `Bolsa`, `Hemocomponente`, `StatusHemocomponente`, `TipoHemocomponente` | ✓ | ✗ | ✗ |
| `shared/` | `TipoABO`, `FatorRh` | — | — | — |

As camadas ficam em `model/<agregado>/`, `repositories/`, `service/` e `cotrollers/`.

Nenhuma entidade usa `@ManyToOne`: todos os vínculos são `Long` soltos — `hospitalId`, `idInstituicao`, `instituicaoAtualId`, `bolsaOrigemId`, `requisicaoAlocadaId`.

---

## Enums

| Enum | Valores | Onde |
|---|---|---|
| `TipoABO` | `A`, `B`, `AB`, `O` | `shared/` |
| `FatorRh` | `POSITIVO`, `NEGATIVO` | `shared/` |
| `TipoHemocomponente` | `HEMACIAS`, `PLASMA`, `PLAQUETAS`, `CRIOPRECIPITADO` | `sangue/` |
| `StatusHemocomponente` | `EM_ANALISE`, `APTO`, `DESCARTADO` | `sangue/` |
| `Prioridade` | `NORMAL`, `EMERGENCIA`, `URGENCIA` | `requisicao/` |
| `StatusRequisicao` | `ABERTA`, `ACEITA`, `ALOCADA`, `ATENDIDA`, `RECUSADA`, `CANCELADA` | `requisicao/` |
| `TipoInstituicao` | `PONTO_COLETA`, `HEMOCENTRO`, `HOSPITAL` | `instituicao/` |
| `TipoUsuario` | `PADRAO`, `ADMIN` | `usuario/` |

`PLASMA` é o nome curto de Plasma Fresco Congelado.

`TipoInstituicao` é papel de negócio: `PONTO_COLETA` gera bolsas, `HEMOCENTRO` processa e aloca, `HOSPITAL` requisita e recebe. `TipoUsuario` é nível de acesso — `ADMIN` é operação do próprio HemoTrack, e continua vinculado a uma instituição.

### `StatusRequisicao` — máquina de estados

```
ABERTA → ACEITA → ALOCADA → ATENDIDA
ABERTA → RECUSADA
ABERTA / ACEITA → CANCELADA
```

| Valor | Significado | HU | Rota |
|---|---|---|---|
| `ABERTA` | criada, aguardando o hemocentro ("Pendente" no HU04) | HU03 | ✓ default |
| `ACEITA` | hemocentro atende, sem hemocomponente reservado ainda | HU05 | ✓ `/aceitar` |
| `ALOCADA` | hemocomponentes reservados por compatibilidade + FEFO | HU06 | ✗ |
| `ATENDIDA` | entregue ao hospital | HU08 | ✗ |
| `RECUSADA` | não atende; exige `motivoRecusa` | HU05 | ✓ `/recusar` |
| `CANCELADA` | cancelada pelo hospital antes do atendimento | — | ✗ |

O HU04 pede "Em separação" e "Em transporte" como status visíveis ao hospital. Não entram neste enum: separação é `ALOCADA`, transporte é estado do transporte (HU07/HU08), ainda não modelado.

---

## `Requisicao`

| Campo | Tipo | Validação |
|---|---|---|
| `id` | `Long` | `@GeneratedValue(IDENTITY)` |
| `dataCriacao` | `Instant` | `@CreationTimestamp`, `updatable = false` |
| `hospitalId` | `Long` | `@NotNull` |
| `tipo` | `TipoHemocomponente` | `@NotNull` |
| `abo` | `TipoABO` | `@NotNull` |
| `rh` | `FatorRh` | `@NotNull` |
| `volumeMl` | `double` | `nullable = false` |
| `prioridade` | `Prioridade` | `@NotNull` |
| `observacoes` | `String` | `@Nullable` |
| `status` | `StatusRequisicao` | `@Nullable`, default `ABERTA` |
| `motivoRecusa` | `String` | `@Nullable` |

A "quantidade" do HU03 é `volumeMl`, não contagem de bolsas.

**DTOs:** só existe `dto/RecusaRequest` (`{ motivoRecusa }`).

---

## `Usuario`

| Campo | Tipo | Validação |
|---|---|---|
| `id` | `Long` | `@GeneratedValue(IDENTITY)` |
| `nomeCompleto` | `String` | `@NotBlank`, `@Size(3..80)` |
| `email` | `String` | `@NotBlank`, `@Email` |
| `password` | `String` | `@NotBlank`, `@Size(min=6)` |
| `cpf` | `String` | `@NotBlank` |
| `idInstituicao` | `Long` | `@NotNull` |
| `tipo` | `TipoUsuario` | `@Nullable`, default `PADRAO` |

`password` está em texto puro e seria serializado pela entidade — é o que impede `/usuarios` de virar REST.

---

## `Instituicao`

| Campo | Tipo | Validação |
|---|---|---|
| `id` | `Long` | `@GeneratedValue(IDENTITY)` |
| `tipo` | `TipoInstituicao` | `@NotNull` |
| `razaoSocial` | `String` | `@NotBlank`, `@Size(3..80)` |
| `cnpj` | `String` | `@NotBlank` |

Não tem campo `estoque` — ver **Estoque** abaixo. O CNPJ único do HU01 ainda não está garantido no banco.

---

## `Bolsa`

Sangue total, como sai da coleta. Não tem tipo de hemocomponente: é o que ainda vai ser fracionado.

| Campo | Tipo | Validação |
|---|---|---|
| `id` | `Long` | `@GeneratedValue(IDENTITY)` |
| `instituicaoAtualId` | `Long` | `@NotNull` |
| `abo` | `TipoABO` | `@NotNull` |
| `rh` | `FatorRh` | `@NotNull` |
| `volumeMl` | `double` | `nullable = false` |
| `dataColeta` | `Instant` | `@CreationTimestamp`, `updatable = false` |
| `validade` | `int` | — |
| `emTransito` | `boolean` | default `false` |

---

## `Hemocomponente`

O que sai do fracionamento de uma bolsa. É a unidade que se aloca para uma requisição.

| Campo | Tipo | Validação |
|---|---|---|
| `id` | `Long` | `@GeneratedValue(IDENTITY)` |
| `bolsaOrigemId` | `Long` | `@NotNull` |
| `instituicaoAtualId` | `Long` | `@NotNull` |
| `requisicaoAlocadaId` | `Long` | `@Nullable` — só quando alocado |
| `tipo` | `TipoHemocomponente` | `@NotNull` |
| `abo` | `TipoABO` | `@NotNull` — herdado da bolsa |
| `rh` | `FatorRh` | `@NotNull` — herdado da bolsa |
| `volumeMl` | `double` | `nullable = false` |
| `dataProcessamento` | `Instant` | `@CreationTimestamp`, `updatable = false` |
| `validade` | `int` | — |
| `status` | `StatusHemocomponente` | `@NotNull` |
| `emTransito` | `boolean` | default `false` |

---

## Estoque

Não é entidade nem coleção dentro de `Instituicao`. É **visão calculada** sobre `Hemocomponente.instituicaoAtualId`.

Só `HEMOCENTRO` tem estoque alocável.

Os quatro rótulos do HU02 também são calculados:

| Status exibido (HU02) | Regra |
|---|---|
| Disponível | `status = APTO` e não vencido e `requisicaoAlocadaId = null` |
| Reservada | `requisicaoAlocadaId != null` |
| Vencida | validade já passou |
| Utilizada | fora de escopo — depende do HU08 |

`StatusHemocomponente` responde só à primeira condição — por isso não existe nem deve existir um enum `StatusEstoque`.

O índice por hash + fila FEFO (entrega do time de Algoritmos) é interno ao serviço de alocação. Não é exposto na API — só o resultado.
