/**
 * Cliente HTTP fino para o back real (Spring Boot).
 *
 * Por quê isso existe separado do `api.ts`: `api.ts` fala a língua do FRONT
 * (Solicitacao, Bolsa, Transporte — os tipos de `data.ts`). Este arquivo fala
 * a língua do BACK (o que o Spring realmente devolve hoje). Quem traduz entre
 * as duas línguas são os arquivos em `lib/adapters/`.
 *
 * Toda requisição sai daqui com o token da sessão, quando houver — é o único
 * ponto do front que monta o header Authorization.
 */

import { apagarToken, lerToken } from "./sessao";

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

  // O back usa application/problem+json (RFC 9457) para erros — ver
  // "Erros" em contrato-api.md.
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

function montarCabecalhos(temCorpo: boolean, token: string | null): Record<string, string> {
  const cabecalhos: Record<string, string> = {};

  if (temCorpo) {
    cabecalhos["Content-Type"] = "application/json";
  }

  if (token !== null) {
    cabecalhos["Authorization"] = `Bearer ${token}`;
  }

  return cabecalhos;
}

async function requisitar<T>(metodo: string, caminho: string, corpo?: unknown): Promise<T> {
  const token = lerToken();
  const temCorpo = corpo !== undefined;

  // Se não tem corpo, a propriedade "body" nem entra no objeto (em vez de
  // entrar como "body: undefined"). O tsconfig usa exactOptionalPropertyTypes,
  // que trata essas duas coisas como diferentes.
  const opcoes: RequestInit = {
    method: metodo,
    headers: montarCabecalhos(temCorpo, token),
    ...(temCorpo ? { body: JSON.stringify(corpo) } : {}),
  };

  const resposta = await fetch(`${BASE_URL}${caminho}`, opcoes);

  // 401 COM token enviado = a sessão morreu (expirou, ou o usuário foi
  // desativado). Derruba e manda pro login.
  //
  // 401 SEM token é outra coisa: é o /auth/login recusando a senha. Esse
  // segue como erro normal, para a tela exibir a mensagem.
  if (resposta.status === 401 && token !== null) {
    apagarToken();
    window.location.assign("/login?sessao=expirada");
    throw new ErroDeApiHttp("Sua sessão expirou. Entre novamente.", 401);
  }

  return tratarResposta<T>(resposta);
}

export function httpGet<T>(caminho: string): Promise<T> {
  return requisitar<T>("GET", caminho);
}

export function httpPost<T>(caminho: string, corpo?: unknown): Promise<T> {
  return requisitar<T>("POST", caminho, corpo);
}

export function httpPut<T>(caminho: string, corpo: unknown): Promise<T> {
  return requisitar<T>("PUT", caminho, corpo);
}

export async function httpDelete(caminho: string): Promise<void> {
  await requisitar<void>("DELETE", caminho);
}