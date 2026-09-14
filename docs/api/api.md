
```
Coleta ────► Hemocentro ────► Hospital
                 │
                 └─ processamento
```
# Hemotrack — API REST

Este documento tem duas partes:

1. **[Estado atual](#estado-atual-implementado)** — o que existe e roda hoje no código, com exemplos reais.
2. **[Desenho alvo](#desenho-alvo-planejado)** — o contrato completo que a API deve ter quando as HUs estiverem fechadas.

Quando os dois divergirem, a parte 1 é a verdade. As divergências conhecidas estão listadas em [Divergências e lacunas](#divergências-e-lacunas).

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

> ⚠ É **`PLASMA`**, não `PLASMA_FRESCO_CONGELADO`. Mandar o valor errado devolve `400`.

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
| `id` | `Long` | não | gerado (`IDENTITY`). **Hoje é aceito no body** — ver lacunas |
| `dataCriacao` | `Instant` (ISO-8601 UTC) | não | preenchido por `@CreationTimestamp`, imutável |
| `hospitalId` | `Long` | **sim** | hoje vem do body (não há token) |
| `tipo` | `TipoHemocomponente` | **sim** | |
| `abo` | `TipoABO` | **sim** | |
| `rh` | `FatorRh` | **sim** | |
| `volumeMl` | `double` | **sim** (primitivo, default `0.0`) | não há validação de mínimo |
| `prioridade` | `Prioridade` | **sim** | |
| `status` | `StatusRequisicao` | não | default `ABERTA`. **Hoje é aceito no body** — ver lacunas |
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

## Divergências e lacunas

### Divergências entre o código e o desenho alvo

| # | Onde | Código hoje | Desenho alvo |
|---|---|---|---|
| 5 | `PUT /requisicoes/{id}` | atualiza só `prioridade` | `PATCH` com campos parciais; `PUT` completo |
| 6 | `GET /requisicoes` | sem filtros | `?status=ABERTA` (HU05) |

### Lacunas que valem virar issue

1. **Falta `@Valid` no `@RequestBody`.** O `RequisicaoController` recebe `@RequestBody Requisicao` sem `@Valid`, então as anotações `@NotNull` da entidade não rodam no controller. Elas só disparam no *flush* do Hibernate, o que vira **`500 Internal Server Error`** em vez de `400 Bad Request` com a lista de campos faltando. Fix: `@Valid @RequestBody` + um `@RestControllerAdvice` para `MethodArgumentNotValidException`.

2. **`status` e `id` são aceitos no body do POST.** Como não há DTO de entrada, um cliente pode criar uma requisição já `ATENDIDA`, ou tentar forçar um `id`. Fix: `RequisicaoRequest` DTO só com os campos que o hospital pode mandar.

3. **Enums persistidos como `ORDINAL`.** Nenhum campo enum tem `@Enumerated(EnumType.STRING)`, então o H2 grava o **índice** (`0`, `1`, `2`…). Combinado com `ddl-auto=update` e um banco em arquivo que sobrevive entre execuções, **reordenar um enum corrompe silenciosamente as linhas existentes** — e a divergência #2 acima é exatamente esse risco já materializado. Fix: `@Enumerated(EnumType.STRING)` em todos os campos enum (e apagar `backend/data/hemotrack-db.mv.db` uma vez depois da mudança).

4. **Sem autenticação.** `hospitalId` vem do body, então qualquer um cria requisição em nome de qualquer hospital, e qualquer um aceita/recusa. Toda a coluna "Quem chama" do desenho alvo está inaplicável hoje.

5. **Sem `hospitalId` como FK.** É um `Long` solto, não um `@ManyToOne` para `Instituicao`. Nada impede apontar para uma instituição inexistente ou para um `HEMOCENTRO`.

6. **Sem testes.** `BackendApplicationTests` só tem o `contextLoads()`. As regras de transição (`409` ao aceitar duas vezes, `400` sem motivo) são exatamente o tipo de coisa que cabe num `@WebMvcTest` e que a rubrica costuma cobrar — e o CI (`.github/workflows/ci.yml`) já roda `mvnw verify` em todo PR para `main`, então o teste passa a valer automaticamente.

7. **Detalhe de nomenclatura:** o setter de `volumeMl` está escrito `setvolumeMl` (v minúsculo). Funciona por acidente — o Jackson resolve a propriedade pelo getter — mas qualquer ferramenta que dependa de convenção JavaBean estrita vai tropeçar. O mesmo vale para `getTipoUsuario()` sobre o campo `tipo` em `Usuario`, que faria a propriedade JSON virar `tipoUsuario`.

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
| 6–11 | erros: `409` (aceitar/recusar fora de `ABERTA`), `404`, `400` (enum inválido, `volumeMl` nulo) e o `500` da lacuna #1 |
| 12–15 | fluxo de recusa: sem motivo → `400`, motivo em branco → `400`, motivo válido → `RECUSADA` |
| 16–18 | limpeza: delete das duas requisições + confirmação `404` |
| final | rotas do desenho alvo que **ainda não existem** (filtro `?status=`, `/alocacoes`, `/cancelar`) |

## Convenções do `.http` (só para quem for editar o arquivo)

- **O corpo JSON é sempre a última coisa do bloco.** Depois da linha em branco que fecha os headers, tudo vira corpo — inclusive linhas `@variavel`. Por isso todo comentário fica **acima** da linha do método.
- `@baseUrl` e `@contentType` estão no topo; não repita a URL literal nos blocos.
- Para encadear um id, nomeie o bloco de origem com `# @name criar` e use `{{criar.response.body.$.id}}` nos seguintes.
- Enums vão como **string**. Mandar o ordinal (`"tipo": 0`) funciona, mas amarra o JSON à ordem de declaração do enum — é exatamente a lacuna #3 acima. Não use.
- Bloco novo = nova linha `### <número>. <o que faz>` + um comentário com o **esperado** (`# Esperado: 409 ...`). É o esperado que transforma o arquivo em teste, não a chamada.

## Conferindo o que gravou no banco

`http://localhost:8080/h2-console` → JDBC URL `jdbc:h2:file:./data/hemotrack-db`, user `sa`, senha em branco.

```sql
SELECT * FROM REQUISICOES;
```

Repare que `TIPO`, `ABO`, `RH`, `PRIORIDADE` e `STATUS` aparecem como **números**, não como texto — é a lacuna #3 acima.

**Para começar do zero:** pare a aplicação e apague `backend/data/hemotrack-db.mv.db`. Com `ddl-auto=update` o schema é recriado no próximo start.

---
---

# Desenho alvo (planejado)

> Tudo abaixo é o contrato que queremos atingir, **não o que está implementado**. Conferir contra a parte 1 antes de usar.

## **Sistema**

### class `Usuario`
**Campos (resposta):** Id, Nome Completo, Email, CPF, InstituicaoId

**Campos (entrada — POST/PUT/PATCH):** Nome Completo, Email, Senha, CPF, InstituicaoId

A senha nunca é devolvida pela API — schema de resposta e de entrada são diferentes.

**Tipos de usuário** (definidos pelo `TipoInstituicao` da instituição vinculada):
- **Usuário Hemocentro** — visualiza requisições recebidas, aceita/recusa, aloca hemocomponentes, gerencia estoque.
- **Usuário Hospital** — cria Requisições De Transfusão (RT), acompanha suas próprias requisições.

### enum `TipoPerfil`
`PADRAO`, `ADMIN`

Campo novo em `Usuario`, ortogonal ao `TipoInstituicao`. `TipoInstituicao` diz *o que* o usuário faz no domínio (Hospital pede, Hemocentro atende); `TipoPerfil` diz *o nível de acesso* — `ADMIN` é operação/suporte do próprio HemoTrack, não um papel de negócio. Um usuário `ADMIN` continua vinculado a uma instituição (pode ser uma instituição interna "HemoTrack Sistema") mas pode chamar rotas que um usuário `PADRAO` não pode — hoje, basicamente os `DELETE`.

*(No código já existe como `TipoUsuario`.)*

Cada rota abaixo indica quem pode chamá-la (HU01: "cada perfil deve ter acesso às funcionalidades correspondentes").

---
### enum `TipoABO`
`A`, `B`, `AB`, `O`
### enum `FatorRh`
`POSITIVO`, `NEGATIVO`
### enum `TipoHemocomponente`
`HEMACIAS`, `PLASMA_FRESCO_CONGELADO`, `PLAQUETAS`, `CRIOPRECIPITADO`

---
### enum `Prioridade`
`NORMAL`, `URGENCIA`, `EMERGENCIA`

### enum `StatusRequisicao`
`ABERTA` → `ACEITA` → `ALOCADA` → `ATENDIDA`
ou `ABERTA` → `RECUSADA`
ou `ABERTA` / `ACEITA` → `CANCELADA` (cancelada pelo hospital)

- **ABERTA** — Pendente (HU03/HU04). Requisição criada, aguardando análise do hemocentro.
- **ACEITA** — hemocentro confirmou que consegue atender (HU05), ainda sem hemocomponentes específicos reservados.
- **ALOCADA** — hemocomponentes já reservados via compatibilidade + FEFO (HU06).
- **ATENDIDA** — entregue. *(A granularidade "em separação"/"em transporte" do HU04 entra quando HU07/HU08 forem modeladas; por ora ATENDIDA cobre tudo depois da alocação.)*
- **RECUSADA** — hemocentro não consegue atender; exige `motivoRecusa` preenchido (HU05).
- **CANCELADA** — cancelada pelo hospital antes de ser atendida.

### class `RequisicaoDeTransfusao`
**Campos:** Id, Data, **hospitalId**, **TipoHemocomponente**, **TipoABO**, **FatorRh**, volumeMl, observacoes (opcional), **Prioridade**, **StatusRequisicao**, **motivoRecusa** (preenchido só quando `RECUSADA`)

---
### class `Bolsa`
**Campos:** id, **TipoABO**, **FatorRh**, volumeMl, dataDeColeta, validade, **instituicaoAtualId**, boolEmTransito.

---
### enum `StatusQualidade`
`EM_ANALISE`, `APTO`, `DESCARTADO`

*(No código já existe como `StatusHemocomponente`.)*

### class `Hemocomponente`
**Campos:** id, **TipoABO**, **FatorRh**, **TipoHemocomponente**, volumeMl, bolsaOrigemId, dataProcessamento, **StatusQualidade**, validade, **instituicaoAtualId**, requisicaoAlocadaId, boolEmTransito.

`StatusQualidade` descreve só o resultado do controle de qualidade (passou/não passou no laboratório). Ele **não** é o mesmo que disponibilidade de estoque — ver seção **Estoque**.

---
### enum `TipoInstituicao`
`PONTO_COLETA`, `HEMOCENTRO`, `HOSPITAL`

### class `Instituicao`
**Campos:** Id, Razao Social, CNPJ, **TipoInstituicao**.

*(Não tem mais campo `Estoque` — ver abaixo.)*

## **Estoque**

`Estoque` não é uma entidade persistida nem um array guardado dentro de `Instituicao`. É uma **visão calculada** a partir de `Hemocomponente.instituicaoAtualId`.

Só `HEMOCENTRO` tem estoque disponível para alocação (`PONTO_COLETA` só gera bolsas; `HOSPITAL` só recebe).

Disponibilidade de um `Hemocomponente` também é calculada, não é um campo próprio:

| "Status" exibido (HU02) | Regra |
|---|---|
| Disponível | `StatusQualidade = APTO` e `validade >= hoje` e `requisicaoAlocadaId = null` |
| Reservada | `requisicaoAlocadaId != null` |
| Vencida | `validade < hoje` |
| Utilizada | fora de escopo por enquanto (depende de HU08 — transporte/entrega) |

A estrutura de índice por hash + fila de prioridade por validade (FEFO), que o time de Algoritmos precisa entregar, é interna ao serviço de alocação — não é exposta na API, só o resultado (bolsas alocadas).

---

# Detalhamento da API

## `/login`

| Método | Rota          | Body | Quem chama | Descrição |
|--------|---------------|------|------------|-----------|
| POST   | `/login` | `{email, senha}` | Público | Autentica o usuário e devolve token. Necessário para HU01 ("informar suas credenciais... permitir o acesso"). |

## `/usuarios`

| Método | Rota          | Body                   | Quem chama | Descrição                                                     |
|--------|---------------|------------------------|------------|---------------------------------------------------------------|
| GET    | `/usuarios`      | —                      | `ADMIN` | Lista todos os **Usuarios** cadastrados (sem senha). Ver todos os usuários do sistema, entre instituições, é sensível — não é operação de um usuário `PADRAO`. |
| POST   | `/usuarios`      | json | Público | Cria um **Usuario** vinculado a uma `instituicaoId` já existente (cadastro, HU01). |
| GET    | `/usuarios/{id}` | —                      | Próprio usuário / `ADMIN` | Retorna **Usuario** com `id`, caso exista (sem senha). |
| DELETE | `/usuarios/{id}` | —                      | `ADMIN` | Ver **Dúvida: DELETE para quem?** abaixo. |
| PUT    | `/usuarios/{id}` | json | Próprio usuário | Atualiza completamente o **Usuario** com `id`, com infos do Body. |
| PATCH  | `/usuarios/{id}` | json (parcial) | Próprio usuário | Atualiza parcialmente **Usuario** com `id`, com dados do Body. |

## `/instituicoes`

| Método | Rota          | Body                   | Quem chama | Descrição                                                     |
|--------|---------------|------------------------|------------|---------------------------------------------------------------|
| GET    | `/instituicoes`| — | Qualquer usuário autenticado | Retorna todas as **Instituicoes** cadastradas. |
| POST   | `/instituicoes`      | json | Público | Cria uma **Instituicao** com infos do Body (cadastro, HU01, antes de existir qualquer usuário). CNPJ deve ser único. |
| GET    | `/instituicoes/{id}` | —    | Qualquer usuário autenticado | Retorna **Instituicao** com `id`, caso exista.|
| GET    | `/instituicoes/{id}/estoque` | —    | Hemocentro dono | Retorna a visão agregada do estoque **calculado** da instituição (só para `HEMOCENTRO`) — contagem por `tipoHemocomponente` + `tipoABO` + `fatorRh`. Parâmetros opcionais: `tipoHemocomponente`, `tipoABO`, `fatorRh` (filtram a agregação); `detalhado=true` (devolve a lista de `Hemocomponente`, não só a contagem). Atende HU02 ("consultar estoque disponível por tipo sanguíneo/componente"). |
| DELETE | `/instituicoes/{id}` | —    | `ADMIN` | Ver **Dúvida: DELETE para quem?** abaixo. |
| PUT    | `/instituicoes/{id}` | json | Usuário da própria instituição | Atualiza completamente a **Instituicao** com `id`, com infos do Body.|
| PATCH  | `/instituicoes/{id}` | json (parcial) | Usuário da própria instituição | Atualiza parcialmente a **Instituicao** com `id`, com dados do Body. |

## `/requisicoes`

| Método | Rota          | Body                   | Quem chama | Descrição                                                     |
|--------|---------------|------------------------|------------|---------------------------------------------------------------|
| GET    | `/requisicoes`| — | Hospital (só as próprias, filtrado por `hospitalId` do token) / Hemocentro (todas, ou `?status=ABERTA` para ver pendentes — HU05) | Lista requisições. Suporta filtro `?status=`. |
| POST   | `/requisicoes`      | json (`hospitalId` vem do usuário autenticado, não do body) | Hospital | Cria uma **RequisicaoDeTransfusao**, status inicial `ABERTA` (HU03). Campos obrigatórios: tipoHemocomponente, tipoABO, fatorRh, volumeMl, prioridade. |
| GET    | `/requisicoes/{id}` | —    | Hospital dono / Hemocentro | Retorna **RequisicaoDeTransfusao** com `id`, caso exista (HU04). |
| POST   | `/requisicoes/{id}/aceitar` | — | Hemocentro | `ABERTA → ACEITA`. Confirma que o hemocentro vai atender (HU05). 409 se a requisição não estiver `ABERTA`. |
| POST   | `/requisicoes/{id}/recusar` | `{motivo}` | Hemocentro | `ABERTA → RECUSADA`. `motivo` obrigatório (HU05). |
| POST   | `/requisicoes/{id}/alocacoes` | — | Hemocentro | Só permitido com status `ACEITA`. Aloca hemocomponentes compatíveis (ABO/Rh + FEFO), muda status para `ALOCADA` e retorna os ids alocados (HU06). 409 se não houver hemocomponente compatível disponível. |
| PATCH  | `/requisicoes/{id}` | json (parcial, campos como `prioridade`, `observacoes`) | Hospital dono | Atualiza dados da requisição — **não altera `status`**, que só muda pelas ações acima. |
| DELETE | `/requisicoes/{id}` | — | `ADMIN` | Ver **Dúvida: DELETE para quem?** abaixo — não é o caminho normal de cancelamento (isso é `status = CANCELADA`). |

## `/bolsas`

| Método | Rota          | Body                   | Quem chama | Descrição                                                     |
|--------|---------------|------------------------|------------|---------------------------------------------------------------|
| GET    | `/bolsas`| — | Hemocentro / Ponto de Coleta (da própria instituição) | Retorna as **Bolsas** da instituição do usuário. Filtros: `tipoABO`, `fatorRh`, `instituicaoAtualId` (`ADMIN` pode ver todas). |
| POST   | `/bolsas`      | json | Ponto de Coleta / Hemocentro | Cria uma **Bolsa** com infos do Body (coleta). `instituicaoAtualId` = ponto de coleta/hemocentro que recebeu. |
| POST   | `/bolsas/{id}/hemocomponentes`      | json | Hemocentro | Processa **Bolsa** indicada e retorna os ids dos **Hemocomponentes** resultantes, já com `instituicaoAtualId` herdado da bolsa. |
| GET    | `/bolsas/{id}` | —    | Hemocentro / Ponto de Coleta dono | Retorna **Bolsa** com `id`, caso exista.|
| PUT    | `/bolsas/{id}` | json | Hemocentro / Ponto de Coleta dono | Atualiza completamente a **Bolsa** com `id`, com infos do Body.|
| PATCH  | `/bolsas/{id}` | json (parcial) | Hemocentro / Ponto de Coleta dono | Atualiza parcialmente **Bolsa** com `id` — inclui mover `instituicaoAtualId` (transporte) e marcar descarte. |
| DELETE | `/bolsas/{id}` | — | `ADMIN` | Ver **Dúvida: DELETE para quem?** abaixo — não é o caminho normal de descarte (isso é status). |

## `/hemocomponentes`

| Método | Rota          | Body                   | Quem chama | Descrição                                                     |
|--------|---------------|------------------------|------------|---------------------------------------------------------------|
| GET    | `/hemocomponentes`| — | Hemocentro (da própria instituição) | Retorna os **Hemocomponentes** da instituição do usuário. Filtros: `tipoHemocomponente`, `tipoABO`, `fatorRh`, `instituicaoAtualId`, `statusQualidade` (`ADMIN` pode ver todos). |
| ~~POST~~   | ~~`/hemocomponentes`~~      | ~~json~~ | — | Não existe — só nasce de `/bolsas/{id}/hemocomponentes`. |
| GET    | `/hemocomponentes/{id}` | —    | Hemocentro dono | Retorna **Hemocomponente** com `id`, caso exista.|
| PUT    | `/hemocomponentes/{id}` | json | Hemocentro dono | Atualiza completamente o **Hemocomponente** com `id`, com infos do Body.|
| PATCH  | `/hemocomponentes/{id}` | json (parcial) | Hemocentro dono | Atualiza parcialmente **Hemocomponente** com `id` — inclui `statusQualidade` (ex.: marcar `DESCARTADO`) e `instituicaoAtualId`. |
| DELETE | `/hemocomponentes/{id}` | — | `ADMIN` | Ver **Dúvida: DELETE para quem?** abaixo — não é o caminho normal de descarte (isso é `statusQualidade = DESCARTADO`). |

## Dúvida: DELETE para quem?

Faz sentido, sim — mas com duas ressalvas:

1. **Precisa existir o papel.** Hoje o domínio só tem Hospital e Hemocentro (via `TipoInstituicao`), que são papéis de negócio, não de acesso. "Admin do sistema" é um conceito novo — por isso o `TipoPerfil` (`PADRAO`/`ADMIN`) acima. Sem isso modelado, "só admin pode deletar" não tem como ser verificado em lugar nenhum.
2. **Mesmo o `ADMIN` deletando por engano/erro, considerar soft delete por baixo do capô.** Pra `Usuario` e `Instituicao` um `DELETE` físico tende a ser inofensivo. Já pra `Bolsa`, `Hemocomponente` e `Requisicao` — que têm indicadores (HU09, mais pra frente) e potencialmente auditoria de saúde pública — a recomendação é a rota continuar respondendo `204` normalmente, mas internamente marcar o registro como removido (`removidoEm`, por exemplo) em vez de apagar a linha. Do ponto de vista do contrato da API não muda nada; muda só a implementação, e evita que uma correção de erro vire perda de histórico.

Pro escopo do projeto (rubrica não cobra RBAC granular), não vale super-engenhar isso: um campo `TipoPerfil` + uma checagem simples no controller/service já resolve.
