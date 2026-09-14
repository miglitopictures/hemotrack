
```
Coleta ────► Hemocentro ────► Hospital
                 │
                 └─ processamento
```
# Hemotrack — API REST

Este documento tem duas partes:

1. **[Estado atual](#estado-atual-implementado)** — o que existe e roda hoje no código, com exemplos reais.
2. **[Desenho alvo](#desenho-alvo-planejado)** — o contrato completo que a API deve ter quando as HUs estiverem fechadas.

Quando os dois divergirem, a parte 1 é a verdade.

---

# Estado atual (implementado)

**Branch:** `rt-api-teste` · **Último commit:** `22315e8` — *refactor: RequisicaoService, config problemdetails*

| Recurso | Estado |
|---|---|
| `/requisicoes` | ✓ **REST funcional** — CRUD + transições `aceitar` / `recusar` |
| `/usuarios` | ⚠ **Não é REST** — é MVC Thymeleaf (devolve HTML, não JSON) |
| `/instituicoes` | ✗ não existe (entidade modelada, sem controller) |
| `/bolsas` | ✗ não existe (entidade modelada, sem controller) |
| `/hemocomponentes` | ✗ não existe (entidade modelada, sem controller) |
| `/login` / autenticação | ✗ não existe — **nenhuma rota é protegida** |

**Base URL:** `http://localhost:8080` · **Console H2:** `http://localhost:8080/h2-console` (JDBC `jdbc:h2:file:./data/hemotrack-db`, user `sa`, senha em branco)

Para subir o backend, ver [`como-rodar.md`](../como-rodar.md).

---

## Camadas

```
RequisicaoController  →  RequisicaoService  →  RequisicaoRepository (JpaRepository)  →  H2
   (@RestController)      (regras de status)      (save/findById/findAll/delete)
```

O `Service` lança `ResponseStatusException` direto (ainda não há exceptions de domínio próprias — ver `note(mig)` no código). Como `spring.mvc.problemdetails.enabled=true`, todo erro volta em **RFC 7807 (ProblemDetail)**:

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Requisição não encontrada",
  "instance": "/requisicoes/99"
}
```

---

## Enums em vigor (valores exatos aceitos pelo JSON)

| Enum | Valores **no código hoje** |
|---|---|
| `TipoHemocomponente` | `HEMACIAS`, `PLASMA`, `PLAQUETAS`, `CRIOPRECIPITADO` |
| `TipoABO` | `A`, `B`, `AB`, `O` |
| `FatorRh` | `POSITIVO`, `NEGATIVO` |
| `Prioridade` | `NORMAL`, `EMERGENCIA`, `URGENCIA` |
| `StatusRequisicao` | `ABERTA`, `ACEITA`, `ALOCADA`, `ATENDIDA`, `RECUSADA`, `CANCELADA` |
| `TipoUsuario` | `PADRAO`, `ADMIN` |
| `TipoInstituicao` | `PONTO_COLETA`, `HEMOCENTRO`, `HOSPITAL` |
| `StatusHemocomponente` | `EM_ANALISE`, `APTO`, `DESCARTADO` |

Os valores vão como **string** e são exatamente estes: qualquer outro devolve `400`. `PLASMA` é o nome curto de Plasma Fresco Congelado.

---

## Recurso `Requisicao`

### Corpo JSON (entrada e saída são o mesmo objeto — não há DTO)

```json
{
  "id": 1,
  "dataCriacao": "2026-09-14T21:14:03.482119Z",
  "hospitalId": 7,
  "tipo": "HEMACIAS",
  "abo": "O",
  "rh": "NEGATIVO",
  "volumeMl": 450.0,
  "prioridade": "EMERGENCIA",
  "status": "ABERTA",
  "observacoes": "Paciente politraumatizado, sala vermelha",
  "motivoRecusa": null
}
```

| Campo | Tipo | Obrigatório no POST | Observação |
|---|---|---|---|
| `id` | `Long` | não | gerado (`IDENTITY`). **Hoje é aceito no body** |
| `dataCriacao` | `Instant` (ISO-8601 UTC) | não | preenchido por `@CreationTimestamp`, imutável |
| `hospitalId` | `Long` | **sim** | hoje vem do body (não há token) |
| `tipo` | `TipoHemocomponente` | **sim** | |
| `abo` | `TipoABO` | **sim** | |
| `rh` | `FatorRh` | **sim** | |
| `volumeMl` | `double` | **sim** (primitivo, default `0.0`) | não há validação de mínimo |
| `prioridade` | `Prioridade` | **sim** | |
| `status` | `StatusRequisicao` | não | default `ABERTA`. **Hoje é aceito no body** |
| `observacoes` | `String` | não | |
| `motivoRecusa` | `String` | não | preenchido só pelo `POST /recusar` |

### Rotas

| Método | Rota | Body | Sucesso | Erros |
|---|---|---|---|---|
| `GET` | `/requisicoes` | — | `200` + array (vazio se não houver nada) | — |
| `POST` | `/requisicoes` | `Requisicao` | `200` + objeto criado | `400` JSON malformado/enum inválido; `500` campo `@NotNull` ausente |
| `GET` | `/requisicoes/{id}` | — | `200` + objeto | `404` |
| `PUT` | `/requisicoes/{id}` | `Requisicao` | `200` + objeto | `404` |
| `POST` | `/requisicoes/{id}/aceitar` | — | `200` + objeto com `status: ACEITA` | `404`; `409` se não estiver `ABERTA` |
| `POST` | `/requisicoes/{id}/recusar` | `{ "motivoRecusa": "..." }` | `200` + objeto com `status: RECUSADA` | `400` motivo vazio; `404`; `409` se não estiver `ABERTA` |
| `DELETE` | `/requisicoes/{id}` | — | `200` corpo vazio | `404` |

**Notas de comportamento que não dá para adivinhar pela tabela:**

- `POST` devolve **`200`**, não `201`, e **não manda header `Location`**.
- `PUT` **não é um PUT de verdade**: o service só copia `prioridade`. Todo o resto do body é ignorado. Na prática é um `PATCH` de um campo só.
- `DELETE` é **hard delete** (`repository.delete`) e devolve `200` com corpo vazio, não `204`.
- `aceitar` e `recusar` só saem de `ABERTA`. Chamar duas vezes → `409 Conflict` na segunda.
- `recusar` valida `motivoRecusa` **antes** de gravar: `null`, `""` ou só espaços → `400`, nada é alterado.
- Máquina de estados implementada hoje: `ABERTA → ACEITA` e `ABERTA → RECUSADA`. **Só isso.** `ALOCADA`, `ATENDIDA` e `CANCELADA` existem no enum mas nenhuma rota chega neles.

---

## Recurso `Usuario` — ⚠ não é REST

`UsuarioController` é `@Controller` (não `@RestController`) e serve páginas Thymeleaf. **Não devolve JSON — não adianta apontar uma ferramenta de API para cá**, use o navegador.

| Método | Rota | Retorno |
|---|---|---|
| `GET` | `/usuarios` ou `/usuarios/` | HTML — lista (`templates/usuarios.html`) |
| `GET` | `/usuarios/novo` | HTML — formulário vazio |
| `GET` | `/usuarios/editar/{id}` | HTML — formulário preenchido |
| `POST` | `/usuarios/salvar` | `form-urlencoded` (não JSON) → `302` redirect para `/usuarios/` |
| `GET` | `/usuarios/remover/{id}` | `302` redirect — **delete via GET** |

Quando esse recurso virar REST, é a hora de: separar DTO de entrada/saída (a senha hoje está em texto puro no campo `password` e seria serializada), trocar `GET /remover/{id}` por `DELETE /usuarios/{id}`, e devolver `400` com os erros de validação em vez de re-renderizar o formulário.

---

# Como testar a API

## Ferramenta: use a que você preferir

Testar a API é **agnóstico de ferramenta**. Postman, Insomnia, Bruno, `curl`, HTTPie — qualquer coisa que fale HTTP serve. Use o que já estiver na sua máquina e no seu fluxo.

O que o repo oferece é o **roteiro de teste versionado**: [`docs/api/requisicoes.http`](./requisicoes.http). Ele lista, na ordem, cada chamada e o resultado esperado — inclusive os casos de erro. Mesmo que você não use o formato `.http`, vale abrir o arquivo e ler: é a lista do que precisa ser verificado. A vantagem de ele estar no repo é que versiona junto com o código e aparece no diff do PR, então quem mexer no controller vê na hora o que quebrou.

### Opção 1 — REST Client / HTTP Client (roda o `.http` direto)

**Opcional**, mas é o caminho mais curto: roda o arquivo do repo sem conversão nenhuma.

| Editor | O que instalar |
|---|---|
| VS Code / VSCodium | extensão **REST Client** — `humao.rest-client` |
| IntelliJ IDEA | nada — o formato `.http` é nativo (HTTP Client) |

No VS Code também dá pela linha de comando:

```bash
code --install-extension humao.rest-client
```

### Opção 2 — Postman / Insomnia / Bruno

Funciona igual. Só lembre que a coleção fica na sua máquina, então ela não substitui o `.http` como artefato do projeto — trate o `.http` como a fonte da verdade do que testar, e a coleção como sua cópia de trabalho. O Postman e o Bruno importam arquivos `.http`; no Insomnia, recrie os blocos na mão (são 18).

Aponte a base para `http://localhost:8080`.

### Opção 3 — curl / HTTPie

Direto no terminal, sem instalar nada além do que já existe:

```bash
BASE=http://localhost:8080

# criar (bloco 1) — guarda o id em RID
RID=$(curl -s -X POST $BASE/requisicoes \
  -H 'Content-Type: application/json' \
  -d '{"hospitalId":7,"tipo":"HEMACIAS","abo":"O","rh":"NEGATIVO","volumeMl":450,"prioridade":"EMERGENCIA"}' \
  | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

curl -s $BASE/requisicoes/$RID | python3 -m json.tool   # bloco 3
curl -s -i -X POST $BASE/requisicoes/$RID/aceitar       # bloco 5
curl -s -i -X POST $BASE/requisicoes/$RID/aceitar       # bloco 6 -> 409
```

Com HTTPie o mesmo fica mais curto: `http POST :8080/requisicoes hospitalId:=7 tipo=HEMACIAS abo=O rh=NEGATIVO volumeMl:=450 prioridade=EMERGENCIA`.

> No **PowerShell**, `curl` é alias de `Invoke-WebRequest` e não aceita essas flags — chame `curl.exe` explicitamente (existe no Windows 10+), e escape as aspas do JSON: `-d '{\"hospitalId\":7,...}'`.

## Rodando os testes

1. Subir o backend (`cd backend && ./mvnw spring-boot:run`) — ver [`como-rodar.md`](../como-rodar.md).
2. Abrir [`docs/api/requisicoes.http`](./requisicoes.http).
3. Com REST Client/IntelliJ: clicar em **Send Request** acima de cada bloco `###` (no IntelliJ, o ▶ na margem). Com outra ferramenta: reproduzir as chamadas na ordem em que estão no arquivo.

Rode **na ordem** na primeira vez: os blocos 3 em diante reutilizam o `id` devolvido pelo bloco 1 — no `.http` isso é automático (`{{criar.response.body.$.id}}`); em outra ferramenta, copie o `id` da resposta do bloco 1 à mão ou guarde numa variável de ambiente.

O arquivo cobre:

| Blocos | O que exercita |
|---|---|
| 1–5 | caminho feliz: criar → listar → buscar → alterar prioridade → aceitar |
| 6–11 | erros: `409` (aceitar/recusar fora de `ABERTA`), `404`, `400` (enum inválido, `volumeMl` nulo) e o `500` de campo obrigatório ausente |
| 12–15 | fluxo de recusa: sem motivo → `400`, motivo em branco → `400`, motivo válido → `RECUSADA` |
| 16–18 | limpeza: delete das duas requisições + confirmação `404` |
| final | rotas do desenho alvo que **ainda não existem** (filtro `?status=`, `/alocacoes`, `/cancelar`) |

## Convenções do `.http` (só para quem for editar o arquivo)

- **O corpo JSON é sempre a última coisa do bloco.** Depois da linha em branco que fecha os headers, tudo vira corpo — inclusive linhas `@variavel`. Por isso todo comentário fica **acima** da linha do método.
- `@baseUrl` e `@contentType` estão no topo; não repita a URL literal nos blocos.
- Para encadear um id, nomeie o bloco de origem com `# @name criar` e use `{{criar.response.body.$.id}}` nos seguintes.
- Enums vão como **string**. Mandar o ordinal (`"tipo": 0`) funciona, mas amarra o JSON à ordem de declaração do enum. Não use.
- Bloco novo = nova linha `### <número>. <o que faz>` + um comentário com o **esperado** (`# Esperado: 409 ...`). É o esperado que transforma o arquivo em teste, não a chamada.

## Conferindo o que gravou no banco

`http://localhost:8080/h2-console` → JDBC URL `jdbc:h2:file:./data/hemotrack-db`, user `sa`, senha em branco.

```sql
SELECT * FROM REQUISICOES;
```

Repare que `TIPO`, `ABO`, `RH`, `PRIORIDADE` e `STATUS` aparecem como **números**, não como texto: nenhum campo enum tem `@Enumerated(EnumType.STRING)`, então o H2 grava o índice. Com `ddl-auto=update` e banco em arquivo, reordenar um enum corrompe as linhas já gravadas — ver a nota em `Prioridade`, no desenho alvo.

**Para começar do zero:** pare a aplicação e apague `backend/data/hemotrack-db.mv.db`. Com `ddl-auto=update` o schema é recriado no próximo start.

---
---

# Desenho alvo (planejado)

> Os **nomes de classe, campo e enum abaixo são os do código**. O que ainda não existe está marcado como tal. Quando esta parte divergir da parte 1, a parte 1 é a verdade.

## Estado da modelagem, por agregado

| Pacote `model/` | Entidades e enums | Repository | Service | Controller |
|---|---|---|---|---|
| `requisicao/` | `Requisicao`, `StatusRequisicao`, `Prioridade`, `dto/RecusaRequest` | ✓ | ✓ | ✓ REST |
| `usuario/` | `Usuario`, `TipoUsuario` | ✓ | ✓ | ⚠ MVC Thymeleaf |
| `instituicao/` | `Instituicao`, `TipoInstituicao` | ✓ | ✗ | ✗ |
| `sangue/` | `Bolsa`, `Hemocomponente`, `StatusHemocomponente`, `TipoHemocomponente` | ✓ | ✗ | ✗ |
| `shared/` | `TipoABO`, `FatorRh` | — | — | — |

As camadas ficam em `model/<agregado>/`, `repositories/`, `service/` e `cotrollers/` — esta última com o typo preservado no código.

Nenhuma entidade usa `@ManyToOne`: todos os vínculos são `Long` soltos (`hospitalId`, `idInstituicao`, `instituicaoAtualId`, `bolsaOrigemId`, `requisicaoAlocadaId`). É uma escolha consciente enquanto não há autenticação nem telas; virar FK de verdade é um passo previsto, não um esquecimento.

---

## `shared/` — tipos compartilhados

### enum `TipoABO`
`A`, `B`, `AB`, `O`

### enum `FatorRh`
`POSITIVO`, `NEGATIVO`

Aparecem em `Requisicao`, `Bolsa` e `Hemocomponente`. São o par que define compatibilidade (HU06).

---

## `usuario/`

### enum `TipoUsuario`
`PADRAO`, `ADMIN`

Nível de acesso, ortogonal ao `TipoInstituicao`. `TipoInstituicao` diz *o que* o usuário faz no domínio (Hospital pede, Hemocentro atende); `TipoUsuario` diz *o quanto* ele pode — `ADMIN` é operação/suporte do próprio HemoTrack, não papel de negócio. Um `ADMIN` continua vinculado a uma instituição.

### class `Usuario`

| Campo | Tipo | Validação no código | Nota |
|---|---|---|---|
| `id` | `Long` | `@GeneratedValue(IDENTITY)` | |
| `nomeCompleto` | `String` | `@NotBlank`, `@Size(3..80)` | |
| `email` | `String` | `@NotBlank`, `@Email` | alvo: único |
| `password` | `String` | `@NotBlank`, `@Size(min=6)` | texto puro hoje; alvo: hash |
| `cpf` | `String` | `@NotBlank` | alvo: único |
| `idInstituicao` | `Long` | `@NotNull` | alvo: FK para `Instituicao` |
| `tipo` | `TipoUsuario` | `@Nullable`, default `PADRAO` | getter é `getTipoUsuario()` |

**Contrato de saída:** `password` nunca é devolvido. Como a entidade é serializada direto, isso exige um DTO de resposta (`UsuarioResponse`) ou `@JsonIgnore` no campo — hoje não existe nenhum dos dois, e é por isso que `/usuarios` ainda não é REST.

**Contrato de entrada:** `nomeCompleto`, `email`, `password`, `cpf`, `idInstituicao`. `id` e `tipo` não vêm do cliente.

**Detalhe de serialização:** o getter chama-se `getTipoUsuario()` sobre o campo `tipo`, então o Jackson vai expor a propriedade como `tipoUsuario`, não `tipo` — diferente de `Requisicao`, onde `tipo` é `tipo`. Renomear o getter para `getTipo()` alinha os dois.

---

## `instituicao/`

### enum `TipoInstituicao`
`PONTO_COLETA`, `HEMOCENTRO`, `HOSPITAL`

Define o papel no fluxo: `PONTO_COLETA` gera bolsas, `HEMOCENTRO` processa e aloca, `HOSPITAL` requisita e recebe.

### class `Instituicao`

| Campo | Tipo | Validação no código | Nota |
|---|---|---|---|
| `id` | `Long` | `@GeneratedValue(IDENTITY)` | |
| `tipo` | `TipoInstituicao` | `@NotNull` | |
| `razaoSocial` | `String` | `@NotBlank`, `@Size(3..80)` | |
| `cnpj` | `String` | `@NotBlank` | alvo: `@Column(unique = true)` — HU01 exige CNPJ único |

Não tem campo `estoque` — ver **Estoque** abaixo.

**Bloqueio para virar REST:** a classe não tem getters nem setters. O JPA funciona (acessa os campos direto), mas o Jackson serializa pelos getters: hoje um `GET /instituicoes` devolveria `{}`. Vale para `Bolsa` e `Hemocomponente` também.

**CNPJ único:** a unicidade do HU01 precisa das duas pontas — `@Column(unique = true)` para o banco garantir, e um `findByCnpj` no repository para o service devolver `409` com mensagem em vez de deixar estourar `DataIntegrityViolationException` (que viraria `500`).

---

## `requisicao/`

### enum `Prioridade`
`NORMAL`, `EMERGENCIA`, `URGENCIA`

> A ordem de declaração no código é `NORMAL, EMERGENCIA, URGENCIA`, que **não** é a ordem de gravidade (`NORMAL < URGENCIA < EMERGENCIA`). Enquanto os enums forem persistidos como `ORDINAL`, reordenar corrompe os dados existentes; e ordenar fila por prioridade não pode usar o ordinal. Alvo: `@Enumerated(EnumType.STRING)` primeiro, reordenar depois, ou manter a ordem e ordenar por um peso explícito.

### enum `StatusRequisicao`

```
ABERTA → ACEITA → ALOCADA → ATENDIDA
ABERTA → RECUSADA
ABERTA / ACEITA → CANCELADA
```

| Valor | Significado | HU | Implementado |
|---|---|---|---|
| `ABERTA` | criada, aguardando análise do hemocentro ("Pendente" no HU04) | HU03 | ✓ default |
| `ACEITA` | hemocentro confirmou que atende, sem hemocomponente reservado ainda | HU05 | ✓ `POST /aceitar` |
| `ALOCADA` | hemocomponentes reservados via compatibilidade + FEFO | HU06 | ✗ |
| `ATENDIDA` | entregue ao hospital | HU08 | ✗ |
| `RECUSADA` | hemocentro não atende; exige `motivoRecusa` | HU05 | ✓ `POST /recusar` |
| `CANCELADA` | cancelada pelo hospital antes do atendimento | — | ✗ |

O HU04 pede "Em separação" e "Em transporte" como status visíveis ao hospital. Eles não entram neste enum: separação é `ALOCADA`, e transporte é estado do `Shipment` (HU07/HU08), que ainda não foi modelado. O que o hospital vê é derivado dos dois.

### class `Requisicao`

| Campo | Tipo | Validação no código | Nota |
|---|---|---|---|
| `id` | `Long` | `@GeneratedValue(IDENTITY)` | alvo: não aceitar no body |
| `dataCriacao` | `Instant` | `@CreationTimestamp`, `updatable = false` | imutável |
| `hospitalId` | `Long` | `@NotNull` | alvo: vir do usuário autenticado, não do body |
| `tipo` | `TipoHemocomponente` | `@NotNull` | |
| `abo` | `TipoABO` | `@NotNull` | |
| `rh` | `FatorRh` | `@NotNull` | |
| `volumeMl` | `double` | `nullable = false` | primitivo: default `0.0`, `@NotNull` não se aplica; alvo: `@Positive` |
| `prioridade` | `Prioridade` | `@NotNull` | |
| `observacoes` | `String` | `@Nullable` | |
| `status` | `StatusRequisicao` | `@Nullable`, default `ABERTA` | alvo: não aceitar no body |
| `motivoRecusa` | `String` | `@Nullable` | preenchido só no `POST /recusar` |

**Quantidade:** o HU03 fala em "quantidade"; o código resolveu isso como `volumeMl` (um `double`), não como número de bolsas. É uma decisão de modelagem que vale registrar: a alocação (HU06) vai somar volumes de hemocomponentes até cobrir o pedido, não contar unidades.

**Hemocentro responsável:** o HU04 pede que a requisição mostre o "hemocentro responsável (quando definido)". Não há campo para isso — falta um `hemocentroId`, preenchido no `aceitar`.

**Setter fora de convenção:** `setvolumeMl` (v minúsculo). Funciona porque o Jackson resolve a propriedade pelo getter, mas quebra qualquer ferramenta que siga JavaBean estrito.

### DTOs

`dto/RecusaRequest` — `{ motivoRecusa }`. É o único DTO que existe, e é o padrão a seguir para os demais:

| DTO alvo | Para quê |
|---|---|
| `RequisicaoRequest` | entrada do `POST`/`PATCH`, sem `id` e sem `status` |
| `UsuarioResponse` | saída sem `password` |
| `AlocacaoResponse` | ids dos hemocomponentes alocados (HU06) |

---

## `sangue/`

### enum `TipoHemocomponente`
`HEMACIAS`, `PLASMA`, `PLAQUETAS`, `CRIOPRECIPITADO`

`PLASMA` é o nome curto de "Plasma Fresco Congelado" — o valor no JSON é `PLASMA`.

### enum `StatusHemocomponente`
`EM_ANALISE`, `APTO`, `DESCARTADO`

Só o resultado do controle de qualidade do laboratório. **Não** é disponibilidade de estoque — ver **Estoque**.

### class `Bolsa`

Sangue total, como sai da coleta. Não tem tipo de hemocomponente: é o que ainda vai ser fracionado.

| Campo | Tipo | Validação no código | Nota |
|---|---|---|---|
| `id` | `Long` | `@GeneratedValue(IDENTITY)` | |
| `instituicaoAtualId` | `Long` | `@NotNull` | onde a bolsa está agora |
| `abo` | `TipoABO` | `@NotNull` | |
| `rh` | `FatorRh` | `@NotNull` | |
| `volumeMl` | `double` | `nullable = false` | |
| `dataColeta` | `Instant` | `@CreationTimestamp`, `updatable = false` | |
| `validade` | `int` | `@NotNull` (sem efeito em primitivo) | **a decidir** — ver abaixo |
| `emTransito` | `boolean` | default `false` | |

### class `Hemocomponente`

O que sai do fracionamento de uma bolsa. É a unidade que se aloca para uma requisição.

| Campo | Tipo | Validação no código | Nota |
|---|---|---|---|
| `id` | `Long` | `@GeneratedValue(IDENTITY)` | |
| `bolsaOrigemId` | `Long` | `@NotNull` | rastreabilidade até a coleta |
| `instituicaoAtualId` | `Long` | `@NotNull` | onde está agora |
| `requisicaoAlocadaId` | `Long` | `@Nullable` | preenchido só quando alocado |
| `tipo` | `TipoHemocomponente` | `@NotNull` | |
| `abo` | `TipoABO` | `@NotNull` | herdado da bolsa |
| `rh` | `FatorRh` | `@NotNull` | herdado da bolsa |
| `volumeMl` | `double` | `nullable = false` | |
| `dataProcessamento` | `Instant` | `@CreationTimestamp`, `updatable = false` | |
| `validade` | `int` | `@NotNull` (sem efeito em primitivo) | **a decidir** — ver abaixo |
| `status` | `StatusHemocomponente` | `@NotNull` | |
| `emTransito` | `boolean` | default `false` | |

**`validade` como `int` é a decisão de modelagem mais importante em aberto.** Um `int` só pode ser *prazo em dias*, não *data de vencimento*. Isso obriga todo cálculo de validade a ser `dataColeta + validade dias`, e a regra FEFO do HU06 ("priorizar vencimento mais próximo") passa a depender de somar as duas colunas em vez de ordenar uma. As opções:

| Opção | Consequência |
|---|---|
| `LocalDate dataValidade` | FEFO vira `ORDER BY data_validade`; o vencido do HU02 vira `data_validade < hoje`. Mais simples de consultar. |
| manter `int validade` (dias) | preserva o prazo por tipo de componente como dado; exige campo derivado ou consulta calculada para ordenar |
| os dois | `int validadeDias` como regra + `LocalDate dataValidade` gravado no fracionamento |

O HU02 pede "data de validade" explicitamente, o que empurra para a primeira ou a terceira.

---

## Estoque

`Estoque` não é entidade nem coleção dentro de `Instituicao`. É **visão calculada** sobre `Hemocomponente.instituicaoAtualId`.

Só `HEMOCENTRO` tem estoque alocável: `PONTO_COLETA` só gera bolsas, `HOSPITAL` só recebe.

A disponibilidade do HU02 também é calculada, não é campo:

| Status exibido (HU02) | Regra |
|---|---|
| Disponível | `status = APTO` **e** não vencido **e** `requisicaoAlocadaId = null` |
| Reservada | `requisicaoAlocadaId != null` |
| Vencida | validade já passou (ver a decisão acima) |
| Utilizada | fora de escopo — depende do HU08 (transporte/entrega) |

Repare que `StatusHemocomponente` responde só à primeira condição. Os quatro rótulos do HU02 saem da combinação dos três campos, e é por isso que não existe (nem deve existir) um enum `StatusEstoque`.

A estrutura de índice por hash + fila de prioridade por validade (FEFO), entrega do time de Algoritmos, é interna ao serviço de alocação. Não é exposta na API — só o resultado.

---

# Detalhamento da API

Cada rota indica quem pode chamá-la (HU01: "cada perfil deve ter acesso às funcionalidades correspondentes") e se já existe. **Nenhuma rota é protegida hoje** — a coluna "Quem chama" é alvo, não realidade.

## `/login` — ✗ não existe

| Método | Rota | Body | Quem chama | Descrição |
|--------|------|------|------------|-----------|
| POST | `/login` | `{email, password}` | Público | Autentica e devolve token (HU01). Enquanto não existir, `hospitalId` vem do body. |

## `/usuarios` — ⚠ existe como MVC, não como REST

| Método | Rota | Body | Quem chama | Descrição |
|--------|------|------|------------|-----------|
| GET | `/usuarios` | — | `ADMIN` | Lista `Usuario` sem `password`. Ver todos os usuários entre instituições é operação sensível. |
| POST | `/usuarios` | `UsuarioRequest` | Público | Cadastro (HU01), vinculado a uma `idInstituicao` existente. |
| GET | `/usuarios/{id}` | — | Próprio usuário / `ADMIN` | Sem `password`. |
| PUT | `/usuarios/{id}` | `UsuarioRequest` | Próprio usuário | Atualização completa. |
| PATCH | `/usuarios/{id}` | parcial | Próprio usuário | Atualização parcial. |
| DELETE | `/usuarios/{id}` | — | `ADMIN` | Ver **DELETE para quem?** |

Migrar o controller atual exige, nesta ordem: `UsuarioResponse` sem `password`, `@RestController` em vez de `@Controller`, `DELETE /usuarios/{id}` no lugar de `GET /usuarios/remover/{id}`, e `400` com a lista de erros de validação em vez de re-renderizar o formulário.

## `/instituicoes` — ✗ não existe (entidade e repository prontos)

| Método | Rota | Body | Quem chama | Descrição |
|--------|------|------|------------|-----------|
| GET | `/instituicoes` | — | Autenticado | Lista todas. |
| POST | `/instituicoes` | json | Público | Cadastro (HU01), antes de existir qualquer usuário. CNPJ único → `409` se repetido. |
| GET | `/instituicoes/{id}` | — | Autenticado | |
| GET | `/instituicoes/{id}/estoque` | — | Hemocentro dono | Estoque **calculado** (HU02): contagem por `tipo` + `abo` + `rh`. Filtros: `tipo`, `abo`, `rh`. `detalhado=true` devolve a lista de `Hemocomponente` em vez da contagem. Só faz sentido para `HEMOCENTRO`. |
| PUT / PATCH | `/instituicoes/{id}` | json | Usuário da própria instituição | |
| DELETE | `/instituicoes/{id}` | — | `ADMIN` | Ver **DELETE para quem?** |

## `/requisicoes` — ✓ implementado (exceto onde indicado)

| Método | Rota | Body | Quem chama | Descrição |
|--------|------|------|------------|-----------|
| GET | `/requisicoes` | — | Hospital (só as próprias) / Hemocentro (todas) | Alvo: filtro `?status=ABERTA` para o hemocentro ver pendentes (HU05) — **✗ o parâmetro é ignorado hoje**. |
| POST | `/requisicoes` | `RequisicaoRequest` | Hospital | Cria com status `ABERTA` (HU03). Obrigatórios: `tipo`, `abo`, `rh`, `volumeMl`, `prioridade`. Alvo: `hospitalId` do token; `201` + `Location`. |
| GET | `/requisicoes/{id}` | — | Hospital dono / Hemocentro | HU04. |
| PATCH | `/requisicoes/{id}` | parcial (`prioridade`, `observacoes`) | Hospital dono | Nunca altera `status`. **Hoje é `PUT` e só copia `prioridade`** — o alvo é trocar o verbo, não o comportamento. |
| POST | `/requisicoes/{id}/aceitar` | — | Hemocentro | `ABERTA → ACEITA` (HU05). `409` fora de `ABERTA`. Alvo: gravar também o `hemocentroId`. |
| POST | `/requisicoes/{id}/recusar` | `RecusaRequest` | Hemocentro | `ABERTA → RECUSADA` (HU05). `motivoRecusa` obrigatório → `400`. |
| POST | `/requisicoes/{id}/alocacoes` | — | Hemocentro | **✗** Só a partir de `ACEITA`. Aloca por compatibilidade ABO/Rh + FEFO, muda para `ALOCADA`, devolve os ids (HU06). `409` sem hemocomponente compatível. |
| POST | `/requisicoes/{id}/cancelar` | — | Hospital dono | **✗** `ABERTA`/`ACEITA` → `CANCELADA`. |
| DELETE | `/requisicoes/{id}` | — | `ADMIN` | Não é o caminho de cancelamento — isso é `CANCELADA`. Ver **DELETE para quem?** |

## `/bolsas` — ✗ não existe (entidade e repository prontos)

| Método | Rota | Body | Quem chama | Descrição |
|--------|------|------|------------|-----------|
| GET | `/bolsas` | — | Hemocentro / Ponto de Coleta da própria instituição | Filtros: `abo`, `rh`, `instituicaoAtualId` (`ADMIN` vê todas). |
| POST | `/bolsas` | json | Ponto de Coleta / Hemocentro | Registra a coleta. `instituicaoAtualId` = quem coletou. |
| GET | `/bolsas/{id}` | — | Dono | |
| POST | `/bolsas/{id}/hemocomponentes` | json | Hemocentro | Fraciona a bolsa e devolve os ids dos `Hemocomponente` gerados, com `bolsaOrigemId` e `instituicaoAtualId` herdados. |
| PUT / PATCH | `/bolsas/{id}` | json | Dono | Inclui mover `instituicaoAtualId` e marcar `emTransito`. |
| DELETE | `/bolsas/{id}` | — | `ADMIN` | Ver **DELETE para quem?** |

## `/hemocomponentes` — ✗ não existe (entidade e repository prontos)

| Método | Rota | Body | Quem chama | Descrição |
|--------|------|------|------------|-----------|
| GET | `/hemocomponentes` | — | Hemocentro da própria instituição | Filtros: `tipo`, `abo`, `rh`, `status`, `instituicaoAtualId` (`ADMIN` vê todos). |
| ~~POST~~ | ~~`/hemocomponentes`~~ | — | — | Não existe: só nasce de `POST /bolsas/{id}/hemocomponentes`. |
| GET | `/hemocomponentes/{id}` | — | Hemocentro dono | |
| PUT / PATCH | `/hemocomponentes/{id}` | json | Hemocentro dono | Inclui `status` (ex.: `DESCARTADO`) e `instituicaoAtualId`. |
| DELETE | `/hemocomponentes/{id}` | — | `ADMIN` | Descarte normal é `status = DESCARTADO`, não `DELETE`. Ver abaixo. |

## DELETE para quem?

Faz sentido, com duas ressalvas:

1. **Precisa existir o papel.** `TipoInstituicao` são papéis de negócio, não de acesso. `TipoUsuario.ADMIN` já está modelado e é o que torna "só admin deleta" verificável — falta só a checagem no service.
2. **Soft delete por baixo do capô.** Para `Usuario` e `Instituicao` o delete físico é inofensivo. Para `Bolsa`, `Hemocomponente` e `Requisicao` — que alimentam os indicadores do HU09 e a rastreabilidade — a rota continua respondendo `204`, mas internamente marca `removidoEm`. O contrato não muda; muda a implementação, e um erro de operação deixa de virar perda de histórico.

Para o escopo do projeto (a rubrica não cobra RBAC granular), um `TipoUsuario` + uma checagem no service resolve. Não vale super-engenhar.
