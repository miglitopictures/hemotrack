import { useQuery } from "@tanstack/react-query";

import { CHAVE_USUARIO_LOGADO, buscarUsuarioLogado } from "./api-auth";

/**
 * Quem está logado, para a interface.
 *
 * Usa a mesma chave que o login preenche em sessao-login.ts — então logo
 * após entrar o dado já está em cache e esta query não dispara requisição.
 * Num F5, ela busca /auth/me de novo (o token sobrevive no localStorage).
 */
export function useUsuarioLogado() {
  return useQuery({
    queryKey: CHAVE_USUARIO_LOGADO,
    queryFn: buscarUsuarioLogado,
    staleTime: 5 * 60 * 1000,
    retry: false,
  });
}