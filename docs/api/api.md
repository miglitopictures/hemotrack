# HemoTrack — API REST

Referência da API **que existe e roda hoje**. Se algo aqui divergir dos outros documentos, este vence.

| Documento | Para quê |
|---|---|
| `api.md` (este) | as rotas que existem hoje |
| [`dominio.md`](./dominio.md) | entidades, campos e enums |
| [`api-alvo.md`](./api-alvo.md) | as rotas planejadas, por HU |
| [`decisoes.md`](./decisoes.md) | decisões de modelagem em aberto |
| [`openapi.yml`](./openapi.yml) | o contrato alvo em OpenAPI 3.1 |
| [`requisicoes.http`](./requisicoes.http) | roteiro de teste executável |

**Branch:** `rt-api-teste` · **Base URL:** `http://localhost:8080`
Para subir o backend e testar, ver [`como-rodar.md`](../como-rodar.md).

---

## O que existe

| Recurso | Estado |
|---|---|
| `/requisicoes` | ✓ REST — CRUD + `aceitar` / `recusar` |
| `/usuarios` | ⚠ MVC Thymeleaf — devolve HTML, não JSON |
| `/instituicoes` | ✗ entidade modelada, sem controller |
| `/bolsas` | ✗ entidade modelada, sem controller |
| `/hemocomponentes` | ✗ entidade modelada, sem controller |
| `/login` | ✗ **nenhuma rota é protegida** |

---

## Camadas e erros

```
RequisicaoController  →  RequisicaoService  →  RequisicaoRepository  →  H2
   (@RestController)      (regras de status)      (JpaRepository)
```

O service lança `ResponseStatusException` direto. Com `spring.mvc.problemdetails.enabled=true`, todo erro sai em RFC 7807:

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

## `/requisicoes`

Entrada e saída são a mesma entidade — não há DTO de request, exceto na recusa.

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

| Campo | Tipo | Obrigatório no POST | Nota |
|---|---|---|---|
| `id` | `Long` | não | gerado; hoje é aceito no body |
| `dataCriacao` | `Instant` ISO-8601 UTC | não | automático, imutável |
| `hospitalId` | `Long` | sim | vem do body — não há token |
| `tipo` | `TipoHemocomponente` | sim | |
| `abo` | `TipoABO` | sim | |
| `rh` | `FatorRh` | sim | |
| `volumeMl` | `double` | sim | primitivo: default `0.0`, sem mínimo |
| `prioridade` | `Prioridade` | sim | |
| `status` | `StatusRequisicao` | não | default `ABERTA`; hoje é aceito no body |
| `observacoes` | `String` | não | |
| `motivoRecusa` | `String` | não | só o `POST /recusar` preenche |

Os valores aceitos de cada enum estão em [`dominio.md`](./dominio.md#enums). Qualquer outro devolve `400`.

### Rotas

| Método | Rota | Body | Sucesso | Erros |
|---|---|---|---|---|
| `GET` | `/requisicoes` | — | `200` + array | — |
| `POST` | `/requisicoes` | `Requisicao` | `200` + objeto | `400` JSON/enum inválido; `500` campo `@NotNull` ausente |
| `GET` | `/requisicoes/{id}` | — | `200` + objeto | `404` |
| `PUT` | `/requisicoes/{id}` | `Requisicao` | `200` + objeto | `404` |
| `POST` | `/requisicoes/{id}/aceitar` | — | `200` + `status: ACEITA` | `404`; `409` fora de `ABERTA` |
| `POST` | `/requisicoes/{id}/recusar` | `{ "motivoRecusa": "..." }` | `200` + `status: RECUSADA` | `400` motivo vazio; `404`; `409` fora de `ABERTA` |
| `DELETE` | `/requisicoes/{id}` | — | `200` corpo vazio | `404` |

### O que a tabela não mostra

- `POST` devolve `200`, não `201`, e sem header `Location`.
- `PUT` só copia `prioridade`. O resto do body é ignorado — na prática é um `PATCH` de um campo.
- `DELETE` é hard delete e devolve `200` vazio, não `204`.
- `aceitar` e `recusar` só saem de `ABERTA`. Chamar duas vezes → `409` na segunda.
- `recusar` valida o motivo antes de gravar: `null`, `""` ou espaços → `400` e nada muda.
- Transições implementadas: `ABERTA → ACEITA` e `ABERTA → RECUSADA`. Só essas. `ALOCADA`, `ATENDIDA` e `CANCELADA` existem no enum sem rota que chegue nelas.

---

## `/usuarios` — não é REST

`UsuarioController` é `@Controller`, serve páginas Thymeleaf. Não devolve JSON: use o navegador.

| Método | Rota | Retorno |
|---|---|---|
| `GET` | `/usuarios` | HTML — lista |
| `GET` | `/usuarios/novo` | HTML — formulário vazio |
| `GET` | `/usuarios/editar/{id}` | HTML — formulário preenchido |
| `POST` | `/usuarios/salvar` | `form-urlencoded` → `302` |
| `GET` | `/usuarios/remover/{id}` | `302` — delete via GET |

O que falta para virar REST está em [`api-alvo.md`](./api-alvo.md).
