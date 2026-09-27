/**
 * A sequência "entrar" completa, usada tanto pela tela de login quanto pelo
 * cadastro (que loga automaticamente no fim).
 *
 * Fica separada porque as duas telas precisam do MESMO comportamento: se o
 * destino mudar, muda num lugar só.
 */

import type { QueryClient } from "@tanstack/react-query";

import {
  CHAVE_USUARIO_LOGADO,
  buscarUsuarioLogado,
  login,
  type UsuarioLogado,
} from "./api-auth";
import { apagarToken, guardarToken } from "./sessao";

export async function autenticarEGuardarSessao(
  queryClient: QueryClient,
  email: string,
  senha: string,
): Promise<UsuarioLogado> {
  const resposta = await login(email, senha);
  guardarToken(resposta.token);

  // Já sai autenticada: o httpClient anexa o token que acabamos de guardar.
  const usuario = await buscarUsuarioLogado();

  // Adianta para o cache do React Query — o app-shell (passo 7) lê a mesma
  // chave e não precisa buscar de novo.
  queryClient.setQueryData(CHAVE_USUARIO_LOGADO, usuario);

  return usuario;
}

export function destinoDoUsuario(usuario: UsuarioLogado): "/hospital" | "/hemocentro" {
  return usuario.instituicao?.tipo === "HEMOCENTRO" ? "/hemocentro" : "/hospital";
}

export function encerrarSessao(queryClient: QueryClient): void {
  apagarToken();

  // Limpa TODO o cache, não só a chave do usuário: estoque, solicitações e
  // transportes em memória são da instituição anterior e não podem vazar
  // para quem entrar em seguida no mesmo navegador.
  queryClient.clear();
}