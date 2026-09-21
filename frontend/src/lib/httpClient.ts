/**
 * Cliente HTTP fino para o back real (Spring Boot).
 *
 * Por quê isso existe separado do `api.ts`: `api.ts` fala a língua do FRONT
 * (Solicitacao, Bolsa, Transporte — os tipos de `data.ts`). Este arquivo fala
 * a língua do BACK (o que o Spring realmente devolve hoje). Quem traduz entre
 * as duas línguas são os arquivos em `lib/adapters/`.
 *
 * Hoje o back não tem `/api/v1` no prefixo (isso está só no documento
 * `contrato-api.md`, ainda não implementado) e não tem autenticação — por
 * isso não há header de Authorization aqui ainda. Quando o Miguel subir
 * `/auth/login`, é só acrescentar o token aqui, num único lugar.
 */

// Acesso com ['...'] porque o tsconfig deste projeto usa uma configuração
// (noPropertyAccessFromIndexSignature) que exige essa forma para variáveis
// de ambiente do Vite.
const BASE_URL = import.meta.env["VITE_API_URL"] ?? "http://localhost:8080";

export class ErroDeApiHttp extends Error {
  readonly status: number;
  constructor(mensagem: string, status: number) {
    super(mensagem);
    this.name = "ErroDeApiHttp";
    this.status = status;
  }
}

async function tratarResposta<T>(resp: Response): Promise<T> {
  if (resp.status === 204) return undefined as T;

  // O back usa application/problem+json (RFC 7807) para erros — ver
  // "Erros" em contrato-api.md. Tentamos ler a mensagem de lá; se não vier
  // nesse formato (ainda comum, já que grande parte do back é WIP), caímos
  // num texto genérico.
  if (!resp.ok) {
    let mensagem = `Erro ${resp.status} ao chamar ${resp.url}`;
    try {
      const corpo = await resp.json();
      mensagem = corpo.detail ?? corpo.title ?? mensagem;
    } catch {
      // corpo não era JSON — mantém a mensagem genérica.
    }
    throw new ErroDeApiHttp(mensagem, resp.status);
  }

  // Algumas rotas (ex.: DELETE) não devolvem corpo.
  const texto = await resp.text();
  return (texto ? JSON.parse(texto) : undefined) as T;
}

export async function httpGet<T>(caminho: string): Promise<T> {
  const resp = await fetch(`${BASE_URL}${caminho}`);
  return tratarResposta<T>(resp);
}

export async function httpPost<T>(caminho: string, corpo?: unknown): Promise<T> {
  // Se não tem corpo, a propriedade "body" nem entra no objeto (em vez de
  // entrar como "body: undefined"). O tsconfig deste projeto usa
  // exactOptionalPropertyTypes, que trata essas duas coisas como diferentes.
  const opcoes: RequestInit = {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    ...(corpo !== undefined ? { body: JSON.stringify(corpo) } : {}),
  };
  const resp = await fetch(`${BASE_URL}${caminho}`, opcoes);
  return tratarResposta<T>(resp);
}

export async function httpPut<T>(caminho: string, corpo: unknown): Promise<T> {
  const resp = await fetch(`${BASE_URL}${caminho}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(corpo),
  });
  return tratarResposta<T>(resp);
}

export async function httpDelete(caminho: string): Promise<void> {
  const resp = await fetch(`${BASE_URL}${caminho}`, { method: "DELETE" });
  await tratarResposta<void>(resp);
}