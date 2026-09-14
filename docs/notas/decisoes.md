# HemoTrack — Decisões de modelagem em aberto

Pontos onde o código e o que as HUs pedem ainda não fecham. Cada um é uma decisão a tomar, não um bug.

---

## 1. `validade` é `int`

`Bolsa` e `Hemocomponente` têm `private int validade`. Um `int` só pode ser *prazo em dias*, não *data de vencimento*.

Consequência: todo cálculo vira `dataColeta + validade dias`, e o FEFO do HU06 ("priorizar vencimento mais próximo") passa a depender de somar duas colunas em vez de ordenar uma.

| Opção | Consequência |
|---|---|
| `LocalDate dataValidade` | FEFO vira `ORDER BY data_validade`; vencido vira `data_validade < hoje` |
| manter `int validade` (dias) | preserva o prazo por tipo de componente; exige campo derivado para ordenar |
| os dois | `int validadeDias` como regra + `LocalDate dataValidade` gravado no fracionamento |

O HU02 pede "data de validade" explicitamente, o que empurra para a primeira ou a terceira.

---

## 2. Entidades sem getters

`Instituicao`, `Bolsa` e `Hemocomponente` não têm getters nem setters. O JPA funciona (acessa os campos direto), mas o Jackson serializa pelos getters: hoje um `GET /instituicoes` devolveria `{}`.

Bloqueia as três rotas que faltam. É pré-requisito, não polimento.

---

## 3. Enums persistidos como `ORDINAL`

Nenhum campo enum tem `@Enumerated(EnumType.STRING)`, então o H2 grava o índice. Com `ddl-auto=update` e banco em arquivo que sobrevive entre execuções, **reordenar um enum corrompe as linhas já gravadas**.

Some com isso: `@Enumerated(EnumType.STRING)` em todos os campos enum, e apagar `backend/data/hemotrack-db.mv.db` uma vez depois da mudança.

---

## 4. Ordem de `Prioridade`

Declarado `NORMAL, EMERGENCIA, URGENCIA`, que não é a ordem de gravidade (`NORMAL < URGENCIA < EMERGENCIA`).

Enquanto for `ORDINAL`, não dá para reordenar sem corromper dados, nem ordenar fila pelo ordinal. Resolver o item 3 primeiro; depois, reordenar ou ordenar por peso explícito.

---

## 5. Falta `hemocentroId` em `Requisicao`

O HU04 exige mostrar o "hemocentro responsável (quando definido)". Não há campo. O lugar natural de preencher é o `POST /aceitar`.

---

## 6. Quantidade é volume, não unidades

O HU03 fala em "quantidade"; o código resolveu como `volumeMl` (`double`). É uma decisão legítima, mas define como a alocação do HU06 fecha o pedido: somando volume de hemocomponentes, não contando bolsas.

Vale confirmar com o time antes do HU06, porque muda o algoritmo.

---

## 7. DTOs a criar

`dto/RecusaRequest` é o único que existe, e é o padrão a seguir.

| DTO | Para quê |
|---|---|
| `RequisicaoRequest` | entrada do `POST`/`PATCH`, sem `id` e sem `status` |
| `UsuarioResponse` | saída sem `password` |
| `AlocacaoResponse` | ids dos hemocomponentes alocados (HU06) |

Sem `RequisicaoRequest`, um cliente pode criar requisição já `ATENDIDA` ou forçar um `id`.

---

## 8. Falta `@Valid` no `@RequestBody`

`RequisicaoController` recebe `@RequestBody Requisicao` sem `@Valid`, então os `@NotNull` da entidade não rodam no controller — só estouram no flush do Hibernate, o que vira `500` em vez de `400` com a lista de campos faltando.

Fix: `@Valid @RequestBody` + um `@RestControllerAdvice` para `MethodArgumentNotValidException`.

---

## 9. CNPJ único

O HU01 exige CNPJ único. Precisa das duas pontas: `@Column(unique = true)` para o banco garantir, e um `findByCnpj` no repository para o service devolver `409` com mensagem — senão `DataIntegrityViolationException` vira `500`.

---

## 10. Sem testes

`BackendApplicationTests` só tem `contextLoads()`. As regras de transição (`409` ao aceitar duas vezes, `400` sem motivo) cabem num `@WebMvcTest`, e o CI já roda `mvnw verify` em todo PR para `main` — então o teste passa a valer automaticamente.

---

## Menores

- `setvolumeMl` (v minúsculo) em `Requisicao`. Funciona porque o Jackson resolve pelo getter, mas quebra ferramenta que siga JavaBean estrito.
- `getTipoUsuario()` sobre o campo `tipo` em `Usuario` faz o Jackson expor a propriedade como `tipoUsuario`, diferente de `Requisicao`. Renomear para `getTipo()` alinha os dois.
- `password` em texto puro. Alvo: hash.
- `@NotNull` em `int validade` e `@Nullable` em `boolean emTransito` não fazem nada — são primitivos.
- Pasta `cotrollers/` tem um typo no nome.
