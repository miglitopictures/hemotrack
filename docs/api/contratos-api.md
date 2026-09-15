# HemoTrack — Contrato API

Fonte: `docs/api/contrato-api.ods`. Entidades e enums em [`dominio.md`](./dominio.md).

**Prefixo de todas as rotas:** `/api/v1`

---

## Convenções

### Papéis

| Papel | Quem é |
|---|---|
| `ADMIN_SISTEMA` | operação do próprio HemoTrack |
| `ADMIN_INSTITUICAO` | administra usuários e cadastro da instituição |
| `OPERADOR` | uso do dia a dia |

O papel e o `instituicaoId` vêm do JWT. Não existe `TipoUsuario`: se o usuário age como hospital ou como hemocentro é derivado de `instituicao.tipo`.

### Qualificadores de `@caller`

| Termo | Significado |
|---|---|
| `PUBLICO` | sem token |
| `AUTENTICADO` | logado **e** vinculado a instituição `APROVADA` |
| `MEMBRO` | autenticado cujo `instituicaoId` = `{id}` da rota |
| `DONO` | autenticado cujo `usuarioId` = `{id}`/`{uid}` da rota |

### Respostas omitidas linha a linha

| Código | Quando |
|---|---|
| `401` | token ausente, inválido ou expirado — qualquer rota autenticada |
| `403` | papel insuficiente, ou instituição ainda `PENDENTE_APROVACAO` tentando usar rota de negócio |

### Erros

`Content-Type: application/problem+json`

```json
{
    "type": "https://hemotrack.dev/erros/cnpj-duplicado",
    "title": "CNPJ já cadastrado",
    "status": 409,
    "detail": "Já existe instituição com o CNPJ 12.345.678/0001-90.",
    "instance": "/api/v1/instituicoes"
}
```

`400` de validação acrescenta `errors`: `[{ "campo": "...", "mensagem": "..." }]`.

### Paginação

Rotas de listagem aceitam `?page` (default `0`) e `?size` (default `20`, máx `100`).

---

## `/auth`

Autenticação por JWT. Claims do token: `usuarioId`, `instituicaoId`, `papel`.

### **POST** `/auth/login`
Autentica um usuário e devolve o token.

**@caller** `PUBLICO`
#### `body`
```json
{
    "email": "miguel@hemope.gov.br",
    "senha": "senhaEmTextoPuro"
}
```
#### `200` - HttpStatus.OK
**@retorna** token e expiração
```json
{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiraEm": "2026-09-15T21:00:00Z"
}
```
#### `400` - HttpStatus.BAD_REQUEST
`email` ou `senha` ausentes ou malformados
#### `401` - HttpStatus.UNAUTHORIZED
credenciais não conferem, ou usuário `ativo = false`

---

### **GET** `/auth/me`
Retorna o usuário autenticado, com seu papel e vínculo institucional. É como o front descobre se está operando como hospital ou hemocentro.

**@caller** `AUTENTICADO`
#### `200` - HttpStatus.OK
**@retorna** `UsuarioResponse` + a instituição resolvida
```json
{
    "id": 1,
    "nome": "Miguel Duarte",
    "email": "miguel@hemope.gov.br",
    "papel": "ADMIN_INSTITUICAO",
    "ativo": true,
    "instituicaoId": 20,
    "instituicao": {
        "id": 20,
        "razaoSocial": "Hemocentro de Pernambuco",
        "cnpj": "12.345.678/0001-90",
        "tipo": "HEMOCENTRO",
        "status": "APROVADA",
        "municipio": "Recife"
    }
}
```

> Única rota autenticada que responde mesmo com instituição `PENDENTE_APROVACAO` — é como o front sabe que precisa mostrar a tela de "aguardando aprovação".

---

## `/instituicoes`

Cadastro, aprovação e dados das instituições (HU01).

### **POST** `/instituicoes`
Cadastra a instituição e seu usuário administrador **em uma única transação**. A instituição nasce `PENDENTE_APROVACAO` e o usuário nasce `ADMIN_INSTITUICAO`.

**@caller** `PUBLICO`
#### `body`
```json
{
    "instituicao": {
        "razaoSocial": "Hemocentro de Pernambuco",
        "cnpj": "12.345.678/0001-90",
        "tipo": "HEMOCENTRO",
        "endereco": "Rua Joaquim Nabuco, 171",
        "municipio": "Recife",
        "telefone": "(81) 3182-4600"
    },
    "administrador": {
        "nome": "Miguel Duarte",
        "email": "miguel@hemope.gov.br",
        "senha": "senhaEmTextoPuro"
    }
}
```
#### `201` - HttpStatus.CREATED
**@retorna** `Location: /api/v1/instituicoes/{id}` + o par criado
```json
{
    "instituicao": {
        "id": 20,
        "razaoSocial": "Hemocentro de Pernambuco",
        "cnpj": "12.345.678/0001-90",
        "tipo": "HEMOCENTRO",
        "status": "PENDENTE_APROVACAO",
        "endereco": "Rua Joaquim Nabuco, 171",
        "municipio": "Recife",
        "telefone": "(81) 3182-4600"
    },
    "administrador": {
        "id": 1,
        "nome": "Miguel Duarte",
        "email": "miguel@hemope.gov.br",
        "papel": "ADMIN_INSTITUICAO",
        "ativo": true,
        "instituicaoId": 20
    }
}
```
#### `400` - HttpStatus.BAD_REQUEST
campos obrigatórios ausentes ou inválidos
#### `409` - HttpStatus.CONFLICT
CNPJ já cadastrado (HU01, cenário 2) ou e-mail já em uso

---

### **GET** `/instituicoes`
Lista instituições **aprovadas**. É o que o hospital usa para achar hemocentros.

**@caller** `AUTENTICADO`
#### `query`
`tipo`, `municipio`, `page`, `size`
#### `200` - HttpStatus.OK
**@retorna** array de `Instituicao`
```json
[
    {
        "id": 20,
        "razaoSocial": "Hemocentro de Pernambuco",
        "cnpj": "12.345.678/0001-90",
        "tipo": "HEMOCENTRO",
        "status": "APROVADA",
        "endereco": "Rua Joaquim Nabuco, 171",
        "municipio": "Recife",
        "telefone": "(81) 3182-4600"
    }
]
```

---

### **GET** `/instituicoes/{id}`
Retorna uma instituição.

**@caller** `AUTENTICADO`
#### `200` - HttpStatus.OK
**@retorna** `Instituicao`
#### `404` - HttpStatus.NOT_FOUND
não existe

---

### **PATCH** `/instituicoes/{id}`
Atualiza dados cadastrais. Não altera `tipo`, `cnpj` nem `status`.

**@caller** `ADMIN_INSTITUICAO` (da própria) · `ADMIN_SISTEMA`
#### `body`
```json
{
    "razaoSocial": "Fundação Hemope",
    "endereco": "Rua Joaquim Nabuco, 171 — Graças",
    "telefone": "(81) 3182-4700"
}
```
#### `200` - HttpStatus.OK
**@retorna** `Instituicao` atualizada
#### `400` - HttpStatus.BAD_REQUEST
campo inválido, ou tentativa de alterar campo imutável
#### `404` - HttpStatus.NOT_FOUND

---

### **PATCH** `/instituicoes/{id}/aprovar`
Aprova o cadastro: `status = APROVADA`. Libera as rotas de negócio para os usuários dela.

**@caller** `ADMIN_SISTEMA`
#### `200` - HttpStatus.OK
**@retorna** `Instituicao` com `status = APROVADA`
#### `404` - HttpStatus.NOT_FOUND
#### `409` - HttpStatus.CONFLICT
já estava `APROVADA`

---

### **DELETE** `/instituicoes/{id}`
Remove a instituição.

**@caller** `ADMIN_SISTEMA`
#### `204` - HttpStatus.NO_CONTENT
removida
#### `404` - HttpStatus.NOT_FOUND
#### `409` - HttpStatus.CONFLICT
há requisição em aberto ou estoque não vazio

---

## `/instituicoes/{id}/usuarios`

Gestão de usuários **dentro** de uma instituição. Não existe listagem global de usuários.

### **GET** `/instituicoes/{id}/usuarios`
Lista os usuários da instituição.

**@caller** `MEMBRO` · `ADMIN_SISTEMA`
#### `query`
`papel`, `ativo`, `page`, `size`
#### `200` - HttpStatus.OK
**@retorna** array de `UsuarioResponse` (nunca inclui `senha`)
```json
[
    {
        "id": 1,
        "nome": "Miguel Duarte",
        "email": "miguel@hemope.gov.br",
        "papel": "ADMIN_INSTITUICAO",
        "ativo": true,
        "instituicaoId": 20
    },
    {
        "id": 2,
        "nome": "Pablo Tamborini",
        "email": "pablo@hemope.gov.br",
        "papel": "OPERADOR",
        "ativo": true,
        "instituicaoId": 20
    }
]
```
#### `404` - HttpStatus.NOT_FOUND

---

### **POST** `/instituicoes/{id}/usuarios`
Cadastra um usuário na instituição. É a única rota que aceita `papel` na entrada.

**@caller** `ADMIN_INSTITUICAO` (da própria)
#### `body`
```json
{
    "nome": "Pablo Tamborini",
    "email": "pablo@hemope.gov.br",
    "senha": "senhaEmTextoPuro",
    "papel": "OPERADOR"
}
```
#### `201` - HttpStatus.CREATED
**@retorna** `Location: /api/v1/instituicoes/{id}/usuarios/{uid}` + `UsuarioResponse`
#### `400` - HttpStatus.BAD_REQUEST
#### `404` - HttpStatus.NOT_FOUND
#### `409` - HttpStatus.CONFLICT
e-mail já cadastrado

> `ADMIN_SISTEMA` não é atribuível por esta rota.

---

### **GET** `/instituicoes/{id}/usuarios/{uid}`
Retorna um usuário.

**@caller** `DONO` · `ADMIN_INSTITUICAO` · `ADMIN_SISTEMA`
#### `200` - HttpStatus.OK
**@retorna** `UsuarioResponse`
#### `404` - HttpStatus.NOT_FOUND

---

### **PATCH** `/instituicoes/{id}/usuarios/{uid}`
Altera papel ou ativa/desativa. Não altera nome, e-mail nem senha — isso é `PATCH /usuarios/{id}`.

**@caller** `ADMIN_INSTITUICAO` (da própria)
#### `body`
```json
{
    "papel": "OPERADOR",
    "ativo": false
}
```
#### `200` - HttpStatus.OK
**@retorna** `UsuarioResponse`
#### `400` - HttpStatus.BAD_REQUEST
#### `404` - HttpStatus.NOT_FOUND
#### `409` - HttpStatus.CONFLICT
rebaixaria ou desativaria o **último** `ADMIN_INSTITUICAO`

---

### **DELETE** `/instituicoes/{id}/usuarios/{uid}`
Remove o usuário da instituição.

**@caller** `ADMIN_INSTITUICAO` · `ADMIN_SISTEMA`
#### `204` - HttpStatus.NO_CONTENT
#### `404` - HttpStatus.NOT_FOUND
#### `409` - HttpStatus.CONFLICT
removeria o **último** `ADMIN_INSTITUICAO`

---

## `/usuarios`

Autogestão. Só existe esta rota fora do escopo institucional.

### **PATCH** `/usuarios/{id}`
Atualiza os próprios dados. Não altera `papel` nem `instituicaoId`.

**@caller** `DONO`
#### `body`
```json
{
    "nome": "Miguel M. Duarte",
    "email": "miguel.duarte@hemope.gov.br",
    "senha": "novaSenhaEmTextoPuro"
}
```
#### `200` - HttpStatus.OK
**@retorna** `UsuarioResponse`
#### `400` - HttpStatus.BAD_REQUEST
#### `404` - HttpStatus.NOT_FOUND

> Alterar `email` para um já existente responde `409`.

---

## `/instituicoes/{id}/estoque`

Estoque de hemocomponentes (HU02). É visão sobre `Hemocomponente.instituicaoId` — hospital também tem estoque, mas só hemocentro cadastra.

### **GET** `/instituicoes/{id}/estoque`
Lista os hemocomponentes da instituição.

**@caller** `MEMBRO`
#### `query`
`tipo`, `abo`, `rh`, `status`, `page`, `size`
#### `200` - HttpStatus.OK
**@retorna** array de `Hemocomponente`
```json
[
    {
        "id": 501,
        "codigoBolsa": "HMP-2026-000501",
        "tipo": "HEMACIAS",
        "abo": "O",
        "rh": "NEGATIVO",
        "dataColeta": "2026-09-01",
        "dataValidade": "2026-10-13",
        "status": "DISPONIVEL",
        "instituicaoId": 20
    }
]
```
#### `404` - HttpStatus.NOT_FOUND

---

### **POST** `/instituicoes/{id}/estoque`
Registra um novo hemocomponente. Nasce `DISPONIVEL` (HU02, cenário 1).

**@caller** `MEMBRO` de `HEMOCENTRO`
#### `body`
```json
{
    "codigoBolsa": "HMP-2026-000501",
    "tipo": "HEMACIAS",
    "abo": "O",
    "rh": "NEGATIVO",
    "dataColeta": "2026-09-01",
    "dataValidade": "2026-10-13"
}
```
#### `201` - HttpStatus.CREATED
**@retorna** `Location` + `Hemocomponente` com `status = DISPONIVEL`
#### `400` - HttpStatus.BAD_REQUEST
campo faltando, `dataColeta` no futuro ou `dataValidade` anterior à coleta
#### `404` - HttpStatus.NOT_FOUND
#### `409` - HttpStatus.CONFLICT
`codigoBolsa` já existe

---

### **GET** `/instituicoes/{id}/estoque/{idHemoc}`
Retorna um hemocomponente.

**@caller** `MEMBRO`
#### `200` - HttpStatus.OK
**@retorna** `Hemocomponente`
#### `404` - HttpStatus.NOT_FOUND

---

### **PATCH** `/instituicoes/{id}/estoque/{idHemoc}`
Atualiza o status. Transições validadas.

**@caller** `MEMBRO`
#### `body`
```json
{
    "status": "DESCARTADO"
}
```
#### `200` - HttpStatus.OK
**@retorna** `Hemocomponente` atualizado; grava linha no histórico
#### `400` - HttpStatus.BAD_REQUEST
`status` fora do enum
#### `404` - HttpStatus.NOT_FOUND
#### `409` - HttpStatus.CONFLICT
transição inválida — ver a máquina de estados em `dominio.md`

> `DISPONIVEL ⇄ RESERVADO` é feito pelas rotas de requisição, não aqui. Esta rota é para descarte e para o `TRANSFUNDIDO` no hospital.

---

### **DELETE** `/instituicoes/{id}/estoque/{idHemoc}`
Remove hemocomponente cadastrado por engano. **Descarte normal é `PATCH status=DESCARTADO`**, não isto.

**@caller** `ADMIN_SISTEMA`
#### `204` - HttpStatus.NO_CONTENT
#### `404` - HttpStatus.NOT_FOUND
#### `409` - HttpStatus.CONFLICT
está `RESERVADO` ou `TRANSFUNDIDO`

---

## `/disponibilidade`

### **GET** `/disponibilidade`
Contagem agregada de hemocomponentes `DISPONIVEL` e não vencidos, por instituição. É como o hospital descobre onde pedir antes de abrir a requisição.

**@caller** `AUTENTICADO`
#### `query`
`tipo`, `abo`, `rh`, `municipio`
#### `200` - HttpStatus.OK
**@retorna** array agregado
```json
[
    {
        "instituicaoId": 20,
        "nome": "Hemocentro de Pernambuco",
        "municipio": "Recife",
        "quantidade": 42
    }
]
```

> Agregado de propósito: não expõe `codigoBolsa` nem validade de estoque alheio.

---

## `/hemocomponentes`

### **GET** `/hemocomponentes/{id}/historico`
Rastreio: toda mudança de status e de instituição, em ordem cronológica.

**@caller** `AUTENTICADO`
#### `200` - HttpStatus.OK
**@retorna** array de eventos
```json
[
    {
        "data": "2026-09-01T09:12:00Z",
        "status": "DISPONIVEL",
        "instituicaoId": 20,
        "usuarioId": 2
    },
    {
        "data": "2026-09-10T14:30:00Z",
        "status": "RESERVADO",
        "instituicaoId": 20,
        "usuarioId": 2
    },
    {
        "data": "2026-09-11T08:05:00Z",
        "status": "RESERVADO",
        "instituicaoId": 31,
        "usuarioId": 7
    }
]
```
#### `404` - HttpStatus.NOT_FOUND

---

## `/requisicoes`

Ciclo de vida da requisição (HU03 a HU08). Só esta família muda `StatusRequisicao`.

### **GET** `/requisicoes`
Lista requisições. `HOSPITAL` vê as próprias; `HEMOCENTRO` vê as pendentes e as que aceitou (HU04, HU05).

**@caller** `MEMBRO` de `HOSPITAL` · `MEMBRO` de `HEMOCENTRO`
#### `query`
`status`, `prioridade`, `page`, `size`
#### `200` - HttpStatus.OK
**@retorna** array de `Requisicao`

---

### **POST** `/requisicoes`
Cria a requisição. Nasce `ABERTA` — o "Pendente" do HU03.

**@caller** `MEMBRO` de `HOSPITAL`
#### `body`
```json
{
    "itens": [
        { "tipo": "HEMACIAS", "abo": "O", "rh": "NEGATIVO", "quantidade": 3 },
        { "tipo": "PLAQUETAS", "abo": "O", "rh": "NEGATIVO", "quantidade": 1 }
    ],
    "prioridade": "URGENCIA",
    "observacoes": "Cirurgia agendada para 16/09."
}
```
#### `201` - HttpStatus.CREATED
**@retorna** `Location: /api/v1/requisicoes/{id}` + `Requisicao`
```json
{
    "id": 77,
    "hospitalId": 31,
    "hemocentroId": null,
    "itens": [
        { "tipo": "HEMACIAS", "abo": "O", "rh": "NEGATIVO", "quantidade": 3 },
        { "tipo": "PLAQUETAS", "abo": "O", "rh": "NEGATIVO", "quantidade": 1 }
    ],
    "prioridade": "URGENCIA",
    "status": "ABERTA",
    "observacoes": "Cirurgia agendada para 16/09.",
    "motivoRecusa": null,
    "hemocomponenteIds": [],
    "criadaEm": "2026-09-15T12:00:00Z",
    "atualizadaEm": "2026-09-15T12:00:00Z"
}
```
#### `400` - HttpStatus.BAD_REQUEST
`itens` vazio, `quantidade < 1` ou campo obrigatório faltando (HU03, cenário 2)

> `hospitalId` vem do JWT. `id` e `status` no body são ignorados.

---

### **GET** `/requisicoes/{id}`
Detalhe da requisição (HU04, cenário 2).

**@caller** `MEMBRO` do `HOSPITAL` dono · `MEMBRO` de `HEMOCENTRO`
#### `200` - HttpStatus.OK
**@retorna** `Requisicao`
#### `404` - HttpStatus.NOT_FOUND

---

### **PATCH** `/requisicoes/{id}`
Atualiza a requisição. Só `prioridade` e `observacoes`.

**@caller** `MEMBRO` do `HOSPITAL` dono
#### `body`
```json
{
    "prioridade": "EMERGENCIA",
    "observacoes": "Antecipada para hoje."
}
```
#### `200` - HttpStatus.OK
**@retorna** `Requisicao` atualizada
#### `400` - HttpStatus.BAD_REQUEST
#### `404` - HttpStatus.NOT_FOUND
#### `409` - HttpStatus.CONFLICT
`status != ABERTA`

---

### **PATCH** `/requisicoes/{id}/aceitar`
Hemocentro assume a requisição: `status = ACEITA` e grava `hemocentroId` (HU05, cenário 2).

**@caller** `MEMBRO` de `HEMOCENTRO`
#### `200` - HttpStatus.OK
**@retorna** `Requisicao` com `status = ACEITA` e `hemocentroId` preenchido
#### `404` - HttpStatus.NOT_FOUND
#### `409` - HttpStatus.CONFLICT
`status != ABERTA` — inclui o caso de já ter sido aceita por outro hemocentro

---

### **PATCH** `/requisicoes/{id}/recusar`
Recusa com motivo obrigatório (HU05, cenário 3). Terminal.

**@caller** `MEMBRO` de `HEMOCENTRO`
#### `body`
```json
{
    "motivoRecusa": "Sem estoque de O- para o volume solicitado."
}
```
#### `200` - HttpStatus.OK
**@retorna** `Requisicao` com `status = RECUSADA` e `motivoRecusa` gravado
#### `400` - HttpStatus.BAD_REQUEST
`motivoRecusa` ausente ou em branco
#### `404` - HttpStatus.NOT_FOUND
#### `409` - HttpStatus.CONFLICT
`status != ABERTA`

---

### **PATCH** `/requisicoes/{id}/alocar`
Reserva hemocomponentes do próprio estoque (HU06): `status = ALOCADA` e os hemocomponentes vão para `RESERVADO`.

**@caller** `MEMBRO` do `HEMOCENTRO` dono
#### `body`
```json
{
    "hemocomponenteIds": [501, 502, 503, 610]
}
```
#### `200` - HttpStatus.OK
**@retorna** `Requisicao` com `status = ALOCADA` e `hemocomponenteIds` preenchido
#### `400` - HttpStatus.BAD_REQUEST
lista vazia, ou a seleção não fecha a `quantidade` de algum item
#### `404` - HttpStatus.NOT_FOUND
requisição ou algum hemocomponente inexistente
#### `409` - HttpStatus.CONFLICT
`status != ACEITA`; hemocomponente de outra instituição, já `RESERVADO`, vencido ou incompatível com o item

> Sugestão FEFO: `GET /instituicoes/{id}/estoque?tipo&abo&rh&status=DISPONIVEL` já sai ordenado por `dataValidade` crescente. A seleção é explícita — o serviço valida, não escolhe sozinho.

---

### **PATCH** `/requisicoes/{id}/enviar`
Despacha o lote: `status = EM_TRANSITO` (o "Em transporte" do HU04).

**@caller** `MEMBRO` do `HEMOCENTRO` dono
#### `200` - HttpStatus.OK
**@retorna** `Requisicao` com `status = EM_TRANSITO`
#### `404` - HttpStatus.NOT_FOUND
#### `409` - HttpStatus.CONFLICT
`status != ALOCADA`

---

### **PATCH** `/requisicoes/{id}/receber`
Hospital confirma o recebimento: `status = ATENDIDA` e os hemocomponentes passam para o estoque do hospital (`instituicaoId` = hospital), mantendo `RESERVADO` até serem transfundidos.

**@caller** `MEMBRO` do `HOSPITAL` dono
#### `200` - HttpStatus.OK
**@retorna** `Requisicao` com `status = ATENDIDA`; grava uma linha de histórico por hemocomponente
#### `404` - HttpStatus.NOT_FOUND
#### `409` - HttpStatus.CONFLICT
`status != EM_TRANSITO`

---

### **PATCH** `/requisicoes/{id}/cancelar`
Cancela a requisição: `status = CANCELADA`. Hemocomponentes já alocados voltam para `DISPONIVEL`.

**@caller** `MEMBRO` do `HOSPITAL` dono
#### `200` - HttpStatus.OK
**@retorna** `Requisicao` com `status = CANCELADA`
#### `404` - HttpStatus.NOT_FOUND
#### `409` - HttpStatus.CONFLICT
já `EM_TRANSITO`, `ATENDIDA`, `RECUSADA` ou `CANCELADA`

---

### **DELETE** `/requisicoes/{id}`
Remove a requisição do sistema. Não é caminho de cancelamento.

**@caller** `ADMIN_SISTEMA`
#### `204` - HttpStatus.NO_CONTENT
#### `404` - HttpStatus.NOT_FOUND
#### `409` - HttpStatus.CONFLICT
há hemocomponente ainda `RESERVADO` para ela

---

## Resumo

| Recurso | Rotas |
|---|---|
| `/auth` | `POST /login` · `GET /me` |
| `/instituicoes` | `POST` · `GET` · `GET /{id}` · `PATCH /{id}` · `PATCH /{id}/aprovar` · `DELETE /{id}` |
| `/instituicoes/{id}/usuarios` | `GET` · `POST` · `GET /{uid}` · `PATCH /{uid}` · `DELETE /{uid}` |
| `/usuarios` | `PATCH /{id}` |
| `/instituicoes/{id}/estoque` | `GET` · `POST` · `GET /{idHemoc}` · `PATCH /{idHemoc}` · `DELETE /{idHemoc}` |
| `/disponibilidade` | `GET` |
| `/hemocomponentes` | `GET /{id}/historico` |
| `/requisicoes` | `GET` · `POST` · `GET /{id}` · `PATCH /{id}` · `DELETE /{id}` · `PATCH /{id}/aceitar` · `PATCH /{id}/recusar` · `PATCH /{id}/alocar` · `PATCH /{id}/enviar` · `PATCH /{id}/receber` · `PATCH /{id}/cancelar` |

**31 rotas.** Delete físico só para `ADMIN_SISTEMA`, e sempre com `409` quando quebraria rastreabilidade — o histórico de hemocomponente é append-only e nunca é apagado.

### Cobertura das HUs

| HU | Coberta por |
|---|---|
| HU01 | `POST /instituicoes`, `POST /auth/login`, `GET /auth/me` |
| HU02 | família `/instituicoes/{id}/estoque` |
| HU03 | `POST /requisicoes` |
| HU04 | `GET /requisicoes`, `GET /requisicoes/{id}` |
| HU05 | `GET /requisicoes?status=ABERTA`, `/aceitar`, `/recusar` |
| HU06 | `GET /instituicoes/{id}/estoque` (filtro + FEFO), `/alocar` |
| HU07 | ✗ sem rota — falta `/requisicoes/{id}/rota` (grafo) |
| HU08 | parcial: `/enviar` e `/receber` cobrem o status; falta telemetria (GPS/temperatura simulados) |
| HU09 | ✗ sem rota — falta `/indicadores` |

Nenhuma rota do contrato atual foi retirada para cobrir as três lacunas: elas entram como acréscimo, provavelmente na Entrega 04.
