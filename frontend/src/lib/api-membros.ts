/**
 * Chamadas de gestão de membros da instituição.
 *
 * Os tipos de entrada espelham os DTOs do backend. A resposta das três
 * rotas é o mesmo `UsuarioDoBack` que o cadastro já devolve — é o
 * UsuarioResponse do back, e a senha nunca vem nele.
 */

import type { UsuarioDoBack } from "./api-auth";
import { httpGet, httpPatch, httpPost } from "./httpClient";

/** Corpo do POST /instituicoes/{id}/usuarios. O papel não vai na entrada:
 *  quem é criado por essa rota nasce sempre OPERADOR. */
export type OperadorRequest = {
  nome: string;
  email: string;
  senha: string;
};

/** Chave do React Query. Leva o id da instituição para que o cache de uma
 *  não vaze para outra no mesmo navegador. */
export function chaveMembros(instituicaoId: number) {
  return ["membros", instituicaoId] as const;
}

export function listarMembros(instituicaoId: number): Promise<UsuarioDoBack[]> {
  return httpGet<UsuarioDoBack[]>(`/instituicoes/${instituicaoId}/usuarios`);
}

export function cadastrarOperador(
  instituicaoId: number,
  dados: OperadorRequest,
): Promise<UsuarioDoBack> {
  return httpPost<UsuarioDoBack>(`/instituicoes/${instituicaoId}/usuarios`, dados);
}

export function alterarAtivoMembro(
  instituicaoId: number,
  usuarioId: number,
  ativo: boolean,
): Promise<UsuarioDoBack> {
  return httpPatch<UsuarioDoBack>(`/instituicoes/${instituicaoId}/usuarios/${usuarioId}`, {
    ativo,
  });
}
